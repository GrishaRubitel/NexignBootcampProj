package com.bootcamp_proj.bootcampproj.standalone_services;

import com.bootcamp_proj.bootcampproj.additional_classes.BrtTransaction;
import com.bootcamp_proj.bootcampproj.additional_classes.MonthStack;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonents;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStats;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
import java.util.*;

/**
 * BRT-сервис для манипуляции информацией об абонентах оператора "ромашка"
 */
@Service
@EnableAsync
public class BrtHandler {
    private static final String BOOTCAMP_PROJ_GROUP = "bootcamp-proj-group";
    private static final String DATA_TOPIC = "data-topic";
    private static final String PART_ZERO = "0";
    //private static final String CDR_FILE = "../../../../temp/CDR.txt";
    private static final String HOST = "http://localhost:";
    private static final String SINGLE_PAY_PARAM = "/api/hrs/single-pay?param=";
    private static final String MONTHLY_PAY_PARAM = "/api/hrs/monthly-pay?param=";
    private static final String PORT = "8082";

    private static WeakHashMap<Long, BrtAbonents> brtAbonentsMap = new WeakHashMap<>();
    private static LinkedList<String> monthlyTariffs;
    private static MonthStack monthHolder;
    private static RestTemplate restTemplate;

    /**
     * Контроллер для взаимодействия сервиса с таблицей тарифов базы данных BRT
     */
    @Autowired
    TariffStatsService tariffStatsService;
    /**
     * Контроллер для взаимодействия сервиса с таблицей абонентов ромашки базы данных BRT
     */
    @Autowired
    BrtAbonentsService brtAbonentsService;

    /**
     * Конструктор для заполнения стэка различными UNIX-time, которые соответствуют первым секундам каждого месяца 2023-го года.
     * Верхний элемент стека не равен новому месяцу. Он нужен чтобы показать ежемесячную плату.
     */
    @PostConstruct
    private void initializeStack() {
        monthHolder = fillStack();
    }

