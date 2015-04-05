package com.gmail.mooman219.janios;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class Attacker {

    private final static String address = "localhost";
    private final static int port = 8081;

    public static void main(String[] args) throws InterruptedException {
        generateMalformedGETRequest();
        //generateLotsOfConnections();
    }

    public static void generateMalformedGETRequest() {
        try {
            Socket socket = new Socket(address, port);
            socket.getOutputStream().write(
                    ("GTET / HTTP/1.1\n"
                    + "Host: localhost:8081\n"
                    + "Connection: keep-alive\n"
                    + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\n"
                    + "User-Agent: Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36\n"
                    + "DNT: 1\n"
                    + "Accept-Encoding: gzip, deflate, sdch\n"
                    + "Accept-Language: en-US,en;q=0.8\r\n\r\n")
                    .getBytes());
            socket.close();
            System.out.println("Generated request.");
        } catch (IOException e) {
            System.err.println("Could not connect: " + e);
        }
    }

    public static void generateLotsOfConnections() {
        for (int i = 0; i < 3000; i++) {
            try {
                new Socket(address, port);
                System.out.println(i);
            } catch (IOException e) {
                System.err.println("Could not connect: " + e);
            }
        }
    }
}
