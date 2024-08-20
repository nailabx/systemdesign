import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KStream;
import org.apache.kafka.streams.KTable;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;

public class KafkaStreamProcessor {

    private static final Gson gson = new Gson();

    public static void main(String[] args) {

        StreamsBuilder builder = new StreamsBuilder();

        // Read the event topic as a KStream of Strings
        KStream<String, String> eventStream = builder.stream("event", Consumed.with(Serdes.String(), Serdes.String()));

        // Read the status topic as a KTable of Strings
        KTable<String, String> statusTable = builder.table("status", Consumed.with(Serdes.String(), Serdes.String()));

        // Deserialize the event stream to EventRecord objects
        KStream<String, EventRecord> eventRecordStream = eventStream.mapValues(value -> gson.fromJson(value, EventRecord.class));

        // Deserialize the status table to StatusRecord objects
        KTable<String, StatusRecord> statusRecordTable = statusTable.mapValues(value -> gson.fromJson(value, StatusRecord.class));

        // Filter and process the events based on status
        eventRecordStream.transform(() -> new Transformer<String, EventRecord, KeyValue<String, EventRecord>>() {

            private ProcessorContext context;
            private KeyValueStore<String, Long> stateStore;

            @Override
            public void init(ProcessorContext context) {
                this.context = context;
                this.stateStore = (KeyValueStore<String, Long>) context.getStateStore("UUIDStore");
            }

            @Override
            public KeyValue<String, EventRecord> transform(String key, EventRecord event) {
                if ("INITIATED".equals(event.getStatus()) || "COMPLETED".equals(event.getStatus())) {
                    // Check if the UUID exists in the status topic
                    StatusRecord statusRecord = statusRecordTable.get(key);
                    if (statusRecord == null) {
                        // UUID not found, store the timestamp and schedule a recheck in 2 hours
                        stateStore.put(key, System.currentTimeMillis());
                        context.schedule(Duration.ofHours(2), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
                            Long storedTime = stateStore.get(key);
                            if (storedTime != null && System.currentTimeMillis() - storedTime >= Duration.ofHours(2).toMillis()) {
                                // Recheck if the UUID now exists in the status topic
                                StatusRecord recheckStatusRecord = statusRecordTable.get(key);
                                if (recheckStatusRecord == null) {
                                    // UUID is still absent, mark as absent or handle accordingly
                                    markUuidAsAbsent(key);
                                }
                                // Remove the UUID from the state store
                                stateStore.delete(key);
                            }
                        });
                    } else {
                        // UUID found, proceed to call the API
                        sendNotification(event);
                    }
                } else {
                    // For other statuses, call the API directly
                    sendNotification(event);
                }
                return null; // No output to downstream
            }

            @Override
            public void close() {
                // Cleanup resources if necessary
            }

        }, "UUIDStore");

        // Start the Kafka Streams application
        // (add your Kafka Streams configuration here)
    }

    private static void sendNotification(EventRecord event) {
        // Implement your API call logic here
        System.out.println("Calling sendnotification API for event: " + event);
    }

    private static void markUuidAsAbsent(String uuid) {
        // Implement your logic to mark the UUID as absent
        System.out.println("UUID " + uuid + " is marked as absent.");
    }

    // Define the EventRecord and StatusRecord classes with their respective fields and methods
    static class EventRecord {
        private String esUuid;
        private String status;

        // Getters and setters

        public String getEsUuid() {
            return esUuid;
        }

        public void setEsUuid(String esUuid) {
            this.esUuid = esUuid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    static class StatusRecord {
        private String esUuid;
        private String status;

        // Getters and setters

        public String getEsUuid() {
            return esUuid;
        }

        public void setEsUuid(String esUuid) {
            this.esUuid = esUuid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
