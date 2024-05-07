package com.bootcamp_proj.bootcampproj.psql_tariffs_stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import jakarta.persistence.*;

@Entity
@Table(name = "tariffs")
public class TariffStats {

    @Id
    private String tariff_id;
    private String tariff_name;
    private int num_of_minutes;
    private double price_incoming_calls;
    private double price_outcoming_calls;
    private double price_outcoming_calls_camo;
    private double price_of_period;
    private int internet_traffic;
    private int internet_max_speed;
    private int num_of_incoming_sms;
    private int num_of_outcoming_sms;
    private double price_incoming_sms;
    private double price_outcoming_sms;
    private String other_info;

    public TariffStats(String tariff_id, String tariff_name, int num_of_minutes, int price_incoming_calls, int price_outcoming_calls, int price_outcoming_calls_camo, int price_of_period) {
        this.tariff_id = tariff_id;
        this.tariff_name = tariff_name;
        this.num_of_minutes = num_of_minutes;
        this.price_incoming_calls = price_incoming_calls;
        this.price_outcoming_calls = price_outcoming_calls;
        this.price_outcoming_calls_camo = price_outcoming_calls_camo;
        this.price_of_period = price_of_period;
        internet_traffic = 0;
        internet_max_speed = 0;
        num_of_incoming_sms = 0;
        num_of_outcoming_sms = 0;
        price_incoming_sms = 0;
        price_outcoming_sms = 0;
        other_info = "";
    }

    public TariffStats(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        this.tariff_id = jsonNode.get("tariff_id").asText();
        this.tariff_name = jsonNode.get("tariff_name").asText();
        this.num_of_minutes = jsonNode.get("num_of_minutes").asInt();
        this.price_incoming_calls = jsonNode.get("price_incoming_calls").asDouble();
        this.price_outcoming_calls = jsonNode.get("price_outcoming_calls").asDouble();
        this.price_outcoming_calls_camo = jsonNode.get("price_outcoming_calls_camo").asDouble();
        this.price_of_period = jsonNode.get("price_of_period").asDouble();
    }

    public TariffStats() {
    }

    public String getTariff_id() {
        return tariff_id;
    }

    public String getTariff_name() {
        return tariff_name;
    }

    public int getNum_of_minutes() {
        return num_of_minutes;
    }

    public double getPrice_incoming_calls() {
        return price_incoming_calls;
    }

    public double getPrice_outcoming_calls() {
        return price_outcoming_calls;
    }

    public double getPrice_outcoming_calls_camo() {
        return price_outcoming_calls_camo;
    }

    public double getPrice_of_period() {
        return price_of_period;
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
