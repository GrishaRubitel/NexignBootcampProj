package com.bootcamp_proj.bootcampproj.standalone_services;


import com.bootcamp_proj.bootcampproj.additional_classes.HrsCallbackRecord;
import com.bootcamp_proj.bootcampproj.additional_classes.HrsTransaction;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStats;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsRepository;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@Service
@RequestMapping("/api/hrs")
public class HrsHandler {
    private static final String IN_CALL_TYPE_CODE = "02";
    private Map<String, TariffStats> tariffStats = new HashMap<>();

    @Autowired
    BrtAbonentsService brtAbonentsService;
    @Autowired
    TariffStatsService tariffStatsService;
    @Autowired
    private TariffStatsRepository tariffStatsRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/single-pay")
    @ResponseStatus(HttpStatus.OK)
    public String singlePay(@RequestParam String param) {
        //return param;
        return tariffDispatch(decodeParam(param));
    }

    protected String tariffDispatch(String message) {
        HrsTransaction record = new HrsTransaction(message);
        switch (record.getTariffId()) {
            case "11":
                return classicTariff(record);
            case "12":
                return monthlyTariff();
        }
        return "jopa";
    }

    private String classicTariff(HrsTransaction record) {
        double timeSpent = record.getCallLength();
        timeSpent = Math.ceil(timeSpent / 60);

        TariffStats tStats;
        if ((tStats = checkTariffContainment(record.getTariffId())) == null) {
            System.out.println("Tariff not in base");
            return "Tariff not in base";
        }
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

        return hrsCallbackRecord.toJson();
    }

    private String monthlyTariff() {
        return "MONTHLY PAYMENT";
    }

    private TariffStats checkTariffContainment(String tariff) {
        if (!tariffStats.containsKey(tariff)) {
            TariffStats temp;
            if ((temp = tariffStatsService.getTariffStats(tariff)) == null) {
                return null;
            } else {
                tariffStats.put(temp.getTariff_id(), temp);
            }
        }
        return tariffStats.get(tariff);
    }

    private static String decodeParam(String encodedString) {
        String decodedString = "";
        try {
            // Декодируем строку
            decodedString = URLDecoder.decode(encodedString, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decodedString;
    }
}
