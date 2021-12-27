/*
 * hjStreamServer.java
 * Streaming server: streams video frames in UDP packets
 * SRTSP Protocol tunneled on a DTLS channel
 * for clients to play in real time the transmitted movies
 */

// See How the Streamserver can use the procided DTLSSockets ...
import java.io.*;
import java.net.*;
import java.util.Properties;

class hjStreamServer {

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.out.println("Erro, usar: myStreamServer <movie> <SRTSPConfig> <DTLSConfig> <ServerConfig>");
            System.exit(-1);
        }

	// Deal with required configurations
	
        Properties config = new Properties();
        config.load(new FileInputStream(args[4]));
        SocketAddress inSocketAddress = parseSocketAddress(config.getProperty("selfaddress"));
        SocketAddress outSocketAddressSet = parseSocketAddress(config.getProperty("destaddress"));

	// SRTSP Protocol configurations ...
        Properties srtspConfig = new Properties();
        srtspConfig.load(new FileInputStream(args[2]));
        SRTSPProtocol srtsp = new SRTSPProtocol(srtspConfig); 

	// DTLS configurations ...
        Properties dtlsConfig = new Properties();
        dtlsConfig.load(new FileInputStream(args[3]));



	// Now the remaining stuff of original StreamServer typical code ...
	// as you already have in your previous PA#1 ... 
	
        int size;
        int count = 0;
        long time;
        DataInputStream g = new DataInputStream(new FileInputStream(args[0]));
        byte[] buff = new byte[4096];

	// Ok the new stuff for DTLSSokets
        DTLSSocket outSocket = new DTLSSocket(config, dtlsConfig, true, inSocketAddress);
        outSocket.beginHandshake(outSocketAddressSet);

	// and now ... I send transparenty my Datagram Packets ...

        DatagramPacket p = new DatagramPacket(buff, buff.length);
	
        long t0 = System.nanoTime(); // tempo de referencia para este processo
        long q0 = 0;

        while (g.available() > 0) {
            size = g.readShort();
            time = g.readLong();
            if (count == 0) q0 = time;
            count += 1;
            g.readFully(buff, 0, size);
            p.setData(buff, 0, size);
            p.setSocketAddress(outSocketAddressSet);
            long t = System.nanoTime();
            Thread.sleep(Math.max(0, ((time - q0) - (t - t0)) / 1000000));
            outSocket.send(p,ssp);
            System.out.print(".");
        }
        System.out.println("DONE! all frames sent: " + count);
    }

    private static InetSocketAddress parseSocketAddress(String socketAddress) {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }
}
