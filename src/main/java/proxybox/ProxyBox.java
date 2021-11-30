package proxybox;

import common.SecureDatagramSocket;
import common.Utils;
import sapkdp.ClientSAPKDP;
import sapkdp.messages.PlainTicketCreds;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
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
                        password = Utils.toHex(md.digest(args[++i].getBytes(StandardCharsets.UTF_8)));
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


    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }

    public static void main(String[] args) throws Exception {
        argparse(args);

        Properties properties = Utils.loadConfig(proxyinfo);
        int strserverPort = Integer.parseInt(properties.getProperty("strserverPort"));
        String mpegplayers = properties.getProperty("mpegplayers");
        String sigserverAddr = properties.getProperty("sigserverAddr");
        String proxyBoxID = properties.getProperty("proxyBoxID");

        //TODO: SAPKDP
        ClientSAPKDP clientSAPKDP = new ClientSAPKDP(proxyBoxID, username, keystore, storepass, password, sigserverAddr);
        clientSAPKDP.handshake("cars", "resources/coin_3040021e1fa718b.voucher");

        PlainTicketCreds ticket = clientSAPKDP.getClientTicket();
        byte[] rtssCipherTicket = clientSAPKDP.getRtssCipherTicket();

        //TODO: SRTSP


        SocketAddress inSocketAddress = parseSocketAddress("TODO" + ":" + strserverPort);
        Set<SocketAddress> outSocketAddressSet = Arrays.stream(mpegplayers.split(","))
                .map(ProxyBox::parseSocketAddress)
                .collect(Collectors.toSet());

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
}
