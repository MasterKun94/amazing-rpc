package httpService.proxy;

import java.util.ArrayList;
import java.util.List;

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

    public StringBuilder parsePath(String[][] pathVarMap) {
        String[] parseUrl = url.clone();
        if (index.size() > 0) {
            for (String[] pathVar : pathVarMap) {
                parseUrl[Integer.valueOf(pathVar[0])] = pathVar[1];
            }
        }
        StringBuilder urlBuilder = new StringBuilder();
        for (String s : parseUrl) {
            if (s != null && !s.equals(""))
                urlBuilder.append("/").append(s);
        }
        return urlBuilder;
    }

    public String getIndex(String name) {
        for (int index : index) {
            if (url[index].trim().equals(name.trim())) {
                return String.valueOf(index);
            }
        }
        throw new IllegalArgumentException(this.toString());
    }
}
