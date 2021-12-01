package common;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import srtsp.messages.HeaderSRTSP;
import srtsp.messages.PlainMsgSRTSP;
import sapkdp.messages.HeaderSAPKDP;
import sapkdp.messages.PlainMsgSAPKDP;
import sapkdp.messages.PlainTicketCreds;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

/**
 * Utilities
 */
public class Utils
        extends UtilsBase {
    /**
     * Criacao de uma chave AES
     *
     * @param bitLength
     * @param random
     * @return Chave AES
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public static SecretKey createKeyForAES(int bitLength, SecureRandom random) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256, random);
        return generator.generateKey();
    }

    /**
     * Criar um IV para usar em AES e modo CTR
     * <p>
     * IV composto por 4 bytes (numero de emensagem)
     * 4 bytes de random e um contador de 8 bytes.
     *
     * @param messageNumber - Numero da mensagem
     * @param random        - source ou seed para random
     * @return Vector IvParameterSpec inicializado
     */
    public static IvParameterSpec createCtrIvForAES(int messageNumber, SecureRandom random) {
        byte[] ivBytes = new byte[16];

        // initially randomize

        random.nextBytes(ivBytes);

        // set the message number bytes

        ivBytes[0] = (byte) (messageNumber >> 24);
        ivBytes[1] = (byte) (messageNumber >> 16);
        ivBytes[2] = (byte) (messageNumber >> 8);
        ivBytes[3] = (byte) (messageNumber >> 0);

        // set the counter bytes to 1

        for (int i = 0; i != 7; i++) {
            ivBytes[8 + i] = 0;
        }

        ivBytes[15] = 1;

        return new IvParameterSpec(ivBytes);
    }

    public static IvParameterSpec createCtrIvForBLOWFISH(SecureRandom random) {
        byte[] ivBytes = new byte[8];
        random.nextBytes(ivBytes);
        return new IvParameterSpec(ivBytes);
    }

    /**
     * Converte um byte array de 8 bits numa string
     *
     * @param bytes  array contendo os caracteres
     * @param length N. de bytes a processar
     * @return String que representa os bytes
     */
    public static String toString(byte[] bytes, int length) {
        char[] chars = new char[length];
        for (int i = 0; i != chars.length; i++) {
            chars[i] = (char) (bytes[i] & 0xff);
        }
        return new String(chars);
    }

    public static IvParameterSpec createCtrIvForCCM(
        SecureRandom    random)
    {
        byte[]          ivBytes = new byte[13];

        random.nextBytes(ivBytes);

        return new IvParameterSpec(ivBytes);
    }


    /**
     * Convete um array de caracteres de 8 bits numa string
     *
     * @param bytes - Array que contem os caracteres
     * @return String com a representacao dos bytes
     */
    public static String toString(byte[] bytes) {
        return toString(bytes, bytes.length);
    }

    /**
     * Converte a string passada num array de bytes
     * a partir dos 8 bits de cada caracter contido no array
     *
     * @param string - String a converter
     * @return - retorna representacao em array de bytes
     */
    public static byte[] toByteArray(String string) {
        byte[] bytes = new byte[string.length()];
        char[] chars = string.toCharArray();

        for (int i = 0; i != chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }

        return bytes;
    }


    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }


    public static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if (digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: " + hexChar);
        }
        return digit;
    }

    public static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    public static String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    public static void generatePBId() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        FileInputStream is = new FileInputStream("keystore/this.keystore");

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(is, "srsc2122".toCharArray());

        String keyAlias = "proxybox";
        KeyPair proxyBoxKp = null;
        byte[] id = new byte[123];
        byte[] random256 = new byte[32];
        SecureRandom random = new SecureRandom();

        Key key = keystore.getKey(keyAlias, "srsc2122".toCharArray());
        if (key instanceof PrivateKey) {
            Certificate cert = (Certificate) keystore.getCertificate(keyAlias);
            PublicKey publicKey = cert.getPublicKey();
            proxyBoxKp = new KeyPair(publicKey, (PrivateKey) key);
        }

        if (proxyBoxKp != null) {
            byte[] encoded = proxyBoxKp.getPublic().getEncoded();
            System.arraycopy(encoded, 0, id, 0, encoded.length);
        }
        random.nextBytes(random256);
        System.arraycopy(random256, 0, id, 91, 32);
        DataOutputStream temp = new DataOutputStream(new FileOutputStream("id.txt"));
        String tempString = Utils.encodeHexString(id);
        temp.writeBytes(tempString);
        temp.flush();
        temp.close();
    }

    public static Properties loadConfig(String configFile) {
        Properties properties = null;
        try {
            InputStream inputStream = new FileInputStream(configFile);
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static void loadBC() {
        Provider provider = Security.getProvider("BC");
        if (provider == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static KeyPair getKeyPair(String keystoreFile, char[] storepass, String keyalias) {
        KeyPair kp = null;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(keystoreFile), storepass);

            Key k = ks.getKey(keyalias, storepass);
            if (k instanceof PrivateKey)
                kp = new KeyPair(ks.getCertificate(keyalias).getPublicKey(), (PrivateKey) k);
            else
                throw new Exception("Private key for " + keyalias + "not found in " + keystoreFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return kp;
    }

    public static PublicKey getPubKey(String keystoreFile, char[] storepass, String alias) {
        PublicKey pubKey = null;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(keystoreFile), storepass);

            Key k = ks.getKey(alias, storepass);
            if (k instanceof PrivateKey)
                pubKey = ks.getCertificate(alias).getPublicKey();
            else
                throw new Exception("Private key for " + alias + "not found in " + keystoreFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return pubKey;
    }

    public static void logSent(Object msg) {
        System.out.println("[SENT] " + msg);
    }

    public static void logReceived(Object msg) {
        System.out.println("[RECV] " + msg);
    }


    public static void writeWithHeaderSAPKDP(DataOutputStream out, int version, PlainMsgSAPKDP.Type type, byte[] payload) throws IOException {
        HeaderSAPKDP header = new HeaderSAPKDP(version, type.value, (short) payload.length);
        out.write(header.encode());
        out.write(payload);
    }

    public static void writeWithHeaderRTSTP(DataOutputStream out, int version, PlainMsgSRTSP.Type type, byte[] payload) throws IOException {
        HeaderSRTSP header = new HeaderSRTSP(version, type.value, (short) payload.length);
        out.write(header.encode());
        out.write(payload);
    }


    public static void writeSignature(DataOutputStream out, String algorithm, String provider, PrivateKey key, byte[] data) {
        try {
            Signature sig = Signature.getInstance(algorithm, provider);
            sig.initSign(key, new SecureRandom());
            sig.update(data);
            byte[] sigBytes = sig.sign();

            out.writeInt(sigBytes.length);
            out.write(sigBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | IOException | NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    public static void writeIntCheck(DataOutputStream out, Mac mac, byte[] data) {
        try {
            out.write(Utils.genIntCheck(mac, data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean readVerifyingIntCheck(DataInputStream in, Mac mac, byte[] data) {
        byte[] intCheck = readIntCheck(in, mac);
        return verifyIntCheck(mac, data, intCheck);
    }

    public static byte[] readIntCheck(DataInputStream in, Mac mac) {
        byte[] intCheck = null;
        try {
            intCheck = new byte[mac.getMacLength()];
            in.read(intCheck);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return intCheck;
    }

    public static boolean verifyIntCheck(Mac mac, byte[] data, byte[] intCheck) {
        mac.update(data);
        return MessageDigest.isEqual(intCheck, mac.doFinal());
    }


    public static boolean readVerifyingSig(DataInputStream in, String algorithm, String provider, PublicKey key, byte[] data) {
        byte[] sigBytes = readSig(in);
        return verifySig(algorithm, provider, key, data, sigBytes);
    }

    public static byte[] readSig(DataInputStream in) {
        byte[] sigBytes = null;
        try {
            int sigSize = in.readInt();
            sigBytes = new byte[sigSize];
            in.read(sigBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sigBytes;
    }

    public static boolean verifySig(String algorithm, String provider, PublicKey key, byte[] data, byte[] sigBytes) {
        Signature sig = null;
        try {
            sig = Signature.getInstance(algorithm, provider);
            sig.initVerify(key);
            sig.update(data);
            return sig.verify(sigBytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] readConsumingHeader(DataInputStream in, int expectedType) throws IOException {
        byte[] headerBytes = new byte[HeaderSAPKDP.BYTE_LEN];
        in.read(headerBytes); //TODO: verify read bytes
        HeaderSAPKDP header = new HeaderSAPKDP(headerBytes);

        if (header.getMsgType() != expectedType) {
            return null; // unexpected message type
        }

        PlainMsgSAPKDP.Type type = PlainMsgSAPKDP.Type.fromOpcode(header.getMsgType());
        byte[] payload = new byte[header.getPayloadSize()];
        in.read(payload); //TODO: verify read bytes

        return payload;
    }

    public static byte[] pbeCipher(int opmode, String pw, String ciphersuite, String provider, byte[] salt, int iterationCounter, byte[] plaintext) {
        try {
            PBEKeySpec pbeSpec = new PBEKeySpec(pw.toCharArray());
            Key k = SecretKeyFactory.getInstance(ciphersuite).generateSecret(pbeSpec);
            Cipher c = Cipher.getInstance(ciphersuite, provider);
            c.init(opmode, k, new PBEParameterSpec(salt, iterationCounter));
            return c.doFinal(plaintext);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Mac getHMAC(String algorithm, byte[] macKey, String macSuite) {
        try {
            Mac hmac = Mac.getInstance(algorithm);
            hmac.init(new SecretKeySpec(macKey, macSuite));
            return hmac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] genIntCheck(Mac mac, byte[] data) {
        mac.update(data);
        return mac.doFinal();
    }

    public static String readSting(DataInputStream in) throws IOException {
        return new String(readByteArray(in));
    }

    public static byte[] readByteArray(DataInputStream in) throws IOException {
        int read;
        int size = in.readInt();
        byte[] buf = new byte[size];
        read = in.read(buf);
        if (read != size)
            throw new IOException("read " + read + " should have been " + size);
        return buf;
    }

    public static void writeByteArray(DataOutputStream out, byte[] buf) throws IOException {
        out.writeInt(buf.length);
        out.write(buf);
    }

    public static void writeString(DataOutputStream out, String string) throws IOException {
        writeByteArray(out, string.getBytes());
    }

    public static byte[] joinByteArrays(byte[][] byteArrays) {
        int size = 0;
        for (byte[] b : byteArrays)
            size += b.length;
        byte[] allByteArray = new byte[size];

        ByteBuffer buf = ByteBuffer.wrap(allByteArray);
        for (byte[] b : byteArrays)
            buf.put(b);

        return buf.array();
    }

    public static PlainTicketCreds decryptTicket(byte[] ticketCipher, String ciphersuite, PrivateKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher c = Cipher.getInstance(ciphersuite);
        c.init(Cipher.DECRYPT_MODE, key);
        return (PlainTicketCreds) PlainMsgSAPKDP.deserialize(PlainMsgSAPKDP.Type.PB_TKCREDS.value, c.doFinal(ticketCipher));
    }

    public static byte[] encryptTicket(PlainTicketCreds ticket, String ciphersuite, PublicKey key) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        Cipher c = Cipher.getInstance(ciphersuite);
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(PlainMsgSAPKDP.serialize(ticket));
    }

    public static SecureDatagramSocket secureDatagramSocketWithReusableAddress(int port) throws IOException, InterruptedException {
        SecureDatagramSocket newSocket = new SecureDatagramSocket(null);
        newSocket.setReuseAddress(true);
        newSocket.bind(new InetSocketAddress(port));
        return newSocket;
    }

    public static void writeCryptoConf(PlainTicketCreds ticket, String filename) {
        Properties prop = new Properties();
        prop.setProperty("algorithm", ticket.getCiphersuiteConf().split("/")[0]);
        prop.setProperty("options", ticket.getCiphersuiteConf());
        prop.setProperty("ivBytes", ticket.getCryptoSA());
        prop.setProperty("keyBytes", Utils.encodeHexString(ticket.getSessionkeyBytes()));
        prop.setProperty("hmac", ticket.getMacsuite());
        prop.setProperty("hmacBytes", Utils.encodeHexString(ticket.getMackeyBytes()));
        try {
            prop.store(new FileOutputStream(filename), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
