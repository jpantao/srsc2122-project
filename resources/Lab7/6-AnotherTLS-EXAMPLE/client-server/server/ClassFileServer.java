import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

/* ClassFileServer.java --  a simple HTTP GET request file
 * supporting HTTP or HTTP/SSL (one-way or mutual authentication)
 */

public class ClassFileServer extends ClassServer {

    private String docroot;
   // the default port for the server. 
   // We also can pass a port as an argument - see below
    private static int DefaultServerPort = 2001; 

    /**
     * Constructs a ClassFileServer.
     *
     * @param path : path where the server reads the files
     *               modeling a doc (root) directory in the server side
     */
    public ClassFileServer(ServerSocket ss, String docroot) throws IOException
    {
	super(ss);
	this.docroot = docroot;
    }

    /**
     * getBytes() returns the file as an array of bytes
     * the file name is represented by the argument <b>path</b>.
     *
     * @return the bytes for the file
     * @exception FileNotFoundException if the file is not found in
     * <b>path</b> and can not be loaded or readable(ex., permission problems).
     */
    public byte[] getBytes(String path)
	throws IOException
    {
	System.out.println("file asked by the client to send: " + path);
	File f = new File(docroot + File.separator + path);
	int length = (int)(f.length());
	if (length == 0) {
	    throw new IOException("File length is zero: " + path);
	} else {
	    FileInputStream fin = new FileInputStream(f);
	    DataInputStream in = new DataInputStream(fin);

	    byte[] bytecodes = new byte[length];
	    in.readFully(bytecodes);
	    return bytecodes;
	}
    }

    /**
     * Main method 
     * It creates the class server
     * It takes two command line arguments, the
     * port on which the server accepts requests and the
     * root of the path. To start up the server: <br><br>
     *
     * <code>   java ClassFileServer <port> <path>
     * </code><br><br> or add -Djavax.net.ssl.trustStore=servertruststore
     * for authenticated clientes when client authentication is wanted or
     * required
     *
     * <code>   new ClassFileServer(port, docroot);
     * </code>
     */
    public static void main(String args[])
    {
	System.out.println(
	    "USAGE: java ClassFileServer port docroot [TLS [true]]\n" +
	    "or, in case of client authentication required ...\n" +
       	    "USAGE: java [-Djavax.net.ssl.trustStore=servertruststore] ClassFileServer port docroot [TLS [true]]");
	System.out.println("");
	System.out.println(
	    "------------------------------------------------\n"+
	    "If the third argument is TLS, it will start as\n" +
	    "a TLS/SSL file server. Otherwise, it will be\n" +
	    "an ordinary HTTP (not secure) file server. \n" +
	    "If the fourth argument is true,it will require\n" +
	    "client authentication as well, implementing \n" +
            "a mutual authentication process.\n" +
            "In the case of mutual authentication\n" +
            "it is necessary to control the trustability of the\n" +
	    "certificate sent by potential clients.\n" +
	    "To do this, it is necessary to import \n" +
            "the necessary trust certificates to servertruststore\n" +
	    "a repository of trusted certificates accepted \n"+
	    "by this server.\n" +
            "In this case it is necessary to pass the appropriate\n" +
            "servertruststore to the server, when starting the server\n" +
	    "------------------------------------------------"	    
	     ); 

	int port = DefaultServerPort; // Default port if no arguments
	String docroot = "";          // Default doc dir if no arguments

	if (args.length >= 1) {
	    port = Integer.parseInt(args[0]); // Port if not default
	}

	if (args.length >= 2) {
	    docroot = args[1];  // root of doc dir, for requesting
	}
	String type = "PlainSocket";
	if (args.length >= 3) {
	    type = args[2];
	}
	try {
            // Create a Socket Factory of type mentioned
            // Plain Socket ... or SSL Socket and creates
            // the server socket in the specified port

            // See the getServerSocketFactory code below

	    ServerSocketFactory ssf =
		ClassFileServer.getServerSocketFactory(type);
	    ServerSocket ss = ssf.createServerSocket(port);


	    // This server only enables TLSv1.2
	    // and the cipher suite below
	    ((SSLServerSocket)ss).setEnabledProtocols(new String[] { "TLSv1.2"});
	    ((SSLServerSocket)ss).setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_GCM_SHA256"});

	    if (args.length >= 4 && args[3].equals("true")) {

            // In this case, mutual authentication will be used
            // so, client authentication will be required		

	    ((SSLServerSocket)ss).setNeedClientAuth(true);

	    }
	    new ClassFileServer(ss, docroot);
	} catch (IOException e) {
	    System.out.println("Problem with sockets: unable to start ClassServer: " +  e.getMessage());
	    e.printStackTrace();
	}
    }

    private static ServerSocketFactory getServerSocketFactory(String type) {
	if (type.equals("TLS")) {
	    SSLServerSocketFactory ssf = null;
	    try {
		// set up key manager to do server authentication
		SSLContext ctx;
		KeyManagerFactory kmf;
		KeyStore ks;
                
                // Depending on the passphrase used to protect the
                // serverkeystore used. Remember, this is the keystore
                // where the server stores its keys as generate ...
		// See the keytool for details
		// The keystore created with the required password	

		char[] kspwd = "hjhjhjhj".toCharArray();
		char[] ksepwd = "hjhjhjhj".toCharArray();
		
		ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream("serverkeystore"), kspwd);
		kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, ksepwd);
		ctx = SSLContext.getInstance("TLS");
		ctx.init(kmf.getKeyManagers(), null, null);
          
                // only to deal with the keystore internal representation
                // you have KeyStore class with a lot of methods - see
                // the Java documentation.
                // Ex., KeyStore.getKey() takes a string corresponding to
                // an alias name and a char array representing a password
                // protecting the entry of the keystore, returning
                // java.security.Key object 

	        ssf = ctx.getServerSocketFactory();
		return ssf;
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    return ServerSocketFactory.getDefault();
	}
	return null;
    }
}
