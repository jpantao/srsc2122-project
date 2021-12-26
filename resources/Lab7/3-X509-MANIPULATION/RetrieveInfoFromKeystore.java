import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class RetrieveInfoFromKeystore {
    public static void main(String[] args) throws Exception {

	if (args.length < 3)
	    {
	    System.err.println("Usar: java RetrieveInfoFromKeystore keystore password aliasentry");
	    System.exit(1);
	    }
    
	System.out.println("Keystore: "+args[0] +" PWD: "+ args[1] +" ENTRY: "+ args[2]);

    // Ficheiro da keystore
        FileInputStream is = new FileInputStream(args[0]); 

    // Passar a password

	KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	keystore.load(is, args[1].toCharArray());

    // Passar o identificador (alias) da entry da keystore

	String alias = args[2];

    // Obter chave privada

	Key key = keystore.getKey(alias, args[1].toCharArray());
	if (key instanceof PrivateKey) {

	    System.out.println("OK !");

	    // Retornar certificado da chave publica da entry
	    Certificate cert = keystore.getCertificate(alias);

	    // Obter chave Publica da entry
	    PublicKey publicKey = cert.getPublicKey();

	    // Retornar o par de chaves da entry
	    KeyPair kp = new KeyPair(publicKey, (PrivateKey) key);

	    String pubKeyStringHEX = Utils3.toHex(publicKey.getEncoded());

      

	    System.out.println("---------------- HEXADECIMAL--------------");
	    System.out.println("Public Key");
	    System.out.println(pubKeyStringHEX);
	    System.out.println("------------------------------------------");
	    System.out.println("Private Key");
	    System.out.println(key.getFormat());
	    System.out.println("------------------------------------------");

	}
	else System.out.println("Not instance of Private Key ...");
    }
}