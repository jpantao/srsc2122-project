
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

/* ClassFileServer.java -- um file server que pode
 * transferir ficheiros por http
 * Teste para HTTP ou HTTPS
 *
 */

public class ClassFileServer extends ClassServer {

    private String docroot;

    private static int DefaultServerPort = 2001;

    /**
     * ClassFileServer.
     *
     * @param path - path para localizar ficheiros pedidos
     */
    public ClassFileServer(ServerSocket ss, String docroot) throws IOException
    {
	super(ss);
	this.docroot = docroot;
    }

    /**
     * Retorna array de bytes ocntendo os bytes para o
     * ficheiro representado no argumento
     *
     * @return - retorna bytes do ficheiro
     * @exception FileNotFoundException (erro no acesso/leitura do ficheiro)
     */
    public byte[] getBytes(String path)
	throws IOException
    {
        System.out.println("Pedido:");
	System.out.println("GET: " + path);
	File f = new File(docroot + File.separator + path);
	int length = (int)(f.length());
	if (length == 0) {
	    throw new IOException("Ficheiro nulo: " + path);
	} else {
	    FileInputStream fin = new FileInputStream(f);
	    DataInputStream in = new DataInputStream(fin);

	    byte[] bytecodes = new byte[length];
	    in.readFully(bytecodes);
	    return bytecodes;
	}
    }

    /**
     * Cria class server para ler ficheiros
     * Argumentos: porto e raiz do path
     *
     * java ClassFileServer <port> <path>
     */
    public static void main(String args[])
    {
	System.out.println("Help: java ClassFileServer port docroot [TLS [true]]");
	System.out.println("");
	System.out.println("TLS true significa que o servidor suporta TLS\n");
	System.out.println("Noutro caso funciona apenas com HTTP\n");

	int port = DefaultServerPort;
	String docroot = "";

	if (args.length >= 1) {
	    port = Integer.parseInt(args[0]);
	}

	if (args.length >= 2) {
	    docroot = args[1];
	}
	String type = "Sockets nao TLS";
	if (args.length >= 3) {
	    type = args[2];
	}
	try {
	    ServerSocketFactory ssf =
		ClassFileServer.getServerSocketFactory(type);
	    ServerSocket ss = ssf.createServerSocket(port);


	   
	   
	   
            //	       

	    String[] supported = ((SSLServerSocket)ss).getSupportedCipherSuites();
	   
	   
            System.out.println("Retorno de suites suportadas ...\n");
	    for (int i=0 ; i< supported.length; i++ ) 	   
                 System.out.println(supported[i]);			       	       
	   
	   
	   
	    String[] anonCipherSuitesSupported = new String [supported.length];
	    int numAnonCipherSuitesSupported = 0;
	    for (int i=0 ; i< supported.length; i++ ) 
	     {
		if (supported[i].indexOf("_anon_") > 0) 
		   {
		   anonCipherSuitesSupported[numAnonCipherSuitesSupported++] = supported[i];
		   }
    	     }

            System.out.println("\n\nRetorno de suites anonimas suportadas ...\n");
	    for (int i=0 ; i< anonCipherSuitesSupported.length; i++ )  
            System.out.println(anonCipherSuitesSupported[i]);
	       
            System.out.println("\n\nHabilitadas inicialmente ...\n");
	    String[] oldEnabled=((SSLServerSocket)ss).getEnabledCipherSuites();
	   

	    for (int i=0 ; i< oldEnabled.length; i++ ) 	   
	     System.out.println(oldEnabled[i]);
	       
	    String[] newEnabled = new String[oldEnabled.length + numAnonCipherSuitesSupported];
	     
	    System.arraycopy(oldEnabled, 0 , newEnabled, 0 ,oldEnabled.length);
	    System.arraycopy(anonCipherSuitesSupported, 0 , newEnabled,  
			     oldEnabled.length, numAnonCipherSuitesSupported);

	   
	    ((SSLServerSocket)ss).setEnabledCipherSuites(newEnabled);

            System.out.println("\n\nHabilitadas agora ...\n");
	    for (int i=0 ; i< newEnabled.length; i++ ) 	   
	     System.out.println(newEnabled[i]);

 
            // Ok, agora aceita-se tudo... incluindo as suites anonimas e Kerb based
	   
	   
	    if (args.length >= 4 && args[3].equals("true")) {
		((SSLServerSocket)ss).setNeedClientAuth(true);
         	System.out.println("Ok, vou funcionar com TLS - porta:" + port);
	       
	    }
	    new ClassFileServer(ss, docroot);
	} catch (IOException e) {
	    System.out.println("Erro a iniciar ClassServer: " +
			       e.getMessage());
	    e.printStackTrace();
	}
    }

    private static ServerSocketFactory getServerSocketFactory(String type) {
	if (type.equals("TLS")) {
	    SSLServerSocketFactory ssf = null;
	    try {
		// setup key manager  - para autenticacao do servidor
		SSLContext ctx;
		KeyManagerFactory kmf;
		KeyStore ks;
		char[] passphrase = "serverserver".toCharArray();

		ctx = SSLContext.getInstance("TLS");
		kmf = KeyManagerFactory.getInstance("SunX509");
		ks = KeyStore.getInstance("JKS");

		ks.load(new FileInputStream("serverkeystore"), passphrase);
		kmf.init(ks, passphrase);
		ctx.init(kmf.getKeyManagers(), null, null);

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




