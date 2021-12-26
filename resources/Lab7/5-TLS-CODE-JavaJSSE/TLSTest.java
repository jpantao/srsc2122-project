import java.util.Arrays;
import java.util.Base64;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.*;
import javax.net.SocketFactory;
import javax.net.ssl.*;

public class TLSTest
{
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: TLSTest host port");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        SocketFactory factory = SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
       
        System.out.println("\nMy supported TLS protocols .....");
        System.out.println("--------------------------------");       
        for (String i: socket.getSupportedProtocols()) {
	   System.out.println(i);
	}
       
        System.out.println("\nMy Enabled TLS protocols .....");
        System.out.println("--------------------------------");       
        for (String i: socket.getEnabledProtocols()) {
	   System.out.println(i);
	}

        System.out.println("\nMy supported TLS ciphersuites .....");
        System.out.println("--------------------------------");       
        for (String i: socket.getSupportedCipherSuites()) {
        System.out.println(i);
	}

        System.out.println("\nMy Enabled TLS ciphersuites .....");
        System.out.println("--------------------------------");       
        for (String i: socket.getEnabledCipherSuites()) {	     
        System.out.println(i);
	}
	   
       
        // You can set what you want for the TLS session establishment
	// in the TLS handshake you will start
	// See: https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLSocket.html

	// Can also have the security association parameters
 	// SSLParameters sslsessionparams= socket.getSSLParameters();	 

	   
        socket.startHandshake();

        // Now you can retrieve the handshaked session context
	SSLSession sslsession= socket.getHandshakeSession();
        // You ca also retrieve the  handshaked session established


	  
	// Here are the obtained certificates / certification chain 
	// The idea is now to validate for the proper needs
        Certificate[] certs = socket.getSession().getPeerCertificates();
        String speerhost= socket.getSession().getPeerHost();
        String sprotocol= socket.getSession().getProtocol();
        String sciphersuite= socket.getSession().getCipherSuite();
        byte[] sessionid = socket.getSession().getId();
        byte[] sessionidb = Base64.getEncoder().encode(sessionid);
        String base64sid= new String(sessionidb);

        System.out.println("Certs retrieved: " + certs.length);
        for (Certificate cert : certs) {
            System.out.println("Certificate is: " + cert);
            if(cert instanceof X509Certificate) {
                try {
                    ( (X509Certificate) cert).checkValidity();
                    System.out.println("Certificate is active for current date");
                } catch(CertificateExpiredException cee) {
                    System.out.println("Certificate is expired");
		}

       	System.out.println("Established Peer Host: " + speerhost);
	System.out.println("Established Protocol: " + sprotocol);	       	       
        System.out.println("Established Ciphersuite: " + sciphersuite);
        System.out.println("Established SessionID: " + base64sid);
	       
            }
        }
    }
}