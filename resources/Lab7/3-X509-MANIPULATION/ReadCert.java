/*
 * ReadCert.java
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.cert.*;

/**
 * Demo de como fazer uma pequena classe capaz de ler um certificado
 * de um ficheiro file.cer, para acesso aos atributos do mesmo.
 * O certificado tem que ser inicialmente exportado para o formato
 * adequado
 * ex: keytool -export -keystore XXX.keystore -alias certkey -file certkey.cer
 * 
 * Inicialmente para gerar o certificado pode ser usado...
 * ex: keytool -genkey -keystore XXX.keystore -alias certkey
 */
public class ReadCert {

    /**
     * OID para associar a um CRL Distribution Point segundo a norma X509v3
     */
    public static final String CRL_DISTRIBUTION_POINT_OID = "2.5.29.31";

    /**
     * Runs the application
     * @param args the command line arguments
     */
    public static void main(String args[]) {
	InputStream in = null;
	try {

	    // Instanciar uma factory para um certifi. x509
	    CertificateFactory cf = CertificateFactory.getInstance("X.509");

	    // Para ler o certificado 
	    in = new BufferedInputStream(new FileInputStream(args[0]));
	    X509Certificate cert = (X509Certificate) cf.generateCertificate(in);

	    // Os atributos podem ser acedidos facilmente
	    System.out.println("Subject: " + cert.getSubjectDN().getName());
	    System.out.println("Validity: " + cert.getNotBefore() + " - " 
			       + cert.getNotAfter());
	    System.out.println("Issuer: " + cert.getIssuerDN().getName());
	    System.out.println("Signature Algorithm: " + cert.getSigAlgName());

	    // Para imprimir o CRL dist. point no caso de haver algum definido
	    byte[] crlDP = cert.getExtensionValue(CRL_DISTRIBUTION_POINT_OID);
	    if (crlDP != null) {
		URL crlUrl = extractURL(crlDP);
		if (crlUrl != null) {
		    System.out.println("CRL Distribution Point: " + crlUrl);
		    CRL crl = cf.generateCRL(crlUrl.openStream());
		    System.out.println("Certificate revoked: " + crl.isRevoked(cert));
		} 
	    } 
	} catch (Exception e) {
	    e.printStackTrace();
	} 
	finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (Exception e) {

		    // nada mais a fazer
		} 
	    } 
	} 
    } 

    /**
     * Extrair um URL de um ficheiro encoded com DER
     * (RFC2459 (http://www.ietf.org/rfc/rfc2459.txt)
     * @param bytes Bytes a extrair do URL
     * @return String com a URL
     * @throws MalformedURLException : problemas de parsing
     */
    public static URL extractURL(byte[] bytes) throws MalformedURLException {
	String s = new String(bytes);
	int urlOffset = s.lastIndexOf("http");
	return (urlOffset >= 0) ? new URL(s.substring(urlOffset)) : null;
    } 
}
