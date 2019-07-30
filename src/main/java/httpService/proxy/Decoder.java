package httpService.proxy;

public interface Decoder<T> {
    T decode(String entity) throws Exception;
}
