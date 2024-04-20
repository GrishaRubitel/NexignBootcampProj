package com.bootcamp_proj.bootcampproj.standalone_services;


import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@EnableAsync
public class HrsHandler {
    private static final String BOOTCAMP_PROJ_GROUP = "bootcamp-proj-group";
    private static final String DATA_TOPIC = "data-topic";
    private static final String TRIGGER_TOPIC = "trigger-topic";
    private static final String PART_ONE = "1";
    private static final String PART_ZERO = "0";

    @Autowired
    BrtAbonentsService brtAbonentsService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = DATA_TOPIC, partitions = PART_ONE)
    })
    private void consumeFromDataTopic(String message) {
        System.out.println("HRS: Received message from data-topic: " + message);
    }

    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = TRIGGER_TOPIC, partitions = PART_ZERO)
    })
    private void consumeFromTriggerTopic(String message) {
        System.out.println("HRS: Received message from trigger-topic: " + message);
    }
}
