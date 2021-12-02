package extra;

import common.Utils;

import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class VoucherMinter {

    private static final String COIN_NAME = "CoinName=Movie Voucher Inc.";
    private static final String COIN_ISSUER = "CoinIssuer=MEGAMOVIES";
    private static final String COIN_VAlUES = "CoinValue=";
    private static final String EXPIRE_DATE = "ExpireDate=";
    private static final String COIN_PUB_KEY = "CoinPublicKey=";
    private static final String COIN_AUTH = "CoinAuthenticity=";
    private static final String ISSUER_SIG = "IssuerSignature=";
    private static final String ISSUER_PUB_KEY = "IssuerPublicKey=";
    private static final String INT_PROOF_1 = "IntegrityProof1=";
    private static final String INT_PROOF_2 = "IntegrityProof2=";
    private static final String VOUCHER_PATH = "resources/coin_";
    private static final String VOUCHER_EXTENSION = ".voucher";
    private static final String PROVIDER = "BC";
    private static final String SHA_512_WITH_ECDSA = "SHA512withECDSA";
    private static final String KEYSTORE_PATH = "keystores/coinissuer.keystore";
    private static final String SRSC_2122 = "srsc2122";
    private static final String COINISSUER = "coinissuer";
    private static final String VOUCHER_MINTER = "----------- VOUCHER MINTER -----------";
    private static final String HORIZONTAL_LINE = "--------------------------------------";
    private static final String ASK_VALUE = "Insert value (default = 1): ";
    private static final String ASK_DAYS = "Insert expire in days (default = 365): ";
    private static final String GENERATING_COIN_S_PRIVATE_KEY = "Generating coin's private key....";
    private static final String SIG_ALGO = "SHA512WITHECDSA";
    private static final String PROOF1_ALGO = "SHA256";
    private static final String PROOF2_ALGO = "RIPEMD256";
    private static final String SHA_256_OK = "SHA256 ok";
    private static final String RIPEMD_OK = "RIPEMD ok";
    private static final String EC = "EC";
    private static final Logger LOGGER = Logger.getLogger(VoucherMinter.class.getName());
    private static final int NEWLINE = 10;
    private static final Map<String, Date> spentVouchers = new HashMap<>();
    private static String fileFingerPrint;


    static public void main(String[] args) throws Exception {

//        File file = new File("vouchers/coin_3040021e2ab0122.voucher");
//        System.out.println((verifyVoucher(Files.readAllBytes(file.toPath()))));
        Utils.loadBC();
        mintVoucher();
    }

    public static int verifyVoucher(byte[] voucher) throws IOException, ParseException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchProviderException, CertificateException, KeyStoreException, UnrecoverableKeyException {
        Properties properties = new Properties();
        try {
            properties.load(new ByteArrayInputStream(voucher));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String coinValue = properties.getProperty("CoinValue");
        String rawDate = properties.getProperty("ExpireDate");
        DateFormat dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
        Date expireDate = dateFormat.parse(rawDate);
        String coinPublicKey = properties.getProperty("CoinPublicKey");
        String coinAuthenticity = properties.getProperty("CoinAuthenticity");
        String issuerSignature = properties.getProperty("IssuerSignature");
        String issuerPublicKey = properties.getProperty("IssuerPublicKey");
        String proof1 = properties.getProperty("IntegrityProof1");
        String proof2 = properties.getProperty("IntegrityProof2");

        if (spentVouchers.containsKey(coinAuthenticity))
            return -1;
        else
            spentVouchers.put(coinAuthenticity, new Date());
        // TODO MAKE IT PERSISTENCE


        ByteArrayOutputStream toVerify = new ByteArrayOutputStream();
        ByteArrayInputStream is = new ByteArrayInputStream(voucher);
        BufferedReader bfReader = new BufferedReader(new InputStreamReader(is));
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);


        if (!MessageDigest.isEqual(hash(toVerify.toByteArray(), PROOF1_ALGO), Utils.decodeHexString(proof1)))
            return -1;
        LOGGER.info(SHA_256_OK);

        if (!MessageDigest.isEqual(hash(toVerify.toByteArray(), PROOF2_ALGO), Utils.decodeHexString(proof2)))
            return -1;
        LOGGER.info(RIPEMD_OK);


        toVerify = new ByteArrayOutputStream();
        is = new ByteArrayInputStream(voucher);
        bfReader = new BufferedReader(new InputStreamReader(is));
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);
        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);

        if (!checkSignature(coinPublicKey, coinAuthenticity, toVerify))
            return -1;

        toVerify.write(bfReader.readLine().getBytes());
        toVerify.write(NEWLINE);


        if (!checkSignature(issuerPublicKey, issuerSignature, toVerify))
            return -1;

        if (expireDate.getTime() < (new Date().getTime()))
            return -1;
        return Integer.parseInt(coinValue);
    }

    private static boolean checkSignature(String coinPublicKey, String coinAuthenticity, ByteArrayOutputStream toVerify) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        LOGGER.info("Checking hash:" + coinAuthenticity + " with key: " + coinPublicKey);
        Signature ecdsaVerify;
        byte[] byteKey;
        X509EncodedKeySpec X509publicKey;
        KeyFactory kf;
        PublicKey publicKey;
        boolean result;
        ecdsaVerify = Signature.getInstance(SIG_ALGO, PROVIDER);
        byteKey = Utils.decodeHexString(coinPublicKey);
        X509publicKey = new X509EncodedKeySpec(byteKey);
        kf = KeyFactory.getInstance(EC, PROVIDER);
        publicKey = kf.generatePublic(X509publicKey);
        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(toVerify.toByteArray());
        result = ecdsaVerify.verify(Utils.decodeHexString(coinAuthenticity));
        if (!result)
            return false;
        return true;
    }

    private static void mintVoucher() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, InvalidKeyException, SignatureException {
        KeyPair issuerKp;
        KeyPair voucherKp;
        byte[] sigBytes;
        String sigBytesString;
        int value;
        int expire;
        Date expireDate;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int[] consoleResult;
        consoleResult = getInfoFromConsole();
        value = consoleResult[0];
        expire = consoleResult[1];
        expireDate = calculateDateAhead(expire);
        issuerKp = getIssuerKeys();
        voucherKp = generateVoucherKeys();
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
        sigBytes = sign(voucherKp.getPrivate(), baos);
        sigBytesString = Utils.encodeHexString(sigBytes);
        fileFingerPrint = sigBytesString.substring(0, 15);
        baos.write((COIN_AUTH + sigBytesString).getBytes());
        baos.write(NEWLINE);
        // signs all the ABOVE fields
        assert issuerKp != null;
        sigBytes = sign(issuerKp.getPrivate(), baos);
        sigBytesString = Utils.encodeHexString(sigBytes);
        baos.write((ISSUER_SIG + sigBytesString).getBytes());
        baos.write(NEWLINE);
        baos.write((ISSUER_PUB_KEY + Utils.encodeHexString(issuerKp.getPublic().getEncoded())).getBytes());
        baos.write(NEWLINE);
        // both hash all the above
        byte[] sha256Hash = hash(baos.toByteArray(), PROOF1_ALGO);
        byte[] ripemdHash = hash(baos.toByteArray(), PROOF2_ALGO);
        baos.write((INT_PROOF_1 + Utils.encodeHexString(sha256Hash)).getBytes());
        baos.write(NEWLINE);
        baos.write((INT_PROOF_2 + Utils.encodeHexString(ripemdHash)).getBytes());
        baos.write(NEWLINE);
        baos.write(NEWLINE);
        baos.flush();
        baos.writeTo(new FileOutputStream(VOUCHER_PATH + fileFingerPrint + VOUCHER_EXTENSION));
    }

    private static byte[] hash(byte[] input, String algo) throws NoSuchAlgorithmException {
        LOGGER.info("Hashing with: " + algo);
        MessageDigest hash = null;
        try {
            hash = MessageDigest.getInstance(algo, PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            hash = MessageDigest.getInstance(algo);
            e.printStackTrace();
        }

        assert hash != null;
        hash.update(input);
        return hash.digest();
    }

    private static byte[] sign(PrivateKey key, ByteArrayOutputStream baos) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        LOGGER.info("Signing with key: " + key);
        Signature signature = null;
        try {
            signature = Signature.getInstance(SHA_512_WITH_ECDSA, PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            signature = Signature.getInstance(SHA_512_WITH_ECDSA);
        }
        assert signature != null;
        signature.initSign(key, new SecureRandom());
        signature.update(baos.toByteArray());
        return signature.sign();
    }

    private static KeyPair generateVoucherKeys() throws NoSuchAlgorithmException {
        LOGGER.info(GENERATING_COIN_S_PRIVATE_KEY);
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance(EC, PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            kpg = KeyPairGenerator.getInstance(EC);
        }
        assert kpg != null;
        return kpg.generateKeyPair();
    }

    private static KeyPair getIssuerKeys() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        FileInputStream is = new FileInputStream("keystores/coinissuer.keystore");
//        FileInputStream is = new FileInputStream(KEYSTORE_PATH);

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(is, SRSC_2122.toCharArray());

        String alias = COINISSUER;

        Key key = keystore.getKey(alias, SRSC_2122.toCharArray());
        if (key instanceof PrivateKey) {
            Certificate cert = keystore.getCertificate(alias);
            PublicKey publicKey = cert.getPublicKey();
            return new KeyPair(publicKey, (PrivateKey) key);
        }
        return null;
    }

    private static Date calculateDateAhead(int expire) {
        Date dNow = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(dNow);
        cal.add(Calendar.DATE, expire); //minus number would decrement the days
        return cal.getTime();
    }

    private static int[] getInfoFromConsole() {
        int[] result = new int[2];
        Scanner sc = new Scanner(System.in);
        System.out.println(VOUCHER_MINTER);
        System.out.println(HORIZONTAL_LINE);
        System.out.print(ASK_VALUE);
        try {
            result[0] = Integer.parseInt(sc.nextLine());

        } catch (NumberFormatException e) {
            result[0] = 1;
        }

        System.out.print(ASK_DAYS);
        try {
            result[1] = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            result[1] = 365;
        }

        return result;
    }
}
