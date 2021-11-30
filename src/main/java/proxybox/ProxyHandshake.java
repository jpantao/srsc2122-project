package proxybox;

import common.Utils;
import rtstp.messages.PlainMsgRTSTP;
import rtstp.messages.PlainPBReqAndCreds;

import javax.crypto.Mac;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class ProxyHandshake {

    private static final String CONFIG_FILE = "config/srtsp.properties";

    private final InetSocketAddress inSocketAddress;
    private final InetSocketAddress outSocketAddress;
    private static final int VERSION = 1;
    private static final SecureRandom random = new SecureRandom();
    private long expectedNonce;

    private final Properties properties;
    private final Mac mac;

    public ProxyHandshake(String serverAddr, int listenPort) throws IOException {
        inSocketAddress = parseSocketAddress(InetAddress.getLocalHost().getHostAddress() + ":" + listenPort );
        outSocketAddress = parseSocketAddress(serverAddr);

        properties = Utils.loadConfig(CONFIG_FILE);
        String macSuite = properties.getProperty("mac-ciphersuite");
        byte[] macKeyBytes = Utils.decodeHexString(properties.getProperty("mac-keybytes"));
        mac = Utils.getHMAC(macSuite, macKeyBytes, macSuite);

    }

    public void start(byte[] ticket, byte[] sigBytes) throws IOException {
        byte[] inBuffer = new byte[4 * 1024];
        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
        DatagramSocket inSocket = new DatagramSocket(inSocketAddress);
        DatagramSocket outSocket = new DatagramSocket();

        //TODO: (round 1)
        outSocket.send(round1Packet(ticket, sigBytes));
        inSocket.receive(inPacket);

        //TODO: (round 2)
//        processRound2(inPacket);
//        outSocket.send(round3Packet());

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
        byte[] payload = PlainMsgRTSTP.serialize(msg);
        Utils.writeWithHeaderRTSTP(dos, VERSION, msg.getType(), payload);
        Utils.writeByteArray(dos, sigBytes);
        Utils.writeIntCheck(dos, mac, payload);
        Utils.logSent(msg);

        byte[] packet = baos.toByteArray();
        return new DatagramPacket(packet, packet.length);
    }

    private DatagramPacket round3Packet() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);


        DatagramPacket datagramPacket = makeSRTSPPacket(baos.toByteArray(), 3);
        dos.close();
        baos.close();
        return datagramPacket;
    }

    private DatagramPacket makeSRTSPPacket(byte[] data, int msgType) throws IOException {
        byte[] outBuffer = new byte[4 * 1024];
        DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, outSocketAddress);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        baos.write(VERSION);
        baos.write(msgType);
        baos.write(data);
        baos.flush();
        outPacket.setData(baos.toByteArray(), 0, baos.size());
        dos.close();
        baos.close();
        return outPacket;
    }


}
