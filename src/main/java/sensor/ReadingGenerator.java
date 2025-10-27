package sensor;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import sensor.client.ServerClient;
import sensor.dto.ReadingDto;
import sensor.utils.CalibrationUtil;
import sensor.utils.CsvReader;
import sensor.utils.NeighborInfo;
import com.google.protobuf.Empty;
import sensor.grpc.ReadingResponse;
import sensor.grpc.SensorServiceGrpc;

import java.net.InetSocketAddress;
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
    private SensorServiceGrpc.SensorServiceBlockingStub neighborClient;
    private int updateNeighborCounter = 0;
    private final int updateNeighborEvery = 10;

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

    public ReadingGenerator(ServerClient serverClient, long sensorId, String csvPath, NeighborInfo neighbor) throws Exception {
        this.serverClient = serverClient;
        this.sensorId = sensorId;
        this.csvReadings = CsvReader.readCsv(csvPath);
        this.neighbor = neighbor;
        if (neighbor != null) {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", neighbor.getPort());
            
            ManagedChannel channel = NettyChannelBuilder
                    .forAddress(address)
                    .usePlaintext()
                    .build();
            neighborClient = SensorServiceGrpc.newBlockingStub(channel);
        }
    }

    public void runLoop() throws Exception {
        while (true) {
            updateNeighborCounter++;
            if (updateNeighborCounter >= updateNeighborEvery) {
                updateNeighborCounter = 0; // reset
                refreshNeighbor();
            }
            if (neighbor != null && neighborClient != null) {
                try {
                    ReadingResponse neighborReading = neighborClient.getLastReading(Empty.getDefaultInstance());
                    neighbor.setLastReading(convertToMap(neighborReading));
                } catch (Exception e) {
                    System.err.println("Failed to get reading from neighbor: " + e.getMessage());
                }
            }
            Thread.sleep(1000);
            activeSeconds++;

            int rowIndex = (int)(activeSeconds % csvReadings.size());
            Map<String, Object> reading = new HashMap<>(csvReadings.get(rowIndex));
            System.out.println(reading);
            lastReading = reading;

            if (neighbor != null && neighbor.getLastReading() != null) {
                Map<String, Object> neighborReading = neighbor.getLastReading();

                boolean hasValidNeighborData = neighborReading.values().stream()
                        .anyMatch(v -> v != null && ((v instanceof Number && ((Number)v).doubleValue() != 0) || v instanceof String));

                if (hasValidNeighborData) {
                    reading = CalibrationUtil.calibrate(reading, neighborReading);
                } else {
                    System.out.println("Skipping calibration because neighbor data is not yet valid.");
                }
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

                        InetSocketAddress address = new InetSocketAddress(neighbor.getIp(), neighbor.getPort());
                        var channel = NettyChannelBuilder.forAddress(address)
                                .usePlaintext()
                                .build();
                        neighborClient = SensorServiceGrpc.newBlockingStub(channel);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh neighbor: " + e.getMessage());
        }
    }
}