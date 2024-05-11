package com.bootcamp_proj.bootcampproj.psql_brt_abonents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ORM сущность для работы с таблицей "users" базы данных оператора "Ромашки"
 */
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

    public void increaseMoneyBalance(double value) {
        money_balance += value;
    }

    public void decreaseMoneyBalance(double value) {
        money_balance -= value;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setTariff_id(String tariff_id) {
        this.tariff_id = tariff_id;
    }
}
