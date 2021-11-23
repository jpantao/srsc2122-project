package sigserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class SignalingServer {

    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);
    private static final SecureRandom RND = new SecureRandom();
    private static JsonObject users, movies;


    static {
        try {
            users = JsonParser.parseReader(new FileReader("resources/users.json")).getAsJsonObject();
            movies = JsonParser.parseReader(new FileReader("resources/movies.json")).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try {
            System.out.println("Handler " + socket.getInetAddress().getHostAddress());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());



            //TODO: (round 1) recv PB-Hello




            //TODO: (round 2) send SS-AuthenticationRequest
            //TODO: (round 3) recv PB-Authentication
            //TODO: (round 4) send SS-PaymentRequest
            //TODO: (round 5) recv PB-Payment
            //TODO: (round 6) send PB-Payment SS-TicketCredentials


//            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: SignalingServer <port>");
            System.exit(-1);
        }

        try (ServerSocket srv = new ServerSocket(Integer.parseInt(args[0]))) {
            // server is listening on port 1234
            srv.setReuseAddress(true);

            while (true) {
                // listen for connections
                Socket client = srv.accept();
                System.out.println("New client connected: " + client.getInetAddress().getHostAddress());

                // launch handler
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
