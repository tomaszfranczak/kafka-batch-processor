package io.github.tomaszfranczak.batchprocessor.transaction;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
class TransactionErrorHandlingConfig {

    @Bean
    DefaultErrorHandler transactionErrorHandler(ProducerFactory<?, ?> producerFactory, TransactionTopicProperties topic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                deadLetterTemplate(producerFactory),
                (record, exception) -> new TopicPartition(topic.deadLetterName(), record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000, 2));
        errorHandler.addNotRetryableExceptions(InvalidTransactionException.class);
        return errorHandler;
    }

    private static KafkaTemplate<String, Object> deadLetterTemplate(ProducerFactory<?, ?> producerFactory) {
        DelegatingByTypeSerializer valueSerializer = new DelegatingByTypeSerializer(Map.of(
                byte[].class, new ByteArraySerializer(),
                Transaction.class, new JacksonJsonSerializer<>()));
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                producerFactory.getConfigurationProperties(), new StringSerializer(), valueSerializer));
    }
}
