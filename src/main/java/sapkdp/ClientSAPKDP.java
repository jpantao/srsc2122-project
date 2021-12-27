package sapkdp;

import common.Utils;
import sapkdp.messages.*;

import javax.crypto.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("DuplicatedCode")
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


    private PlainTicketCreds clientTicket;
    private byte[] rtssCipherTicket;
    private byte[] payloads;
    private byte[] sigBytes;

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

    public PlainTicketCreds getClientTicket() {
        return clientTicket;
    }

    public byte[] getRtssCipherTicket() {
        return rtssCipherTicket;
    }

    public byte[] getPayloads() {
        return payloads;
    }

    public byte[] getSigBytes() {
        return sigBytes;
    }

    public void handshake(String movieID, String coinFile) throws IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        String[] addr = sigserverAddr.split(":");

//        String ksName = "keystores/clienttruststore";
//        char[]  ksPass = "srsc2122".toCharArray();   // password da keystore
//        char[]  ctPass = "srsc2122".toCharArray();
//        String[] confciphersuites={"TLS_RSA_WITH_AES_256_CBC_SHA256"};
//        String[] confprotocols={"TLSv1.2"};
//        KeyStore ks = KeyStore.getInstance("JKS");
//        ks.load(new FileInputStream(ksName), ksPass);
//        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//        kmf.init(ks, ctPass);
//        SSLContext sc = SSLContext.getInstance("TLS");
//        sc.init(kmf.getKeyManagers(), null, null);





//        String ksName = "keystores/serverkeystore";
        String tsName = "keystores/truststore";
        char[]  ksPass = "srsc2122".toCharArray();   // password da keystore
        char[]  ctPass = "srsc2122".toCharArray();
//        String[] confciphersuites={"TLS_RSA_WITH_AES_256_CBC_SHA256"};
//        String[] confprotocols={"TLSv1.2"};


        SSLContext sc = SSLContext.getInstance("TLS");

//        KeyStore ksKeys = KeyStore.getInstance("JKS");
        KeyStore ksTrust = KeyStore.getInstance("JKS");
//        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

//        ksKeys.load(new FileInputStream(ksName), ksPass);
        ksTrust.load(new FileInputStream(tsName), ksPass);
//        kmf.init(ksKeys, ctPass);
        tmf.init(ksTrust);
        sc.init(null, tmf.getTrustManagers(), null);
//        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);


        SSLSocketFactory f = sc.getSocketFactory();







        try (SSLSocket sock = (SSLSocket) f.createSocket(addr[0], Integer.parseInt(addr[1]))) {
            sock.startHandshake();
            DataInputStream in = new DataInputStream(sock.getInputStream());
            DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            String dsaSuite = properties.getProperty("dsa-ciphersuite");
            String provider = properties.getProperty("provider");

            int msgType;
            byte[] payload;
            byte[] sigBytes;

            // (round 1)
            PlainPBHello hello = new PlainPBHello(userID, pboxID);
            payload = PlainMsgSAPKDP.serialize(hello);
            Utils.writeWithHeaderSAPKDP(out, VERSION, hello.getType(), payload);
            Utils.logSent(hello);

            // (round 2)
            msgType = PlainMsgSAPKDP.Type.SS_AUTHREQ.value;
            payload = Utils.readConsumingHeader(in, msgType);
            PlainSSAuthReq authReq = (PlainSSAuthReq) PlainMsgSAPKDP.deserialize(msgType, payload);
            Utils.logReceived(authReq);

            // (round 3)
            PlainPBAuth auth = genPlainAuth(authReq, movieID);
            payload = encryptAuth(auth, authReq.getSalt(), authReq.getCounter(), userPW);
            Utils.writeWithHeaderSAPKDP(out, VERSION, auth.getType(), payload);
            Utils.writeIntCheck(out, mac, payload);
            Utils.logSent(auth);

            // (round 4)
            msgType = PlainMsgSAPKDP.Type.SS_PAYREQ.value;
            payload = Utils.readConsumingHeader(in, msgType);
            sigBytes = Utils.readSig(in);
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
            Utils.writeWithHeaderSAPKDP(out, VERSION, payment.getType(), payload);
            Utils.writeSignature(out, dsaSuite, provider, keyPair.getPrivate(), payload);
            Utils.writeIntCheck(out, mac, payload);
            Utils.logSent(payment);

            // (round 6)
            msgType = PlainMsgSAPKDP.Type.PB_TKCREDS.value;
            payload = Utils.readConsumingHeader(in, msgType);
            this.payloads = Arrays.copyOf(payload, payload.length);
            int clientTicketCipherSize = in.readInt();
            int rtssTicketCipherSize = in.readInt();
            sigBytes = Utils.readSig(in);
            if (!Utils.readVerifyingIntCheck(in, mac, payload))
                throw new Exception("IntCheck6 failed");
            if (!Utils.verifySig(dsaSuite, provider, keyring.get(SIGSERVER_ALIAS), payload, sigBytes))
                throw new Exception("Signature for ticket creds could not be verified");

            this.sigBytes = sigBytes;

            ByteArrayInputStream bai = new ByteArrayInputStream(payload);
            DataInputStream dai = new DataInputStream(bai);
            byte[] cipherClientTC = new byte[clientTicketCipherSize];
            dai.read(cipherClientTC);
            byte[] cipherRtssTC = new byte[rtssTicketCipherSize];
            dai.read(cipherRtssTC);

            PlainTicketCreds ticket = Utils.decryptTicket(cipherClientTC, properties.getProperty("asym-ciphersuite"),keyPair.getPrivate());

            if (ticket.getNonce() != payment.getN4() + 1)
                throw new Exception("n4' != n4+1");
            Utils.logReceived(ticket);

            this.clientTicket = ticket;
            this.rtssCipherTicket = cipherRtssTC;

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

    private byte[] extractBytes(byte[] buf, int size) throws IOException {
        byte[] extracted = new byte[size];
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        in.read(extracted);
        return extracted;
    }



}
