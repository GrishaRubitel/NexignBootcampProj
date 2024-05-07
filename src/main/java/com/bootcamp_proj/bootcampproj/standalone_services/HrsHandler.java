package com.bootcamp_proj.bootcampproj.standalone_services;


import com.bootcamp_proj.bootcampproj.additional_classes.HrsCallbackRecord;
import com.bootcamp_proj.bootcampproj.additional_classes.HrsTransaction;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_hrs_user_minutes.UserMinutes;
import com.bootcamp_proj.bootcampproj.psql_hrs_user_minutes.UserMinutesService;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStats;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@RestController
@Service
@RequestMapping("/api/hrs")
public class HrsHandler {
    private static final String IN_CALL_TYPE_CODE = "02";
    private static final int ZERO = 0;
    private static final String TARIFF_BY_DEFAULT = "11";

    private Map<Long, UserMinutes> usersWithTariff = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    BrtAbonentsService brtAbonentsService;
    @Autowired
    TariffStatsService tariffStatsService;
    @Autowired
    private UserMinutesService userMinutesService;

    @GetMapping("/single-pay")
    @ResponseStatus(HttpStatus.OK)
    public String singlePay(@RequestParam String param, @RequestParam String tariffStats) {
        return tariffDispatch(decodeParam(param), decodeParam(tariffStats));
    }

    @GetMapping("/monthly-pay")
    @ResponseStatus(HttpStatus.OK)
    public String monthlyPay(@RequestParam String param, @RequestParam String tariffStats) {
        return payDay(decodeParam(param), decodeParam(tariffStats));
    }

    private String payDay(String param, String tariffStats) {
        try {
            JsonNode jsonNodeMsisdn = objectMapper.readTree(param);
            long msisdn = jsonNodeMsisdn.get("msisdn").asLong();
            String tariff = jsonNodeMsisdn.get("tariffId").asText();

            JsonNode jsonNodeTariff = objectMapper.readTree(tariffStats);
            double price = jsonNodeTariff.get("price_of_period").asDouble();

            UserMinutes tempUser = checkUserContainment(msisdn, tariff);
            tempUser.zeroAllMinutes();
            userMinutesService.saveUserMinutes(tempUser);

            HrsCallbackRecord hrsCallbackRecord = new HrsCallbackRecord(tempUser.getMsisdn(), price);
            return hrsCallbackRecord.toJson();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return param;
    }

    private String tariffDispatch(String message, String tariffStat) {
        HrsTransaction record = new HrsTransaction(message);
        TariffStats usedTariff = new TariffStats(tariffStat);

        if (usedTariff.getPrice_of_period() == 0) {
            return noPriceTariff(record, record.getTariffId(), usedTariff);
        }
        return withPriceTariff(record, usedTariff);
    }

    private String noPriceTariff(HrsTransaction record, String tariff, TariffStats usedTariff) {
        double timeSpent = Math.ceil(record.getCallLength() / 60);

        double price;

        if (record.getCallId().equals(IN_CALL_TYPE_CODE)) {
            price = usedTariff.getPrice_incoming_calls();
        } else {
            if (record.getInNet()) {
                price = usedTariff.getPrice_outcoming_calls_camo();
            } else {
                price = usedTariff.getPrice_outcoming_calls();
            }
        }
        double sum = timeSpent * price;

        HrsCallbackRecord hrsCallbackRecord = new HrsCallbackRecord(record.getMsisdn(), sum);
        return hrsCallbackRecord.toJson();
    }

    private String withPriceTariff(HrsTransaction record, TariffStats usedTariff) {
        double timeSpent = Math.ceil(record.getCallLength() / 60);

        UserMinutes userMinutes = checkUserContainment(record.getMsisdn(), record.getTariffId());

        String returnVal;

        if (userMinutes.getUsed_minutes_in() + timeSpent <= usedTariff.getNum_of_minutes()) {
            userMinutes.increaseMinutes((int) timeSpent);
            returnVal = new HrsCallbackRecord(record.getMsisdn(), 0).toJson();
        } else {
            userMinutes.setAllMinutes(usedTariff.getNum_of_minutes());
            returnVal = noPriceTariff(record, TARIFF_BY_DEFAULT, usedTariff);
        }

        userMinutesService.saveUserMinutes(userMinutes);
        return returnVal;
    }

    private UserMinutes checkUserContainment(long msisdn, String tariff) {
        if (!usersWithTariff.containsKey(msisdn)) {
            UserMinutes temp = new UserMinutes(msisdn, tariff, ZERO, ZERO);
            usersWithTariff.put(temp.getMsisdn(), temp);
            return temp;
        } else {
            return usersWithTariff.get(msisdn);
        }
    }

    private static String decodeParam(String encodedString) {
        String decodedString = "";
        try {
            decodedString = URLDecoder.decode(encodedString, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decodedString;
    }
}
