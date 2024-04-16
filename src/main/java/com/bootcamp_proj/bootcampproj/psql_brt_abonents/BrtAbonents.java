package com.bootcamp_proj.bootcampproj.psql_brt_abonents;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class BrtAbonents {
    @Id
    private long msisdn;
    private int tariffid;
    private double money;

    public long getMsisdn() {
        return msisdn;
    }

    public BrtAbonents() {}

    public BrtAbonents(long msisdn, int tariffId, double money) {
        this.msisdn = msisdn;
        this.tariffid = tariffId;
        this.money = money;
    }
}
