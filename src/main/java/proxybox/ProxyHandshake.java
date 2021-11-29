package proxybox;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Properties;

public class ProxyHandshake {
    private final InetSocketAddress inSocketAddress;
    private final InetSocketAddress outSocketAddress;
    private static final int VERSION = 1;
    private static final SecureRandom random = new SecureRandom();
    private long expectedNonce;

    public ProxyHandshake() throws IOException {
        InputStream inputStream = new FileInputStream("pt/unl/fct/srsc/proxy/config.properties");
        if (inputStream == null) {
            System.err.println("Configuration file not found!");
            System.exit(1);
        }
        Properties properties = new Properties();
        properties.load(inputStream);
        String outAddress = properties.getProperty("streamingserver");
        String inAddress = properties.getProperty("remote");
        inSocketAddress = parseSocketAddress(inAddress);
        outSocketAddress = parseSocketAddress(outAddress);
    }

    public void start() throws IOException {
        byte[] inBuffer = new byte[4 * 1024];
        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
        DatagramSocket inSocket = new DatagramSocket(inSocketAddress);
        DatagramSocket outSocket = new DatagramSocket();
        outSocket.send(round1Packet());
        inSocket.receive(inPacket);
        processRound2(inPacket);
        outSocket.send(round3Packet());

    }

    private void processRound2(DatagramPacket inPacket) {

    }


    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }


    private DatagramPacket round1Packet() throws IOException {
        byte[] opaqueTicket = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        File file = new File("pt/unl/fct/srsc/common/config.properties");
//        dos.writeBytes("ip=localhost\n");
//        dos.writeBytes("port=9999\n");
//        dos.write(Files.readAllBytes(file.toPath()));
        dos.write(opaqueTicket);
        long nonce = random.nextLong();
        expectedNonce = nonce++;
        dos.writeLong(nonce);

        DatagramPacket datagramPacket = makeSRTSPPacket(baos.toByteArray(), 1);

        dos.close();
        baos.close();
        return datagramPacket;
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
