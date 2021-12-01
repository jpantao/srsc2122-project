package streamingserver;

import common.Utils;

import java.io.*;
import java.net.*;
import java.util.Properties;


public class StreamingServerHandshake {


        public static final String CONFIG_FILE = "config/srtsp.properties";

        private final Properties properties;

        private final SocketAddress inSocketAddress;
//        private final SocketAddress outSocketAddress;
        private static final int VERSION = 1;

    public StreamingServerHandshake(int port) throws IOException {
        properties = Utils.loadConfig(CONFIG_FILE);

        String inAddress = InetAddress.getLocalHost().getHostAddress() + ":" + port;
        inSocketAddress = parseSocketAddress(inAddress);
    }

    public void go() throws IOException {
        DatagramSocket inSocket = new DatagramSocket(inSocketAddress);
        DatagramSocket outSocket = new DatagramSocket();
        byte[] inBuffer = new byte[4 * 1024];
        DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
        System.out.println("Listening....");


        //TODO: (round 1)
        inSocket.receive(inPacket);
        processRound1(inPacket);

        //TODO: (round 2)
//        outSocket.send(round2Packet());

        //TODO: (round 3)
//        inSocket.receive(inPacket);
//        processRound3(inPacket);

        //TODO: (round 4)
    }

    private void processRound3(DatagramPacket inPacket) {

    }

    private void processRound1(DatagramPacket inPacket) {
        System.out.println(Utils.encodeHexString(inPacket.getData()));
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
