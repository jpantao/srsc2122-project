
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.*;

public class SecureDatagramSocket extends DatagramSocket {
    byte[]	keyBytes = new byte[] {
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab,(byte)0xcd, (byte)0xef,
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab,(byte)0xcd, (byte)0xef,
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab,(byte)0xcd, (byte)0xef ,
            0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab,(byte)0xcd, (byte)0xef
    }; // 256 bit key

    byte[]	ivBytes =
            new byte[] { 0x08, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00 ,
                    0x08, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00
            }; // 128 bit IV

    public SecureDatagramSocket() throws SocketException {
        super();
    }


    @Override
    public void send(DatagramPacket datagramPacket) throws IOException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        Cipher          cipher = null;
        Mac hMac = null;

        try {
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        try {
            hMac = Mac.getInstance("HmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Key hMacKey =new SecretKeySpec(key.getEncoded(), "HmacSHA512");

//        System.out.println("input : " + Utils.toHex(datagramPacket.getData(), datagramPacket.getLength()));

        // cifrar Alice (Correta)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        byte[] cipherText = new byte[cipher.getOutputSize(datagramPacket.getLength() + hMac.getMacLength())];

        int ctLength = 0;
        try {
            ctLength = cipher.update(datagramPacket.getData(), 0, datagramPacket.getLength(), cipherText, 0);
        } catch (ShortBufferException e) {
            e.printStackTrace();
        }

        try {
            hMac.init(hMacKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        hMac.update(datagramPacket.getData(), 0, datagramPacket.getLength());

        try {
            ctLength += cipher.doFinal(hMac.doFinal(), 0, hMac.getMacLength(), cipherText, ctLength);
        } catch (ShortBufferException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        datagramPacket.setData(cipherText, 0, ctLength);
        super.send(datagramPacket);
        @Override
        public synchronized void receive(DatagramPacket datagramPacket) throws IOException {
            super.receive(datagramPacket);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher          cipher = null;
            Mac hMac = null;

            try {
                cipher = Cipher.getInstance("AES/CTR/NoPadding");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                e.printStackTrace();
            }

            try {
                hMac = Mac.getInstance("HmacSHA512");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            Key hMacKey =new SecretKeySpec(key.getEncoded(), "HmacSHA512");

            try {

                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }

            byte[] plainText = new byte[0];
            try {
                plainText = cipher.doFinal(datagramPacket.getData(), 0, datagramPacket.getLength());
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }
            int    messageLength = plainText.length - hMac.getMacLength();

            try {
                hMac.init(hMacKey);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            hMac.update(plainText, 0, messageLength);

            byte[] messageHash = new byte[hMac.getMacLength()];
            System.arraycopy(plainText, messageLength, messageHash, 0, messageHash.length);

//        System.out.println("plain : "+ Utils.toHex(plainText, messageLength));
//        System.out.println("Verified w/ message-integrity and message-authentication :" + MessageDigest.isEqual(hMac.doFinal(), messageHash));

            datagramPacket.setData(plainText, 0, messageLength);

        }


    }
}
