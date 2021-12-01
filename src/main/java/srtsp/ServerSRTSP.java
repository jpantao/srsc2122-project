package srtsp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ServerSRTSP {

    public static final int VERSION = 1;
    public static final String CONFIG_FILE = "config/srtsp.properties";
    private static final int BUF_SIZE = 4096;

    private static ServerSRTSP instance;

    public static ServerSRTSP getInstance() {
        if(instance == null)
            instance = new ServerSRTSP();
        return instance;
    }

    public ServerSRTSP() {
    }

    public void handleHandshake(int port){
        try {
            DatagramSocket socket = new DatagramSocket(port);
            byte[] buf = new byte[BUF_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            InetAddress clientAddr = packet.getAddress();
            int clientPort = packet.getPort();









        } catch (SocketException e) {
            e.printStackTrace();
        }

    }





}
