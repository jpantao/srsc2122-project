import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class JavaHttpsInstrumentationExample
{
     public static void main(String[] args)
     {
       new JavaHttpsInstrumentationExample().checkCertificate();
     }
   
     private void checkCertificate()
     {
      // Put what you want ...
      String https_url = "https://www.google.com/";
      URL url;
      try 
        {
          url = new URL(https_url);
          HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
          //dumpl the cert info
          print_https_cert(con);
          //dump the content
          print_content(con);
         }
         catch (MalformedURLException e) 
           {
           e.printStackTrace();
	   }
         catch (IOException e) 
	   {
	   e.printStackTrace();
	   }
     }
   
     private void print_https_cert(HttpsURLConnection con)
     {
	 if(con!=null)
	 {
         try 
            {
            System.out.println("===============================");
	    System.out.println("Inspect everything from");
       	    System.out.println("the HTTPS/TLS Connection");
            System.out.println("===============================");

            System.out.println("===============================");
	    System.out.println("About the HTTPS/TLS Connection");
            System.out.println("===============================");	    
            System.out.println("Response Code : " + con.getResponseCode());
	    System.out.println("Established Cipher Suite : " + con.getCipherSuite());
	    // See also other HttpsURLConnection methods
	    // You can instrument lots of other things ...
	    System.out.println("\n");

            System.out.println("===============================");
	    System.out.println("About the Received Certificates");
            System.out.println("===============================");
	       
	    // Let's see the certification chain ...
	    Certificate[] certs = con.getServerCertificates();
	    for(Certificate cert : certs)
	    {
              	       
	       System.out.println("\n--------------------------------\n");
	       System.out.println("Cert Type : " + cert.getType());	       
	       System.out.println("Cert Hash Code : " + cert.hashCode());
	       System.out.println("\n--------------------------------\n");	       
	       
    	       System.out.println("All Certificate Representation:");
	       System.out.println(cert.toString());

       	       System.out.println("Let's extract the Public Key form the Certificate");
               PublicKey pubkey = cert.getPublicKey();
               System.out.println("Cert Public Key Algorithm : ");
	       System.out.println(pubkey.getAlgorithm());
               System.out.println("Cert Public Key Format : ");
       	       System.out.println(pubkey.getFormat());
               System.out.println("Cert Public Key Encoding (hex) : ");
       	       System.out.println(Utils3.toHex(pubkey.getEncoded()));	       

	    }
	       
	       
	   }
	   catch (SSLPeerUnverifiedException e) 
	    {
             e.printStackTrace();
	    }
	   catch (IOException e)
	    {
             e.printStackTrace();
            }
	 }
      }

     private void print_content(HttpsURLConnection con)
     {
	if(con!=null)
	  {
	     try 
	       {
		  System.out.println("****** Content of the URL ********");
            	  BufferedReader br = 
		  new BufferedReader(
		       new InputStreamReader(con.getInputStream()));
		  
		  String input;
                  while ((input = br.readLine()) != null)
		    {
	             System.out.println(input);
		    }
		     br.close();
	       }
	      catch (IOException e) 
	       {
		     e.printStackTrace();
	       }
	  }
     }
}
