package httpService.util;

import java.util.List;

public class Monitors {
    private List<Monitor> decoratorList;

    public void addMonitor(Monitor monitor) {
        this.decoratorList.add(monitor);
    }


}
