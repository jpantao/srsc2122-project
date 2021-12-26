

import java.io.*;
import java.net.*;
import javax.net.*;

/*
 * ClassServer.java -- a simple file server that can serve
 * Http get request in both clear and secure channel
 */


public abstract class ClassServer implements Runnable {

    private ServerSocket server = null;
   

    protected ClassServer(ServerSocket ss)
    {
	server = ss;
	newListener();
    }

    /**
     * Retorna um array de bytes con os bytes do ficheiro
     * pretendido pelo cliente (argumento path)
     *
     * @return  bytes do ficheiro
     * @exception FileNotFoundException no caso de nao ser
     * possivel acesso/leitura ao path/ficheiro dado
     * @exception IOException (erros, I/O na classe)
     */
    public abstract byte[] getBytes(String path)
	throws IOException, FileNotFoundException;

    /**
     * listener
     */
    public void run()
    {
	Socket socket;

	// accept da conneccao
	try {
	    socket = server.accept();
	} catch (IOException e) {
	    System.out.println("Class Server terminou: " + e.getMessage());
	    e.printStackTrace();
	    return;
	}


	// Nova thread para tratar conexao
	newListener();

	try {
	    DataOutputStream out =
		new DataOutputStream(socket.getOutputStream());
	    try {
	        // Obter path que vem no header enviado pelo cliente...

		BufferedReader in =
		    new BufferedReader(
			new InputStreamReader(socket.getInputStream()));
		String path = getPath(in);
	        // Obter conteudo
		byte[] bytecodes = getBytes(path);
		// Resposta (HTTP/1.0 ou superior)
		try {
		    out.writeBytes("HTTP/1.0 200 OK\r\n");
		    out.writeBytes("Content-Length: " + bytecodes.length +
				   "\r\n");
		    out.writeBytes("Content-Type: text/html\r\n\r\n");
		    out.write(bytecodes);
		    out.flush();
		} catch (IOException ie) {
		    ie.printStackTrace();
		    return;
		}

	    } catch (Exception e) {
		e.printStackTrace();
		// Mandar resposta com erro (http)
		out.writeBytes("HTTP/1.0 400 " + e.getMessage() + "\r\n");
		out.writeBytes("Content-Type: text/html\r\n\r\n");
		out.flush();
	    }

	} catch (IOException ex) {
	    // algo de anormal ... mostrar o que se passou
	    // candidato para depois mandar para um log de erros

	    System.out.println("erro na resposta: " + ex.getMessage());
	    ex.printStackTrace();

	} finally {
	    try {
		socket.close();
	    } catch (IOException e) {
	    }
	}
    }

    /**
     * Criar nova thread que espera por clientes
     */
    private void newListener()
    {
	(new Thread(this)).start();
    }

    /**
     * Retorna  path do ficheiro tal como vinha no header
     */
    private static String getPath(BufferedReader in)
	throws IOException
    {
	String line = in.readLine();
	String path = "";
	// extrai e processa a linha que traz o pedido
	if (line.startsWith("GET /")) {
	    line = line.substring(5, line.length()-1).trim();
	    int index = line.indexOf(' ');
            if (index != -1) {
                path = line.substring(0, index);
            }
	}

	// Tratar o restante header
	do {
	    line = in.readLine();
	} while ((line.length() != 0) &&
	         (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));

	if (path.length() != 0) {
	    return path;
	} else {
	    throw new IOException("Uhm ... nao compreendo o header ...");
	}
    }
}
