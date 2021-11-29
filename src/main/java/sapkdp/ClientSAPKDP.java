package sapkdp;


import common.Utils;
import sapkdp.messages.*;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class ClientSAPKDP {

    public static final int VERSION = 1;
    public static final String CONFIG_FILE = "config/sapkdp.properties";

    //TODO: convert alias to args or props!
    // Pubkey alias: coinissuer, proxybox, streamingserver e signalingserver
    public static final String PROXYBOX_ALIAS = "proxybox";
    public static final String COINISSUER_ALIAS = "coinissuer";
    public static final String SIGSERVER_ALIAS = "signalingserver";
    public static final String STRSERVER_ALIAS = "streamingserver";

    private final Map<String, PublicKey> keyring;
    private final KeyPair keyPair;
    private final String pboxID;
    private final String userID;
    private final String userPW;
    private final String sigserverAddr;
    private final Properties properties;
    private final Mac mac;

    public ClientSAPKDP(String pboxID, String userID, String keystoreFile, char[] storepass, String userPW, String sigserverAddr) {
        this.sigserverAddr = sigserverAddr;
        this.pboxID = pboxID;
        this.userID = userID;
        this.userPW = userPW;

        Utils.loadBC();

        properties = Utils.loadConfig(CONFIG_FILE);
        String macSuite = properties.getProperty("mac-ciphersuite");
        byte[] macKeyBytes = Utils.decodeHexString(properties.getProperty("mac-keybytes"));
        mac = Utils.getHMAC(macSuite, macKeyBytes, macSuite);

        // load keypair from keystore
        keyPair = Utils.getKeyPair(keystoreFile, storepass, PROXYBOX_ALIAS);

        // load public keys into keyring
        keyring = new HashMap<>();
        keyring.put(SIGSERVER_ALIAS, Utils.getPubKey(keystoreFile, storepass, SIGSERVER_ALIAS));
        keyring.put(COINISSUER_ALIAS, Utils.getPubKey(keystoreFile, storepass, COINISSUER_ALIAS));
        keyring.put(STRSERVER_ALIAS, Utils.getPubKey(keystoreFile, storepass, STRSERVER_ALIAS));
    }


    public void handshake(String movieID, String coinFile) {
        String[] addr = sigserverAddr.split(":");
        try (Socket sock = new Socket(addr[0], Integer.parseInt(addr[1]))) {
            DataInputStream in = new DataInputStream(sock.getInputStream());
            DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            String dsaSuite = properties.getProperty("dsa-ciphersuite");
            String provider = properties.getProperty("provider");

            int msgType;
            byte[] payload;

            // (round 1)
            PlainPBHello hello = new PlainPBHello(userID, pboxID);
            payload = PlainMsgSAPKDP.serialize(hello);
            Utils.sendWithHeader(out, VERSION, hello.getType(), payload);
            Utils.logSent(hello);

            // (round 2)
            msgType = PlainMsgSAPKDP.Type.SS_AUTHREQ.msgType;
            payload = Utils.readConsumingHeader(in, msgType);
            PlainSSAuthReq authReq = (PlainSSAuthReq) PlainMsgSAPKDP.deserialize(msgType, payload);
            Utils.logReceived(authReq);

            // (round 3)
            PlainPBAuth auth = genPlainAuth(authReq, movieID);
            payload = encryptAuth(auth, authReq.getSalt(), authReq.getCounter(), userPW);
            Utils.sendWithHeader(out, VERSION, auth.getType(), payload);
            Utils.sendIntCheck(out, mac, payload);
            Utils.logSent(auth);

            // (round 4)
            msgType = PlainMsgSAPKDP.Type.SS_PAYREQ.msgType;
            payload = Utils.readConsumingHeader(in, msgType);
            byte[] sigBytes = Utils.readSig(in);
            if (!Utils.readVerifyingIntCheck(in, mac, payload))
                throw new Exception("IntCheck4 failed");
            if (!Utils.verifySig(dsaSuite, provider, keyring.get(SIGSERVER_ALIAS), payload, sigBytes))
                throw new Exception("Signature for payment request could not be verified");
            PlainSSPaymentReq paymentReq = (PlainSSPaymentReq) PlainMsgSAPKDP.deserialize(msgType, payload);
            if (paymentReq.getN2Prime() != auth.getN2() + 1)
                throw new Exception("n2' != n2+1");
            Utils.logReceived(paymentReq);

            // (round 5)
            PlainPBPayment payment = genPlainPayment(paymentReq, coinFile);
            payload = PlainMsgSAPKDP.serialize(payment);
            Utils.sendWithHeader(out, VERSION, payment.getType(), payload);
            Utils.sendSignature(out, dsaSuite, provider, keyPair.getPrivate(), payload);
            Utils.sendIntCheck(out, mac, payload);
            Utils.logSent(payment);

            //TODO: (round 6)


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private PlainPBAuth genPlainAuth(PlainSSAuthReq req, String movieID) {
        int n1Prime = req.getN1() + 1;
        int n2 = ThreadLocalRandom.current().nextInt();
        return new PlainPBAuth(n1Prime, n2, movieID);
    }

    private PlainPBPayment genPlainPayment(PlainSSPaymentReq req, String coinFile) throws IOException {
        int n3Prime = req.getN3() + 1;
        int n4 = ThreadLocalRandom.current().nextInt();
        byte[] coinBytes = Files.readAllBytes((new File(coinFile)).toPath());
        return new PlainPBPayment(n3Prime, n4, coinBytes);
    }

    private byte[] encryptAuth(PlainPBAuth auth, byte[] salt, int counter, String password) throws IOException {
        String ciphersuite = this.properties.getProperty("pbe-ciphersuite");
        String provider = this.properties.getProperty("provider");
        byte[] plaintext = PlainMsgSAPKDP.serialize(auth);
        return Utils.pbeCipher(Cipher.ENCRYPT_MODE, password, ciphersuite, provider, salt, counter, plaintext);
    }


}
