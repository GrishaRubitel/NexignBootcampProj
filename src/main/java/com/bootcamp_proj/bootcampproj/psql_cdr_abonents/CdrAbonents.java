package com.bootcamp_proj.bootcampproj.psql_cdr_abonents;

import jakarta.persistence.*;

@Entity
@Table(name="cdr_abonents")
public class CdrAbonents {
    @Id
    private long msisdn;

    public CdrAbonents() {}

    public long getMsisdn() {
        return msisdn;
    }

    @Override
    public String toString() {
        return Long.toString(msisdn);
    }
}
