package proxybox;

import common.SecureDatagramSocket;
import common.Utils;
import sigserver.sapkdp.ProtoSAPKDP;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

class ProxyBox {

    public static final String KEYSTORE = "resources/this.keystore";
    public static final String PROXYBOX_KEYALIAS = "proxybox";
    public static final String SIGSERVER_KEYALIAS = "signalingserver";

    public static final char[] KEYSTORE_PASS = "srsc2122".toCharArray();


    private static String username;
    private static String password;
    private static String keystore;
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
                default:
                    System.err.println("Unknown option");
            }
    }


    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }

    public static void main(String[] args) throws Exception {
        argparse(args);

        InputStream inputStream = new FileInputStream(proxyinfo);
        Properties properties = new Properties();
        properties.load(inputStream);
        String strserver = properties.getProperty("strserver");
        String mpegplayers = properties.getProperty("mpegplayers");
        String sigserver = properties.getProperty("sigserver");
        String proxyBoxID = properties.getProperty("proxyBoxID");

        // SAPKDP
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream(KEYSTORE), KEYSTORE_PASS);

        //TODO: use truststores instead of having all the keypairs in on keystores
        PrivateKey prvkey = (PrivateKey) ks.getKey(PROXYBOX_KEYALIAS, KEYSTORE_PASS);
        PublicKey pubkey = ks.getCertificate(SIGSERVER_KEYALIAS).getPublicKey();

        ProtoSAPKDP sapkdp = new ProtoSAPKDP(proxyBoxID, username, password, sigserver, prvkey, pubkey);
        sapkdp.handshake("cars");

        //TODO: SRTSP


        SocketAddress inSocketAddress = parseSocketAddress(strserver);
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
