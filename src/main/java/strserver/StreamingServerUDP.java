package strserver;

/*
 * hjStreamServer.java
 * Streaming server: streams video frames in UDP packets
 * for clients to play in real time the transmitted movies
 */

import common.DTLSSocket;
import common.SecureDatagramSocket;
import common.Utils;

import java.io.*;
import java.net.*;
import java.util.Properties;

class StreamingServerUDP {

    public static final String CONFIG_FILE = "config/strserver.properties";
    public static final String MOVIES_DIR = "movies";
    public static final String KEYSTORE_FILE = "keystores/streamingserver.keystore";
    public static final char[] KEYSTORE_PASS = "srsc2122".toCharArray();


    static public void main(String[] args) throws Exception {
//        if (args.length != 3) {
//            System.out.println("Erro, usar: mySend <movie> <ip-multicast-address> <port>");
//            System.out.println("        or: mySend <movie> <ip-unicast-address> <port>");
//            System.exit(-1);
//        }

        Properties tlsConfigs = Utils.loadConfig("config/streamingserver_dtls.properties");
        Properties properties = Utils.loadConfig(CONFIG_FILE);
        int servicePort = Integer.parseInt(properties.getProperty("port"));

        SecureDatagramSocket s = Utils.secureDatagramSocketWithReusableAddress(tlsConfigs, servicePort, true);
        ((DTLSSocket) s).beginHandshake(new InetSocketAddress(10000));

        StreamingServerHandshake handShake = new StreamingServerHandshake(KEYSTORE_FILE, KEYSTORE_PASS, servicePort, s);
        handShake.go();

        int size;
        int count = 0;
        long time;
        DataInputStream g = new DataInputStream(new FileInputStream(MOVIES_DIR + "/" + handShake.getMovieID() + ".dat"));
        byte[] buff = new byte[4096];

        InetSocketAddress addr = new InetSocketAddress(handShake.getIp(), handShake.getPort());
        DatagramPacket p = new DatagramPacket(buff, buff.length, addr);
        long t0 = System.nanoTime(); // tempo de referencia para este processo
        long q0 = 0;

        while (g.available() > 0) {
            size = g.readShort();
            time = g.readLong();
            if (count == 0) q0 = time; // tempo de referencia no stream
            count += 1;
            g.readFully(buff, 0, size);
            p.setData(buff, 0, size);
            p.setSocketAddress(addr);
            long t = System.nanoTime();
            Thread.sleep(Math.max(0, ((time - q0) - (t - t0)) / 1000000));

            // send packet (with a frame payload)
            // Frames sent in clear (no encryption)
            s.send(p);
            System.out.print(".");
        }

        System.out.println("DONE! all frames sent: " + count);
    }

}