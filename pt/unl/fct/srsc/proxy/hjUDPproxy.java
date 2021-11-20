package pt.unl.fct.srsc.proxy;

/* hjUDPproxy, 20/Mar/18
 *
 * This is a very simple (transparent) UDP proxy
 * The proxy can listening on a remote source (server) UDP sender
 * and transparently forward received datagram packets in the
 * delivering endpoint
 *
 * Possible Remote listening endpoints:
 *    Unicast IP address and port: configurable in the file config.properties
 *    Multicast IP address and port: configurable in the code
 *
 * Possible local listening endpoints:
 *    Unicast IP address and port
 *    Multicast IP address and port
 *       Both configurable in the file config.properties
 */

import com.sun.xml.internal.ws.api.ha.StickyFeature;
import pt.unl.fct.srsc.common.SecureDatagramSocket;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

class hjUDPproxy {


    private static void startSADKDP(String sigserverAddr) {
        String[] addr = sigserverAddr.split(":");

        try (Socket socket = new Socket(addr[0], Integer.parseInt(addr[1]))) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            //TODO: request movie code

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        InputStream inputStream = new FileInputStream("pt/unl/fct/srsc/proxy/config.properties");
        // Always null
//        if (inputStream == null) {
//            System.err.println("Configuration file not found!");
//            System.exit(1);
//        }

        Properties properties = new Properties();
        properties.load(inputStream);
        String remote = properties.getProperty("remote");
        String destinations = properties.getProperty("localdelivery");



        SocketAddress inSocketAddress = parseSocketAddress(remote);
        Set<SocketAddress> outSocketAddressSet = Arrays.stream(destinations.split(",")).map(
                hjUDPproxy::parseSocketAddress).collect(Collectors.toSet());

        // Changed HERE
        SecureDatagramSocket inSocket = new SecureDatagramSocket(inSocketAddress);
        DatagramSocket outSocket = new DatagramSocket();
        byte[] buffer = new byte[4 * 1024];

        while (true) {
            DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
            inSocket.receive(inPacket);  // if remote is unicast

            System.out.print("*");
            for (SocketAddress outSocketAddress : outSocketAddressSet) {
                // Changed HERE
                outSocket.send(new DatagramPacket(inPacket.getData(), inPacket.getLength(), outSocketAddress));
            }
        }
    }

    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }
}
