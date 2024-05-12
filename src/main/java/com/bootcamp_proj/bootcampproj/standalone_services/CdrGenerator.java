package com.bootcamp_proj.bootcampproj.standalone_services;

import com.bootcamp_proj.bootcampproj.additional_classes.AbonentHolder;
import com.bootcamp_proj.bootcampproj.additional_classes.ConcurentRecordHolder;
import com.bootcamp_proj.bootcampproj.psql_cdr_abonents.CdrAbonents;
import com.bootcamp_proj.bootcampproj.psql_cdr_abonents.CdrAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_transactions.Transaction;
import com.bootcamp_proj.bootcampproj.psql_transactions.TransactionService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Logger;
import java.util.Base64;

@Service
@EnableAsync
public class CdrGenerator implements InitializingBean {
    private static final String OUT_CALL_TYPE_CODE = "01";
    private static final String IN_CALL_TYPE_CODE = "02";
    private static final int DELAY = 300;
    private static final double CALL_CHANCE = 0.7;
    private static final double CRM_CHANCE = 0.1;
    private static final double CALL_CHANCE_EQUATOR = 1 - (1 - CALL_CHANCE) / 2;
    private static final String TEMP_CDR_TXT = "./temp/CDR.txt";
    private static final String CDR_ABONENTS_TXT = "./temp/CrmAbonents.txt";
    private static final String DATA_TOPIC = "data-topic";
    private static final int PART_ZERO_INT = 0;
    private static final String URL_START = "http://localhost:8082/abonents/";
    private static final String URL_PAY = "/pay?money=";
    private static final String URL_LIST = "list";
    private static final String CREATE_URL = "create";
    private static final String TARIFF_ID_URL = "?tariffId=";
    private static final String CHANGE_TARIFF_URL = "/changeTariff";
    private static final String URL_BREAK = "/";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BASIC = "Basic ";
    private static final String COLON = ":";
    private static final String DEFAULT_AUTH = "admin:admin";
    private static int aFCMarker = 10;

    private static LinkedList<AbonentHolder> abonents;
    private static LinkedList<String> abonentsForCrm;
    private static ConcurentRecordHolder records;
    private static final Logger logger = Logger.getLogger(CdrGenerator.class.getName());
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Random random = new Random();

