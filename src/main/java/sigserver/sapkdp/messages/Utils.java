package sigserver.sapkdp.messages;

import java.io.DataInputStream;
import java.io.IOException;

public class Utils {


    static String getString(DataInputStream dai) throws IOException {
        int read;
        int strSize = dai.readInt();
        byte[] strBytes = new byte[strSize];
        read = dai.read(strBytes);
        if (read != strSize)
            throw new IOException("read " + read + " should have been " + strSize);
        return new String(strBytes);
    }
}
