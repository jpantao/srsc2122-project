package common;

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
    String options;
    private static final SecureRandom random = new SecureRandom();
    public static final int HEADER_SIZE = 5;
    private boolean noHMAC = false;
    String algorithm;
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
            inputStream = new FileInputStream("config/config.properties");
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



        algorithm = properties.getProperty("algorithm");

        options = properties.getProperty("options");
        if ((options.contains("CCM")) || (options.contains("GCM")))
            noHMAC = true;
        String hmac = properties.getProperty("hmac");
        keyBytes = Utils.decodeHexString(properties.getProperty("keyBytes"));
        ivBytes = Utils.decodeHexString(properties.getProperty("ivBytes"));
        byte[] hmacBytes = Utils.decodeHexString(properties.getProperty("hmacBytes"));


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

        if (!noHMAC) {
            try {
                hMac = Mac.getInstance(hmac);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            hMacKey = new SecretKeySpec(hmacBytes, hmac);
        }

        ivSpec = new IvParameterSpec(ivBytes);
        initCipher(opmode, algorithm, ivSpec);

    }

    private void initCipher(int opmode, String algorithm, IvParameterSpec ivSpec) {
        key = new SecretKeySpec(keyBytes, algorithm);
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
            if (noHMAC) {
                if (options.contains("GCM")) {
                    ivSpec = Utils.createCtrIvForAES(random.nextInt(), random);
                    initCipher(Cipher.ENCRYPT_MODE, algorithm, ivSpec);
                } else {
                    ivSpec = Utils.createCtrIvForCCM(random);
                    initCipher(Cipher.ENCRYPT_MODE, algorithm, ivSpec);
                }
            }
            ctLength = cipher.doFinal(datagramPacket.getData(), 0, datagramPacket.getLength(), cipherText, 0);
        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        if (!noHMAC) {
            try {
                hMac.init(hMacKey);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            hMac.update(cipherText, 0, ctLength);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        byte firstByte = 0b00010000;
//        int halfByte = (firstByte >> 4);
//        int otherHalfByte = (firstByte & 0b00001111);
        dos.writeByte(firstByte);
        dos.writeInt(ctLength);
        dos.write(cipherText, 0, ctLength);
        if (noHMAC)
            dos.write(ivSpec.getIV());
        else
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
        boolean drop = false;

        ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData());
        DataInputStream dis = new DataInputStream(bais);

        byte firstByte = dis.readByte();
//        TODO v2
//        firstHalfByte =
//        secondHalfByte =
        int payloadSize = dis.readInt();

        dis.close();
        bais.close();

//        datagramPacket.getData()[59] = 4;
        if (noHMAC) {
            if (options.contains("GCM")) {
                ivBytes = new byte[16];
                System.arraycopy(datagramPacket.getData(), HEADER_SIZE + payloadSize, ivBytes, 0, ivBytes.length);
            } else {
                ivBytes = new byte[13];
                System.arraycopy(datagramPacket.getData(), HEADER_SIZE + payloadSize, ivBytes, 0, ivBytes.length);
            }
        } else {
            byte[] messageHash = new byte[hMac.getMacLength()];
            System.arraycopy(datagramPacket.getData(), HEADER_SIZE + payloadSize, messageHash, 0, messageHash.length);

            try {
                hMac.init(hMacKey);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            hMac.update(datagramPacket.getData(), HEADER_SIZE, payloadSize);


            if (!MessageDigest.isEqual(hMac.doFinal(), messageHash)) {
                drop = true;
            }
        }


        if (!drop) {
            int ptLength = 0;
            byte[] plainText = new byte[cipher.getOutputSize(payloadSize)];

            try {
                if (noHMAC) {
                    ivSpec = new IvParameterSpec(ivBytes);
                    initCipher(Cipher.DECRYPT_MODE, algorithm, ivSpec);
                }
                ptLength = cipher.doFinal(datagramPacket.getData(), HEADER_SIZE, payloadSize, plainText, 0);
                datagramPacket.setData(plainText, 0, ptLength);
            } catch (AEADBadTagException e) {
                System.out.println("Integrity check failed");
                datagramPacket.setData(plainText, 0, 0);
            } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }
        } else {
            datagramPacket.setData(datagramPacket.getData(), 0, 0);
        }
    }
}
