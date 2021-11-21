package messages;

public class SSAuthenticationRequest extends MessageSAPKDP{

    public static final int MSG_TYPE = 2;

    int n1;
    private byte[] salt;
    private int counter;

    public SSAuthenticationRequest(int n1, byte[] salt, int counter) {
        super(MSG_TYPE);
        this.n1 = n1;
        this.salt = salt;
        this.counter = counter;
    }

    public int getN1() {
        return n1;
    }

    public byte[] getSalt() {
        return salt;
    }

    public int getCounter() {
        return counter;
    }
}
