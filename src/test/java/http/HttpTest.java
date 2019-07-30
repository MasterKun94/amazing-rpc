package http;

import httpService.annotation.*;
import httpService.proxy.HttpMethod;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Future;

@ServiceContext(host = "127.0.0.1:8080", path = "/message", showRequest = true, showResponse = true)
public interface HttpTest {
    Message getMessage();

    Future<List<Message>> postMessage(@RequestBody List<Message> messages);

    TreeMap<String, Message> putMessage(@RequestBody Message message, @RequestHeaders("head") String head);

    @RequestMapping(value = "/message/{path}", method = HttpMethod.DELETE)
    Future<String> deleteMessage(@RequestParam("message") String message, @PathVariable("path") String path);
}
