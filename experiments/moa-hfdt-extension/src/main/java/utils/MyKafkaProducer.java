package utils;

import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class MyKafkaProducer {
    private String topicName;
    private Properties properties;
    private Producer<String, String> producer;

    public MyKafkaProducer() {
        this.topicName = "experiment";
        setupProducerWithProperties("localhost:9092");
    }

    public MyKafkaProducer(String topicName) {
        this.topicName = topicName;
        setupProducerWithProperties("localhost:9092");
    }

    public MyKafkaProducer(String topicName, String bootstrapServers) {
        this.topicName = topicName;
        setupProducerWithProperties(bootstrapServers);
    }

    private void setupProducerWithProperties(String bootstrapServers) {
        /* Source: tutorialspoint.com */
        this.properties = new Properties();

        properties.put("bootstrap.servers", bootstrapServers);

        properties.put("acks", "all"); // Set acknowledgements for producer requests.

        properties.put("retries", 0); // If the request fails, the producer can automatically retry,
        properties.put("batch.size", 16384); // Specify buffer size in config
        properties.put("linger.ms", 1); // Reduce the no of requests less than 0

        // The buffer.memory controls the total amount of memory available to the producer for buffering.
        properties.put("buffer.memory", 33554432);

        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        this.producer = new KafkaProducer<String, String>(properties);
    }

    public void sendMessage(String key, String message) throws Exception {
        Future<RecordMetadata> response;
        ProducerRecord<String, String> data = new ProducerRecord<String, String>(this.topicName, key, message);
        response = this.producer.send(data);
        while (!response.isDone()); // TODO: better solution for waiting
    }

    public void closeProducer() {
        this.producer.close();
    }

    @Override
    protected void finalize() throws Throwable {
        closeProducer();
        super.finalize();
    }
}