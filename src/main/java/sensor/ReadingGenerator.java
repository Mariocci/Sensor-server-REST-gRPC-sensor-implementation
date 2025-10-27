package sensor;

import sensor.client.ServerClient;
import sensor.dto.ReadingDto;
import sensor.grpc.ReadingResponse;
import sensor.utils.CalibrationUtil;
import sensor.utils.CsvReader;
import sensor.utils.NeighborInfo;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import sensor.grpc.SensorServiceGrpc;
import sensor.grpc.ReadingResponse;
import com.google.protobuf.Empty;

import java.util.ArrayList;
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
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(neighbor.getIp(), neighbor.getPort())
                    .usePlaintext()
                    .build();
            neighborClient = SensorServiceGrpc.newBlockingStub(channel);
        }
    }

    public void runLoop() throws Exception {
        while (true) {
            ReadingResponse neighborReading = neighborClient.getLastReading(Empty.getDefaultInstance());
            neighbor.setLastReading(convertToMap(neighborReading));
            Thread.sleep(1000);
            activeSeconds++;

            int rowIndex = (int)(activeSeconds % csvReadings.size());
            Map<String, Object> reading = new HashMap<>(csvReadings.get(rowIndex));
            System.out.println(reading);
            lastReading = reading;

            if (neighbor != null && neighbor.getLastReading() != null) {
                reading = CalibrationUtil.calibrate(reading, neighbor.getLastReading());
            }


            ReadingDto readingDto = new ReadingDto();
            readingDto.setTemperature(getDouble(reading, "Temperature"));
            readingDto.setPressure(getDouble(reading, "Pressure"));
            readingDto.setHumidity(getDouble(reading, "Humidity"));
            readingDto.setCo(getDouble(reading, "CO"));
            readingDto.setSo2(getDouble(reading, "SO2"));

            System.out.println("Sensor " + sensorId + " sending reading: " +
                    "temp=" + readingDto.getTemperature() +
                    ", pressure=" + readingDto.getPressure() +
                    ", humidity=" + readingDto.getHumidity() +
                    ", co=" + readingDto.getCo() +
                    ", so2=" + readingDto.getSo2());

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
}
