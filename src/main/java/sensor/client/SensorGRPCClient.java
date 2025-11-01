package sensor.client;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import com.google.protobuf.Empty;
import sensor.grpc.SensorServiceGrpc;
import sensor.grpc.ReadingResponse;

public class SensorGRPCClient {

    private final ManagedChannel channel;
    private final SensorServiceGrpc.SensorServiceBlockingStub stub;

    public SensorGRPCClient(String host, int port) {
        this.channel = NettyChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = SensorServiceGrpc.newBlockingStub(channel);
    }

    public ReadingResponse getLastReading() {
        return stub.getLastReading(Empty.getDefaultInstance());
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

}
