package source;

import com.google.pubsub.v1.ProjectSubscriptionName;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.streaming.connectors.gcp.pubsub.PubSubSource;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

public class PubsubDynamicSource implements ScanTableSource {
    private final String project;
    private final String topic;
    private final DecodingFormat<DeserializationSchema<RowData>> decodingFormat;
    private final DataType producedDataType;
    private static Logger logger = LoggerFactory.getLogger(PubsubDynamicSource.class);

    public PubsubDynamicSource(String project, String topic, DecodingFormat<DeserializationSchema<RowData>> decodingFormat, DataType producedDataType) {


        this.project = project;
        this.topic = topic;
        this.decodingFormat = decodingFormat;
        this.producedDataType = producedDataType;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return ChangelogMode.insertOnly();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext) {
        // create runtime classes that are shipped to the cluster

        final DeserializationSchema<RowData> deserializer =
                decodingFormat.createRuntimeDecoder(runtimeProviderContext, producedDataType);

        try {
            logger.info("create pubsub source");
            PubSubSource<RowData> source = PubSubSource.newBuilder()
                    .withDeserializationSchema(deserializer)
                    .withProjectName(project)
                    .withSubscriptionName(topic)
                    .withPubSubSubscriberFactory(new ReturnImmediatelyPubSubSubscriberFactory(
                            ProjectSubscriptionName.format(project, topic),
                            3,
                            Duration.ofSeconds(15),
                            100
                    ))
                    .build();
            return SourceFunctionProvider.of(source, false);
        } catch (IOException e) {
            throw new RuntimeException("failed to create source",e);
        }
    }


    @Override
    public DynamicTableSource copy() {
        return new PubsubDynamicSource(
                project, topic, decodingFormat, producedDataType);
    }

    @Override
    public String asSummaryString() {
        return "PubSub";
    }
}
