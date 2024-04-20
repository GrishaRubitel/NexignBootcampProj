package com.bootcamp_proj.bootcampproj.standalone_services;
import com.bootcamp_proj.bootcampproj.additional_classes.abonentHolder;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@EnableAsync
public class CdrGenerator implements InitializingBean {
    private static final String OUT_CALL_TYPE_CODE = "01";
    private static final String IN_CALL_TYPE_CODE = "02";
    public static final String DATA_TOPIC = "data-topic";
    public static final int DELAY = 300;
    public static final double CALL_CHANCE = 0.7;
    public static final String TEMP_CDR_TXT = "./temp/CDR.txt";

    private int counter = 0;

    @Autowired
    private CdrAbonentsService cdrAbonentsService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static CdrGenerator instance = null;
    @Autowired
    private BrtAbonentsService brtAbonentsService;

    @Override
    public void afterPropertiesSet() throws Exception {
        instance = this;
    }

    public static CdrGenerator getInstance() {
        return instance;
    }

    private LinkedList<abonentHolder> sqlSelectPhoneNumbers(int unixStart) {
        LinkedList<abonentHolder> target = new LinkedList<>();
        Iterator<CdrAbonents> source = cdrAbonentsService.findAll().iterator();
        source.forEachRemaining((i) -> target.add(new abonentHolder(i.getMsisdn(), unixStart - 10)));

        return target;
    }

    public void switchEmulator() throws InterruptedException, IOException {
        int unixStart = 1672531200;
        int unixFinish = 1704067199;

        transactionService.trunkTable();

        LinkedList<abonentHolder> abonents = sqlSelectPhoneNumbers(unixStart);
        Random random = new Random();

        LinkedList<String> records = new LinkedList<String>();

        while (unixStart <= unixFinish) {
            Thread.sleep(DELAY);
            if (random.nextDouble() >= CALL_CHANCE) {

                generateCallRecord(unixStart, random, abonents, records);

                if (counter == 10) {
                    sendTransactionsData(records);
                }
            }
            unixStart += random.nextInt(100,1000);
        }
    }

    private int randUnixCallStart(Random random, int curr, int last1, int last2) {
        return random.nextInt(Math.max(last1, last2) + 3, curr - 2);
    }

    @Async
    public void generateCallRecord(int unixCurr,
                                   Random random,
                                   LinkedList<abonentHolder> abonents,
                                   LinkedList<String> records) throws IOException {

        abonentHolder msisdn1 = abonents.get(random.nextInt(abonents.size()));
        abonentHolder msisdn2 = abonents.get(random.nextInt(abonents.size()));

        while (msisdn1 == msisdn2) {
            msisdn2 = abonents.get(random.nextInt(abonents.size()));
        }

        int start = randUnixCallStart(random, unixCurr, msisdn1.getUnixLastCall(), msisdn2.getUnixLastCall());
        String t1;
        String t2;

        if (random.nextBoolean()) {
            t1 = IN_CALL_TYPE_CODE;
            t2 = OUT_CALL_TYPE_CODE;
        } else {
            t1 = OUT_CALL_TYPE_CODE;
            t2 = IN_CALL_TYPE_CODE;
        }

        buildStandaloneRecord(t1, msisdn1.getMsisdn(), msisdn2.getMsisdn(), start, unixCurr, records);

        if (counter == 10) {
            sendTransactionsData(records);
        }

        if (brtAbonentsService.findInjection(msisdn2.getMsisdn())) {
            buildStandaloneRecord(t2, msisdn2.getMsisdn(), msisdn1.getMsisdn(), start, unixCurr, records);
        }
    }

    private void buildStandaloneRecord(String type,
                                       long m1,
                                       long m2,
                                       int start,
                                       int end,
                                       LinkedList<String> records) {

        Transaction rec = new Transaction(m1, m2, type, start, end);
        transactionService.insertRecord(rec);
        records.add(rec.toString());
        counter++;
        System.out.println("CDR: Добавлена новая запись " + counter + "/10");
    }

    private void sendTransactionsData(LinkedList<String> records) throws IOException {
        System.out.println("CDR: Достигнут предел");

        String plainText = "";

        for (String elem : records) {
            plainText += elem + "\n";
        }

        try (BufferedWriter bR = new BufferedWriter(new FileWriter(TEMP_CDR_TXT))) {
            bR.write(plainText);
        }

        String content = new String(Files.readAllBytes(Paths.get(TEMP_CDR_TXT)));

        kafkaTemplate.send(DATA_TOPIC, 0,null, content);
        counter = 0;

        records.clear();
    }
}
