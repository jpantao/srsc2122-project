package sigserver.sapkdp.messages;

public interface Serializer<T> {

    byte[] serialize(T t);

    T deserialize(byte[] buf);

}
