package common;// This is a reference / suggestion or guideline showing the
// principle for an implementation of a DTLS Socket, to be used
// for general purpos DTLS support over Datagram Sockets (UDP)

// You wil need some imports ... not very different than the TLS/TCP case
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.util.Properties; // just because I use configurations as
                             // Java properties in configuration files
import javax.net.ssl.*;
import java.security.*;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.FINISHED;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import javax.net.ssl.SSLEngineResult.HandshakeStatus.*;


public class DTLSSocket extends DatagramSocket {

    private final SSLEngine engine; // The SSLEngine
    // In this case I will take the possibility for different configs
    // of DTLS endpoints ...
    private static final String MUTUAL = "MUTUAL";
    private static final String PROXY = "PROXY ONLY";  //client side
    private static final String SERVER = "STREAMING ONLY"; //server side
    private static final String SSL_CONTEXT = "DTLS";

    public DTLSSocket(Properties certificatesConfig, Properties dtlsConfig, boolean is_server, SocketAddress address) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        super(address); // address for the socket

	// for the following configs, take a look on exemplified
	// dtls congif files ... Possibly you have other intersting
	// config support - jsin, xml or whatever ... would be great ;-)
        String protocol = dtlsConfig.getProperty("TLS-PROT-ENF");
        this.engine = createSSLContext(certificatesConfig).createSSLEngine();
        if (is_server) //server endpoint
            setServerAuth(dtlsConfig.getProperty("TLS-AUTH"));
        else // client endpoint
            setProxyAuth(dtlsConfig.getProperty("TLS-AUTH"));

	// and for both ... In this way I have a common way to
	// have common enabled ciphersuites for sure ...
	// But you can decide to try with different csuites for each side
        // but dont forget ... ou must have something in common
	// The same for protocol versions you want to enable
	
