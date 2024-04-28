package com.bootcamp_proj.bootcampproj.standalone_services;


import com.bootcamp_proj.bootcampproj.additional_classes.BrtTransaction;
import com.bootcamp_proj.bootcampproj.additional_classes.HrsCallbackRecord;
import com.bootcamp_proj.bootcampproj.additional_classes.HrsTransaction;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStats;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsRepository;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@EnableAsync
public class HrsHandler {
    private static final String BOOTCAMP_PROJ_GROUP = "bootcamp-proj-group";
    private static final String DATA_TOPIC = "data-topic";
    private static final String TRIGGER_TOPIC = "trigger-topic";
    private static final String IN_CALL_TYPE_CODE = "02";
    private static final String PART_ZERO = "0";
    private static final int PART_ZERO_INT = 0;
    private static final String PART_ONE = "1";
    public static final int PART_TWO_INT = 2;

    @Autowired
    BrtAbonentsService brtAbonentsService;
    @Autowired
    TariffStatsService tariffStatsService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private TariffStatsRepository tariffStatsRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = DATA_TOPIC, partitions = PART_ONE)
    })
    private void consumeFromDataTopic(String message) {
        System.out.println("HRS-D-P1 from BRT: " + message);
        tariffDispatch(message);
    }

    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = TRIGGER_TOPIC, partitions = PART_ZERO)
    })
    private void consumeFromTriggerTopic(String message) {
        System.out.println("HRS-T-P0 from START: " + message);
        if (message.equals("hrs_start")) {
            kafkaTemplate.send(TRIGGER_TOPIC, PART_ZERO_INT, null, "cdr_start");
        }
    }

    @Async
    protected void tariffDispatch(String message) {
        String jopa = "INSTANT CALLBACK";
        kafkaTemplate.send(DATA_TOPIC, PART_TWO_INT,null, jopa);
        HrsTransaction record = new HrsTransaction(message);
        switch (record.getCallId()) {
            case "11":
                classicTariff(record);
                break;
            case "12":
                monthlyTariff();
                break;
        }
    }

    private void classicTariff(HrsTransaction record) {
        int timeSpent = record.getCallLength();
        timeSpent = (int) Math.ceil(timeSpent / 60);

        TariffStats tStats = tariffStatsService.getTariffStats(record.getTariffId());
        double price;

        if (record.getCallId().equals(IN_CALL_TYPE_CODE)) {
            price = tStats.getPrice_incoming_calls();
        } else {
            if (record.getInNet()) {
                price = tStats.getPrice_outcoming_calls_camo();
            } else {
                price = tStats.getPrice_outcoming_calls();
            }
        }
        double sum = timeSpent * price;

        HrsCallbackRecord hrsCallbackRecord = new HrsCallbackRecord(record.getMsisdn(), sum);

        kafkaTemplate.send(DATA_TOPIC, PART_TWO_INT,null, hrsCallbackRecord.toJson());
    }

    private void monthlyTariff() {
        String jopa = "MONTHLY PAYMENT";
        kafkaTemplate.send(DATA_TOPIC, PART_TWO_INT,null, jopa);
    }
}
