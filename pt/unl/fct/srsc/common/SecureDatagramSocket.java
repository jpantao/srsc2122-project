package pt.unl.fct.srsc.common;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;


// E (M, key1) || HMAC(M, key2)



public class SecureDatagramSocket extends DatagramSocket {
    byte[] keyBytes = new byte[]{
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef
    }; // 256 bit key

    byte[] ivBytes =
            new byte[]{0x08, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00,
                    0x08, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00
            }; // 128 bit IV

    SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
    IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
    Cipher cipher = null;
    Mac hMac = null;
    Key hMacKey;

    public SecureDatagramSocket() throws SocketException {
        super();
    }

    public SecureDatagramSocket(SocketAddress inSocketAddress) throws SocketException {
        super(inSocketAddress);
    }


    @Override
    public void send(DatagramPacket datagramPacket) throws IOException {
        init();
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



//        try {
//            ctLength += cipher.doFinal(hMac.doFinal(), 0, hMac.getMacLength(), cipherText, ctLength);
//        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
//            e.printStackTrace();
//        }



        int sizeOfCt = ctLength;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(1);
        dos.writeInt(sizeOfCt);
        dos.write(cipherText, 0, ctLength);
        dos.write(hMac.doFinal(), 0, hMac.getMacLength());
        dos.flush();
        datagramPacket.setData(baos.toByteArray(), 0, baos.size());
        super.send(datagramPacket);
    }

    private void init() {
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
        hMacKey = new SecretKeySpec(key.getEncoded(), "HmacSHA512");
    }


    @Override
    public synchronized void receive(DatagramPacket datagramPacket) throws IOException {
        super.receive(datagramPacket);
        init();

        ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData());
        DataInputStream dis = new DataInputStream(bais);
        byte[] buff = new byte[4096];

        byte firstByte = dis.readByte();
        int payloadSize = dis.readInt();


        try {
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        byte[] plainText = new byte[0];
        try {
            plainText = cipher.doFinal(datagramPacket.getData(), 5, payloadSize);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
//        int messageLength = plainText.length - hMac.getMacLength();

        try {
            hMac.init(hMacKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        hMac.update(plainText, 0, payloadSize);

        byte[] messageHash = new byte[hMac.getMacLength()];
        System.arraycopy(datagramPacket.getData(), 5 + payloadSize, messageHash, 0, messageHash.length);

        // TODO discard packet if corrupted?

//                System.out.println("plain : "+ Utils.toHex(plainText, messageLength));
//                System.out.println("Verified w/ message-integrity and message-authentication :" + MessageDigest.isEqual(hMac.doFinal(), messageHash));


        datagramPacket.setData(plainText, 0, payloadSize);

    }
}
