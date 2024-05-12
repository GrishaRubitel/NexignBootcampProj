package com.bootcamp_proj.bootcampproj.additional_classes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Наследник класс для обработки записей из CDR файла.
 * Расширен полями и методами, необходимыми для работы внутри HRS
 */
public class HrsTransaction extends BrtTransaction {
    private double timeSpent = 0;

    public HrsTransaction(String json) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(json);
            this.msisdn = jsonNode.get("msisdn").asLong();
            this.callId = jsonNode.get("call_id").asText();
            this.unixStart = jsonNode.get("unix_start").asInt();
            this.unixEnd = jsonNode.get("unix_end").asInt();
            this.tariffId = jsonNode.get("tariff_id").asText();
            this.inNet = jsonNode.get("is_msisdn_to_camomile_client").asBoolean();
        } catch (IOException e) {
            throw new Exception();
        }
    }

    public int getCallLength() {
        return unixEnd - unixStart;
    }

    public double getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(double timeSpent) {
        this.timeSpent = timeSpent;
    }
}