    @Autowired
    private CdrAbonentsService cdrAbonentsService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    private static CdrGenerator instance = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        instance = this;
    }

    /**
     * Инстанс нужен чтобы запустить генерацию после старта работы сервиса
     * @return Инстанс
     */
    public static CdrGenerator getInstance() {
        return instance;
    }

    /**
     * Метод извлекает из базы данных CDR номера абонентов, относительно которых будет производиться генерация.
     * Класс AbonentHolder нужен чтобы как-то хранить номер телефона и время последнего звонка, ради избежания
     * параллельных звонков. Проверить звонки на параллельность можно скриптом в папке psql_test_scripts.
     * Если разница везде положительна, то звонки последовательны
     * @param unixStart Время последнего звонка каждого абонента
     */
    private void sqlSelectPhoneNumbers(int unixStart) {
        abonents = new LinkedList<>();
        Iterator<CdrAbonents> source = cdrAbonentsService.findAll().iterator();
        source.forEachRemaining((i) -> abonents.add(new AbonentHolder(i.getMsisdn(), unixStart - 10)));

        abonentsForCrm = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(CDR_ABONENTS_TXT))) {
            String line;
            while ((line = reader.readLine()) != null) {
                abonentsForCrm.add(line);
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    /**
     * "Бесконечный" метод, генерирующий звонки
     * @throws InterruptedException
     * @throws IOException
     */
    public void switchEmulator() throws InterruptedException, IOException {
        int unixStart = 1672531200;
        int unixFinish = 1704067199;

        transactionService.trunkTable();

        sqlSelectPhoneNumbers(unixStart);

        records = new ConcurentRecordHolder();

        while (unixStart <= unixFinish) {
            Thread.sleep(DELAY);
            double dur = random.nextDouble();
            if (dur  >= CALL_CHANCE) {
                shuffle();
                if (dur < CALL_CHANCE_EQUATOR) {
                    generateCallRecord(unixStart, abonents.get(0), abonents.get(1));
                } else {
                    generateCallRecord(unixStart, abonents.get(0), abonents.get(1));
                    generateCallRecord(unixStart, abonents.get(2), abonents.get(3));
                }
            } else if (dur <= CRM_CHANCE) {
                generateCrmOperation();
            }

            checkLength();

            unixStart += random.nextInt(100,1000);
        }
    }

    /**
     * Метод тасовки списка абонентов алгоритром Кнута. Гарантированно тасуются лишь первый четыре абонента, так
     * как выбираться будут лишь они
     */
    private void shuffle() {
        for (int i = 0; i < abonents.size() / 2; i++) {
            int index = random.nextInt(abonents.size() - 1);
            AbonentHolder temp = abonents.get(index);
            abonents.set(index, abonents.get(i));
            abonents.set(i, temp);
        }
    }

    /**
     * Составитель отдельных записей, которые будут записаны в CDR файл
     * @param unixCurr Время окончания звонка
     * @param msisdn1 Первый абонент
     * @param msisdn2 Второй абонент
     * @throws IOException
     * @throws InterruptedException
     */
    @Async
    protected void generateCallRecord(int unixCurr,
                                      AbonentHolder msisdn1,
                                      AbonentHolder msisdn2) throws IOException, InterruptedException {

        Thread.sleep(random.nextInt(0,300));

        int start = random.nextInt(Math.max(msisdn1.getUnixLastCall(), msisdn2.getUnixLastCall()) + 3, unixCurr - 2);
        String t1;
        String t2;

        if (random.nextBoolean()) {
            t1 = IN_CALL_TYPE_CODE;
            t2 = OUT_CALL_TYPE_CODE;
        } else {
            t1 = OUT_CALL_TYPE_CODE;
            t2 = IN_CALL_TYPE_CODE;
        }

        buildStandaloneRecord(t1, msisdn1.getMsisdn(), msisdn2.getMsisdn(), start, unixCurr);
        buildStandaloneRecord(t2, msisdn2.getMsisdn(), msisdn1.getMsisdn(), start, unixCurr);

        msisdn1.setUnixLastCall(unixCurr);
        msisdn2.setUnixLastCall(unixCurr);
    }

    /**
     * Генерирует запись в виде строки
     * @param type Тип звонка
     * @param m1 Первый абонент
     * @param m2 Второй абонент
     * @param start Время начала звонка
     * @param end Время окончания звонка
     */
    private void buildStandaloneRecord(String type, long m1, long m2, int start, int end) {

        Transaction rec = new Transaction(m1, m2, type, start, end);
        transactionService.insertRecord(rec);
        records.add(rec.toString());
        logger.info("CDR: New Record Added " + records.getListLength() + "/10");
    }

    /**
     * Метод отправляет CDR файл в топик Кафки
     * @throws IOException
     */
    private void sendTransactionsData() throws IOException {
        logger.info("CDR: Limit Reached");

        StringBuilder plainText = new StringBuilder();

        for (String elem : records.getRecordHolder()) {
            plainText.append(elem).append("\n");
        }

        try (BufferedWriter bR = new BufferedWriter(new FileWriter(TEMP_CDR_TXT))) {
            bR.write(plainText.toString());
        }

        String content = new String(Files.readAllBytes(Paths.get(TEMP_CDR_TXT)));

        kafkaTemplate.send(DATA_TOPIC, PART_ZERO_INT,null, content);

        records.clear();
    }

    /**
     * Проверка количества хранимых записей. Если их больше 10, то записи публикуются в кафку
     * @throws IOException
     */
    private void checkLength() throws IOException {
        if (records.getListLength() >= 10) {
            sendTransactionsData();
            records.clear();
        }
    }

    /**
     * Метод генерации обращений к CRM
     */
    @Async
    protected void generateCrmOperation() {
        int dur = random.nextInt(0, 4);
        String url;
        String randNum = abonentsForCrm.get(random.nextInt(aFCMarker));

        HttpMethod method = HttpMethod.GET;
        String authParam = DEFAULT_AUTH;
        String body = null;

        switch (dur) {
            case 0:
                url = URL_START + URL_LIST;
                break;
            case 1:
                url = URL_START + URL_LIST + URL_BREAK + randNum;
                authParam = randNum + COLON;
                break;
            case 2:
                url = URL_START + randNum + URL_PAY + random.nextInt(1, 100000);
                method = HttpMethod.PUT;
                authParam = randNum + COLON;
                break;
            case 3:
                url = URL_START + randNum + CHANGE_TARIFF_URL + TARIFF_ID_URL + random.nextInt(11, 12);
                method = HttpMethod.PUT;
                break;
            case 4:
                url = URL_START + CREATE_URL;
                method = HttpMethod.POST;
                randNum = abonentsForCrm.get(random.nextInt(aFCMarker++, abonentsForCrm.size() - 1));
                int tariffId = random.nextInt(11, 12);
                int money = random.nextInt(1, 100000);
                body = String.format("{\"msisdn\": %d, \"tariffId\": %d, \"money\": %.2f}", randNum, tariffId, money);
                break;
            default:
                url = URL_START + URL_LIST;
                break;
        }
        logger.info("CDR: Sending URL To CRM: " + url);
        try {
            if (body == null) {
                logger.info("CDR Response From CRM: " + sendRestToCrm(url, authParam, method));
            } else {
                logger.info("CDR Response From CRM: " + sendRestToCrm(url, authParam, method, body));
            }
        } catch (Exception e) {
            logger.warning("CDR Response From CRM - Not Correct Request: " + e.getMessage());
        }
    }

    /**
     * Метод составляет HTTP запрос для CRM
     * @param url URL запрос
     * @param authParams Заголовок авторизации
     * @param httpMethod HTTP метод
     * @return Результат обращения
     */
    private ResponseEntity<String> sendRestToCrm(String url, String authParams, HttpMethod httpMethod) {
        HttpHeaders headers = new HttpHeaders();
        String encodedAuthParams = Base64.getEncoder().encodeToString(authParams.getBytes());
        logger.info(BASIC + encodedAuthParams);
        headers.add(AUTH_HEADER, BASIC + encodedAuthParams);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return finalSendToBrt(url, httpMethod, entity);
    }

    /**
     * Метод составляет HTTP запрос для CRM. Данная перегрузка работает с Request Body
     * @param url URL запрос
     * @param authParams Заголовок авторизации
     * @param httpMethod HTTP метод
     * @param body Request Body
     * @return Результат обращения
     */
    private ResponseEntity<String> sendRestToCrm(String url, String authParams, HttpMethod httpMethod, String body) {
        HttpHeaders headers = new HttpHeaders();
        String encodedAuthParams = Base64.getEncoder().encodeToString(authParams.getBytes());
        logger.info(BASIC + encodedAuthParams);
        headers.add(AUTH_HEADER, BASIC + encodedAuthParams);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return finalSendToBrt(url, httpMethod, entity);
    }

    /**
     * Отправка запроса на CRM
     * @param url URL запрос
     * @param method HTTP метод
     * @param entity Заголовки и Request Body (опционально)
     * @return Результат обращения
     */
    private ResponseEntity<String> finalSendToBrt(String url, HttpMethod method, HttpEntity<String> entity) {
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, method, entity, String.class);
            return response;
        } catch(Exception e) {
            logger.warning(e.getMessage());
            return new ResponseEntity<>("Service unavailable. Try Again Later", HttpStatus.BAD_REQUEST);
        }
    }
}
