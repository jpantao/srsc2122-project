package sigserver.sapkdp;

import common.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sigserver.SignalingServer;
import sigserver.sapkdp.messages.*;

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
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtoSAPKDP {


    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);

    public static final int VERSION = 2;
    public static final String CONFIG = "resources/sapkdp.properties";

    private final String sigserver, proxyBoxID, userID, userPW;
    private final PrivateKey prv;
    private final PublicKey pubSigserver;

    private final Properties properties;


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

    public ProtoSAPKDP(String proxyBoxID, String userID, String userPW, String sigserver, PrivateKey prv, PublicKey pubSigserver) {
        this.proxyBoxID = proxyBoxID;
        this.userID = userID;
        this.userPW = userPW;
        this.sigserver = sigserver;
        this.pubSigserver = pubSigserver;
        this.prv = prv;

        properties = new Properties();
        try {
            properties.load(new FileInputStream(CONFIG));
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

            Mac hmac = Mac.getInstance(properties.getProperty("HMACsuite"));
            hmac.init(new SecretKeySpec(Utils.decodeHexString(
                    properties.getProperty("hmackey")), properties.getProperty("HMACsuite")));


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
            Cipher pbeCipher = genPBECipher(Cipher.ENCRYPT_MODE, userPW, properties.getProperty("PBEsuite"), properties.getProperty("provider"), authReq.getSalt(), authReq.getCounter());

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
            out.write(intCheck);

            System.out.println("Sent " + pbAuthentication);

            //TODO: (round 4) recv SS-PaymentRequest
            msgType = MessageSAPKDP.Type.SS_PAYREQ.msgType;
            Signature signature = Signature.getInstance(properties.getProperty("ECDSAsuite"), properties.getProperty("provider"));
            signature.initVerify(pubSigserver);

            headerBytes = new byte[Header.BYTE_LEN];
            in.read(headerBytes);
            header = new Header(headerBytes);

            payload = new byte[header.getPayloadSize()];
            in.read(payload);
            hmac.update(payload);

            byte[] sigBytes = new byte[in.readInt()];
            in.read(sigBytes);

            byte[] messageHash = new byte[hmac.getMacLength()];
            in.read(messageHash);

            if (!MessageDigest.isEqual(messageHash, hmac.doFinal())) {
                //TODO: handle error
                socket.close();
                return;
            }

            signature.update(payload);
            if (!signature.verify(sigBytes)) {
                System.out.println("DEEEEEBUG");
                //TODO: handle error
                socket.close();
                return;
            }

            SSPaymentRequest paymentRequest = (SSPaymentRequest) MessageSAPKDP.deserialize(msgType, payload);
            System.out.println("Recv " + paymentRequest);

            //TODO: (round 5) send PB-Payment
            //TODO: (round 6) recv PB-Payment SS-TicketCredentials

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Cipher genPBECipher(int opmode, String pw, String ciphersuite, String provider,byte[] salt, int iterationCounter) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchProviderException {
        PBEKeySpec pbeSpec = new PBEKeySpec(pw.toCharArray());
        Key k = SecretKeyFactory.getInstance(ciphersuite).generateSecret(pbeSpec);
        Cipher c = Cipher.getInstance(ciphersuite, provider);
        c.init(opmode, k, new PBEParameterSpec(salt, iterationCounter));
        return c;
    }


}
