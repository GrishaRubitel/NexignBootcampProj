package com.bootcamp_proj.bootcampproj.standalone_services;

import com.bootcamp_proj.bootcampproj.additional_classes.BrtTransaction;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonents;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStats;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class BrtHandler {
    private static Map<Long, BrtAbonents> brtAbonentsMap = new HashMap<>();
    private static Map<String, TariffStats> tariffStats = new HashMap<>();

    @Autowired
    TariffStatsService tariffStatsService;
    @Autowired
    BrtAbonentsService brtAbonentsService;

    private void cdrDataHandler(String message) throws IOException {
        selectAllTariffs();
        selectAllAbonents();

        try (BufferedReader br = new BufferedReader(new StringReader(message))) {

            String line;
            while ((line = br.readLine()) != null) {
                BrtTransaction temp = new BrtTransaction(line);

                if (checkAbonent(temp.getMsisdn())) {
                    temp.setTariffId(brtAbonentsMap.get(temp.getMsisdn()).getTariffId());
                    temp.setInNet(checkAbonent(temp.getMsisdnTo()));

                    String js = temp.toJson();
                    System.out.println("BRT: преобразование в JSON: " + js);
                }
            }
        }
    }

    private boolean checkAbonent(long rec) throws IOException {
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
}
