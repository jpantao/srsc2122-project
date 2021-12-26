

import java.net.*;
import java.io.*;
import javax.net.ssl.*;

import javax.security.cert.*;

/*
 * This example demostrates how to use a SSLSocket as client to
 * send a HTTP request and get response from an HTTPS server.
 * It assumes that the client is not behind a firewall
 */

public class SSLSocketClient {

    public static void main(String[] args) throws Exception {
	try {
	    SSLSocketFactory factory =
		(SSLSocketFactory)SSLSocketFactory.getDefault();
	    SSLSocket socket =
//		(SSLSocket)factory.createSocket("www.verisign.com", 443);
		(SSLSocket)factory.createSocket(args[0], Integer.parseInt(args[1]));

	    /*
	     * send http request
	     *
	     * Before any application data is sent or received, the
	     * SSL socket will do SSL handshaking first to set up
	     * the security attributes.
	     *
	     * SSL handshaking can be initiated by either flushing data
	     * down the pipe, or by starting the handshaking by hand.
	     *
	     * Handshaking is started manually in this example because
	     * PrintWriter catches all IOExceptions (including
	     * SSLExceptions), sets an internal error flag, and then
	     * returns without rethrowing the exception.
	     *
	     * Unfortunately, this means any error messages are lost,
	     * which caused lots of confusion for others using this
	     * code.  The only way to tell there was an error is to call
	     * PrintWriter.checkError().
	     */

            System.out.println("Supported Protocols");
	    System.out.println("---------------------------------------");
       	    String sprotocols[]=socket.getSupportedProtocols();
            for (int i = 0; i < sprotocols.length; i++) 
		System.out.println(sprotocols[i]);
	    System.out.println("---------------------------------------");

            System.out.println("Supported Ciphersuites");
	    System.out.println("---------------------------------------");
       	    String ssuites[]=socket.getSupportedCipherSuites();
            for (int i = 0; i < ssuites.length; i++) 
		System.out.println(ssuites[i]);
	    System.out.println("---------------------------------------");

            System.out.println("Enabled Ciphersuites");
	    System.out.println("---------------------------------------");
       	    String esuites[]=socket.getEnabledCipherSuites();
            for (int i = 0; i < esuites.length; i++) 
		System.out.println(esuites[i]);
	    System.out.println("---------------------------------------");

	    String forceesuites[]={"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"};
	    //	    String forceesuites[]={"TLS_DHE_DSS_WITH_AES_128_GCM_SHA256"};
            socket.setEnabledCipherSuites(forceesuites);

            System.out.println("Enabled Ciphersuites");
	    System.out.println("---------------------------------------");
       	    String nesuites[]=socket.getEnabledCipherSuites();
            for (int i = 0; i < nesuites.length; i++) 
		System.out.println(nesuites[i]);
	    System.out.println("---------------------------------------");

	    socket.startHandshake();

	    PrintWriter out = new PrintWriter(
				  new BufferedWriter(
				  new OutputStreamWriter(
     				  socket.getOutputStream())));

	    out.println("GET /index.html HTTP/1.0");
	    out.println();
	    out.flush();

	    /*
	     * Make sure there were no surprises
	     */
	    if (out.checkError())
		System.out.println(
		    "SSLSocketClient:  java.io.PrintWriter error");

	    /* read response */
	    BufferedReader in = new BufferedReader(
				    new InputStreamReader(
				    socket.getInputStream()));

	    String inputLine;
	    while ((inputLine = in.readLine()) != null)
		System.out.println(inputLine);

	    in.close();
	    out.close();
	    socket.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
