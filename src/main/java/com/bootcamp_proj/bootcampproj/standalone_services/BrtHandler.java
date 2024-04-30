package com.bootcamp_proj.bootcampproj.standalone_services;

import com.bootcamp_proj.bootcampproj.additional_classes.BrtTransaction;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonents;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStats;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Service
@EnableAsync
public class BrtHandler {
    private static final String BOOTCAMP_PROJ_GROUP = "bootcamp-proj-group";
    private static final String DATA_TOPIC = "data-topic";
    private static final String PART_ZERO = "0";
    private static final String CDR_FILE = "../../../../temp/CDR.txt";
    private static final String HOST = "http://localhost:";
    private static final String SINGLE_PAY_PARAM = "/api/hrs/single-pay?param=";
    private static final String PORT = "8082";

    private static Map<Long, BrtAbonents> brtAbonentsMap;
    private static Map<String, TariffStats> tariffStats;
    private static RestTemplate restTemplate;

    @Autowired
    TariffStatsService tariffStatsService;
    @Autowired
    BrtAbonentsService brtAbonentsService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = DATA_TOPIC, partitions = PART_ZERO)
    })
    private void consumeFromDataTopic(String message) {
        System.out.println("BRT-D-P0 from BRT: \n" + message);
        cdrDataHandler(message);
    }


    protected void cdrDataHandler(String message) {
        //selectAllTariffs();
        selectAllAbonents();
        restTemplate = new RestTemplate();

        LinkedList<String> records = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new StringReader(message))) {

            String line;
            while ((line = br.readLine()) != null) {
                BrtTransaction temp = new BrtTransaction(line);

                if (checkAbonent(temp.getMsisdn())) {
                    temp.setTariffId(brtAbonentsMap.get(temp.getMsisdn()).getTariffId());
                    temp.setInNet(checkAbonent(temp.getMsisdnTo()));

                    String url = HOST + PORT + SINGLE_PAY_PARAM + encodeParams(temp.toJson());;

                    try {
                        System.out.println("BRT API Callback: \n" + restTemplate.getForObject(url, String.class));
                    } catch (Exception e) {
                        System.out.println("BRT API: Exception happened");
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkAbonent(long rec) {
        if (brtAbonentsMap.containsKey(rec)) {
            return true;
        } else {
            BrtAbonents temp = brtAbonentsService.findById(rec);
            if (temp == null) {
                return false;
            }
            brtAbonentsMap.put(temp.getMsisdn(), temp);
            return true;
        }
    }

    private void selectAllAbonents() {
        brtAbonentsMap = new HashMap<>();

        for (BrtAbonents elem : brtAbonentsService.findAll()) {
            brtAbonentsMap.put(elem.getMsisdn(), elem);
        }
    }

    private void selectAllTariffs() throws IOException {
        tariffStats = new HashMap<>();

        for (TariffStats elem : tariffStatsService.getAllTariffStats()) {
            tariffStats.put(elem.getTariff_id(), elem);
        }
    }

    private void startWithExistingFile() {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(CDR_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        cdrDataHandler(content.toString());
    }

    private String callSinglePayAPI(String param) {
        String url = HOST + PORT + SINGLE_PAY_PARAM + param;
        return restTemplate.getForObject(url, String.class);
    }

    private static String encodeParams(String params) {
        String encodedParams = "";
        try {
            encodedParams = URLEncoder.encode(params, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encodedParams;
    }
}
