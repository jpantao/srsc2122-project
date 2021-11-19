package pt.unl.fct.srsc.common;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.rmi.CORBA.Util;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.*;
import java.util.Properties;


// E (M, key1) || HMAC(M, key2)



public class SecureDatagramSocket extends DatagramSocket {
    public static final int HEADER_SIZE = 5;
    byte[] keyBytes;

    byte[] ivBytes;
    SecretKeySpec key;
    IvParameterSpec ivSpec;
    Mac hMac;
    Cipher cipher;
    Key hMacKey;


    public SecureDatagramSocket() throws SocketException {
        super();
        init();
    }


    public SecureDatagramSocket(SocketAddress inSocketAddress) throws SocketException {
        super(inSocketAddress);
        init();
    }


    private void init() {


        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream("pt/unl/fct/srsc/common/config.properties");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("Configuration file not found!");
            System.exit(1);
        }

        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String algorithm = properties.getProperty("algorithm");
        String options = properties.getProperty("options");
        String hmac = properties.getProperty("hmac");
        keyBytes = Utils.decodeHexString(properties.getProperty("keyBytes"));
        ivBytes = Utils.decodeHexString(properties.getProperty("ivBytes"));
        byte[] hmacBytes = Utils.decodeHexString(properties.getProperty("hmacBytes"));
        key = new SecretKeySpec(keyBytes, algorithm);
        ivSpec = new IvParameterSpec(ivBytes);
        cipher = null;
        hMac = null;
        try {
            cipher = Cipher.getInstance(options);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        try {
            hMac = Mac.getInstance(hmac);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        hMacKey = new SecretKeySpec(hmacBytes, hmac);
    }

    @Override
    public void send(DatagramPacket datagramPacket) throws IOException {
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
        hMac.update(cipherText, 0, ctLength);



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
        dos.close();
        baos.close();
    }



    @Override
    public synchronized void receive(DatagramPacket datagramPacket) throws IOException {
        super.receive(datagramPacket);

        ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData());
        DataInputStream dis = new DataInputStream(bais);

        byte firstByte = dis.readByte();
        int payloadSize = dis.readInt();

        byte[] messageHash = new byte[hMac.getMacLength()];
        System.arraycopy(datagramPacket.getData(), HEADER_SIZE + payloadSize, messageHash, 0, messageHash.length);

        try {
            hMac.init(hMacKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


        hMac.update(datagramPacket.getData(), HEADER_SIZE, payloadSize);


        if (!MessageDigest.isEqual(hMac.doFinal(), messageHash)) {
            System.out.println("Integrity check failed");
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        byte[] plainText = new byte[0];
        try {
            plainText = cipher.doFinal(datagramPacket.getData(), HEADER_SIZE, payloadSize);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
//        int messageLength = plainText.length - hMac.getMacLength();





//                System.out.println("plain : "+ Utils.toHex(plainText, payloadSize));
//                System.out.println("Verified w/ message-integrity and message-authentication :" + MessageDigest.isEqual(hMac.doFinal(), messageHash));


        datagramPacket.setData(plainText, 0, payloadSize);

    }
}
