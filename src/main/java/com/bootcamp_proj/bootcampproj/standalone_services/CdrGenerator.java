package com.bootcamp_proj.bootcampproj.standalone_services;
import com.bootcamp_proj.bootcampproj.additional_classes.abonentHolder;
import com.bootcamp_proj.bootcampproj.psql_cdr_abonents.CdrAbonents;
import com.bootcamp_proj.bootcampproj.psql_cdr_abonents.CdrAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_transactions.Transaction;
import com.bootcamp_proj.bootcampproj.psql_transactions.TransactionService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.*;

import com.google.gson.JsonObject;

@Service
@EnableAsync
public class CdrGenerator implements InitializingBean {
    private static final String OUT_CALL_TYPE_CODE = "01";
    private static final String IN_CALL_TYPE_CODE = "02";
    public static final String DATA_TOPIC = "data-topic";
    public static final String CALL_ID = "callId";
    public static final String MSISDN = "msisdn";
    public static final String MSISDN1 = "msisdnTo";
    public static final String UNIX_START = "unixStart";
    public static final String UNIX_END = "unixEnd";

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

    private LinkedList<abonentHolder> sqlSelectPhoneNumbers(int unixStart) {
        LinkedList<abonentHolder> target = new LinkedList<>();
        Iterator<CdrAbonents> source = cdrAbonentsService.findAll().iterator();
        source.forEachRemaining((i) -> target.add(new abonentHolder(i.getMsisdn(), unixStart - 10)));

        return target;
    }

    public void switchEmulator() throws InterruptedException {
        int unixStart = 1672531200;
        int unixFinish = 1704067199;

        transactionService.trunkTable();

        LinkedList<abonentHolder> abonents = sqlSelectPhoneNumbers(unixStart);
        Random random = new Random();

        int counter = 0;

        JsonObject cdrHolder = new JsonObject();

        while (unixStart <= unixFinish) {
            Thread.sleep(300);
            if (random.nextDouble() >= 0.7) {

                cdrHolder = mergeJsonObjects(cdrHolder, generateCallRecord(unixStart, counter, random, abonents));

                counter++;
                System.out.println("CDR: Добавлена новая запись " + counter + "/5");
                if (counter == 5) {
                    System.out.println("CDR: Достигнут предел");
                    System.out.println("CDR: " + cdrHolder);

                    kafkaTemplate.send(DATA_TOPIC, 0,null, cdrHolder.toString());
                    kafkaTemplate.flush();

                    counter = 0;
                    cdrHolder = new JsonObject();
                }
            }
            unixStart += random.nextInt(100,1000);
        }
    }

    private int randUnixCallStart(Random random, int curr, int last1, int last2) {
        return random.nextInt(Math.max(last1, last2) + 3, curr - 2);
    }

    @Async
    public JsonObject generateCallRecord(int unixCurr, int counter, Random random, LinkedList<abonentHolder> abonents) {
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

        Transaction rec1 = buildStandaloneRecord(t1, msisdn1.getMsisdn(), msisdn2.getMsisdn(), start, unixCurr);
        Transaction rec2 = buildStandaloneRecord(t2, msisdn2.getMsisdn(), msisdn1.getMsisdn(), start, unixCurr);

        JsonObject json = new JsonObject();
        json.add(String.valueOf(2 * counter), serializeTransaction(rec1));
        json.add(String.valueOf(2 * counter + 1), serializeTransaction(rec2));

        return json;
    }

    private Transaction buildStandaloneRecord(String type, long m1, long m2, int start, int end) {
        Transaction rec = new Transaction(m1, m2, type, start, end);
        transactionService.insertRecord(rec);
        return rec;
    }

    private JsonObject serializeTransaction(Transaction transaction) {
        JsonObject transactionJson = new JsonObject();

        transactionJson.addProperty(CALL_ID, transaction.getCallId());
        transactionJson.addProperty(MSISDN, transaction.getMsisdn());
        transactionJson.addProperty(MSISDN1, transaction.getMsisdnTo());
        transactionJson.addProperty(UNIX_START, transaction.getUnixStart());
        transactionJson.addProperty(UNIX_END, transaction.getUnixEnd());

        return transactionJson;
    }

    private JsonObject mergeJsonObjects(JsonObject json1, JsonObject json2) {
        JsonObject mergedJson = new JsonObject();

        for (String key : json1.keySet()) {
            mergedJson.add(key, json1.get(key));
        }

        for (String key : json2.keySet()) {
            mergedJson.add(key, json2.get(key));
        }

        return mergedJson;
    }
}
