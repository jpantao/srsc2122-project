package sigserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sapkdp.ServerSAPKDP;
import common.Utils;


import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;


public class SignalingServer {

    public static final String CONFIG_FILE = "config/sigserver.properties";
    public static final String KEYSTORE_FILE = "keystores/signalingserver.keystore";
    public static final char[] KEYSTORE_PASS = "srsc2122".toCharArray();

    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);
    private static JsonObject users, movies;

    private static  Properties properties;
    private static ServerSAPKDP serverSAPKDP;

    static {
        try {
            Utils.loadBC();
            users = JsonParser.parseReader(new FileReader("resources/users.json")).getAsJsonObject();
            movies = JsonParser.parseReader(new FileReader("resources/movies.json")).getAsJsonObject();
            Utils.loadBC();
            properties = Utils.loadConfig(CONFIG_FILE);
            serverSAPKDP = ServerSAPKDP.getInstance();
            serverSAPKDP.load(KEYSTORE_FILE, KEYSTORE_PASS, users, movies);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Integer.parseInt(properties.getProperty("port"));

        try (ServerSocket srv = new ServerSocket(port)) {
            // server is listening on port 1234
            srv.setReuseAddress(true);

            while (true) {
                // listen for connections
                Socket client = srv.accept();
                System.out.println("New client connected: " + client.getInetAddress().getHostAddress());

                // launch handler
                new Thread(() -> serverSAPKDP.handleHandshake(client)).start();
                break; // only handle 1 for now
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
