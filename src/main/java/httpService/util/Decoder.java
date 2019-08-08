package httpService.util;

public interface Decoder<T> {
    T decode(String entity) throws Exception;
}
