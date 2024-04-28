package com.bootcamp_proj.bootcampproj.additional_classes;

public class AbonentHolder {
    long msisdn;
    int unixLastCall;

    public AbonentHolder(long msisdn, int unixLastCall) {
        this.msisdn = msisdn;
        this.unixLastCall = unixLastCall;
    }

    public int getUnixLastCall() {
        return unixLastCall;
    }

    public long getMsisdn() {
        return msisdn;
    }
}
