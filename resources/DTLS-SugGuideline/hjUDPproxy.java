/* This is a very simple (transparent) UDP proxy
 * The proxy can listening on a remote source (server) UDP sender
 * and transparently forward received datagram packets in the
 * delivering endpoint
 *
 * Possible Remote listening endpoints:
 *    Unicast IP address and port: configurable in the file proxy.properties
 *    Multicast IP address and port: configurable in the code
 *
 * Possible local listening endpoints:
 *    Unicast IP address and port
 *    Multicast IP address and port
 *       Both configurable in the file proxy.properties
 */

import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

class hjUDPproxy {

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Erro, usar: myProxy -f <SRTSPConfig> <DTLSConfig> <ProxyConfigs>");
            System.exit(-1);
        }

	// Required confugurations ...

        Properties config = new Properties();
        config.load(new FileInputStream(args[3]));
        SocketAddress inSocketAddress = parseSocketAddress(config.getProperty("self"));
        SocketAddress outSocketAddress = parseSocketAddress(config.getProperty("destinations"));
        Set<SocketAddress> localOutSocketAddressSet = Arrays.stream(config.getProperty("localdelivery").split(",")).map(s -> parseSocketAddress(s)).collect(Collectors.toSet());

        Properties sspConfig = new Properties();
        sspConfig.load(new FileInputStream(args[1]));
        SSPProtocol ssp = new SSPProtocol(sspConfig);

        Properties dtlsConfig = new Properties();
        dtlsConfig.load(new FileInputStream(args[2]));

	// DTLS Socket to receive from the StreamServer

        DTLSSocket inSocket = new DTLSSocket(config, dtlsConfig, false, inSocketAddress);
        inSocket.beginHandshake(outSocketAddress);

	// Datagram Socket to resend to my MediaPlayer
	
        DatagramSocket outSocket = new DatagramSocket();
        byte[] buffer = new byte[4 * 1024];

        while (true) {
            DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
            inSocket.receive(inPacket,ssp);  // if remote is unicast
            System.out.print("*");
            for (SocketAddress address : localOutSocketAddressSet) {
                outSocket.send(new DatagramPacket(buffer, inPacket.getLength(), address));
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
