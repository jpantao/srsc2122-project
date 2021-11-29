package pt.unl.fct.srsc.sapkdp;

import com.google.gson.JsonObject;

import pt.unl.fct.srsc.common.Utils;
import pt.unl.fct.srsc.extra.VoucherMinter;
import pt.unl.fct.srsc.sapkdp.messages.*;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class ServerSAPKDP {

    public static final int VERSION = 1;
    public static final String CONFIG_FILE = "config/sapkdp.properties";

    //TODO: convert alias to args or props! Furthermore, proxybox key alias should be the ProxyBox's ID
    // Pubkey aliases: coinissuer, proxybox, streamingserver e signalingserver
    public static final String PROXYBOX_ALIAS = "proxybox";
    public static final String COINISSUER_ALIAS = "coinissuer";
    public static final String SIGSERVER_ALIAS = "signalingserver";
    public static final String STRSERVER_ALIAS = "streamingserver";

    private static ServerSAPKDP instance;

    private Map<String, PublicKey> keyring;
    private KeyPair keyPair;

    private static Properties properties;
    private static Mac mac;
    private static JsonObject users;
    private static JsonObject movies;

    public static ServerSAPKDP getInstance() {
        return instance == null ? new ServerSAPKDP() : instance;
    }

    private ServerSAPKDP() {

    }

    public void load(String keystoreFile, char[] storepass, JsonObject usersDB, JsonObject moviesDB) {
        properties = Utils.loadConfig(CONFIG_FILE);
        String macSuite = properties.getProperty("mac-ciphersuite");
        byte[] macKeyBytes = Utils.decodeHexString(properties.getProperty("mac-keybytes"));
        mac = Utils.getHMAC(macSuite, macKeyBytes, macSuite);
        users = usersDB;
        movies = moviesDB;

        // load keypair from keystore
        keyPair = Utils.getKeyPair(keystoreFile, storepass, SIGSERVER_ALIAS);

        // load public keys into keyring
        keyring = new HashMap<>();
        keyring.put(PROXYBOX_ALIAS, Utils.getPubKey(keystoreFile, storepass, PROXYBOX_ALIAS));
        keyring.put(COINISSUER_ALIAS, Utils.getPubKey(keystoreFile, storepass, COINISSUER_ALIAS));
        keyring.put(STRSERVER_ALIAS, Utils.getPubKey(keystoreFile, storepass, STRSERVER_ALIAS));
    }

    public void handleHandshake(Socket sock) {
        try {
            DataInputStream in = new DataInputStream(sock.getInputStream());
            DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            String dsaSuite = properties.getProperty("dsa-ciphersuite");
            String provider = properties.getProperty("provider");

            int msgType;
            byte[] payload;

            // (round 1)
            msgType = PlainMsgSAPKDP.Type.PB_HELLO.msgType;
            payload = Utils.readConsumingHeader(in, msgType);
            PlainPBHello hello = (PlainPBHello) PlainMsgSAPKDP.deserialize(msgType, payload);
            Utils.logReceived(hello);

            // (round 2)
            PlainSSAuthReq authReq = genAuthReq();
            payload = PlainMsgSAPKDP.serialize(authReq);
            Utils.sendWithHeader(out, VERSION, authReq.getType(), payload);
            Utils.logSent(authReq);

            // (round 3)
            msgType = PlainMsgSAPKDP.Type.PB_AUTH.msgType;
            payload = Utils.readConsumingHeader(in, msgType);
            if (!Utils.readVerifyingIntCheck(in, mac, payload))
                throw new Exception("IntCheck3 failed");
            PlainPBAuth auth = decryptAuth(msgType, payload, authReq.getSalt(), authReq.getCounter(), hello.getUserID());
            if (auth.getN1Prime() != authReq.getN1() + 1)
                throw new Exception("n1' != n1+1");
            Utils.logReceived(auth);

            // (round 4)
            PlainSSPaymentReq paymentReq = genPaymentReq(auth);
            payload = PlainMsgSAPKDP.serialize(paymentReq);
            Utils.sendWithHeader(out, VERSION, paymentReq.getType(), payload);
            Utils.sendSignature(out, dsaSuite, provider, keyPair.getPrivate(), payload);
            Utils.sendIntCheck(out, mac, payload);
            Utils.logSent(paymentReq);

            // (round 5)
            msgType = PlainMsgSAPKDP.Type.PB_PAYMENT.msgType;
            payload = Utils.readConsumingHeader(in, msgType);
            byte[] sigBytes = Utils.readSig(in);
            if (!Utils.readVerifyingIntCheck(in, mac, payload))
                throw new Exception("IntCheck5 failed");
            if (!Utils.verifySig(dsaSuite, provider, keyring.get(PROXYBOX_ALIAS), payload, sigBytes))
                throw new Exception("Signature for payment could not be verified");
            PlainPBPayment payment = (PlainPBPayment) PlainMsgSAPKDP.deserialize(msgType, payload);
            if (payment.getN3Prime() != paymentReq.getN3() + 1)
                throw new Exception("n3' != n3+1");
            float voucherVal = VoucherMinter.verifyVoucher(payment.getPaymentCoin());
            if (voucherVal < 0)
                throw new Exception("Voucher could not be verified");
            Utils.logReceived(payment);

            //TODO: (round 6)
            if (!canAccess(auth.getMovieID(), voucherVal))
                throw new Exception("Voucher value is insufficient");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PlainSSAuthReq genAuthReq() {
        int n1 = ThreadLocalRandom.current().nextInt();
        byte[] salt = new byte[20];
        ThreadLocalRandom.current().nextBytes(salt);
        int counter = ThreadLocalRandom.current().nextInt(1024, 2048);
        return new PlainSSAuthReq(n1, salt, counter);
    }

    private boolean canAccess(String movieID, float payment) {
        return movies.get(movieID).getAsJsonObject().get("ppvprice").getAsFloat() <= payment;
    }

    private PlainTicketCreds genTCforClient(String movieID, float payment) {
        String ip = properties.getProperty("strserverIP");
        int port = Integer.parseInt(properties.getProperty("strserverPORT"));
        String cipherSuite = properties.getProperty("ciphersuite");
        String cryptoSA = properties.getProperty("cryptoSA");
        String sessionKey = properties.getProperty("key");


        return null;
    }

    private PlainSSPaymentReq genPaymentReq(PlainPBAuth auth) {
        float price = movies.get(auth.getMovieID()).getAsJsonObject().get("ppvprice").getAsFloat();
        int n2Prime = auth.getN2() + 1;
        int n3 = ThreadLocalRandom.current().nextInt();
        return new PlainSSPaymentReq(price, n2Prime, n3);
    }

    private synchronized static PlainPBAuth decryptAuth(int msgType, byte[] ciphertext, byte[] salt, int counter, String userID) {
        String ciphersuite = properties.getProperty("pbe-ciphersuite");
        String provider = properties.getProperty("provider");
        String password = users.get(userID).getAsJsonObject().get("password").getAsString();
        byte[] plaintext = Utils.pbeCipher(Cipher.DECRYPT_MODE, password, ciphersuite, provider, salt, counter, ciphertext);

        return (PlainPBAuth) PlainMsgSAPKDP.deserialize(msgType, plaintext);
    }


}
