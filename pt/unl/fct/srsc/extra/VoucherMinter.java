package pt.unl.fct.srsc.extra;

import pt.unl.fct.srsc.common.Utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class VoucherMinter {

    public static final String COIN_NAME = "CoinName=Movie Voucher Inc.";
    public static final String COIN_ISSUER = "CoinIssuer=MEGAMOVIES";
    public static final String COIN_VAlUES = "CoinValue=";
    public static final String EXPIRE_DATE = "ExpireDate=";
    public static final String COIN_PUB_KEY = "CoinPublicKey=";
    public static final String COIN_AUTH = "CoinAuthenticity=";
    public static final String ISSUER_SIG = "IssuerSignature=";
    public static final String ISSUER_PUB_KEY = "IssuerPublicKey=";
    public static final String INT_PROOF_1 = "IntegrityProof1=";
    public static final String INT_PROOF_2 = "IntegrityProof2=";
    public static final int NEWLINE = 10;


    static public void main(String []args ) throws Exception {
        int value;
        int expire;
        KeyPair issuerKp = null;
        KeyPair voucherKp;
        Date expireDate;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
        baos.write((COIN_PUB_KEY + Utils.encodeHexString(voucherKp.getPublic().getEncoded())).getBytes());
        baos.write(NEWLINE);


        System.out.print("Signing with coin's private key....");
        Signature signature = null;
        try {
            signature = Signature.getInstance("SHA512withECDSA", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            signature = Signature.getInstance("SHA512withECDSA");
        }
        signature.initSign(voucherKp.getPrivate(), new SecureRandom());
        signature.update(baos.toByteArray());
        String sigBytes = Utils.encodeHexString(signature.sign());
        System.out.println("Done!");
        baos.write((COIN_AUTH + sigBytes).getBytes());
        baos.write(NEWLINE);


        System.out.print("Signing with Issuer's private key...");
        try {
            signature = Signature.getInstance("SHA512withECDSA", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            signature = Signature.getInstance("SHA512withECDSA");
        }
        if (issuerKp != null) {
            signature.initSign(issuerKp.getPrivate(), new SecureRandom());
        }
        signature.update(baos.toByteArray());
        String issuerSigBytes = Utils.encodeHexString(signature.sign());
        System.out.println("Done!");

        baos.write((ISSUER_SIG + issuerSigBytes).getBytes());
        baos.write(NEWLINE);

        baos.write((ISSUER_PUB_KEY + Utils.encodeHexString(issuerKp.getPublic().getEncoded())).getBytes());
        baos.write(NEWLINE);


        System.out.print("Hashing with sha256...");
        MessageDigest   hash = null;
        try {
            hash = MessageDigest.getInstance("SHA256", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            hash = MessageDigest.getInstance("SHA256");
        }

        byte[] sha256 = null;
        if (hash != null) {
            hash.update(baos.toByteArray());
            sha256 = hash.digest();
        }
        System.out.println("Done!");

        System.out.print("Hashing with RIPMD-256...");
        try {
            hash = MessageDigest.getInstance("RIPEMD256", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            hash = MessageDigest.getInstance("RIPEMD256");
            e.printStackTrace();
        }

        byte[] ripemd256 = null;
        if (hash != null) {
            hash.update(baos.toByteArray());
            ripemd256 = hash.digest();
        }
        System.out.println("Done!");

        baos.write((INT_PROOF_1 + Utils.encodeHexString(sha256)).getBytes());
        baos.write(NEWLINE);
        baos.write((INT_PROOF_2 + Utils.encodeHexString(ripemd256)).getBytes());
        baos.write(NEWLINE);
        baos.flush();
        baos.writeTo(new FileOutputStream("pt/unl/fct/srsc/extra/coin_" + sigBytes.substring(0, 15) + ".voucher"));
    }
}
