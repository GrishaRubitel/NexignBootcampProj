package com.bootcamp_proj.bootcampproj.kafka_config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic cdrTopic() {
        return TopicBuilder.name("data-topic").build();
    }

    @Bean
    public NewTopic startServiceTopic() {
        return TopicBuilder.name("trigger-topic").build();
    }
}
