package com.bootcamp_proj.bootcampproj.standalone_services;

import com.bootcamp_proj.bootcampproj.additional_classes.BrtTransaction;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;

@Service
@EnableAsync
public class BrtHandler {
    private static final String BOOTCAMP_PROJ_GROUP = "bootcamp-proj-group";
    private static final String DATA_TOPIC = "data-topic";
    private static final String TRIGGER_TOPIC = "trigger-topic";
    private static final String PART_ZERO = "0";

    @Autowired
    BrtAbonentsService brtAbonentsService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = DATA_TOPIC, partitions = PART_ZERO)
    })
    private void consumeFromDataTopic(String message) throws IOException {
        cdrDataHandler(message);
    }

    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = TRIGGER_TOPIC, partitions = PART_ZERO)
    })
    private void consumeFromTriggerTopic(String message) {
        System.out.println("BRT: Received message from trigger-topic: \n" + message);
    }

    @Async
    protected void cdrDataHandler(String message) throws IOException {

        try (BufferedReader br = new BufferedReader(new StringReader(message))) {

            String line;
            while ((line = br.readLine()) != null) {
                BrtTransaction temp = new BrtTransaction(line);

                if (brtAbonentsService.findInjection(temp.getMsisdn())) {
                    temp.setTariffId(brtAbonentsService.getTariffId(temp.getMsisdn()));

                    String js = temp.toJson();
                    System.out.println("BRT: преобразование в JSON: " + js);

                    kafkaTemplate.send(DATA_TOPIC, 1,null, js);
                }
            }
        }
    }

    private void sendDataToHrs(LinkedList<BrtTransaction> records) {

    }
}
