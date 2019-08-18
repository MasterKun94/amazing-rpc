package http;

import httpService.util.fallBack.FallBackInfo;

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class TestFallBack implements HttpTest {
    private Message message = new Message() {
        {
            setMessage("fall back");
        }
    };
    List<Message> messages1 = Collections.singletonList(message);

    @Override
    public Message getMessage() {
        return message;
    }

    @Override
    public Future<List<Message>> postMessage(List<Message> messages) {
        Message message = new Message() {
            {
                setMessage(FallBackInfo.getCause().toString());
            }
        };
        List<Message> messages1 = Collections.singletonList(message);
        return CompletableFuture.completedFuture(messages1);
    }

    @Override
    public TreeMap<String, Message> putMessage(Message message0, String head) {
        Message message = new Message() {
            {
                setMessage(FallBackInfo.getCause().toString());
            }
        };
        TreeMap<String, Message> messageTreeMap = new TreeMap<>();
        messageTreeMap.put("test", message);
        return messageTreeMap;
    }

    @Override
    public Future<String> deleteMessage(String message0, String path) {
        Message message = new Message() {
            {
                setMessage(FallBackInfo.getCause().toString());
            }
        };
        return CompletableFuture.completedFuture(FallBackInfo.getCause().toString());
    }
}
