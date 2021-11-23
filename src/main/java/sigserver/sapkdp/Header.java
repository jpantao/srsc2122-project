package sigserver.sapkdp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Header {

    public static int BYTE_LEN = 3;

    private static int VRS_MASK = -16;
    private static int TYP_MASK = 15;

    private final int version;
    private final int msgType;
    private final short payloadSize;

    public Header(int version, int msgType, short payloadSize) {
        this.version = version;
        this.msgType = msgType;
        this.payloadSize = payloadSize;
    }

    public Header(byte[] header) {
        this.version = (header[0] & VRS_MASK) >> 4;
        this.msgType = header[0] & TYP_MASK;

        ByteBuffer buf = ByteBuffer.allocate(2);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put(header[1]);
        buf.put(header[2]);
        this.payloadSize = buf.getShort(0);
    }

    public int getVersion() {
        return version;
    }

    public int getMsgType() {
        return msgType;
    }

    public short getPayloadSize() {
        return payloadSize;
    }

    public byte[] encode(){
        byte vt = (byte) ((ProtoSAPKDP.VERSION << 4) | msgType);
        ByteBuffer buf = ByteBuffer.allocate(BYTE_LEN);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put(vt);
        buf.putShort(payloadSize);
        return buf.array();
    }

    @Override
    public String toString() {
        return "Header{" +
                "version=" + version +
                ", msgType=" + msgType +
                ", payloadSize=" + payloadSize +
                '}';
    }
}
