package pt.unl.fct.srsc.proxy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Properties;

public class Handshake {
    private final InetSocketAddress inSocketAddress;
    private final InetSocketAddress outSocketAddress;

    public Handshake() throws IOException {
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
        byte[] buff = new byte[4 * 1024];
        DatagramPacket outPacket = new DatagramPacket(buff, buff.length, outSocketAddress );
        DatagramSocket inSocket = new DatagramSocket(inSocketAddress);
        DatagramSocket outSocket = new DatagramSocket();
        outPacket.setData(new byte[] {0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00 });
        outSocket.send(outPacket);

    }

    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }
}
