/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.store.kafka;


public class GerdiKafkaConsumer extends Thread {
//
//    private final Queue<ResearchData> queue;
//    private final KafkaConsumer<String, ByteBuffer> consumer;
//    private final String id;
//
//    public GerdiKafkaConsumer(Queue<ResearchData> queue, String id) {
//        super();
//        this.queue = queue;
//        this.id = id;
//        Properties props = new Properties();
//        props.put("bootstrap.servers", "kafka-1.kafka-service.default.svc.cluster.local:9092");
//        props.put("group.id", id); // TODO: How-to use this attribute
//        props.put("enable.auto.commit", "true");
//        props.put("auto.commit.interval.ms", "1000");
//        props.put("key.deserializer", "de.gerdiproject.store.data.kafka.serializer.StoreDataDeserializer");
//        props.put("value.deserializer", "de.gerdiproject.store.data.kafka.serializer.StoreDataDeserializer");
//        consumer = new KafkaConsumer<String, ByteBuffer>(props);
//        consumer.subscribe(Arrays.asList("store"));
//    }
//
//    // TODO: Only read relevant elements
//    @Override
//    public void run() {
//        ResearchData val = null;
//        while (true) {
//            ConsumerRecords<String, ByteBuffer> records = consumer.poll(Duration.ofMillis(100)); // TODO: How is the offset incremented? Must not loose unprocessed files
//            for (ConsumerRecord<String, ByteBuffer> record : records) {
//                try {
//                    val = ResearchData.fromByteBuffer(record.value()); // TODO: try-catch
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (val == null) continue;
//            boolean inserted = false; // Better strategy: Count attempts and log if it exceeds a certain threshold
//            do {
//                inserted = queue.offer(val);
//            } while (!inserted);
//        }
//    }
}
