package proxybox;

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

import common.SecureDatagramSocket;
import common.Utils;
import sapkdp.ClientSAPKDP;
import sapkdp.messages.PlainTicketCreds;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

class ProxyBox {


    public static final char[] KEYSTORE_PASS = "srsc2122".toCharArray();

    private static String username;
    private static String password;
    private static String keystore;
    private static char[] storepass;
    private static String proxyinfo;


    private static void argparse(String[] args) {
        for (int i = 0; i < args.length; i++)
            switch (args[i]) {
                case "-user":
                    username = args[++i];
                    break;
                case "-password":
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA-512");
                        password = Utils.toHex(md.digest(args[++i].getBytes()));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    break;
                case "-keystore":
                    keystore = args[++i];
                    break;
                case "-proxyinfo":
                    proxyinfo = args[++i];
                    break;
                case "-storepass":
                    storepass = args[++i].toCharArray();
                    break;
                default:
                    System.err.println("Unknown option");
            }
        if (storepass == null)
            storepass = KEYSTORE_PASS;

    }

    public static void main(String[] args) throws Exception {
        argparse(args);

        Properties properties = Utils.loadConfig(proxyinfo);
        String mpegplayers = properties.getProperty("mpegplayers");
        String sigserver = properties.getProperty("sigserverAddr");
        String proxyBoxID = properties.getProperty("proxyBoxID");
        int strserverPort = Integer.parseInt(properties.getProperty("strserverPort"));

        // SAPKDP
        ClientSAPKDP client = new ClientSAPKDP(proxyBoxID, username, keystore, storepass, password, sigserver);
        client.handshake("cars", "resources/coin_3040021e1fa718b.voucher");

        PlainTicketCreds ticket = client.getClientTicket();
        byte[] rtssCipherTicket = client.getRtssCipherTicket();
        byte[] payloads = client.getPayloads();
        byte[] sigBytes = client.getSigBytes();

        String streamingAddr = ticket.getIp() + ":" + strserverPort;
        ProxyHandshake handShake = new ProxyHandshake(streamingAddr, ticket.getClientPort());
        handShake.start(rtssCipherTicket, sigBytes);

        SocketAddress inSocketAddress = parseSocketAddress(ticket.getIp() + ":" + strserverPort);
        Set<SocketAddress> outSocketAddressSet = Arrays.stream(mpegplayers.split(",")).map(
                ProxyBox::parseSocketAddress).collect(Collectors.toSet());

        SecureDatagramSocket inSocket = new SecureDatagramSocket(inSocketAddress);
        DatagramSocket outSocket = new DatagramSocket();
        byte[] buffer = new byte[4 * 1024];

        while (true) {
            DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
            inSocket.receive(inPacket);  // if remote is unicast

            System.out.print("*");
            for (SocketAddress outSocketAddress : outSocketAddressSet) {
                // Changed HERE
                if (inPacket.getLength() > 0)
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
