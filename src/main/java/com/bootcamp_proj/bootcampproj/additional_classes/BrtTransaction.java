package com.bootcamp_proj.bootcampproj.additional_classes;
import com.bootcamp_proj.bootcampproj.psql_transactions.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BrtTransaction extends Transaction {
    private String tariffId;

    public BrtTransaction(String rec) {
        String[] split = rec.split(IN_BREAK);

        transactionId = Long.parseLong(split[0]);
        callId = split[1];
        msisdn = Long.parseLong(split[2]);
        msisdnTo = Long.parseLong(split[3]);
        unixStart = Integer.parseInt(split[4]);
        unixEnd = Integer.parseInt(split[5]);
    }

    public String getTariffId() {
        return tariffId;
    }

    public void setTariffId(String tariffId) {
        this.tariffId = tariffId;
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
}
