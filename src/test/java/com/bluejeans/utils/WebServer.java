/*
 * Copyright Blue Jeans Network.
 */
package com.bluejeans.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Small web server
 *
 * @author Dinesh Ilindra
 */
public class WebServer {

    /**
     * WebServer constructor.
     */
    public void start(final int port) {
        ServerSocket s;
        System.out.println("Webserver starting up on port " + port);
        try {
            // create the main server socket
            s = new ServerSocket(port);
        } catch (final Exception e) {
            System.out.println("Error: " + e);
            return;
        }
        System.out.println("Waiting for connection");
        for (;;) {
            try {
                // wait for a connection
                final Socket remote = s.accept();
                // remote is now the connected socket
                System.out.println("Connection, sending data.");
                final BufferedReader in = new BufferedReader(new InputStreamReader(remote.getInputStream()));
                final PrintWriter out = new PrintWriter(remote.getOutputStream());

                // read the data sent. We basically ignore it,
                // stop reading once a blank line is hit. This
                // blank line signals the end of the client HTTP
                // headers.
                String str = ".";
                while (!str.equals("")) {
                    str = in.readLine();
                }
                str = in.readLine();
                // Send the response
                // Send the headers
                out.println("HTTP/1.0 200 OK");
                out.println("Content-Type: text/html");
                out.println("Server: Bot");
                // this blank line signals the end of the headers
                out.println("");
                // Send the HTML page
                out.println("<H1>Welcome to the Ultra Mini-WebServer</H2>");
                out.flush();
                remote.close();
                if (str.equals("DIE")) {
                    break;
                }
            } catch (final Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }

    /**
     * Start the application.
     *
     * @param args
     *            Command line parameters are not used.
     */
    public static void main(final String args[]) {
        final WebServer ws = new WebServer();
        ws.start(80);
    }
}
