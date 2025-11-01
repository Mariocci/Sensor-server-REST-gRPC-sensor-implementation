package sensor;

import sensor.client.ServerClient;
import sensor.server.SensorGRPCServer;
import sensor.utils.NeighborInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public class Sensor {
    private final double latitude;
    private final double longitude;
    private final int port;
    private long id;
    private NeighborInfo neighbor;
    private String ip;


    public Sensor(double latitude, double longitude, int grpcPort) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.port = grpcPort;
        this.id = -1;
        this.neighbor = null;
        this.ip = "127.0.0.1";
    }

    public void start() throws Exception {

        System.out.printf("Sensor started at [lat=%.5f, lon=%.5f] using gRPC port %d%n", latitude, longitude, port);

        ServerClient serverClient = ServerClient.create("http://localhost:8080");

        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("latitude", latitude);
        registrationData.put("longitude", longitude);
        registrationData.put("port", port);
        registrationData.put("ip", "127.0.0.1");

        Map<String, Object> response = serverClient.registerSensor(registrationData).execute().body();

        assert response != null;
        long sensorId = ((Number) response.get("id")).longValue();
        this.id = sensorId;
        System.out.println("Registered sensor with ID: " + sensorId);

        var nearestResponse = serverClient.getNearest(sensorId).execute();
        if (nearestResponse.isSuccessful() && nearestResponse.body() != null) {
            Sensor nearestSensor = nearestResponse.body();
            if (nearestSensor.getId() != sensorId) {
                neighbor = new NeighborInfo(
                        nearestSensor.getId(),
                        nearestSensor.getIp(),
                        nearestSensor.getPort()
                );
                System.out.printf("Sensor %d has neighbor: ID=%d, IP=%s, Port=%d%n",
                        sensorId,
                        neighbor.getId(),
                        neighbor.getIp(),
                        neighbor.getPort());
            }
        } else {
            System.out.println("No neighbor found at this moment.");
        }

        ReadingGenerator readingGenerator = new ReadingGenerator(
                serverClient,
                sensorId,
                "C:\\Users\\mario\\Projects\\Sensor\\src\\main\\java\\data\\readings.csv",
                neighbor
        );
        SensorGRPCServer grpcServer = new SensorGRPCServer(port, readingGenerator);
        grpcServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down sensor " + sensorId);
            try {
                readingGenerator.stop();
                grpcServer.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        new Thread(() -> {
            try {
                readingGenerator.runLoop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        grpcServer.blockUntilShutdown();

    }

    public static void main(String[] args) throws Exception {
        System.setProperty("io.grpc.netty.shaded.io.netty.transport.noNative", "true");
        double latitude = 45.75 + Math.random() * 0.1;
        double longitude = 15.87 + Math.random() * 0.13;
        int grpcPort = getAvailablePort();

        Sensor sensor = new Sensor(latitude, longitude, grpcPort);
        sensor.start();
    }

    private static int getAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("No available port found", e);
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getPort() {
        return port;
    }

    public long getId() {
        return id;
    }

    public NeighborInfo getNeighbor() {
        return neighbor;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setNeighbor(NeighborInfo neighbor) {
        this.neighbor = neighbor;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}

