package streamingserver;

import common.SecureDatagramSocket;
import common.Utils;
import sapkdp.messages.PlainTicketCreds;
import srtsp.messages.PlainMsgSRTSP;
import srtsp.messages.PlainPBReqAndCreds;
import srtsp.messages.PlainRTSSVerification;

import javax.crypto.Mac;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

    private DatagramSocket socket;
    private Map<String, PublicKey> keyring;
    private KeyPair keyPair;

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

    public void go() {

        byte[] inBuffer = new byte[4 * 1024];
        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
        System.out.println("Listening....");

        try {
            // (round 1)
            socket.receive(inPacket);
            PlainPBReqAndCreds reqAndCreds = processRound1(inPacket);
            PlainTicketCreds ticket = Utils.decryptTicket(reqAndCreds.getTicket(), properties.getProperty("asym-ciphersuite"), keyPair.getPrivate());
            Utils.logReceived(ticket);

            //TODO: (round 2)
            //change port for socket simulating connections (helps adding multiple client support in the future)
            writeCryptoConf(ticket);
            socket = new SecureDatagramSocket(socket.getLocalSocketAddress());
            socket.send(round2Packet(reqAndCreds, ticket.getIp(), ticket.getClientPort()));

            //TODO: (round 3)
            //TODO: (round 4)
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void processRound3(DatagramPacket inPacket) {


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

    private DatagramPacket round2Packet(PlainPBReqAndCreds reqAndCreds, String ip, int port) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        int na2 = ThreadLocalRandom.current().nextInt();
        PlainRTSSVerification msg = new PlainRTSSVerification(reqAndCreds.getNa1()+1, na2, true);
        dos.write(PlainMsgSRTSP.serialize(msg));
        byte[] packet = baos.toByteArray();
        Utils.logSent(msg);
        return new DatagramPacket(packet, packet.length, InetAddress.getByName(ip), port);
    }


    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }



    private void writeCryptoConf(PlainTicketCreds ticket){
        Properties prop = new Properties();
        prop.setProperty("algorithm",  ticket.getCiphersuiteConf().split("/")[0]);
        prop.setProperty("options", ticket.getCiphersuiteConf());
        prop.setProperty("ivBytes", ticket.getCryptoSA());
        prop.setProperty("keyBytes", Utils.encodeHexString(ticket.getSessionkeyBytes()));
        prop.setProperty("hmac", ticket.getMacsuite());
        prop.setProperty("hmacBytes", Utils.encodeHexString(ticket.getMackeyBytes()));

        try {
            prop.store(new FileOutputStream(CRYPTOCONF_FILE), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private DatagramPacket makePacket(byte[] data, int msgType) throws IOException {
//        byte[] outBuffer = new byte[4 * 1024];
//        DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, outSocketAddress );
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        DataOutputStream dos = new DataOutputStream(baos);
//        baos.write(VERSION);
//        baos.write(msgType);
//        baos.write(data);
//        baos.flush();
//        outPacket.setData(baos.toByteArray(), 0, baos.size());
//        dos.close();
//        baos.close();
//        return outPacket;
//    }
}
