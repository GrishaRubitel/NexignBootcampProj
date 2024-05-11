package com.bootcamp_proj.bootcampproj.additional_classes;

import org.json.JSONObject;
import com.bootcamp_proj.bootcampproj.psql_transactions.Transaction;

/**
 * Наследник класс для обработки записей из CDR файла. Расширен полями и методами,
 * необходимыми для работы внутри BRT
 */
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("call_id", callId);
        jsonObject.put("msisdn", msisdn);
        jsonObject.put("is_msisdn_to_camomile_client", inNet);
        jsonObject.put("unix_start", unixStart);
        jsonObject.put("unix_end", unixEnd);
        jsonObject.put("tariff_id", tariffId);
        return jsonObject.toString();
    }
}
