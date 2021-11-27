package pt.unl.fct.srsc.common;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.*;
import java.util.Properties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class SecureDatagramSocket extends DatagramSocket {
    public static final int HEADER_SIZE = 5;
    byte[] keyBytes;
    byte[] ivBytes;
    SecretKeySpec key;
    IvParameterSpec ivSpec;
    Mac hMac = null;
    Cipher cipher = null;
    Key hMacKey;
    public SecureDatagramSocket() throws SocketException {
        super();
        init(Cipher.ENCRYPT_MODE);
    }


    public SecureDatagramSocket(SocketAddress inSocketAddress) throws SocketException {
        super(inSocketAddress);
        init(Cipher.DECRYPT_MODE);
    }


    private void init(int opmode) {

        Provider provider = Security.getProvider("BC");
        if (provider == null) {
            Security.addProvider(new BouncyCastleProvider());
        }


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
        try {
            cipher = Cipher.getInstance(options, "BC");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            try {
                cipher = Cipher.getInstance(options);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
                ex.printStackTrace();
            }
        }

        try {
            hMac = Mac.getInstance(hmac);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        hMacKey = new SecretKeySpec(hmacBytes, hmac);

        try {
            cipher.init(opmode, key, ivSpec);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            try {
                cipher.init(opmode, key);
            } catch (InvalidKeyException ex) {
                ex.printStackTrace();
            }
        }

    }

    @Override
    public void send(DatagramPacket datagramPacket) throws IOException {

        byte[] cipherText = new byte[cipher.getOutputSize(datagramPacket.getLength())];

        int ctLength = 0;

        try {
            ctLength = cipher.doFinal(datagramPacket.getData(), 0, datagramPacket.getLength(), cipherText, 0);
        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

  /*      try {
            hMac.init(hMacKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        hMac.update(cipherText, 0, ctLength);

        int sizeOfCt = ctLength;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        byte firstByte = 0b00010000;
//        int halfByte = (firstByte >> 4);
//        int otherHalfByte = (firstByte & 0b00001111);
        dos.writeByte(firstByte);
        dos.writeInt(sizeOfCt);
        dos.write(cipherText, 0, ctLength);
        dos.write(hMac.doFinal(), 0, hMac.getMacLength());
        dos.flush();*/
        System.out.printf("Original plaintext size: %d, hexBytes: %s\n", datagramPacket.getLength(), Utils.encodeHexString(datagramPacket.getData()));
        System.out.printf("Sent Ciphertext size: %d, hexBytes: %s\n", ctLength, Utils.encodeHexString(cipherText));
        datagramPacket.setData(cipherText, 0, ctLength);

        super.send(datagramPacket);
//        dos.close();
//        baos.close();
    }



    @Override
    public synchronized void receive(DatagramPacket datagramPacket) throws IOException {
        super.receive(datagramPacket);

//        ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData());
//        DataInputStream dis = new DataInputStream(bais);

//        byte firstByte = dis.readByte();
        //TODO v2
//        firstHalfByte =
//        secondHalfByte =
//        int payloadSize = dis.readInt();

//        dis.close();
//        bais.close();

//        byte[] messageHash = new byte[hMac.getMacLength()];
//        System.arraycopy(datagramPacket.getData(), HEADER_SIZE + payloadSize, messageHash, 0, messageHash.length);

//        try {
//            hMac.init(hMacKey);
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        }


//      Integrity debbuging

//        byte[] temp = datagramPacket.getData();
//        temp[10] ~= 1 ^ 9;
//        hMac.update(temp, HEADER_SIZE, payloadSize);

//        hMac.update(datagramPacket.getData(), HEADER_SIZE, payloadSize);


//        if (!MessageDigest.isEqual(hMac.doFinal(), messageHash)) {
//            System.out.println("Integrity check failed");
//            TODO DROP PACKET?
//        }



        int ptLength = 0;
        byte[] plainText = new byte[cipher.getOutputSize(datagramPacket.getLength())];

        try {
            ptLength = cipher.doFinal(datagramPacket.getData(), 0, datagramPacket.getLength(), plainText, 0);

        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        System.out.printf("Received Ciphertext size: %d, hexBytes: %s\n", datagramPacket.getLength(), Utils.encodeHexString(datagramPacket.getData()));
        System.out.printf("Original plaintext size: %d, hexBytes: %s\n", ptLength, Utils.encodeHexString(plainText));
        datagramPacket.setData(plainText, 0, ptLength);

    }
}
