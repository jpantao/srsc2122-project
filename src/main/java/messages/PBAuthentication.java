package messages;

public class PBAuthentication extends MessageSAPKDP {

    public static final int MSG_TYPE = 3;

    private final byte[] pbeAuth;
    private final byte[] intCheck;

    public PBAuthentication(int msgType, byte[] pbeAuth, byte[] intCheck) {
        super(msgType);
        this.pbeAuth = pbeAuth;
        this.intCheck = intCheck;
    }

    public byte[] getPbeAuth() {
        return pbeAuth;
    }

    public byte[] getIntCheck() {
        return intCheck;
    }
}
