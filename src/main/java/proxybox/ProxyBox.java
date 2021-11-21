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
import messages.MessageSAPKDP;
import messages.PBHello;
import messages.SSAuthenticationRequest;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

class ProxyBox {
    private static String username;
    private static String password;
    private static String keystore;
    private static String proxyinfo;

    private static String proxyUID;
    private static String strserver;
    private static String sigserver;
    private static String mpegplayers;

    private static void loadConfig() {
        try {
            InputStream inputStream = new FileInputStream(proxyinfo);
            Properties properties = new Properties();
            properties.load(inputStream);
            strserver = properties.getProperty("strserver");
            mpegplayers = properties.getProperty("mpegplayers");
            sigserver = properties.getProperty("sigserver");
            proxyUID = properties.getProperty("proxyUID");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void execSAPKDP(String movie) {
        String[] addr = sigserver.split(":");

        try (Socket socket = new Socket(addr[0], Integer.parseInt(addr[1]))) {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            MessageSAPKDP msg;

            //(round 1) send PB-Hello
            out.writeObject(new PBHello(username, proxyUID));

            //(round 2) receive SS-AuthenticationRequest
            msg = (MessageSAPKDP) in.readObject();
            if (msg.getMsgType() != SSAuthenticationRequest.MSG_TYPE) {
                //TODO: handle error
                socket.close();
                return;
            }
            SSAuthenticationRequest ssAuthenticationRequest = (SSAuthenticationRequest) msg;


            //TODO: (round 3) PB-Authentication
            //TODO: (round 5) PB-Payment

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static void argparse(String[] args) {
        for (int i = 0; i < args.length; i++)
            switch (args[i]) {
                case "-user":
                    username = args[++i];
                    break;
                case "-password":
                    password = args[++i];
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
        loadConfig();

        // SAPKDP
        execSAPKDP("cars.dat");

        // SRTSP


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
