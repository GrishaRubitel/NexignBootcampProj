package com.bootcamp_proj.bootcampproj.psql_brt_abonents;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class BrtAbonents {
    @Id
    private long msisdn;
    private int tariffId;
    private double moneyBalance;

    public BrtAbonents() {}

    public BrtAbonents(long msisdn, int tariffId, double moneyBalance) {
        this.msisdn = msisdn;
        this.tariffId = tariffId;
        this.moneyBalance = moneyBalance;
    }

    //public void selectNetUsers()
}