    /**
     * Метод "отлавливает" новые сообщения из кафка-топика "data-topic:0", куда CDR-генератор отправляет CDR-файлы
     * @param message CDR-файл
     */
    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = DATA_TOPIC, partitions = PART_ZERO)
    })
    private void consumeFromDataTopic(String message) {
        System.out.println("BRT-D-P0 from BRT: \n" + message);
        cdrDataHandler(message);
    }

    /**
     * Метод извлекает из базы данных информацию о тарифах и абонентах "ромашки". После чего обрабатывает отдельные записи
     * из CDR-файла. Каждая отдельная запись проверяется на нахождение первого абонента (msisdn) в БД "ромашки", и если
     * нахождение потверждается, сервис запрашивает у HRS стоимость произошедшего звонка
     * @param message CDR-файл
     */
    protected void cdrDataHandler(String message) {
        selectAllTariffs();
        selectAllAbonents();
        restTemplate = new RestTemplate();

        try (BufferedReader br = new BufferedReader(new StringReader(message))) {

            String line;
            while ((line = br.readLine()) != null) {
                BrtTransaction temp = new BrtTransaction(line);

                checkMonthChangement(temp.getUnixEnd());

                if (checkAbonent(temp.getMsisdn())) {
                    temp.setTariffId(brtAbonentsMap.get(temp.getMsisdn()).getTariffId());
                    temp.setInNet(checkAbonent(temp.getMsisdnTo()));

                    System.out.println("BRT: Sending " + temp.toJson());

                    proceedPayment(sendGetToHrs(temp.toJson(), SINGLE_PAY_PARAM));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод формирует URL запрос для обращения к HRS и осуществляет его
     * @param record JSON с полезной для HRS нагрузкой
     * @param urlParam Вызываемый метод (ежемесячная плата или плата за отдельный звонок)
     * @return Возвращает номер абонента и количество средств для списания
     */
    private String sendGetToHrs(String record, String urlParam) {
        String url = HOST + PORT + urlParam + encodeParams(record);
        String response;
        try {
            response = restTemplate.getForObject(url, String.class);
            System.out.println("BRT API Callback: \n" + response);
            return response;
        } catch (Exception e) {
            System.out.println("BRT API: Exception happened" + e.getMessage());
        }
        return null;
    }

    /**
     * Каждый раз, когда сервис получает новый CDR-файл, вызывается этот метод для проверки смены месяца.
     * Если месяц меняется, то происходит ежемесечное списание средств у всех абонентов с соответствующим тарифным планом
     * @param timestamp UNIX-time для проверки месяца
     */
    private void checkMonthChangement(int timestamp) {
        if (monthHolder.checkTop(timestamp)) {
            for (BrtAbonents abonent : brtAbonentsMap.values()) {
                if (monthlyTariffs.contains(abonent.getTariffId())) {
                    sendGetToHrs(abonent.toJson(), MONTHLY_PAY_PARAM);
                }
            }
        }
    }

    /**
     * Метод обрабатывает JSON, полученный от HRS, и списывает средства
     * @param cheque JSON с номером абонента и количеством списываемых средствам
     */
    private void proceedPayment(String cheque) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(cheque);
            long msisdn = jsonNode.get("msisdn").asLong();
            double price = jsonNode.get("callCost").asDouble();

            BrtAbonents abonent = brtAbonentsMap.get(msisdn);
            if (price != 0) {
                abonent.decreaseMoneyBalance(price);
                brtAbonentsService.commitUserTransaction(abonent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод занимается нахождением запрашиваемого абонента в базе данных "ромашки"
     * @param rec Номер телефона абонента
     * @return Возвращает определенный "булеан", соответствующий состоянию нахождения или ненахождения абонента в БД ромашки
     */
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

    /**
     * Метод извлекает всех абонентов из БД "ромашки"
     */
    private void selectAllAbonents() {
        brtAbonentsMap = new WeakHashMap<>();

        for (BrtAbonents elem : brtAbonentsService.findAll()) {
            brtAbonentsMap.put(elem.getMsisdn(), elem);
        }
    }

    /**
     * Метод извлекает все тарифы из БД "ромашки"
     */
    private void selectAllTariffs(){
        monthlyTariffs = new LinkedList<>();

        for (TariffStats elem : tariffStatsService.getAllTariffStats()) {
            if (elem.getPrice_of_period() != 0) {
                monthlyTariffs.add(elem.getTariff_id());
            }
        }
    }

//    /**
//     * Метод для мануального запуска BRT-сервиса при помощи информации из файла
//     */
//
//    private void startWithExistingFile() {
//        StringBuilder content = new StringBuilder();
//        try (BufferedReader reader = new BufferedReader(new FileReader(CDR_FILE))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                content.append(line).append("\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        cdrDataHandler(content.toString());
//    }

    /**
     * Метод для кодирования параметра URL-запроса
     * @param params Передаваемый в URL параметр
     * @return Закодированный URL параметр
     */
    private static String encodeParams(String params) {
        String encodedParams = "";
        try {
            encodedParams = URLEncoder.encode(params, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encodedParams;
    }

    /**
     * Метод, выполняемый в пост-конструкторе, для занесения UNIX-time в "стек начала месяцев"
     * @return Возвращает объект класса, в котором хранится стек
     */
    private MonthStack fillStack() {
        MonthStack monthHolder = new MonthStack();

        monthHolder.push(1701388800);
        monthHolder.push(1698796800);
        monthHolder.push(1696118400);
        monthHolder.push(1693526400);
        monthHolder.push(1690848000);
        monthHolder.push(1688169600);
        monthHolder.push(1685577600);
        monthHolder.push(1682899200);
        monthHolder.push(1680307200);
        monthHolder.push(1677628800);
        monthHolder.push(1675209600);
        monthHolder.push(1672571200);

        return monthHolder;
    }
}
