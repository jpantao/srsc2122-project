package pt.unl.fct.srsc.extra;

import pt.unl.fct.srsc.common.Utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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
    public static final String VOUCHER_PATH = "pt/unl/fct/srsc/extra/coin_";
    public static final String VOUCHER_EXTENSION = ".voucher";
    public static final String BC = "BC";
    public static final String RIPEMD_256 = "RIPEMD256";
    public static final String SHA_256 = "SHA256";
    public static final String DONE = "Done!";
    public static final String HASHING_WITH_RIPMD_256 = "Hashing with RIPMD-256...";
    public static final String HASHING_WITH_SHA_256 = "Hashing with SHA256...";
    public static final String SIGNING_WITH_ISSUER_S_PRIVATE_KEY = "Signing with Issuer's private key...";
    public static final String SHA_512_WITH_ECDSA = "SHA512withECDSA";
    public static final String SIGNING_WITH_COIN_S_PRIVATE_KEY = "Signing with coin's private key....";
    public static final String KEYSTORE_PATH = "pt/unl/fct/srsc/common/this.keystore";
    public static final String SRSC_2122 = "srsc2122";
    public static final String COINISSUER = "coinissuer";
    public static final String VOUCHER_MINTER = "----------- VOUCHER MINTER -----------";
    public static final String HORIZONTAL_LINE = "--------------------------------------";
    public static final String ASK_VALUE = "Insert value (default = 1): ";
    public static final String ASK_DAYS = "Insert expire in days (default = 365): ";
    public static final String GENERATING_COIN_S_PRIVATE_KEY = "Generating coin's private key....";
    private static int value;
    private static int expire;
    private static KeyPair issuerKp = null;
    private static KeyPair voucherKp;
    private static Date expireDate;
    private static ByteArrayOutputStream baos;
    private static String sigBytes;
    private static String issuerSigBytes;
    private static byte[] sha256 = null;
    private static byte[] ripemd256 = null;


    static public void main(String []args ) throws Exception {
        getInfoFromConsole();
        calculateDateAhead();
        getIssuerKeys();
        calculateCoinKeys();
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
        // signs the ABOVE fields
        coinSign();
        baos.write((COIN_AUTH + sigBytes).getBytes());
        baos.write(NEWLINE);
        // signs all the ABOVE fields
        issuerSign();
        baos.write((ISSUER_SIG + issuerSigBytes).getBytes());
        baos.write(NEWLINE);
        baos.write((ISSUER_PUB_KEY + Utils.encodeHexString(issuerKp.getPublic().getEncoded())).getBytes());
        baos.write(NEWLINE);
        // both hash all the above
        md5Hash();
        calculateRipemd();
        baos.write((INT_PROOF_1 + Utils.encodeHexString(sha256)).getBytes());
        baos.write(NEWLINE);
        baos.write((INT_PROOF_2 + Utils.encodeHexString(ripemd256)).getBytes());
        saveVoucher();
    }

    private static void saveVoucher() throws IOException {
        baos.write(NEWLINE);
        baos.flush();
        baos.writeTo(new FileOutputStream(VOUCHER_PATH + sigBytes.substring(0, 15) + VOUCHER_EXTENSION));
    }

    private static void calculateRipemd() throws NoSuchAlgorithmException {
        System.out.print(HASHING_WITH_RIPMD_256);
        MessageDigest hash = null;
        try {
            hash = MessageDigest.getInstance(RIPEMD_256, BC);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            hash = MessageDigest.getInstance(RIPEMD_256);
            e.printStackTrace();
        }

        assert hash != null;
        hash.update(baos.toByteArray());
        ripemd256 = hash.digest();
        System.out.println(DONE);
    }

    private static void md5Hash() throws NoSuchAlgorithmException {
        System.out.print(HASHING_WITH_SHA_256);
        MessageDigest   hash = null;
        try {
            hash = MessageDigest.getInstance(SHA_256, BC);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            hash = MessageDigest.getInstance(SHA_256);
        }

        assert hash != null;
        hash.update(baos.toByteArray());
        sha256 = hash.digest();
        System.out.println(DONE);
    }

    private static void issuerSign() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = null;
        System.out.print(SIGNING_WITH_ISSUER_S_PRIVATE_KEY);
        try {
            signature = Signature.getInstance(SHA_512_WITH_ECDSA, BC);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            signature = Signature.getInstance(SHA_512_WITH_ECDSA);
        }
        if (issuerKp != null) {
            assert signature != null;
            signature.initSign(issuerKp.getPrivate(), new SecureRandom());
        }
        assert signature != null;
        signature.update(baos.toByteArray());
        issuerSigBytes = Utils.encodeHexString(signature.sign());
        System.out.println(DONE);
    }

    private static void coinSign() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        System.out.print(SIGNING_WITH_COIN_S_PRIVATE_KEY);
        Signature signature = null;
        try {
            signature = Signature.getInstance(SHA_512_WITH_ECDSA, BC);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            signature = Signature.getInstance(SHA_512_WITH_ECDSA);
        }
        assert signature != null;
        signature.initSign(voucherKp.getPrivate(), new SecureRandom());
        signature.update(baos.toByteArray());
        sigBytes = Utils.encodeHexString(signature.sign());
        System.out.println(DONE);
    }

    private static void calculateCoinKeys() throws NoSuchAlgorithmException {
        newLIne();
        System.out.print(GENERATING_COIN_S_PRIVATE_KEY);
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("EC", BC);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            kpg = KeyPairGenerator.getInstance("EC");
        }
        assert kpg != null;
        voucherKp =  kpg.generateKeyPair();
        System.out.println(DONE);
    }

    private static void newLIne() {
        System.out.println();
    }

    private static void getIssuerKeys() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        FileInputStream is = new FileInputStream(KEYSTORE_PATH);

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, SRSC_2122.toCharArray());

        String alias = COINISSUER;

        Key key = keystore.getKey(alias, SRSC_2122.toCharArray());
        if (key instanceof PrivateKey) {
            Certificate cert = keystore.getCertificate(alias);
            PublicKey publicKey = cert.getPublicKey();
            issuerKp = new KeyPair(publicKey, (PrivateKey) key);
        }
    }

    private static void calculateDateAhead() {
        Date dNow = new Date( );
        Calendar cal = Calendar.getInstance();
        cal.setTime(dNow);
        cal.add(Calendar.DATE, expire); //minus number would decrement the days
        expireDate = cal.getTime();
    }

    private static void getInfoFromConsole() {
        baos = new ByteArrayOutputStream();
        Scanner sc = new Scanner(System.in);
        System.out.println(VOUCHER_MINTER);
        System.out.println(HORIZONTAL_LINE);
        System.out.print(ASK_VALUE);
        try {
            value = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            value = 1;
        }

        System.out.print(ASK_DAYS);
        try {
            expire = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            expire = 365;
        }
    }
}
