package httpService.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UrlParser {
    private final String[] url;
    private final List<Integer> index;

    private UrlParser(String path) {
        url = path.split("/");
        index = new ArrayList<>();
        for (int i = 0; i < url.length; i++) {
            String s = url[i];
            if (s.startsWith("{") && s.endsWith("}")) {
                index.add(i);
                url[i] = url[i].substring(1, s.length() - 1);
            }
        }
    }

    public static UrlParser of(String url) {
        return new UrlParser(url);
    }

    public String[] parsePath(Map<String, String> pathVarMap) {
        String[] parseUrl = url.clone();
        for (int idx : index) {
            parseUrl[idx] = pathVarMap.get(parseUrl[idx].trim());
        }
        return parseUrl;
    }
}
