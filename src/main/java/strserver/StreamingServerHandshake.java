package strserver;

import common.SecureDatagramSocket;
import common.Utils;
import sapkdp.messages.PlainTicketCreds;
import srtsp.messages.*;

import javax.crypto.Mac;
import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;


public class StreamingServerHandshake {

    private static final int VERSION = 1;
    public static final String CONFIG_FILE = "config/srtsp.properties";
    public static final String CRYPTOCONF_FILE = "config/config.properties";

    //TODO: convert alias to args or props! Furthermore, proxybox key alias should be the ProxyBox's ID
    // Pubkey aliases: coinissuer, proxybox, streamingserver e signalingserver
    public static final String PROXYBOX_ALIAS = "proxybox";
    public static final String COINISSUER_ALIAS = "coinissuer";
    public static final String SIGSERVER_ALIAS = "signalingserver";
    public static final String STRSERVER_ALIAS = "streamingserver";

    private final Properties properties;
    private static Mac mac;

    private int expectedNonce;

    private DatagramSocket socket;
    private Map<String, PublicKey> keyring;
    private KeyPair keyPair;

    private String ip;
    private int port;
    private String movieID;

    public StreamingServerHandshake(String keystoreFile, char[] storepass, int port) throws IOException {
        Utils.loadBC();
        properties = Utils.loadConfig(CONFIG_FILE);

        String macSuite = properties.getProperty("mac-ciphersuite");
        byte[] macKeyBytes = Utils.decodeHexString(properties.getProperty("mac-keybytes"));
        mac = Utils.getHMAC(macSuite, macKeyBytes, macSuite);


        socket = new DatagramSocket(port);

        // load keypair from keystore
        keyPair = Utils.getKeyPair(keystoreFile, storepass, STRSERVER_ALIAS);

        // load public keys into keyring
        keyring = new HashMap<>();
        keyring.put(PROXYBOX_ALIAS, Utils.getPubKey(keystoreFile, storepass, PROXYBOX_ALIAS));
        keyring.put(COINISSUER_ALIAS, Utils.getPubKey(keystoreFile, storepass, COINISSUER_ALIAS));
        keyring.put(SIGSERVER_ALIAS, Utils.getPubKey(keystoreFile, storepass, SIGSERVER_ALIAS));
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getMovieID() {
        return movieID;
    }

    public void go() {

        byte[] inBuffer;
        DatagramPacket inPacket;
        System.out.println("Listening....");

        try {
            // (round 1)
            inBuffer = new byte[4 * 1024];
            inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            socket.receive(inPacket);
            PlainPBReqAndCreds reqAndCreds = processRound1(inPacket);
            PlainTicketCreds ticket = Utils.decryptTicket(reqAndCreds.getTicket(), properties.getProperty("asym-ciphersuite"), keyPair.getPrivate());
            Utils.logReceived(ticket);

            // (round 2)
            //change port for socket simulating connections (helps multiple client support in the future)
            Utils.writeCryptoConf(ticket, CRYPTOCONF_FILE);
            System.out.println("listen port " + ticket.getPort());
            socket.close();
            SecureDatagramSocket newSocket = new SecureDatagramSocket(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), ticket.getPort()));
            newSocket.send(round2Packet(reqAndCreds.getNa1(), inPacket.getAddress().getHostAddress(), inPacket.getPort()));

            // (round 3)
            inBuffer = new byte[4 * 1024];
            inPacket = new DatagramPacket(inBuffer, inBuffer.length);
            newSocket.receive(inPacket);
            PlainPBAckVerification verificationAck = processRound3(inPacket);

            // (round 4)
            newSocket.send(round4Packet(verificationAck.getN3(), inPacket.getAddress().getHostAddress(), inPacket.getPort()));

            ip = inPacket.getAddress().getHostAddress();
            port = inPacket.getPort();
            movieID = ticket.getMovieID();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    private PlainPBAckVerification processRound3(DatagramPacket inPacket) throws Exception {
        ByteArrayInputStream bai = new ByteArrayInputStream(inPacket.getData());
        DataInputStream dai = new DataInputStream(bai);

        int msgType = PlainMsgSRTSP.Type.PB_VER_ACK.value;
        byte[] payload = Utils.readConsumingHeader(dai, msgType);
        PlainPBAckVerification verificationAck = (PlainPBAckVerification) PlainMsgSRTSP.deserialize(msgType, payload);

        if (verificationAck.getN2Prime() != expectedNonce)
            throw new Exception("na2' != na2 + 1");
        Utils.logReceived(verificationAck);

        return verificationAck;
    }

    private PlainPBReqAndCreds processRound1(DatagramPacket inPacket) throws Exception {
        ByteArrayInputStream bai = new ByteArrayInputStream(inPacket.getData());
        DataInputStream dai = new DataInputStream(bai);

        String dsaSuite = properties.getProperty("dsa-ciphersuite");
        String provider = properties.getProperty("provider");

        int msgType = PlainMsgSRTSP.Type.PB_REQ_N_AUTH.value;
        byte[] payload = Utils.readConsumingHeader(dai, msgType);
        byte[] payloadUsedInSignature = Utils.readByteArray(dai);
        byte[] sigBytes = Utils.readSig(dai);
        if (!Utils.readVerifyingIntCheck(dai, mac, payload))
            throw new Exception("IntCheck1 failed");
        if (!Utils.verifySig(dsaSuite, provider, keyring.get(SIGSERVER_ALIAS), payloadUsedInSignature, sigBytes))
            throw new Exception("Signature for authentication and creds could not be verified");
        PlainPBReqAndCreds msg = (PlainPBReqAndCreds) PlainMsgSRTSP.deserialize(msgType, payload);
        Utils.logReceived(msg);
        return msg;
    }

    private DatagramPacket round2Packet(int na1, String ip, int port) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        int na2 = ThreadLocalRandom.current().nextInt();
        expectedNonce = na2 + 1;
        PlainRTSSVerification msg = new PlainRTSSVerification(na1 + 1, na2, true);
        Utils.writeWithHeaderRTSTP(dos, VERSION, msg.getType(), PlainMsgSRTSP.serialize(msg));
        byte[] packet = baos.toByteArray();
        Utils.logSent(msg);
        System.out.println(ip + port);
        return new DatagramPacket(packet, packet.length, InetAddress.getByName(ip), port);
    }

    private DatagramPacket round4Packet(int na3, String ip, int port) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        PlainRTSSSyncInitFrame msg = new PlainRTSSSyncInitFrame(na3 + 1, 0);
        Utils.writeWithHeaderRTSTP(dos, VERSION, msg.getType(), PlainMsgSRTSP.serialize(msg));
        byte[] packet = baos.toByteArray();
        Utils.logSent(msg);
        System.out.println(ip + port);
        return new DatagramPacket(packet, packet.length, InetAddress.getByName(ip), port);
    }

}
