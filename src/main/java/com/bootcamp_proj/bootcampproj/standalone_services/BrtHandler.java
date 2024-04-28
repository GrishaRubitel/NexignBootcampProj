package com.bootcamp_proj.bootcampproj.standalone_services;

import com.bootcamp_proj.bootcampproj.additional_classes.BrtTransaction;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

@Service
@EnableAsync
public class BrtHandler {
    @Autowired
    BrtAbonentsService brtAbonentsService;


    @Async
    protected void cdrDataHandler(String message) throws IOException {

        try (BufferedReader br = new BufferedReader(new StringReader(message))) {

            String line;
            while ((line = br.readLine()) != null) {
                BrtTransaction temp = new BrtTransaction(line);

                if (brtAbonentsService.findInjection(temp.getMsisdn())) {
                    temp.setTariffId(brtAbonentsService.getTariffId(temp.getMsisdn()));

                    temp.setInNet(brtAbonentsService.findInjection(temp.getMsisdn()));

                    String js = temp.toJson();
                    System.out.println("BRT: преобразование в JSON: " + js);

                }
            }
        }
    }
}
