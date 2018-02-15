package com.example.ssl;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;

public class MyHTTPSClient {

    public static void main(String[] args) throws Exception {
        SSLContext sslContext = createSSLContext("/home/kslisenko/work/ssl/ssltest/src/test/resources/server.jks",
                "storepass", "keypass");

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket("localhost", 8888);

        System.out.println("SSL client connected");

        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

        socket.startHandshake();

        SSLSession session = socket.getSession();
        System.out.println("SSL session, protocol: " + session.getProtocol() +
                " cipter suite " + session.getCipherSuite());

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println(reader.readLine());
        socket.close();
    }

    private static SSLContext createSSLContext(String keyStorePath, String keyStorePass, String keyPass) throws Exception {
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