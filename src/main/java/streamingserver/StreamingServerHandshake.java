package streamingserver;

import common.Utils;
import sapkdp.messages.PlainTicketCreds;
import srtsp.messages.PlainMsgSRTSP;
import srtsp.messages.PlainPBReqAndCreds;
import sun.security.krb5.internal.Ticket;

import javax.crypto.Mac;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class StreamingServerHandshake {

    private static final int VERSION = 1;
    public static final String CONFIG_FILE = "config/srtsp.properties";

    //TODO: convert alias to args or props! Furthermore, proxybox key alias should be the ProxyBox's ID
    // Pubkey aliases: coinissuer, proxybox, streamingserver e signalingserver
    public static final String PROXYBOX_ALIAS = "proxybox";
    public static final String COINISSUER_ALIAS = "coinissuer";
    public static final String SIGSERVER_ALIAS = "signalingserver";
    public static final String STRSERVER_ALIAS = "streamingserver";

    private final Properties properties;
    private static Mac mac;

    private  DatagramSocket socket;
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

    public void go()  {

        byte[] inBuffer = new byte[4 * 1024];
        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
        System.out.println("Listening....");

        try {
            // (round 1)
            socket.receive(inPacket);
            PlainPBReqAndCreds reqAndCreds = processRound1(inPacket);
            PlainTicketCreds ticket= Utils.decryptTicket(reqAndCreds.getTicket(), properties.getProperty("asym-ciphersuite"), keyPair.getPrivate());

            //TODO: (round 2)
            socket = new DatagramSocket(ticket.getClientPort());

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

    private DatagramPacket round2Packet() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);



        return null;
    }


    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
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
