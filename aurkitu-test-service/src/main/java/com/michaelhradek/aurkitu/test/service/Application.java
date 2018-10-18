package com.michaelhradek.aurkitu.test.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class Application {

    public static void main(String args[]) {

        if (args == null || args.length != 1) {
            System.err.println("Usage: java Application <port number>");
            System.exit(1);
        }

        final int portNumber = Integer.parseInt(args[0]);

        try {
            // Establish sockets
            ServerSocket serverSocket = new ServerSocket(portNumber);
            Socket clientSocket = serverSocket.accept();

            // Establish streams
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            int inputLine, outputLine;

            // Receive a request from the client

            // Respond to the client

        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}