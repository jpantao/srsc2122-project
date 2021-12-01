package proxybox;

import common.Utils;
import srtsp.messages.PlainMsgSRTSP;
import srtsp.messages.PlainPBReqAndCreds;

import javax.crypto.Mac;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class ProxyHandshake {

    private static final String CONFIG_FILE = "config/srtsp.properties";

    private static final int VERSION = 1;
    private static final SecureRandom random = new SecureRandom();
    private long expectedNonce;

    private final Properties properties;
    private final Mac mac;
    private final DatagramSocket socket;
    private final int servicePort;
    private final InetAddress serverAddr;

    private final byte[] payloadUsedInSignature;

    public ProxyHandshake(DatagramSocket socket, String serverAddr, int servicePort, byte[] payloadUsedInSignature) throws IOException {
        Utils.loadBC();
        this.properties = Utils.loadConfig(CONFIG_FILE);
        String macSuite = properties.getProperty("mac-ciphersuite");
        byte[] macKeyBytes = Utils.decodeHexString(properties.getProperty("mac-keybytes"));
        this.mac = Utils.getHMAC(macSuite, macKeyBytes, macSuite);

        this.socket = socket;
        this.serverAddr = InetAddress.getByName(serverAddr);
        this.servicePort = servicePort;
        this.payloadUsedInSignature = payloadUsedInSignature;
    }

    public void start(byte[] ticket, byte[] sigBytes) throws IOException {
        byte[] inBuffer = new byte[4 * 1024];
        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);

        // (round 1)
        socket.send(round1Packet(ticket, sigBytes));

        //TODO: (round 2)
        socket.receive(inPacket);
        processRound2(inPacket);

        //TODO: (round 3)
        //TODO: (round 4)

    }

    private void processRound2(DatagramPacket inPacket) {

    }


    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }


    private DatagramPacket round1Packet(byte[] ticket, byte[] sigBytes) throws IOException {
        byte[] buf = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int na1 = ThreadLocalRandom.current().nextInt();
        expectedNonce = na1 + 1;

        PlainPBReqAndCreds msg = new PlainPBReqAndCreds(ticket, na1);
        byte[] payload = PlainMsgSRTSP.serialize(msg);
        Utils.writeWithHeaderRTSTP(dos, VERSION, msg.getType(), payload);
        Utils.writeByteArray(dos, payloadUsedInSignature);
        Utils.writeByteArray(dos, sigBytes);
        Utils.writeIntCheck(dos, mac, payload);
        Utils.logSent(msg);

        byte[] packet = baos.toByteArray();
        return new DatagramPacket(packet, packet.length, serverAddr, servicePort);
    }

    private DatagramPacket round3Packet() throws IOException {

        return null;
    }




}
