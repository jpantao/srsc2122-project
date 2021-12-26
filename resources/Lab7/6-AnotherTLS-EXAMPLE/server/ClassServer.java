

import java.io.*;
import java.net.*;
import javax.net.*;

/*
 * ClassServer.java -- a simple file server
 * It can serve the HTTP GET request, in both clear 
 * or SSL secure channel. It also supports SSL
 * one-way authentication (server only) or
 * mutual authentication (client and server)
 * according with a specific option in the
 * ClassServerFile class (at the main() level) 
 */

/**
 * Based on ClassServer.java in tutorial/rmi
 */
public abstract class ClassServer implements Runnable {

    private ServerSocket server = null;
    /**
     * Constructs a ClassServer based on <b>ss</b> and
     * obtains a file's bytecodes using the method <b>getBytes</b>.
     *
     */
    protected ClassServer(ServerSocket ss)
    {
	server = ss;
	newListener();
    }

    /**
     * Returns an array of bytes. The array contains the bytes for
     * the file represented by the argument <b>path</b>.
     *
     * @return the bytes for the file
     * @exception FileNotFoundException if the file corresponding
     * to <b>path</b> could not be loaded.
     * @exception IOException if error occurs reading the class
     */
    public abstract byte[] getBytes(String path)
	throws IOException, FileNotFoundException;

    /**
     * The "listen" thread that accepts a connection to the
     * server, parses the header to obtain the file name
     * and then sends back the file bytes (or error
     * if the file is not found or if the response was incorrectly formed).
     */
    public void run()
    {
	Socket socket;

	// accept a connection
	try {
	    socket = server.accept();
	} catch (IOException e) {
	    System.out.println("Class Server died: " + e.getMessage());
	    e.printStackTrace();
	    return;
	}

	// This creates a new thread to accept the next connection
        // code below
	newListener();

	try {
	    DataOutputStream out =
		new DataOutputStream(socket.getOutputStream());
	    try {
		// get path to class file from header
		BufferedReader in =
		    new BufferedReader(
			new InputStreamReader(socket.getInputStream()));
		String path = getPath(in);
		// retrieve bytecodes of the file
		byte[] bytecodes = getBytes(path);
		// and sends bytecodes in response (assumes HTTP/1.0 or later)
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

		// also manages exceptions sending HTTP error codes
                // according with the protocol

	    } catch (Exception e) {
		e.printStackTrace();
		// write out error response
		out.writeBytes("HTTP/1.0 400 " + e.getMessage() + "\r\n");
		out.writeBytes("Content-Type: text/html\r\n\r\n");
		out.flush();
	    }

	} catch (IOException ex) {
	    // eat exception (could log error to log file, but
	    // write out to stdout for now).
	    System.out.println("error writing response: " + ex.getMessage());
	    ex.printStackTrace();

	} finally {
	    try {
		socket.close();
	    } catch (IOException e) {
	    }
	}
    }

    /**
     * Code that creates a new thread to listen.
     */
    private void newListener()
    {
	(new Thread(this)).start();
    }

    /**
     * Returns the path to the file obtained from
     * parsing the HTML header.
     */
    private static String getPath(BufferedReader in)
	throws IOException
    {
	String line = in.readLine();
	String path = "";
	// extract class from GET line as stated by the HTTP protocol
        // We will only process the HTTP GET command
	if (line.startsWith("GET /")) {
	    line = line.substring(5, line.length()-1).trim();
	    int index = line.indexOf(' ');
            if (index != -1) {
                path = line.substring(0, index);
            }
	}

	// process the rest of header
	do {
	    line = in.readLine();
	} while ((line.length() != 0) &&
	         (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));

	if (path.length() != 0) {
	    return path;
	} else {
	    throw new IOException("Malformed HTTP Header - dont understand");
	}
    }
}
