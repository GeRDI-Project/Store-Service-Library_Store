package de.gerdiproject.store.kafka;

import de.gerdiproject.store.data.model.ResearchData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.Queue;

public class GerdiKafkaConsumer extends Thread {

    private final Queue<ResearchData> queue;
    private final KafkaConsumer<String, ByteBuffer> consumer;
    private final String id;

    public GerdiKafkaConsumer(Queue<ResearchData> queue, String id){
        this.queue = queue;
        this.id = id;
        Properties props = new Properties();
        props.put("bootstrap.servers", "kafka-1.kafka-service.default.svc.cluster.local:9092");
        props.put("group.id", id); // TODO: How-to use this attribute
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "de.gerdiproject.store.data.kafka.serializer.StoreDataDeserializer");
        props.put("value.deserializer", "de.gerdiproject.store.data.kafka.serializer.StoreDataDeserializer");
        consumer = new KafkaConsumer<String, ByteBuffer>(props);
        consumer.subscribe(Arrays.asList("store"));
    }

    // TODO: Only read relevant elements
    @Override
    public void run() {
        ResearchData val = null;
        while (true) {
            ConsumerRecords<String, ByteBuffer> records = consumer.poll(Duration.ofMillis(100)); // TODO: How is the offset incremented? Must not loose unprocessed files
            for (ConsumerRecord<String, ByteBuffer> record : records)
                val = ResearchData.fromByteBuffer(record.value()); // TODO: try-catch
                if (val == null) continue;
                boolean inserted = false; // Better strategy: Count attempts and log if it exceeds a certain threshold
                do {
                    inserted = queue.offer(val);
                } while (!inserted);
        }
    }
}
