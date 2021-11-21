package sigserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SignalingServer {

    public static void main(String[] args) {

        ServerSocket srv = null;

        try {

            // server is listening on port 1234
            srv = new ServerSocket(1234);
            srv.setReuseAddress(true);

            while (true) {
                // listen for connections
                Socket client = srv.accept();
                System.out.println("New client connected" + client.getInetAddress().getHostAddress());

                // launch handler
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void handleClient(Socket socket) {
        while (true) {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                //TODO: (round 2) SS-AuthenticationRequest
                //TODO: (round 4) SS-PaymentRequest
                //TODO: (round 6) SS-TicketCredentials


                System.out.println("Hello " + socket.getInetAddress().getHostAddress());

                Thread.sleep(1000);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

}
