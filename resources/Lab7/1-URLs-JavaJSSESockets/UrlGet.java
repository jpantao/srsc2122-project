import java.net.*;
import java.io.*;

/** Read a remote file using the standard URL class
 *  instead of connecting explicitly to the HTTP server.
 *  Trust issues:
 *  Why the client will validate the certification chain of a
 *  generic HTTPS server somewhere in the Internet ... ?
 *  Example: Check with different servers and see also the
 *  the certification chain sent fro the server with:
 *  - Your preferred browser
 *  - Can also use openssl s_client -connect host:port
 */

public class UrlGet {
    public static void main(String[] args) {
	checkUsage(args); // in this case we use the URL as args[0]
	try {
	    URL url = new URL(args[0]);

	    BufferedReader in = 
		new BufferedReader(new InputStreamReader(url.openStream()));
	    String line;
	   
	   
            System.out.println("Obtained Answer:");
            System.out.println("----------------");
	    while ((line = in.readLine()) != null) {
		System.out.println("> " + line);
	    }
	    in.close();
	} catch(MalformedURLException mue) { // URL constructor
	    System.out.println(args[0] + "is an invalid URL: " + mue);
	} catch(IOException ioe) { // Stream constructors
	    System.out.println("IOException: " + ioe);
	}
    }
   
   
    private static void checkUsage(String[] args) {
	if (args.length != 1) {
	    System.out.println("Usage: UrlGet <URL>");
	    System.exit(-1);
	}

    }

}

