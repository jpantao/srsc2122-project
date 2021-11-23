package sigserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sigserver.sapkdp.Header;
import sigserver.sapkdp.ProtoSAPKDP;
import sigserver.sapkdp.messages.MessageSAPKDP;
import sigserver.sapkdp.messages.PBHello;
import sigserver.sapkdp.messages.SSAuthenticationRequest;


public class SignalingServer {

    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);
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
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            Header header;
            byte[] headerBytes;
            byte[] payload;


            // (round 1) recv PB-Hello
            headerBytes = new byte[Header.BYTE_LEN];
            in.read(headerBytes);
            header = new Header(headerBytes);

            if (header.getVersion() != ProtoSAPKDP.VERSION || header.getMsgType() != MessageSAPKDP.Type.PB_HELLO.msgType) {
                //TODO: handle error
                socket.close();
                return;
            }

            payload = new byte[header.getPayloadSize()];
            in.read(payload);
            PBHello pbHello = (PBHello) MessageSAPKDP.deserialize(header.getMsgType(), payload);
            System.out.println("PB_Hello received from " + pbHello.getUserID() + ":" + pbHello.getProxyID());


            //TODO: (round 2) send SS-AuthenticationRequest
            int n1 = NONCE_COUNTER.getAndIncrement();
            byte[] salt = new byte[20];
            ThreadLocalRandom.current().nextBytes(salt);
            int counter = ThreadLocalRandom.current().nextInt(1024, 2048);

            payload = MessageSAPKDP.serialize(new SSAuthenticationRequest(n1, salt, counter));
            header = new Header(ProtoSAPKDP.VERSION, MessageSAPKDP.Type.SS_AUTHREQ.msgType, (short) payload.length);
            out.write(header.encode());
            out.write(payload);

            //TODO: (round 3) recv PB-Authentication
            //TODO: (round 4) send SS-PaymentRequest
            //TODO: (round 5) recv PB-Payment
            //TODO: (round 6) send PB-Payment SS-TicketCredentials


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        if (args.length > 2) {
            System.out.println("usage: SignalingServer <port>");
            System.exit(-1);
        }

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8888;

        try (ServerSocket srv = new ServerSocket(port)) {
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
