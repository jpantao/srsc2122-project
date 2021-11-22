package sigserver;

import messages.MessageSAPKDP;
import messages.PBHello;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import messages.SSAuthenticationRequest;


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

            MessageSAPKDP msg;

            //(round 1)  receive PB-Hello
            msg = (MessageSAPKDP) in.readObject();
            if (msg.getMsgType() != PBHello.MSG_TYPE) {
                //TODO: handle error
                socket.close();
                return;
            }
            PBHello pbHello = (PBHello) msg;
            // TODO: verify pbHello

            // (round 2) send SS-AuthenticationRequest
            byte[] salt = new byte[8];
            RND.nextBytes(salt);
            int n1 = NONCE_COUNTER.getAndIncrement();
            out.writeObject(new SSAuthenticationRequest(n1, salt, 2048));



            //TODO: (round 4) SS-PaymentRequest
            //TODO: (round 6) SS-TicketCredentials


            Thread.sleep(1000);
        } catch (InterruptedException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        if (args.length != 1){
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
