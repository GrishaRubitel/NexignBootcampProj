package com.bootcamp_proj.bootcampproj.standalone_services;
import com.bootcamp_proj.bootcampproj.additional_classes.AbonentHolder;
import com.bootcamp_proj.bootcampproj.additional_classes.ConcurentRecordHolder;
import com.bootcamp_proj.bootcampproj.psql_cdr_abonents.CdrAbonents;
import com.bootcamp_proj.bootcampproj.psql_cdr_abonents.CdrAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_transactions.Transaction;
import com.bootcamp_proj.bootcampproj.psql_transactions.TransactionService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

@Service
@EnableAsync
public class CdrGenerator implements InitializingBean {
    private static final String OUT_CALL_TYPE_CODE = "01";
    private static final String IN_CALL_TYPE_CODE = "02";
    private static final int DELAY = 300;
    private static final double CALL_CHANCE = 0.7;
    private static final double CALL_CHANCE_EQUATOR = 1 - (1 - CALL_CHANCE) / 2;
    private static final String TEMP_CDR_TXT = "./temp/CDR.txt";
    private static final String DATA_TOPIC = "data-topic";
    private static final int PART_ZERO_INT = 0;

    private static final Random random = new Random();
    private static LinkedList<AbonentHolder> abonents;
    private static ConcurentRecordHolder records;
    private static final Logger logger = Logger.getLogger(CdrGenerator.class.getName());

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

    public static CdrGenerator getInstance() {
        return instance;
    }

    private LinkedList<AbonentHolder> sqlSelectPhoneNumbers(int unixStart) {
        LinkedList<AbonentHolder> target = new LinkedList<>();
        Iterator<CdrAbonents> source = cdrAbonentsService.findAll().iterator();
        source.forEachRemaining((i) -> target.add(new AbonentHolder(i.getMsisdn(), unixStart - 10)));

        return target;
    }

    public void switchEmulator() throws InterruptedException, IOException {
        int unixStart = 1672531200;
        int unixFinish = 1704067199;

        transactionService.trunkTable();

        abonents = sqlSelectPhoneNumbers(unixStart);
        records = new ConcurentRecordHolder();

        double dur;

        while (unixStart <= unixFinish) {
            Thread.sleep(DELAY);
            dur = random.nextDouble();
            if (dur  >= CALL_CHANCE) {
                shuffle();
                if (dur < CALL_CHANCE_EQUATOR) {
                    generateCallRecord(unixStart, abonents.get(0), abonents.get(1));
                } else {
                    generateCallRecord(unixStart, abonents.get(0), abonents.get(1));
                    generateCallRecord(unixStart, abonents.get(2), abonents.get(3));
                }
            }

            checkLength();

            unixStart += random.nextInt(100,1000);
        }
    }

    private void shuffle() {
        for (int i = 0; i < abonents.size() / 2; i++) {
            int index = random.nextInt(abonents.size() - 1);
            AbonentHolder temp = abonents.get(index);
            abonents.set(index, abonents.get(i));
            abonents.set(i, temp);
        }
    }

    @Async
    public void generateCallRecord(int unixCurr,
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

    private void buildStandaloneRecord(String type, long m1, long m2, int start, int end) {

        Transaction rec = new Transaction(m1, m2, type, start, end);
        transactionService.insertRecord(rec);
        records.add(rec.toString());
        logger.info("CDR: Добавлена новая запись " + records.getListLength() + "/10");
    }

    private void sendTransactionsData() throws IOException {
        logger.info("CDR: Достигнут предел");

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

    private void checkLength() throws IOException {
        if (records.getListLength() >= 10) {
            sendTransactionsData();
            records.clear();
        }
    }
}
