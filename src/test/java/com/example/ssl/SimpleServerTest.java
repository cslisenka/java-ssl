package com.example.ssl;

import com.sun.net.httpserver.*;
import org.junit.Test;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimpleServerTest {

    @Test
    public void testHTTPServer() throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 5);
        server.createContext("/test", new RequestHandler());
        server.start();

        System.out.println("Started server at http://localhost:8000/test");

        Thread.sleep(10_000_000);
    }

    @Test
    public void testHTTPsServer() throws Exception {
        startHTTPsServer();
    }

    @Test
    public void testHTTPsServer_oneWayAuth() throws Exception {
        Thread t = startHTTPsServer();

        URL url = new URL("https://localhost:8443/test");
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        SSLContext context = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        context.init(null, trustManagers, null);
        con.setSSLSocketFactory(context.getSocketFactory());
//        con.setHostnameVerifier(new HostnameVerifier() {
//            @Override
//            public boolean verify(String s, SSLSession sslSession) {
//                return false;
//            }
//        });

        int responseCode = con.getResponseCode();

        System.out.println(responseCode);

        t.join();
    }

    public Thread startHTTPsServer() throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress("0.0.0.0", 8443), 5);
        server.createContext("/test", new RequestHandler());

        char[] keypass = "keypass".toCharArray();
        char[] storepass = "storepass".toCharArray();

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("src/test/resources/server.jks"), storepass);

        System.out.println(ks);
        System.out.println(ks.getProvider());
        System.out.println(ks.getType());
        System.out.println(ks.size());

        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            System.out.println(ks.getCertificate(alias));
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keypass);

        // Create SSL context object using key manager and trust manager
        // As we trust every client, we don't need trust manager here
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), new TrustManager[]{}, new SecureRandom());

        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters httpsParameters) {
                // Could be different SSL parameters depending on client IP address
//                InetSocketAddress remote = httpsParameters.getClientAddress();
                System.out.println("configure");
                try {
                    SSLContext context = getSSLContext();
                    httpsParameters.setSSLParameters(context.getDefaultSSLParameters());
                } catch (Exception e) {
                    System.out.println("Failed to create HTTPS server");
                    e.printStackTrace();
                }
            }
        });

        Thread t = new Thread(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(8);
            server.setExecutor(executor);
            server.start();
            System.out.println("Started server at https://localhost:8443");
            try {
                executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.start();
        return t;
    }

    static class RequestHandler implements HttpHandler {

        public void handle(HttpExchange httpExchange) throws IOException {
            OutputStream os = null;
            try {
                String response = "This is the response";
                httpExchange.sendResponseHeaders(200, response.length());
                os = httpExchange.getResponseBody();
                os.write(response.getBytes());

                System.out.println("sending response");
            } catch (IOException e) {
                throw e;
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
    }
}