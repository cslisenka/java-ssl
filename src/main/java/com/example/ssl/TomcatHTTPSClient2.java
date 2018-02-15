package com.example.ssl;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;

public class TomcatHTTPSClient2 {
    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.ssl.trustStore",
                "/home/kslisenko/work/ssl/ssltest/src/test/resources/client.jks");
        System.setProperty("javax.net.ssl.trustStoreType", "jks");

        URL url = new URL("https://localhost:8443");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        System.out.println(connection.getResponseCode());

        url = new URL("https://tut.by");
        connection = (HttpsURLConnection) url.openConnection();
        System.out.println(connection.getResponseCode());
    }
}