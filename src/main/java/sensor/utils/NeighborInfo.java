package sensor.utils;

import java.util.Map;

public class NeighborInfo {
    private long id;
    private String ip;
    private int port;
    private Map<String, Object> lastReading;


    public NeighborInfo(long id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public long getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
    public Map<String, Object> getLastReading() {
        return lastReading;
    }

    public void setLastReading(Map<String, Object> lastReading) {
        this.lastReading = lastReading;
    }

    @Override
    public String toString() {
        return "NeighborInfo{id=" + id + ", ip='" + ip + "', port=" + port + "}";
    }
}