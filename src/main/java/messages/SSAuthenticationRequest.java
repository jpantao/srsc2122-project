package messages;

public class SSAuthenticationRequest extends MessageSAPKDP{

    public static final int MSG_TYPE = 2;

    private byte[] salt;


    public SSAuthenticationRequest(int msgType) {
        super(msgType);
    }
}
