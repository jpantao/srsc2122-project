package sigserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sigserver.sapkdp.Header;
import sigserver.sapkdp.ProtoSAPKDP;
import sigserver.sapkdp.messages.*;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static sigserver.sapkdp.ProtoSAPKDP.VERSION;
import static sigserver.sapkdp.ProtoSAPKDP.genPBECipher;


public class SignalingServer {


    public static final String KEYSTORE = "resources/this.keystore";
    public static final String PROXYBOX_KEYALIAS = "proxybox";
    public static final String SIGSERVER_KEYALIAS = "signalingserver";
    public static final char[] KEYSTORE_PASS = "srsc2122".toCharArray();

    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);
    private static JsonObject users, movies;

    private static Properties properties;
    private static PrivateKey prv;
    private static PublicKey pubProxyBox;


    static {
        try {
            users = JsonParser.parseReader(new FileReader("resources/users.json")).getAsJsonObject();
            movies = JsonParser.parseReader(new FileReader("resources/movies.json")).getAsJsonObject();

            Provider provider = Security.getProvider("BC");
            if (provider == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            properties = new Properties();
            properties.load(new FileInputStream(ProtoSAPKDP.CONFIG));

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(KEYSTORE), KEYSTORE_PASS);

            //TODO: use truststores instead of having all the keypairs in on keystores
            prv = (PrivateKey) ks.getKey(PROXYBOX_KEYALIAS, KEYSTORE_PASS);
            pubProxyBox = ks.getCertificate(SIGSERVER_KEYALIAS).getPublicKey();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try {
            System.out.println("Handler " + socket.getInetAddress().getHostAddress());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            Mac hmac = Mac.getInstance(properties.getProperty("HMACsuite"));
            hmac.init(new SecretKeySpec(Utils.decodeHexString(
                    properties.getProperty("hmackey")), properties.getProperty("HMACsuite")));

            int msgType;
            Header header;
            byte[] headerBytes;
            byte[] payload;


            // (round 1) recv PB-Hello
            msgType = MessageSAPKDP.Type.PB_HELLO.msgType;
            headerBytes = new byte[Header.BYTE_LEN];
            in.read(headerBytes);
            header = new Header(headerBytes);
            if (header.getVersion() != ProtoSAPKDP.VERSION || header.getMsgType() != msgType) {
                //TODO: handle error
                socket.close();
                return;
            }

            payload = new byte[header.getPayloadSize()];
            in.read(payload);
            PBHello pbHello = (PBHello) MessageSAPKDP.deserialize(header.getMsgType(), payload);
            System.out.println("Recv " + pbHello);


            // (round 2) send SS-AuthenticationRequest
            msgType = MessageSAPKDP.Type.SS_AUTHREQ.msgType;
            int n1 = NONCE_COUNTER.getAndIncrement();
            byte[] salt = new byte[20];
            ThreadLocalRandom.current().nextBytes(salt);
            int counter = ThreadLocalRandom.current().nextInt(1024, 2048);

            MessageSAPKDP authenticationRequest = new SSAuthenticationRequest(n1, salt, counter);
            payload = MessageSAPKDP.serialize(authenticationRequest);
            header = new Header(ProtoSAPKDP.VERSION, msgType, (short) payload.length);
            out.write(header.encode());
            out.write(payload);
            System.out.println("Sent " + authenticationRequest);

            // (round 3) recv PB-Authentication
            msgType = MessageSAPKDP.Type.PB_AUTH.msgType;
            String pw = users.get(pbHello.getUserID()).getAsJsonObject().get("password").getAsString();

            Cipher pbeCipher = genPBECipher(Cipher.DECRYPT_MODE, pw, properties.getProperty("PBEsuite"), properties.getProperty("provider"), salt, counter);
            headerBytes = new byte[Header.BYTE_LEN];
            in.read(headerBytes);
            header = new Header(headerBytes);

            payload = new byte[header.getPayloadSize()];
            in.read(payload);
            hmac.update(payload);

            byte[] messageHash = new byte[hmac.getMacLength()];
            in.read(messageHash);

            if (!MessageDigest.isEqual(messageHash, hmac.doFinal())) {
                //TODO: handle error
                socket.close();
                return;
            }

            payload = pbeCipher.doFinal(payload);
            PBAuthentication pbAuthentication = (PBAuthentication) MessageSAPKDP.deserialize(msgType, payload);

            if (pbAuthentication.getNonce1() != n1 + 1) {
                //TODO: handle error
                socket.close();
                return;
            }

            System.out.println("Recv " + pbAuthentication);

            //TODO: (round 4) send SS-PaymentRequest
            msgType = MessageSAPKDP.Type.SS_PAYREQ.msgType;
            float price = movies.get(pbAuthentication.getMovieID()).getAsJsonObject().get("ppvprice").getAsFloat();
            int n3 = pbAuthentication.getNonce2();
            SSPaymentRequest paymentRequest = new SSPaymentRequest(price, n3 + 1, NONCE_COUNTER.getAndIncrement());

            payload = MessageSAPKDP.serialize(paymentRequest);
            Signature signature = Signature.getInstance(properties.getProperty("ECDSAsuite"), properties.getProperty("provider"));
            signature.initSign(prv, new SecureRandom());
            signature.update(payload);
            byte[] sigBytes = signature.sign();

            header = new Header(VERSION, msgType, (short) payload.length);

            hmac.update(payload);
            byte[] intCheck = hmac.doFinal();

            out.write(header.encode());
            out.write(payload);
            out.writeInt(sigBytes.length);
            out.write(sigBytes);
            out.write(intCheck);

            System.out.println("Sent " + paymentRequest);


            //TODO: (round 5) recv PB-Payment
            //TODO: (round 6) send SS-TicketCredentials


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
