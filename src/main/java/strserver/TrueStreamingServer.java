package strserver;

/*
 * hjStreamServer.java
 * Streaming server: streams video frames in UDP packets
 * for clients to play in real time the transmitted movies
 */

import common.Utils;
import srtsp.ServerSRTSP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Properties;

class TrueStreamingServer {

    public static final String CONFIG_FILE = "config/strserver.properties";
    public static final int BUF_SIZE = 4096;

    private static final Properties properties = Utils.loadConfig(CONFIG_FILE);
    private static ServerSRTSP serverSRTSP;

    static public void main(String[] args) throws Exception {



        int servicePort = Integer.parseInt(properties.getProperty("port"));

        serverSRTSP = ServerSRTSP.getInstance();

        byte[] buf = new byte[BUF_SIZE];
        DatagramSocket socket = new DatagramSocket(servicePort);

        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet); // packet should contain request and credentials



        }

        // sending movie frames
//        int size;
//        int count = 0;
//        long time;
//        DataInputStream g = new DataInputStream(new FileInputStream(args[0]));
//        byte[] buff = new byte[4096];
//
//        SecureDatagramSocket s = new SecureDatagramSocket();
//        InetSocketAddress addr = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
//        DatagramPacket p = new DatagramPacket(buff, buff.length, addr);
//        long t0 = System.nanoTime(); // tempo de referencia para este processo
//        long q0 = 0;
//
//        while (g.available() > 0) {
//            size = g.readShort();
//            time = g.readLong();
//            if (count == 0) q0 = time; // tempo de referencia no stream
//            count += 1;
//            g.readFully(buff, 0, size);
//            p.setData(buff, 0, size);
//            p.setSocketAddress(addr);
//            long t = System.nanoTime();
//            Thread.sleep(Math.max(0, ((time - q0) - (t - t0)) / 1000000));
//
//            // send packet (with a frame payload)
//            // Frames sent in clear (no encryption)
//            s.send(p);
//            System.out.print(".");
//        }
//
//        System.out.println("DONE! all frames sent: " + count);
    }

}
