package com.bootcamp_proj.bootcampproj.psql_hrs_user_minutes;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="users_minutes")
public class UserMinutes {
    @Id
    private long msisdn;
    private String tariff_id;
    private int used_minutes_in;
    private int used_minutes_out;

    public UserMinutes(long msisdn, String tariff_id, int used_minutes_in, int used_minutes_out) {
        this.msisdn = msisdn;
        this.tariff_id = tariff_id;
        this.used_minutes_in = used_minutes_in;
        this.used_minutes_out = used_minutes_out;
    }

    public void setAllMinutes(int value) {
        setUsed_minutes_in(value);
        setUsed_minutes_out(value);
    }

    public void setUsed_minutes_in(int used_minutes_in) {
        this.used_minutes_in = used_minutes_in;
    }

    public void setUsed_minutes_out(int used_minutes_out) {
        this.used_minutes_out = used_minutes_out;
    }

    public UserMinutes() {}

    public void increaseMinutes(int value) {
        used_minutes_in += value;
        used_minutes_out += value;
    }

    public void decreaseMinutes(int value) {
        used_minutes_in -= value;
        used_minutes_out -= value;
    }

    public void zeroAllMinutes() {
        used_minutes_in = 0;
        used_minutes_out = 0;
    }

    public long getMsisdn() {
        return msisdn;
    }

    public String getTariff_id() {
        return tariff_id;
    }

    public int getUsed_minutes_in() {
        return used_minutes_in;
    }

    public int getUsed_minutes_out() {
        return used_minutes_out;
    }
}
