package com.bootcamp_proj.bootcampproj.psql_brt_abonents;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class BrtAbonents {
    @Id
    private long msisdn;
    private String tariff_id;
    private double money_balance;

    public long getMsisdn() {
        return msisdn;
    }

    public String getTariffId() {
        return tariff_id;
    }

    public double getMoneyBalance() {
        return money_balance;
    }

    public BrtAbonents() {}

    public BrtAbonents(long msisdn, String tariffId, double money) {
        this.msisdn = msisdn;
        this.tariff_id = tariffId;
        this.money_balance = money;
    }

}
