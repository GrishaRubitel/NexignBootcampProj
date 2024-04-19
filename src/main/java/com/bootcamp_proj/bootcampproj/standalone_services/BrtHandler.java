package com.bootcamp_proj.bootcampproj.standalone_services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@EnableAsync
public class BrtHandler {

    public static final String BOOTCAMP_PROJ_GROUP = "bootcamp-proj-group";
    public static final String DATA_TOPIC = "data-topic";
    public static final String TRIGGER_TOPIC = "trigger-topic";
    public static final String PART_ZERO = "0";

    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = DATA_TOPIC, partitions = PART_ZERO)
    })
    public void consumeFromDataTopic(String message) {
        System.out.println("BRT: Received message from data-topic: \n" + message);
    }

    @KafkaListener(topics = TRIGGER_TOPIC, groupId = BOOTCAMP_PROJ_GROUP)
    public void consumeFromTriggerTopic(String message) {
        System.out.println("BRT: Received message from trigger-topic: \n" + message);
    }
}
