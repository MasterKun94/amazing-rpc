package http;

import httpService.annotation.*;
import httpService.util.HttpMethod;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Future;

@ServiceContext(host = "127.0.0.1:8080", contextPath = "/message", showRequest = true, showResponse = true)
public interface HttpTest {
    Message getMessage();

    Future<List<Message>> postMessage(@RequestBody List<Message> messages);

    TreeMap<String, Message> putMessage(@RequestBody Message message, @RequestHeaders("head") String head);

    @RequestMapping(value = "/message/{contextPath}", method = HttpMethod.DELETE)
    Future<String> deleteMessage(@RequestParam("message") String message, @PathVariable("contextPath") String path);
}
