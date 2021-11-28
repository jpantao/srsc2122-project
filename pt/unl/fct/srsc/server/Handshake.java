package pt.unl.fct.srsc.server;

import pt.unl.fct.srsc.common.SecureDatagramSocket;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Properties;


public class Handshake {
        private final SocketAddress inSocketAddress;
        private final SocketAddress outSocketAddress;

    public Handshake() throws IOException {
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
        byte[] buffer = new byte[4 * 1024];







        while (true) {
            DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
            System.out.println("Listening....");
            inSocket.receive(inPacket);  // if remote is unicast


            outSocket.send(new DatagramPacket(inPacket.getData(), inPacket.getLength(), outSocketAddress));
        }
    }


    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }
}
