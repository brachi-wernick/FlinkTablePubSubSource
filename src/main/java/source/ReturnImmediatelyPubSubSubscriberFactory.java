package source;

import com.google.auth.Credentials;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.SubscriberGrpc;
import io.grpc.ManagedChannel;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.apache.flink.streaming.connectors.gcp.pubsub.BlockingGrpcPubSubSubscriber;
import org.apache.flink.streaming.connectors.gcp.pubsub.common.PubSubSubscriber;
import org.apache.flink.streaming.connectors.gcp.pubsub.common.PubSubSubscriberFactory;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.time.Duration;

public class ReturnImmediatelyPubSubSubscriberFactory implements PubSubSubscriberFactory {

    private final int retries;
    private final Duration timeout;
    private final int maxMessagesPerPull;
    private final String projectSubscriptionName;

    ReturnImmediatelyPubSubSubscriberFactory(
            String projectSubscriptionName,
            int retries,
            Duration pullTimeout,
            int maxMessagesPerPull) {
        this.retries = retries;
        this.timeout = pullTimeout;
        this.maxMessagesPerPull = maxMessagesPerPull;
        this.projectSubscriptionName = projectSubscriptionName;
    }

    public PubSubSubscriber getSubscriber(Credentials credentials) throws IOException {
        ManagedChannel channel =
                NettyChannelBuilder.forTarget(SubscriberStubSettings.getDefaultEndpoint())
                        .negotiationType(NegotiationType.TLS)
                        .sslContext(GrpcSslContexts.forClient().ciphers(null).build())
                        .build();

        PullRequest pullRequest =
                PullRequest.newBuilder()
                        .setMaxMessages(maxMessagesPerPull)
                        .setSubscription(projectSubscriptionName)
                        .setReturnImmediately(true)
                        .build();
        SubscriberGrpc.SubscriberBlockingStub stub =
                SubscriberGrpc.newBlockingStub(channel)
                        .withCallCredentials(MoreCallCredentials.from(credentials));
        return new BlockingGrpcPubSubSubscriber(
                projectSubscriptionName, channel, stub, pullRequest, retries, timeout);
    }
}
