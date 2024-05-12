package com.bootcamp_proj.bootcampproj.standalone_services;

import com.bootcamp_proj.bootcampproj.additional_classes.BrtTransaction;
import com.bootcamp_proj.bootcampproj.additional_classes.MonthStack;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonents;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStats;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BRT-сервис для манипуляции информацией об абонентах оператора "ромашка"
 */
@Service
@EnableAsync
@RequestMapping("/api/brt/")
@RestController
public class BrtHandler implements InitializingBean {
    private static final String BOOTCAMP_PROJ_GROUP = "bootcamp-proj-group";
    private static final String DATA_TOPIC = "data-topic";
    private static final String PART_ZERO = "0";
    private static final String CDR_FILE = "./temp/CDR.txt";
    private static final String HOST = "http://localhost:";
    private static final String SINGLE_PAY_PARAM = "/api/hrs/single-pay?param=";
    private static final String MONTHLY_PAY_PARAM = "/api/hrs/monthly-pay?param=";
    private static final String UPDATE_TARIFF_PARAM = "/api/hrs/change-tariff?param=";
    private static final String PORT = "8082";
    private static final String AUTHORIZED = "UserAuthorized";
    private static final String DENY = "AccessProhibited";
    private static final String CUSTOM_HEADER = "Custom-Header";
    private static final String CRM_SIGNATURE = "CRM-Signature";
    private static final String NOT_FOUND = "Data Not Found";
    private static final String PAYMENT_PROCEEDED = "Payment Proceeded";
    private static final String INCORRECT_VALUE = "Incorrect Money Value";
    private static final String INCORRECT_MSISDN = "Incorrect Msisdn";
    private static final String MSISDN_REGEXP = "^7\\d{9,11}$";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String ABONENT_NOT_FOUND = "Abonent Not Found";
    private static final String TARIFF_NOT_FOUND = "Tariff Not Found";
    private static final String ALREADY_SET = "AlreadySet";
    private static final String ALREADY_REGISTRED = "AbonentAlreadyRegistred";
    private static final String SUCCESS = "Success";
    private static final String MONTH_UNIX_TIME_TXT = "./temp/MonthUnixTime.txt";
    private static final String LOGS_PAYMENT_CORROSION = "./logs/PaymentCorrosion.txt";
    private static final String LOGS_CDR_CORROSION = "./logs/CDRCorrosion.txt";
    private static final String BRT_SIGNATURE = "BRT-Signature";

    private static WeakHashMap<Long, BrtAbonents> brtAbonentsMap;
    private static LinkedList<String> monthlyTariffs;
    private static MonthStack monthHolder;
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = Logger.getLogger(BrtHandler.class.getName());
    private static final Map<String, String> unhandledRecords = new HashMap<>();

