package sigserver.sapkdp;

import common.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sigserver.SignalingServer;
import sigserver.sapkdp.messages.MessageSAPKDP;
import sigserver.sapkdp.messages.PBAuthentication;
import sigserver.sapkdp.messages.PBHello;
import sigserver.sapkdp.messages.SSAuthenticationRequest;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtoSAPKDP {

    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);

    public static final int VERSION = 2;


    private final String sigserver, proxyBoxID, userID, userPW;
    private final Key pub, prv;

    // props
    private String hmacsuite, pbesuite;
    private Key hmackey;

    public static void logProtoInstance(ProtoSAPKDP p) {
        System.out.println("--- New SAPKDP instance ---");
        System.out.println("sigserver:\t" + p.sigserver);
        System.out.println("proxyBoxID:\t" + p.proxyBoxID);
        System.out.println("proxyBoxID:\t" + p.proxyBoxID);
        System.out.println("userID:\t" + p.userID);
        System.out.println("userPW:\t" + p.userPW);
//        System.out.println("pub key:\t" + Utils.toHex(p.pub.getEncoded()));
//        System.out.println("prv key:\t" + Utils.toHex(p.prv.getEncoded()));
        System.out.println("---------------------------");
    }

    public ProtoSAPKDP(String proxyBoxID, String userID, String userPW, String sigserver, Key pub, Key prv) {
        this.proxyBoxID = proxyBoxID;
        this.userID = userID;
        this.userPW = userPW;
        this.sigserver = sigserver;
        this.pub = pub;
        this.prv = prv;

        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(SignalingServer.CONFIG));
            this.pbesuite = properties.getProperty("pbesuite");
            this.hmacsuite = properties.getProperty("hmacsuite");
            this.hmackey = new SecretKeySpec(Utils.decodeHexString(properties.getProperty("hmackey")), "HmacSHA512");
            logProtoInstance(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Provider provider = Security.getProvider("BC");
        if (provider == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

    }

    public void handshake(String movieID) {
        String[] addr = sigserver.split(":");

        try (Socket socket = new Socket(addr[0], Integer.parseInt(addr[1]))) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            Mac hmac = Mac.getInstance(hmacsuite);
            hmac.init(hmackey);


            int msgType;
            Header header;
            byte[] headerBytes;
            byte[] payload;

            // (round 1) send PB-Hello
            msgType = MessageSAPKDP.Type.PB_HELLO.msgType;
            MessageSAPKDP pbHello = new PBHello(userID, proxyBoxID);
            payload = MessageSAPKDP.serialize(pbHello);
            header = new Header(VERSION, msgType, (short) payload.length);
            out.write(header.encode());
            out.write(payload);
            System.out.println("Sent " + pbHello);

            // (round 2) recv SS-AuthenticationRequest
            msgType = MessageSAPKDP.Type.SS_AUTHREQ.msgType;
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
            SSAuthenticationRequest authReq = (SSAuthenticationRequest) MessageSAPKDP.deserialize(header.getMsgType(), payload);
            System.out.println("Recv " + authReq);


            // (round 3) send PB-Authentication
            msgType = MessageSAPKDP.Type.PB_AUTH.msgType;
            Cipher pbeCipher = genPBECipher(Cipher.ENCRYPT_MODE, userPW, pbesuite, authReq.getSalt(), authReq.getCounter());

            int n1 = authReq.getNonce();
            int n2 = NONCE_COUNTER.getAndIncrement();
            PBAuthentication pbAuthentication = new PBAuthentication(n1 + 1, n2, movieID);

            payload = MessageSAPKDP.serialize(pbAuthentication);
            payload = pbeCipher.doFinal(payload);
            hmac.update(payload);
            byte[] intCheck = hmac.doFinal();
            header = new Header(VERSION, msgType, (short) payload.length);

            out.write(header.encode());
            out.write(payload);
            out.writeInt(intCheck.length);
            out.write(intCheck);

            System.out.println("hash " + Arrays.toString(intCheck));

            System.out.println("Sent " + pbAuthentication);


            //TODO: (round 4) recv SS-PaymentRequest
            //TODO: (round 5) send PB-Payment
            //TODO: (round 6) recv PB-Payment SS-TicketCredentials

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Cipher genPBECipher(int opmode, String pw, String ciphersuite, byte[] salt, int iterationCounter) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchProviderException {
        PBEKeySpec pbeSpec = new PBEKeySpec(pw.toCharArray());
        Key k = SecretKeyFactory.getInstance(ciphersuite, "BC").generateSecret(pbeSpec);
        Cipher c = Cipher.getInstance(ciphersuite, "BC");
        c.init(opmode, k, new PBEParameterSpec(salt, iterationCounter));
        return c;
    }


}