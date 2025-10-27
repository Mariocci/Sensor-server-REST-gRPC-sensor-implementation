package sensor.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import sensor.grpc.SensorServiceGrpc;
import sensor.grpc.ReadingResponse;
import sensor.ReadingGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SensorGRPCServer {
    private static final Logger logger = Logger.getLogger(SensorGRPCServer.class.getName());
    private final int port;
    private final Server server;

    public SensorGRPCServer(int port, ReadingGenerator generator) {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(new SensorServiceImpl(generator))
                .build();
    }

    public void start() throws IOException {
        server.start();
        logger.info("gRPC Server started on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    public void stop() throws InterruptedException {
        if (server != null) server.shutdown().awaitTermination();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) server.awaitTermination();
    }

    private static class SensorServiceImpl extends SensorServiceGrpc.SensorServiceImplBase {
        private final ReadingGenerator generator;

        public SensorServiceImpl(ReadingGenerator generator) {
            this.generator = generator;
        }

        @Override
        public void getLastReading(com.google.protobuf.Empty request, StreamObserver<ReadingResponse> responseObserver)
        {
            Map<String, Object> reading = generator.getLastReading();

            if (reading == null) {
                reading = new HashMap<>();
            }

            ReadingResponse response = ReadingResponse.newBuilder()
                    .setTemperature((Double) reading.getOrDefault("Temperature", 0.0))
                    .setPressure((Double) reading.getOrDefault("Pressure", 0.0))
                    .setHumidity((Double) reading.getOrDefault("Humidity", 0.0))
                    .setCo((Double) reading.getOrDefault("CO", 0.0))
                    .setSo2((Double) reading.getOrDefault("SO2", 0.0))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
