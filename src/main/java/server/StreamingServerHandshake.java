package server;

import java.io.*;
import java.net.*;
import java.util.Properties;


public class StreamingServerHandshake {
        private final SocketAddress inSocketAddress;
        private final SocketAddress outSocketAddress;
        private static final int VERSION = 1;

    public StreamingServerHandshake() throws IOException {
        InputStream inputStream = new FileInputStream("pt/unl/fct/srsc/proxy/config.properties");
        if (inputStream == null) {
            System.err.println("Configuration file not found!");
            System.exit(1);
        }
        Properties properties = new Properties();
        properties.load(inputStream);
        String inAddress = properties.getProperty("streamingserver");
        String outAddress = properties.getProperty("remote");
        inSocketAddress = parseSocketAddress(inAddress);
        outSocketAddress = parseSocketAddress(outAddress);
    }

    public void go() throws IOException {
        DatagramSocket inSocket = new DatagramSocket(inSocketAddress);
        DatagramSocket outSocket = new DatagramSocket();
        byte[] inBuffer = new byte[4 * 1024];





        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
        System.out.println("Listening....");
        inSocket.receive(inPacket);
        processRound1(inPacket);
        outSocket.send(round2Packet());
        inSocket.receive(inPacket);
        processRound3(inPacket);
    }

    private void processRound3(DatagramPacket inPacket) {

    }

    private void processRound1(DatagramPacket inPacket) {

    }

    private DatagramPacket round2Packet() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);




        DatagramPacket datagramPacket = makePacket(baos.toByteArray(), 2);
        dos.close();
        baos.close();
        return datagramPacket;
    }


    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }

    private DatagramPacket makePacket(byte[] data, int msgType) throws IOException {
        byte[] outBuffer = new byte[4 * 1024];
        DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, outSocketAddress );
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
