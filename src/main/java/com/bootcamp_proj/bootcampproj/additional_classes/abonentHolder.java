package com.bootcamp_proj.bootcampproj.additional_classes;

public class abonentHolder {
    long msisdn;
    int unixLastCall;

    public abonentHolder(long msisdn, int unixLastCall) {
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
