package com.bootcamp_proj.bootcampproj.standalone_services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

public class CrmHandler {
    @GetMapping("/abonents/list/{msisdn}")
    @ResponseStatus(HttpStatus.OK)
    public String getAbonentInfo(@RequestParam String msisdn) {
    }

    @GetMapping("/abonents/list")
    @ResponseStatus(HttpStatus.OK)
    public String getAllAbonentsInfo(@RequestParam String msisdn) {
    }

    @GetMapping("/abonents/list/{msisdn}/pay")
    @ResponseStatus(HttpStatus.OK)
    public String abonentProceedPayment(@RequestParam String msisdn) {
    }

    @GetMapping("/abonents/create/{msisdn}")
    @ResponseStatus(HttpStatus.OK)
    public String managerCreateNewAbonent(@RequestParam String msisdn) {
    }

    @GetMapping("/abonents/{msisdn}/change-tariff")
    @ResponseStatus(HttpStatus.OK)
    public String managerUpdateAbonentTariff(@RequestParam String msisdn) {
    }
}
