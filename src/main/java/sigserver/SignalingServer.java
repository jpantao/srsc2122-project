package sigserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sapkdp.ServerSAPKDP;
import common.Utils;


import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;


public class SignalingServer {

    public static final String CONFIG_FILE = "config/sigserver.properties";
    public static final String KEYSTORE_FILE = "keystores/signalingserver.keystore";
    public static final char[] KEYSTORE_PASS = "srsc2122".toCharArray();

    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);
    private static JsonObject users, movies;

    private static  Properties properties;
    private static ServerSAPKDP serverSAPKDP;

    static {
        try {
            Utils.loadBC();
            users = JsonParser.parseReader(new FileReader("resources/users.json")).getAsJsonObject();
            movies = JsonParser.parseReader(new FileReader("resources/movies.json")).getAsJsonObject();
            Utils.loadBC();
            properties = Utils.loadConfig(CONFIG_FILE);
            serverSAPKDP = ServerSAPKDP.getInstance();
            serverSAPKDP.load(KEYSTORE_FILE, KEYSTORE_PASS, users, movies);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException, UnrecoverableKeyException, CertificateException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Integer.parseInt(properties.getProperty("port"));

        String ksName = "keystores/serverkeystore";
        char[]  ksPass = "hjhjhjhj".toCharArray();   // password da keystore
        char[]  ctPass = "hjhjhjhj".toCharArray();
        String[] confciphersuites={"TLS_RSA_WITH_AES_256_CBC_SHA256"};
        String[] confprotocols={"TLSv1.2"};
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(ksName), ksPass);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, ctPass);
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(kmf.getKeyManagers(), null, null);
        SSLServerSocketFactory ssf = sc.getServerSocketFactory();







        try (SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(port)) {
            // server is listening on port 1234
            s.setReuseAddress(true);
            s.setEnabledProtocols(confprotocols);
            s.setEnabledCipherSuites(confciphersuites);

            while (true) {
                // listen for connections
                SSLSocket client = (SSLSocket) s.accept();
                System.out.println("New client connected: " + client.getInetAddress().getHostAddress());

                // launch handler
                new Thread(() -> serverSAPKDP.handleHandshake(client)).start();
//                break; // only handle 1 for now
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