    private static BrtHandler instance = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        instance = this;
    }

    public static BrtHandler getInstance() {
        return instance;
    }

    /**
     * Пост-конструктор для инициализации чувствительных объектов до начала работы сервиса.
     * Стек заполняется различными UNIX-time, которые соответствуют первым секундам каждого месяца 2023-го года.
     * Верхний элемент стека не равен новому месяцу. Он нужен, чтобы показать ежемесячную плату.
     */
    @PostConstruct
    private void initializeMaps() {
        brtAbonentsMap = selectAllAbonents();
        monthlyTariffs = selectAllTariffs();
        monthHolder = fillStack();
    }

    /**
     * Контроллер для взаимодействия сервиса с таблицей тарифов базы данных BRT
     */
    @Autowired
    TariffStatsService tariffStatsService;

    /**
     * Контроллер для взаимодействия сервиса с таблицей абонентов "ромашки" базы данных BRT
     */
    @Autowired
    BrtAbonentsService brtAbonentsService;

    /**
     * Метод "отлавливает" новые сообщения из кафка-топика "data-topic:0", куда CDR-генератор отправляет CDR-файлы
     * @param message CDR-файл
     */
    @KafkaListener(topics = DATA_TOPIC, groupId = BOOTCAMP_PROJ_GROUP, topicPartitions = {
            @TopicPartition(topic = DATA_TOPIC, partitions = PART_ZERO)
    })
    private void consumeFromDataTopic(String message) {
        logger.info("BRT-D-P0 from BRT: \n" + message);
        cdrDataHandler(message);
    }

    /**
     * API для обработки запросов со стороны CRM по извлечению из БД информации по конкртеному абоненту
     * @param msisdn Номер телефона
     * @param head Заголовок с сигнатуорй источника
     * @return Ответ с информаицей об абоненте или с информацией об ошибке
     */
    @GetMapping("/list/{msisdn}")
    private ResponseEntity<String> returnCurrentAbonents(@PathVariable String msisdn, @RequestHeader(CUSTOM_HEADER) String head) {
        if (!checkSignature(head)) {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        } else {
            long num = Long.parseLong(decodeParam(msisdn));
            if (checkAbonent(num)) {
                return new ResponseEntity<>(brtAbonentsMap.get(num).toJson(), HttpStatus.OK);
            }
            return new ResponseEntity<>(NOT_FOUND, HttpStatus.NO_CONTENT);
        }
    }

    /**
     * API для обработки запросов со стороны CRM по начислению средств на счёт абонента
     * @param msisdn Номер телефона
     * @param value Количество средств
     * @param head Заголовок с сигнатуорй источника
     * @return Ответ с информаицей об абоненте или с информацией об ошибке
     */
    @PostMapping("/{msisdn}/pay")
    private ResponseEntity<String> proceedAbonentPayment(@PathVariable String msisdn, @RequestParam("money") String value, @RequestHeader(CUSTOM_HEADER) String head) {
        if (!checkSignature(head)) {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        } else {
            long num = Long.parseLong(msisdn);
            BigDecimal bDec = BigDecimal.valueOf(Double.parseDouble(value));
            int scale = bDec.scale();
            if (scale >= 2) {
                return new ResponseEntity<>(INCORRECT_VALUE, HttpStatus.BAD_REQUEST);
            }
            if (checkAbonent(num)) {
                BrtAbonents ab = brtAbonentsMap.get(num);
                ab.increaseMoneyBalance(Double.parseDouble(value));
                brtAbonentsMap.put(ab.getMsisdn(), ab);
                brtAbonentsService.commitUserTransaction(ab);
                return new ResponseEntity<>(PAYMENT_PROCEEDED + "\n" + ab.toJson(), HttpStatus.OK);
            }
            return new ResponseEntity<>(NOT_FOUND, HttpStatus.NO_CONTENT);
        }
    }

    /**
     * API для обработки запросов со стороны CRM по регистрации абонента в сети оператора "Ромашка"
     * @param body Тело запроса
     * @param head Заголовок с сигнатуорй источника
     * @return Ответ с информацией об абоненте или с информацией об ошибке
     */
    @PostMapping("/create")
    private ResponseEntity<String> managerCreateNewAbonent(@RequestBody String body, @RequestHeader(CUSTOM_HEADER) String head) {
        if (!checkSignature(head)) {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(body);
            } catch (JsonProcessingException e) {
                return new ResponseEntity<>(BAD_REQUEST, HttpStatus.BAD_REQUEST);
            }
            double money = jsonNode.get("money").asDouble();
            String tariffId = jsonNode.get("tariffId").asText();
            String msisdn = jsonNode.get("msisdn").asText();

            if (money == 0) {
                money = 100;
            }

            Pattern patt = Pattern.compile(MSISDN_REGEXP);
            Matcher matcher = patt.matcher(msisdn);

            if (matcher.matches()) {
                long num = Long.parseLong(msisdn);
                if (!brtAbonentsMap.containsKey(num)) {
                    BrtAbonents rookie = new BrtAbonents(num, tariffId, money);
                    brtAbonentsMap.put(rookie.getMsisdn(), rookie);
                    brtAbonentsService.commitUserTransaction(rookie);
                    return new ResponseEntity<>(rookie.toJson(), HttpStatus.OK);
                }
                return new ResponseEntity<>(ALREADY_REGISTRED, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(INCORRECT_MSISDN, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * API для обработки запросов со стороны CRM по смене тарифа существующего тарифа
     * @param msisdn Номер телефона
     * @param tariffId Новый тариф
     * @param head Заголовок с сигнатуорй источника
     * @return Ответ с информаицей об абоненте или с информацией об ошибке
     */
    @PutMapping("/{msisdn}/changeTariff")
    private ResponseEntity<String> managerUpdateAbonentTariff(@PathVariable String msisdn,
                                                              @RequestParam("tariffId") String tariffId,
                                                              @RequestHeader(CUSTOM_HEADER) String head) {
        if (!checkSignature(head)) {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        } else {
            long num = Long.parseLong(msisdn);
            if (checkAbonent(num)) {
                BrtAbonents ab = brtAbonentsMap.get(num);

                if (ab.getTariffId().equals(tariffId)) {
                    return new ResponseEntity<>(ALREADY_SET, HttpStatus.BAD_REQUEST);
                }
                boolean checker = false;
                Iterable<TariffStats> tariffStatsIterator = tariffStatsService.getAllTariffStats();

                for (TariffStats tariffStats : tariffStatsIterator) {
                    if (tariffStats.getTariff_id().equals(tariffId)) {
                        checker = true;
                        break;
                    }
                }
                if (!checker) {
                    return new ResponseEntity<>(TARIFF_NOT_FOUND, HttpStatus.BAD_REQUEST);
                }

                String cheque = sendRestToHrs(ab.toJson(), MONTHLY_PAY_PARAM, HttpMethod.GET);
                proceedPayment(cheque);
                ab.setTariff_id(tariffId);
                sendRestToHrs(ab.toJson(), UPDATE_TARIFF_PARAM, HttpMethod.PUT);

                brtAbonentsMap.put(ab.getMsisdn(), ab);
                brtAbonentsService.commitUserTransaction(ab);

                return new ResponseEntity<>(ab.toJson(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
    }

    /**
     * API для обработки запросов со стороны CRM по извлечению информации по всем абонентам
     * @param head Номер телефона
     * @return Ответ с информаицей об абонентах или с информацией об ошибке
     */
    @GetMapping("/list")
    private ResponseEntity<String> returnAllAbonents(@RequestHeader(CUSTOM_HEADER) String head) {
        if (!checkSignature(head)) {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        } else {
            Collection<BrtAbonents> abonentsList = selectAllAbonents().values();
            if (!abonentsList.isEmpty()) {
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (BrtAbonents abonent : abonentsList) {
                    String abonentJson = abonent.toJson();
                    jsonBuilder.append(abonentJson);
                    jsonBuilder.append(", ");
                }
                jsonBuilder.delete(jsonBuilder.length() - 2, jsonBuilder.length());
                jsonBuilder.append("]");
                return new ResponseEntity<>(jsonBuilder.toString(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(NOT_FOUND, HttpStatus.NO_CONTENT);
            }
        }
    }

    /**
     * API для обработки запросов со стороны CRM по проверке абонента на нахождение его в БД оператора "Ромашки"
     * @param msisdn Номер телефона
     * @param head Заголовок с сигнатуорй источника
     * @return Ответ с информаицей об абоненте или с информацией об ошибке
     */
    @GetMapping("/check-containment")
    private ResponseEntity<String> checkContainment(@RequestParam("msisdn") String msisdn, @RequestHeader(CUSTOM_HEADER) String head) {
        if (checkAbonent(Long.parseLong(msisdn)) && checkSignature(head)) {
            return new ResponseEntity<>(AUTHORIZED, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Метод для проверки сигнатуры источника
     * @param head Полученный заголовок
     * @return Булеан по соответствию заголовка
     */
    private boolean checkSignature(String head) {
        if (head != null) {
            return head.equals(CRM_SIGNATURE);
        }
        return false;
    }

    /**
     * Метод извлекает из базы данных информацию о тарифах и абонентах "ромашки". После чего обрабатывает отдельные записи
     * из CDR-файла. Каждая отдельная запись проверяется на нахождение первого абонента (msisdn) в БД "ромашки", и если
     * нахождение потверждается, сервис запрашивает у HRS стоимость произошедшего звонка
     * @param message CDR-файл
     */
    protected void cdrDataHandler(String message) {
        LinkedList<BrtTransaction> records = sortRecordsByUnix(collectAllCdrRecords(message));

        for (BrtTransaction record : records) {
            checkMonthChangement(record.getUnixEnd());

            if (checkAbonent(record.getMsisdn())) {
                record.setTariffId(brtAbonentsMap.get(record.getMsisdn()).getTariffId());
                record.setInNet(checkAbonent(record.getMsisdnTo()));

                String checkSuccess = sendRestToHrs(record.toJson(), SINGLE_PAY_PARAM, HttpMethod.GET);

                if (checkSuccess == null) {
                    unhandledRecords.put(record.toJson(), SINGLE_PAY_PARAM);
                    continue;
                } else if (!unhandledRecords.isEmpty()) {
                    proceedUnhandledRecords();
                }
                proceedPayment(checkSuccess);
            }
        }
    }

    /**
     * Метод считывает все записи из CDR файла, записывает их в список и сортирует
     */
    private LinkedList<BrtTransaction> collectAllCdrRecords(String message) {
        LinkedList<BrtTransaction> records = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new StringReader(message))) {
            String line;
            while ((line = br.readLine()) != null) {
                BrtTransaction temp;
                try {
                    temp = new BrtTransaction(line);
                } catch (Exception e) {
                    logger.warning("Incorrect CDR record. Check \"logs/CDRCorrosion.txt\"");
                    writeCorrosionToFile(line, LOGS_CDR_CORROSION);
                    break;
                }
                records.add(temp);
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
        return records;
    }

    /**
     * Сортировка списка по возрастанию unix-time окончания звонка
     * @param list Список для сортировки
     * @return Отсортированный список
     */
    private LinkedList<BrtTransaction> sortRecordsByUnix(LinkedList<BrtTransaction> list) {
        Comparator<BrtTransaction> comparator = Comparator.comparingInt(obj -> {
            try {
                return (int) obj.getClass().getField("unixEnd").get(obj);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                return 0;
            }
        });

        LinkedList<BrtTransaction> sortedList = new LinkedList<>(list);
        sortedList.sort(comparator);
        return sortedList;
    }

    /**
     * Метод формирует URL запрос для обращения к HRS и осуществляет его
     * @param record JSON с полезной для HRS нагрузкой
     * @param urlParam Вызываемый метод (ежемесячная плата или плата за отдельный звонок)
     * @return Возвращает номер абонента и количество средств для списания
     */
    private String sendRestToHrs(String record, String urlParam, HttpMethod httpMethod) {
        String url = HOST + PORT + urlParam + encodeParams(record);

        HttpHeaders headers = new HttpHeaders();
        headers.add(CUSTOM_HEADER, BRT_SIGNATURE);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return response.getBody();
        } else {
            return null;
        }
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
                    proceedPayment(sendRestToHrs(abonent.toJson(), MONTHLY_PAY_PARAM, HttpMethod.GET));
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
            double price = jsonNode.get("money").asDouble();

            BrtAbonents abonent = brtAbonentsMap.get(msisdn);
            if (price != 0) {
                abonent.decreaseMoneyBalance(price);
                brtAbonentsService.commitUserTransaction(abonent);
            }

            logger.info("BRT: Response from HRS - " + cheque);
        } catch (IOException e) {
            logger.warning("Incorrect HRS Response. Check \"logs/PaymentCorrosion.txt\"");
            writeCorrosionToFile(cheque, LOGS_PAYMENT_CORROSION);
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
    private WeakHashMap<Long, BrtAbonents> selectAllAbonents() {
        WeakHashMap<Long, BrtAbonents> brtAbonentsMap = new WeakHashMap<>();

        for (BrtAbonents elem : brtAbonentsService.findAll()) {
            brtAbonentsMap.put(elem.getMsisdn(), elem);
        }

        return brtAbonentsMap;
    }

    /**
     * Метод извлекает все тарифы из БД "ромашки"
     */
    private LinkedList<String> selectAllTariffs(){
        LinkedList<String> monthlyTariffs = new LinkedList<>();

        for (TariffStats elem : tariffStatsService.getAllTariffStats()) {
            if (elem.getPrice_of_period() != 0) {
                monthlyTariffs.add(elem.getTariff_id());
            }
        }

        return monthlyTariffs;
    }

    /**
     * Метод для мануального запуска BRT-сервиса при помощи информации из файла
     */

    public void startWithExistingFile() {
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
            logger.warning(e.getMessage());
        }
        return encodedParams;
    }

    /**
     * Декодирование параметров, переданных в URL
     * @param encodedString Строка для декодирования
     * @return Декодированная строка
     */
    private static String decodeParam(String encodedString) {
        String decodedString = "";
        try {
            decodedString = URLDecoder.decode(encodedString, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        return decodedString;
    }

    /**
     * Метод, выполняемый в пост-конструкторе, для занесения UNIX-time в "стек начала месяцев"
     * @return Возвращает объект класса, в котором хранится стек
     */
    private MonthStack fillStack() {
        MonthStack monthHolder = new MonthStack();

        try (BufferedReader reader = new BufferedReader(new FileReader(MONTH_UNIX_TIME_TXT))) {
            String line;
            while ((line = reader.readLine()) != null) {
                monthHolder.push(Integer.parseInt(line));
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }

        return monthHolder;
    }

    /**
     * Обработка записей, который в свою очередь не были отправлены на HRS
     */
    private void proceedUnhandledRecords() {
        for (Map.Entry<String, String> entry : unhandledRecords.entrySet()) {
            String response = sendRestToHrs(entry.getKey(), entry.getValue(), HttpMethod.GET);
            if (response == null) {
                return;
            }
            proceedPayment(response);
            unhandledRecords.remove(entry.getKey());
        }
    }

    /**
     * Запись, полученных из CDR файла, поврежденных записей в лог
     * @param content Поврежденная строка
     * @param filePath Файл, в который происходить запись
     */
    private static void writeCorrosionToFile(String content, String filePath) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            logger.warning("Error writing the string to the file: " + e.getMessage());
        }
    }
}
