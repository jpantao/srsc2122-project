package common;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

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
    public static SecretKey createKeyForAES(
            int bitLength,
            SecureRandom random)
            throws NoSuchAlgorithmException, NoSuchProviderException {
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

    public static IvParameterSpec createCtrIvForBLOWFISH(
            SecureRandom random) {
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
    public static String toString(
            byte[] bytes,
            int length) {
        char[] chars = new char[length];

        for (int i = 0; i != chars.length; i++) {
            chars[i] = (char) (bytes[i] & 0xff);
        }

        return new String(chars);
    }

    /**
     * Convete um array de caracteres de 8 bits numa string
     *
     * @param bytes - Array que contem os caracteres
     * @return String com a representacao dos bytes
     */
    public static String toString(
            byte[] bytes) {
        return toString(bytes, bytes.length);
    }

    /**
     * Converte a string passada num array de bytes
     * a partir dos 8 bits de cada caracter contido no array
     *
     * @param string - String a converter
     * @return - retorna representacao em array de bytes
     */
    public static byte[] toByteArray(
            String string) {
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
}
