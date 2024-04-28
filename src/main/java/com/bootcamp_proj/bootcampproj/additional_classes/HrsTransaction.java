package com.bootcamp_proj.bootcampproj.additional_classes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class HrsTransaction extends BrtTransaction {

    public HrsTransaction(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(json);
            this.transactionId = jsonNode.get("transactionId").asInt();
            this.msisdn = jsonNode.get("msisdn").asLong();
            this.msisdnTo = jsonNode.get("msisdnTo").asLong();
            this.callId = jsonNode.get("callId").asText();
            this.unixStart = jsonNode.get("unixStart").asInt();
            this.unixEnd = jsonNode.get("unixEnd").asInt();
            this.tariffId = jsonNode.get("tariffId").asText();
            this.inNet = jsonNode.get("inNet").asBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
