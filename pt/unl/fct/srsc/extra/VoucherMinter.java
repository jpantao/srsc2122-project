package pt.unl.fct.srsc.extra;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class VoucherMinter {

    public static final String COIN_NAME = "Coin Name=Movie Voucher Inc.";
    public static final String COIN_ISSUER = "Coin Issuer=MEGAMOVIES";
    public static final String COIN_VAlUES = "Coin Value=";
    public static final String EXPIRE_DATE = "Expire Date=";
    public static final String COIN_PUB_KEY = "Coin PublicKey=";
    public static final String COIN_AUTH = "Coin Authenticity=";
    public static final String ISSUER_SIG = "Issuer Signature=";
    public static final String ISSUER_PUB_KEY = "Issuer PublicKey=";
    public static final String INT_PROOF_1 = "Integrity Proof1=";
    public static final String INT_PROOF_2 = "Integrity Proof2=";
    public static final int NEWLINE = 10;


    static public void main(String []args ) throws Exception {
        int value;
        int expire;
        KeyPair issuerKp;
        KeyPair voucherKp;
        FileInputStream outputStream;
        Date expireDate;



        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);







        Scanner sc= new Scanner(System.in);
        System.out.println("----------- VOUCHER MINTER -----------");
        System.out.println("--------------------------------------");
        System.out.print("Insert value (default = 1): ");
        try {
            value = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            value = 1;
        }

        System.out.print("Insert expire in days (default = 365): ");
        try {
            expire = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            expire = 365;
        }

        Date dNow = new Date( );
        Calendar cal = Calendar.getInstance();
        cal.setTime(dNow);
        cal.add(Calendar.DATE, expire); //minus number would decrement the days
        expireDate = cal.getTime();

        FileInputStream is = new FileInputStream("pt/unl/fct/srsc/common/this.keystore");

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, "srsc2122".toCharArray());

        String alias = "coinissuer";

        Key key = keystore.getKey(alias, "srsc2122".toCharArray());
        if (key instanceof PrivateKey) {
            Certificate cert = keystore.getCertificate(alias);
            PublicKey publicKey = cert.getPublicKey();
            issuerKp = new KeyPair(publicKey, (PrivateKey) key);
        }

        System.out.println("");
        System.out.print("Generating coin's private key....");
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("EC", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            kpg = KeyPairGenerator.getInstance("EC");
        }
        voucherKp =  kpg.generateKeyPair();
        System.out.println("Done!");




        baos.write(COIN_NAME.getBytes());
        baos.write(NEWLINE);
        baos.write(COIN_ISSUER.getBytes());
        baos.write(NEWLINE);
        baos.write((COIN_VAlUES + value).getBytes());
        baos.write(NEWLINE);
        baos.write((EXPIRE_DATE + expireDate).getBytes());
        baos.write(NEWLINE);






        baos.flush();
        baos.writeTo(new FileOutputStream("pt/unl/fct/srsc/common/teste"));

        System.out.print("Signing with coin's private key....");
        System.out.println("Done!");

        System.out.print("Signing with Issuer's private key...");
        System.out.println("Done!");

        System.out.print("Hashing with sha256...");
        System.out.println("Done!");


        System.out.print("Hashing with RIPMD-256...");
        System.out.println("Done!");





    }
}
