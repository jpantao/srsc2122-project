package sigserver;

import messages.MessageSAPKDP;
import messages.PBHello;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class SignalingServer {

    private static int PORT;

    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);


    static {
        try {
            InputStream inputStream = new FileInputStream("resources/sigserver.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            PORT = Integer.parseInt(properties.getProperty("port"));

            JsonObject users = JsonParser.parseReader(new FileReader("resources/users.json")).getAsJsonObject();
            JsonObject movies = JsonParser.parseReader(new FileReader("resources/movies.json")).getAsJsonObject();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try {
            System.out.println("Handler " + socket.getInetAddress().getHostAddress());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            //(round 1)  receive PB-Hello
            MessageSAPKDP msg = (MessageSAPKDP) in.readObject();
            if (msg.getMsgType() != PBHello.MSG_TYPE) {
                //TODO: handle error
                socket.close();
                return;
            }
            PBHello pbHello = (PBHello) msg;
            // TODO: verify pbHello

            //TODO: (round 2) send SS-AuthenticationRequest




            //TODO: (round 4) SS-PaymentRequest
            //TODO: (round 6) SS-TicketCredentials


            Thread.sleep(1000);
        } catch (InterruptedException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        try (ServerSocket srv = new ServerSocket(PORT)) {
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
