package com.bootcamp_proj.bootcampproj.additional_classes;
import com.bootcamp_proj.bootcampproj.psql_transactions.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Array;

public class BrtTransaction extends Transaction {
    private static final String REGEX = ", ";

    protected String tariffId;
    protected boolean inNet;

    public BrtTransaction(String str) {
        String[] starr = str.split(REGEX);

        transactionId = Long.parseLong(starr[0]);
        callId = starr[1];
        msisdn = Long.parseLong(starr[2]);
        msisdnTo = Long.parseLong(starr[3]);
        unixStart = Integer.parseInt(starr[4]);
        unixEnd = Integer.parseInt(starr[5]);
    }

    public BrtTransaction(long msisdn) {
        this.msisdnTo = msisdn;
    }

    public BrtTransaction() {}

    public String getTariffId() {
        return tariffId;
    }

    public void setTariffId(String tariffId) {
        this.tariffId = tariffId;
    }

    public boolean getInNet() {
        return inNet;
    }

    public void setInNet(boolean inNet) {
        this.inNet = inNet;
    }

    @Override
    public String toString() {
        return callId + IN_BREAK + msisdn + IN_BREAK + msisdnTo + IN_BREAK +
                unixStart + IN_BREAK + unixEnd + IN_BREAK + tariffId;
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

    public int getCallLength() {
        return unixEnd - unixStart;
    }
}
