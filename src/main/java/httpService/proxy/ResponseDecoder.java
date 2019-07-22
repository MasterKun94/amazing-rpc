package httpService.proxy;

public interface ResponseDecoder<T> {
    T decode(String entity) throws Exception;
}
