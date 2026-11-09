package io.github.tomaszfranczak.batchprocessor.transaction;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
class TransactionTopicConfig {

    @Bean
    NewTopic transactionsTopic(TransactionTopicProperties topic) {
        return TopicBuilder.name(topic.name())
                .partitions(topic.partitions())
                .replicas(topic.replicas())
                .build();
    }
}
