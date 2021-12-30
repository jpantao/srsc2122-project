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
    public static final String TLS_CONFIG_FILE = "config/signalingserver_tls.properties";
    public static final String KEYSTORE_FILE = "keystores/signalingserver.keystore";
    public static final char[] KEYSTORE_PASS = "srsc2122".toCharArray();

    private static final AtomicInteger NONCE_COUNTER = new AtomicInteger(1);
    private static JsonObject users, movies;

    private static  Properties properties;
    private static  Properties tlsProperties;
    private static ServerSAPKDP serverSAPKDP;

    static {
        try {
            Utils.loadBC();
            users = JsonParser.parseReader(new FileReader("resources/users.json")).getAsJsonObject();
            movies = JsonParser.parseReader(new FileReader("resources/movies.json")).getAsJsonObject();
            Utils.loadBC();

            properties = Utils.loadConfig(CONFIG_FILE);
            tlsProperties = Utils.loadConfig(TLS_CONFIG_FILE);
            serverSAPKDP = ServerSAPKDP.getInstance();
            serverSAPKDP.load(KEYSTORE_FILE, KEYSTORE_PASS, users, movies);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException, UnrecoverableKeyException, CertificateException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Integer.parseInt(properties.getProperty("port"));

        String ksName = "keystores/signalingserver_tls.keystore";
        String tsName = "keystores/truststore";
        char[]  ksPass = "srsc2122".toCharArray();   // password da keystore
        char[]  ctPass = "srsc2122".toCharArray();
        String[] confciphersuites={"TLS_RSA_WITH_AES_256_CBC_SHA256"};
        String[] confprotocols={"TLSv1.2"};


        SSLContext sc = SSLContext.getInstance("TLS");

        KeyStore ksKeys = KeyStore.getInstance("JKS");
        KeyStore ksTrust = KeyStore.getInstance("JKS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

        ksKeys.load(new FileInputStream(ksName), ksPass);
        ksTrust.load(new FileInputStream(tsName), ksPass);
        kmf.init(ksKeys, ctPass);
        tmf.init(ksTrust);
        SecureRandom r = new SecureRandom();
        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), r);


        SSLServerSocketFactory ssf = sc.getServerSocketFactory();

        try (SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(port)) {
            s.setEnabledProtocols(new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"});

            // server is listening on port 1234
            switch (tlsProperties.getProperty("authentication")) {
                case "MUTUAL":
                    s.setUseClientMode(false);
                    s.setNeedClientAuth(true);
                    break;
                case "SIGNALING ONLY":
                    s.setUseClientMode(false);
                    s.setNeedClientAuth(false);
                    break;
                case "PROXY ONLY":
                    s.setUseClientMode(true);
                    break;
            }
            s.setEnabledCipherSuites(tlsProperties.getProperty("ciphersuites").split(","));
            s.setEnabledProtocols(new String [] {tlsProperties.getProperty("tlsversion")});
            s.setReuseAddress(true);
            s.setEnabledProtocols(confprotocols);
            s.setEnabledCipherSuites(confciphersuites);

            while (true) {
                // listen for connections
                SSLSocket client = (SSLSocket) s.accept();
                System.out.println("New client connected: " + client.getInetAddress().getHostAddress());

                // launch handler
//                new Thread(() -> serverSAPKDP.handleHandshake(client)).start();
//                break; // only handle 1 for now
                serverSAPKDP.handleHandshake(client);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
