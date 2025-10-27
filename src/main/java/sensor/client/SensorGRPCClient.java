package sensor.client;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import sensor.grpc.ReadingResponse;
import sensor.grpc.SensorServiceGrpc;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SensorGRPCClient {
    private static final Logger logger = Logger.getLogger(SensorGRPCClient.class.getName());

    private final ManagedChannel channel;
    private final SensorServiceGrpc.SensorServiceBlockingStub blockingStub;

    public SensorGRPCClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        blockingStub = SensorServiceGrpc.newBlockingStub(channel);
    }

    public void stop() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public ReadingResponse getReading() {
        try {
            return blockingStub.getLastReading(Empty.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            logger.warning("RPC failed: " + e.getStatus());
            return null;
        }
    }
}
