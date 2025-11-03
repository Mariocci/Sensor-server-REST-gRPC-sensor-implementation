package sensor;

import sensor.client.SensorGRPCClient;
import sensor.client.ServerClient;
import sensor.dto.ReadingDto;
import sensor.utils.CalibrationUtil;
import sensor.utils.CsvReader;
import sensor.utils.NeighborInfo;

import sensor.grpc.ReadingResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ReadingGenerator {

    private NeighborInfo neighbor;
    private final ServerClient serverClient;
    private final long sensorId;
    private final List<Map<String, Object>> csvReadings;
    private long activeSeconds = 0;
    private Map<String, Object> lastReading;
    private int updateNeighborCounter = 0;
    private final int updateNeighborEvery = 10;
    private SensorGRPCClient neighborGRPCClient;
    private volatile boolean running = true;

    public NeighborInfo getNeighbor() {
        return neighbor;
    }

    public void setNeighbors(NeighborInfo neighbor) {
        this.neighbor = neighbor;
    }

    public ServerClient getServerClient() {
        return serverClient;
    }

    public long getSensorId() {
        return sensorId;
    }

    public List<Map<String, Object>> getCsvReadings() {
        return csvReadings;
    }

    public long getActiveSeconds() {
        return activeSeconds;
    }

    public void setActiveSeconds(long activeSeconds) {
        this.activeSeconds = activeSeconds;
    }

    public Map<String, Object> getLastReading() {
        return lastReading;
    }

    public void setLastReading(Map<String, Object> lastReading) {
        this.lastReading = lastReading;
    }

    public void stop() {
        running = false;
        if (neighborGRPCClient != null) {
            neighborGRPCClient.shutdown();
        }
    }

    public ReadingGenerator(ServerClient serverClient, long sensorId, String csvPath, NeighborInfo neighbor) throws Exception {
        this.serverClient = serverClient;
        this.sensorId = sensorId;
        this.csvReadings = CsvReader.readCsv(csvPath);
        this.neighbor = neighbor;
        if (neighbor != null) {
            neighborGRPCClient = new SensorGRPCClient(neighbor.getIp(), neighbor.getPort());
        }
    }

    public void runLoop() throws Exception {
        while (running) {
            updateNeighborCounter++;
            if (updateNeighborCounter >= updateNeighborEvery) {
                updateNeighborCounter = 0;
                refreshNeighbor();
            }
            Map<String, Object> neighborReading = null;
            boolean neighborAvailable = false;

            if (neighbor != null && neighborGRPCClient != null) {
                try {
                    ReadingResponse neighborResp = neighborGRPCClient.getLastReading();
                    Map<String, Object> neighborMap = convertToMap(neighborResp);

                    if (neighborMap != null && neighbor.getId() != sensorId) {
                        neighbor.setLastReading(neighborMap);
                        neighborReading = neighborMap;
                        neighborAvailable = true;
                    } else {
                        System.out.println("Skipping calibration: neighbor is self or invalid.");
                    }
                } catch (Exception e) {
                    System.err.println("Neighbor unavailable: " + e.getMessage());
                }
            }

            Thread.sleep(1000);
            activeSeconds++;

            int rowIndex = (int) (activeSeconds % csvReadings.size());
            Map<String, Object> reading = new HashMap<>(csvReadings.get(rowIndex));
            System.out.println(reading);
            lastReading = reading;

            if (neighborAvailable && neighborReading != null) {
                reading = CalibrationUtil.calibrate(reading, neighborReading);
            } else {
                System.out.println("Skipping calibration: neighbor data not available.");
            }

            ReadingDto readingDto = new ReadingDto();
            readingDto.setTemperature(getDouble(reading, "Temperature"));
            readingDto.setPressure(getDouble(reading, "Pressure"));
            readingDto.setHumidity(getDouble(reading, "Humidity"));
            readingDto.setCo(getDouble(reading, "CO"));
            readingDto.setSo2(getDouble(reading, "SO2"));

            System.out.println("Sensor " + sensorId + " sending reading: " +
                    "Temp=" + readingDto.getTemperature() +
                    ", Pressure=" + readingDto.getPressure() +
                    ", Humidity=" + readingDto.getHumidity() +
                    ", CO=" + readingDto.getCo() +
                    ", SO2=" + readingDto.getSo2());

            serverClient.sendReading(sensorId, readingDto).execute();
        }
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private Map<String, Object> convertToMap(ReadingResponse resp) {
        Map<String, Object> map = new HashMap<>();
        map.put("Temperature", resp.getTemperature());
        map.put("Pressure", resp.getPressure());
        map.put("Humidity", resp.getHumidity());
        map.put("CO", resp.getCo());
        map.put("SO2", resp.getSo2());
        return map;
    }

    private void refreshNeighbor() {
        try {
            var nearestResponse = serverClient.getNearest(sensorId).execute();
            if (nearestResponse.isSuccessful() && nearestResponse.body() != null) {
                Sensor nearestSensor = nearestResponse.body();
                if (nearestSensor.getId() != sensorId) {
                    NeighborInfo newNeighbor = new NeighborInfo(
                            nearestSensor.getId(),
                            nearestSensor.getIp(),
                            nearestSensor.getPort()
                    );

                    if (neighbor == null || neighbor.getId() != newNeighbor.getId()) {
                        neighbor = newNeighbor;
                        System.out.printf("Sensor %d updated neighbor: ID=%d, IP=%s, Port=%d%n",
                                sensorId,
                                neighbor.getId(),
                                neighbor.getIp(),
                                neighbor.getPort());

                        if (neighborGRPCClient != null) {
                            neighborGRPCClient.shutdown();
                        }

                        neighborGRPCClient = new SensorGRPCClient(neighbor.getIp(), neighbor.getPort());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh neighbor: " + e.getMessage());
        }
    }
}