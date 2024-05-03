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
    private WeakHashMap<String, TariffStats> tariffStats;
    private Map<Long, UserMinutes> usersWithTariff = new HashMap<>();

    @PostConstruct
    private void initializeMap() {
        tariffStats = uploadTariff();
    }

    @Autowired
    BrtAbonentsService brtAbonentsService;
    @Autowired
    TariffStatsService tariffStatsService;
    @Autowired
    private UserMinutesService userMinutesService;


    @GetMapping("/single-pay")
    @ResponseStatus(HttpStatus.OK)
    public String singlePay(@RequestParam String param) {
        return tariffDispatch(decodeParam(param));
    }

    @GetMapping("/monthly-pay")
    @ResponseStatus(HttpStatus.OK)
    public String monthlyPay(@RequestParam String param) {
        return payDay(decodeParam(param));
    }

    private String payDay(String param) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(param);
            long msisdn = jsonNode.get("msisdn").asLong();
            String tariff = jsonNode.get("tariffId").asText();

            UserMinutes tempUser = checkUserContainment(msisdn, tariff);
            tempUser.zeroAllMinutes();
            userMinutesService.saveUserMinutes(tempUser);

            double price = tariffStats.get(tempUser.getTariff_id()).getPrice_of_period();

            HrsCallbackRecord hrsCallbackRecord = new HrsCallbackRecord(tempUser.getMsisdn(), price);
            return hrsCallbackRecord.toJson();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return param;
    }

    private String tariffDispatch(String message) {
        HrsTransaction record = new HrsTransaction(message);
        if (tariffStats.get(record.getTariffId()).getNum_of_minutes() == 0) {
            return noMinutesTariff(record, record.getTariffId());
        }
        return withMinutesTariff(record);
    }

    private String noMinutesTariff(HrsTransaction record, String tariff) {
        double timeSpent = Math.ceil(record.getCallLength() / 60);

        TariffStats tStats = tariffStats.get(tariff);
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

    private String withMinutesTariff(HrsTransaction record) {
        double timeSpent = Math.ceil(record.getCallLength() / 60);

        TariffStats tStats = tariffStats.get(record.getTariffId());

        UserMinutes userMinutes = checkUserContainment(record.getMsisdn(), record.getTariffId());

        String returnVal;

        if (userMinutes.getUsed_minutes_in() + timeSpent <= tStats.getNum_of_minutes()) {
            userMinutes.increaseMinutes((int) timeSpent);
            returnVal = new HrsCallbackRecord(record.getMsisdn(), 0).toJson();
        } else {
            userMinutes.setAllMinutes(tStats.getNum_of_minutes());
            returnVal = noMinutesTariff(record, TARIFF_BY_DEFAULT);
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

    private WeakHashMap<String, TariffStats> uploadTariff() {
        WeakHashMap<String, TariffStats> tS = new WeakHashMap();
        for (TariffStats elem : tariffStatsService.getAllTariffStats()) {
            tS.put(elem.getTariff_id(), elem);
        }
        return tS;
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
