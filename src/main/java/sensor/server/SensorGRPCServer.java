package sensor.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import sensor.grpc.SensorServiceGrpc;
import sensor.grpc.ReadingRequest;
import sensor.grpc.ReadingResponse;
import sensor.ReadingGenerator;

import java.io.IOException;
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

            ReadingResponse response = ReadingResponse.newBuilder()
                    .setTemperature((Double) reading.get("Temperature"))
                    .setPressure((Double) reading.get("Pressure"))
                    .setHumidity((Double) reading.get("Humidity"))
                    .setCo(reading.get("CO") != null ? (Double) reading.get("CO") : 0)
                    .setSo2(reading.get("SO2") != null ? (Double) reading.get("SO2") : 0)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