        engine.setEnabledCipherSuites(dtlsConfig.getProperty("CIPHERSUITES").split(","));
        engine.setEnabledProtocols(new String[]{protocol});
    }

    
    // Now let's go to maege the SSL context (w/ SSL Context class)
    // See JSSE Docs and class slides
    
    private SSLContext createSSLContext(Properties config) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {

	// Need a SSLcontext for DTLS (see above)
        SSLContext sslContext = SSLContext.getInstance(SSL_CONTEXT);

	// Keystores and trusted stores that will be used according to
	// the required configurations ...
 	
        KeyStore ksKeys = KeyStore.getInstance("JKS");
        KeyStore ksTrust = KeyStore.getInstance("JKS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

	// Now load the contents from keystores ...
	// they are in config paths used here as arguments
	// also need to express pwds protecting keystores and entries
	//
	// Of course you can also manage this in a difefret way - ex.,
	// passing the keystores etc ... as properties for the JVM runtime
	// as you can see in Lab examples using TLS / TCP (SSLSockets)
	
        ksKeys.load(new FileInputStream(config.getProperty("key_store_path")), config.getProperty("key_store_pass").toCharArray());
        ksTrust.load(new FileInputStream(config.getProperty("trust_store_path")), config.getProperty("trust_store_pass").toCharArray());
        kmf.init(ksKeys, config.getProperty("key_entry_pass").toCharArray());
        tmf.init(ksTrust);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

	// Now I return my "parameterized" sslContext ... 
        return sslContext;
    }

    // Now this is a little trick: depending on the configs, I will say to
    // the DTLS endpoints who is the server and who is the client
    // depending who is the final app code using my DTLSockets
    // If the streamserver wants to be the DTLS server side in the handshake
    // the proxy will be the DTLS client ... or viceversa
    // Note that becausa I want to be able to support also client-only
    // authentication the trick here is to use server-only authentication and
    // invert the roles, with the proxy taking the DTLS server side ;-)

    // see the involved methods in JSSE documentation (SSLEngine class)

    // Ok ... If I am the proxy... 
    private void setProxyAuth(String authType) {
        switch (authType) {
  	    case MUTUAL: // Nothing to do

            case SERVER:
		// I, proxy will be the DTLS client endpoint
                engine.setUseClientMode(true);
                break;
            case PROXY:
		// I, proxy will be the DTLS server endpoint
		// not requiring the server side authentication
		engine.setUseClientMode(false);
                engine.setNeedClientAuth(false);

                break;
        }
    }

    // If I am the streamserver ...
    private void setServerAuth(String authType) {
        switch (authType) {
            case MUTUAL:
		// I streamserver will act as the DTLS server side
                engine.setUseClientMode(false);
		// But will require the client side authentication
                engine.setNeedClientAuth(true);
                break;
            case SERVER:
		// I stream server will be the DTLS server side
                engine.setUseClientMode(false);
		// and will not require the client side authenticatiob
                engine.setNeedClientAuth(false);
                break;
            case PROXY:
		// I streamserver will work as the DTLS client side
                engine.setUseClientMode(true);
                break;
        }
    }

    // Now the remaining is the "coventional" code from the DTLS-enabled
    // handshake ... See the JSSE Documentation ...
    
    private SSLEngineResult.HandshakeStatus runTasks() {
        Runnable runnable;
        while ((runnable = engine.getDelegatedTask()) != null) {
            runnable.run();
        }
        return engine.getHandshakeStatus();
    }

    // unrwap received TLS msg types and contents
    private SSLEngineResult.HandshakeStatus unwrap() throws IOException {
        SSLSession session = engine.getSession();
        ByteBuffer outBuffer = ByteBuffer.allocate(session.getPacketBufferSize());
        super.receive(new DatagramPacket(outBuffer.array(), 0, outBuffer.capacity()));
        return engine.unwrap(outBuffer, ByteBuffer.allocate(session.getApplicationBufferSize())).getHandshakeStatus();
    }

    // wrap TLS msg types and contents
    private SSLEngineResult.HandshakeStatus wrap(SocketAddress address) throws IOException {
        SSLSession session = engine.getSession();
        ByteBuffer outBuffer = ByteBuffer.allocate(session.getPacketBufferSize());
        SSLEngineResult.HandshakeStatus status = engine.wrap(ByteBuffer.allocate(session.getApplicationBufferSize()), outBuffer).getHandshakeStatus();
        super.send(new DatagramPacket(outBuffer.array(), 0, outBuffer.position(), address));
        return status;
    }

    // unrwap if needed again received TLS msg types and contents
    private SSLEngineResult.HandshakeStatus unwrapAgain() throws SSLException {
        SSLSession session = engine.getSession();
        return engine.unwrap(ByteBuffer.allocate(session.getPacketBufferSize()), ByteBuffer.allocate(session.getApplicationBufferSize())).getHandshakeStatus();
    }

    // Begin the TLS hanshake
//    public void beginHandshake(SocketAddress address) throws IOException {
//        engine.beginHandshake();
//        SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
//        while (status != NOT_HANDSHAKING && status != FINISHED) {
//            switch (status) {
//                case NEED_TASK:
//                    status = runTasks();
//                    break;
//                case NEED_WRAP:
//                    status = wrap(address);
//                    break;
//                case NEED_UNWRAP:
//                    status = unwrap();
//                    break;
//                case NEED_UNWRAP_AGAIN:
//                    status = unwrapAgain();
//                    break;
//            }
//        }
//    }

    // Now is up to you ... and your previous protocols you have for
    // tunneling the packets on top of your DTLS/UDP Sockets

    // In the suggestion I can have protocol handlers to manage any
    // protocol I want to encapsulate as tunneled traffic in  my DTLS  Channels
    // So I can have SRTSP or even SAPKDP if implemeneted in Datagra Sockets
    // which is possibky not your case ...
    // My proocolo handlers here are SRTSPProcol class or SAPKDPProtocol class
    
    // ... Anyway you must manage this according to your previous PA#1 implem.
    
    public void send(DatagramPacket packet) throws IOException {
        encrypt(packet);
        super.send(packet);
    }

    public void receive(DatagramPacket packet) throws IOException {
        decrypt(packet);
        super.receive(packet);
    }



    //   What of you want to encrypt a DatagramPacket and send over the
    //   DTLS Engine  (wrap) ... or to receive an encrypted DatagramPacket
    //   from a DTLS Engine (unwrap)


    private void encrypt(DatagramPacket packet) throws SSLException {
        byte[] buffer = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0, buffer, 0, packet.getLength());
        ByteBuffer inBuffer = ByteBuffer.wrap(buffer);
        ByteBuffer outBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        engine.wrap(inBuffer, outBuffer);
        buffer = new byte[outBuffer.position()];
        System.arraycopy(outBuffer.array(), 0, buffer, 0, outBuffer.position());
        packet.setData(buffer, 0, buffer.length);
    }

    private int decrypt(DatagramPacket packet) throws SSLException {
        byte[] buffer = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0, buffer, 0, packet.getLength());
        ByteBuffer inBuffer = ByteBuffer.wrap(buffer);
        ByteBuffer outBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        int bytesProduced = engine.unwrap(inBuffer, outBuffer).bytesProduced();
        if (bytesProduced == 0)
            return 0;
        System.arraycopy(outBuffer.array(), 0, packet.getData(), 0, outBuffer.position());
        packet.setLength(outBuffer.position());
        return bytesProduced;
    }
}
