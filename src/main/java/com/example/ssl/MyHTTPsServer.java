package com.example.ssl;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.Security;

public class MyHTTPsServer {

    private final int port;
    private final String host;
    private final SSLContext context;

    public static void main(String[] args) throws Exception {
        new MyHTTPsServer(8888, "localhost",
            "/home/kslisenko/work/ssl/ssltest/src/test/resources/server.jks",
                "storepass", "keypass")
            .run();
    }

    public MyHTTPsServer(int port, String host,
             String keyStorePath, String keyStorePass, String keyPass) throws Exception {
        this.port = port;
        this.host = host;
        this.context = createSSLContext(keyStorePath, keyStorePass, keyPass);
    }

    public void run() throws IOException {
        SSLServerSocketFactory sslServerSocketFactory = context.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory
                .createServerSocket(port);

        System.out.println("SSL server started");

        while (true) {
            SSLSocket socket = (SSLSocket) serverSocket.accept();
            new Thread(() -> {
                try {
                    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
                    socket.startHandshake();

                    SSLSession session = socket.getSession();
                    System.out.println("SSL session, protocol: " + session.getProtocol() +
                            " cipter suite " + session.getCipherSuite());

                    // Start handling application content
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    writer.print("HTTP/1.1 200\r\n");
                    writer.flush();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private SSLContext createSSLContext(String keyStorePath, String keyStorePass, String keyPass) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStorePath), keyStorePass.toCharArray());

        if (keyStore.size() == 0) {
            throw new IllegalArgumentException("Keystore doesn't have any certificate");
        }

        // Create key managers
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, keyPass.toCharArray());
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        // Create trust managers
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext;
    }
}
