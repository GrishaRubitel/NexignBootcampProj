package com.bootcamp_proj.bootcampproj.additional_classes;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HrsCallbackRecord {
    private long msisdn;
    private double callCost;

    public HrsCallbackRecord(long msisdn, double callCost) {
        this.msisdn = msisdn;
        this.callCost = callCost;
    }

    public long getMsisdn() {
        return msisdn;
    }

    public double getCallCost() {
        return callCost;
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
}
