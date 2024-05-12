package com.bootcamp_proj.bootcampproj.standalone_services;


import com.bootcamp_proj.bootcampproj.additional_classes.HrsTransaction;
import com.bootcamp_proj.bootcampproj.psql_brt_abonents.BrtAbonentsService;
import com.bootcamp_proj.bootcampproj.psql_hrs_user_minutes.UserMinutes;
import com.bootcamp_proj.bootcampproj.psql_hrs_user_minutes.UserMinutesService;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStats;
import com.bootcamp_proj.bootcampproj.psql_tariffs_stats.TariffStatsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@Service
@RequestMapping("/api/hrs")
public class HrsHandler {
    private static final String IN_CALL_TYPE_CODE = "02";
    private static final int ZERO = 0;
    private static final String TARIFF_BY_DEFAULT = "11";
    public static final String INCORRECT_DATA = "Incorrect Data";
    private static final String BRT_SIGNATURE = "BRT-Signature";
    private static final String CUSTOM_HEADER = "Custom-Header";

    private Map<String, TariffStats> tariffStats;
    private Map<Long, UserMinutes> usersWithTariff = new HashMap<>();
    private static final Logger logger = Logger.getLogger(HrsHandler.class.getName());

    /**
     * Конструктор вызывает метод извлечения из БД информации о тарифах
     */
    @PostConstruct
    private void initializeMap() {
        tariffStats = uploadTariff();
    }

    @Autowired
    BrtAbonentsService brtAbonentsService;
    @Autowired
    TariffStatsService tariffStatsService;
    @Autowired
    private UserMinutesService userMinutesService;

    /**
     * API для обработки отдельных звонков
     * @param param Информация о звонке
     * @param head Служебная информация для подтверждения отправителя (типа декапсуляция)
     * @return Ответ отправителю
     */
    @GetMapping("/single-pay")
    public ResponseEntity<String> singlePay(@RequestParam String param, @RequestHeader(CUSTOM_HEADER) String head) {
        if (checkSignature(head)) {
            return tariffDispatch(decodeParam(param));
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * API для обработки ежемесячных списаний
     * @param param Информация об абоненте
     * @param head Служебная информация для подтверждения отправителя (типа декапсуляция)
     * @return Ответ отправителю
     */
    @GetMapping("/monthly-pay")
    public ResponseEntity<String> monthlyPay(@RequestParam(name = "param") String param, @RequestHeader(CUSTOM_HEADER) String head) {
        if (checkSignature(head)) {
            return payDay(decodeParam(param));
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * API для обработки изменения тарифа абонента
     * @param param Информация об абоненте
     * @param head Служебная информация для подтверждения отправителя (типа декапсуляция)
     * @return Ответ отправителю
     */
    @PutMapping("/change-tariff")
    public ResponseEntity<String> changeTariff(@RequestParam String param,
                                               @RequestHeader(CUSTOM_HEADER) String head) {
        if (checkSignature(head)) {
            return updateLocalTariff(decodeParam(param));
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Метод для обновления тарифа в контексте HRS. Обновляем кеш и таблицу истраченных минут
     * @param param Информация об абоненте (номер и тариф)
     * @return Ответ отправителю
     */
    private ResponseEntity<String> updateLocalTariff(String param) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        long msisdn;
        String tariff;
        try {
            jsonNode = objectMapper.readTree(param);
            msisdn = jsonNode.get("msisdn").asLong();
            tariff = jsonNode.get("tariffId").asText();
        } catch (JsonProcessingException e) {
            return new ResponseEntity<>(INCORRECT_DATA, HttpStatus.BAD_REQUEST);
        }

        TariffStats tS = tariffStats.get(tariff);
        if (tS.getNum_of_minutes() == 0) {
            usersWithTariff.remove(msisdn);
            userMinutesService.deleteUser(msisdn);
        } else {
            UserMinutes user = checkUserContainment(msisdn, tariff);
            usersWithTariff.put(user.getMsisdn(), user);
            userMinutesService.saveUserMinutes(user);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Метод обработки ежемесячных списаний
     * @param param Информация об абоненте (номер и тариф)
     * @return Ответ отправителю
     */
    private ResponseEntity<String> payDay(String param) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(param);
            long msisdn = jsonNode.get("msisdn").asLong();
            String tariff = jsonNode.get("tariffId").asText();

            UserMinutes tempUser = checkUserContainment(msisdn, tariff);
            tempUser.zeroAllMinutes();
            userMinutesService.saveUserMinutes(tempUser);

            double price = round(tariffStats.get(tempUser.getTariff_id()).getPrice_of_period(), 1);

            return new ResponseEntity<>(prepareJson(tempUser.getMsisdn(), price), HttpStatus.OK);

        } catch (IOException e) {
            return new ResponseEntity<>(INCORRECT_DATA, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Метод определяет, какой тариф принадлежит абоненту и есть ли в этом тарифе количество ежемесячных минут
     * @param message Информация об абоненте (номер и тариф)
     * @return
     */
    private ResponseEntity<String> tariffDispatch(String message) {
        HrsTransaction record;
        try {
            record = new HrsTransaction(message);
        } catch (Exception e) {
            return new ResponseEntity<>(INCORRECT_DATA, HttpStatus.BAD_REQUEST);
        }
        if (tariffStats.get(record.getTariffId()).getNum_of_minutes() == 0) {
            String response = noMinutesTariff(record, record.getTariffId());
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        String response = withMinutesTariff(record);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Оработки звонков абонентов, у которых в тарифе не предусмотрено количество минут
     * @param record Информация о звонке
     * @param tariff Тариф абонента
     * @return Стоимость звонка и номер абонента
     */
    private String noMinutesTariff(HrsTransaction record, String tariff) {
        double timeSpent;
        if (record.getTimeSpent() == 0) {
            timeSpent = Math.ceil(record.getCallLength() / 60);
        } else {
            timeSpent = record.getTimeSpent();
        }

        TariffStats tStats = tariffStats.get(tariff);
        double price;

        if (record.getCallId().equals(IN_CALL_TYPE_CODE)) {
            price = tStats.getPrice_incoming_calls();
        } else {
            if (record.getInNet()) {
                price = tStats.getPrice_outcoming_calls_camo();
            } else {
                price = tStats.getPrice_outcoming_calls();
            }
        }
        double sum = round(timeSpent * price, 1);
        return prepareJson(record.getMsisdn(), sum);
    }

    /**
     * Оработки звонков абонентов, у которых в тарифе имеетя какое-то количество минут. Если количество минут
     * израсходовано, будет вызван метод noMinutesTariff и произведен обсчет минут по тарифу 11
     * @param record Информация о звонке
     * @return Стоимость звонка и номер абонента
     */
    private String withMinutesTariff(HrsTransaction record) {
        double timeSpent = Math.ceil(record.getCallLength() / 60);

        TariffStats tStats = tariffStats.get(record.getTariffId());

        UserMinutes userMinutes = checkUserContainment(record.getMsisdn(), record.getTariffId());

        String returnVal;

        if (userMinutes.getUsed_minutes_in() + timeSpent <= tStats.getNum_of_minutes()) {
            userMinutes.increaseMinutes((int) timeSpent);
            returnVal = prepareJson(record.getMsisdn(), 0);
        } else {
            timeSpent -= tStats.getNum_of_minutes() - userMinutes.getUsed_minutes_in();
            userMinutes.setAllMinutes(tStats.getNum_of_minutes());
            record.setTimeSpent(timeSpent);
            returnVal = noMinutesTariff(record, TARIFF_BY_DEFAULT);
        }

        userMinutesService.saveUserMinutes(userMinutes);
        return returnVal;
    }

    /**
     * Проверка абонента на вхождение в базу/кеш абонентов с минутным тарифом
     * @param msisdn Номер телефона
     * @param tariff Тариф
     * @return Абонент с количеством истраченных минут
     */
    private UserMinutes checkUserContainment(long msisdn, String tariff) {
        if (usersWithTariff.containsKey(msisdn)) {
            return usersWithTariff.get(msisdn);
        } else {
            UserMinutes temp = new UserMinutes(msisdn, tariff, ZERO, ZERO);
            usersWithTariff.put(temp.getMsisdn(), temp);
            return temp;
        }
    }

    /**
     * Метод для извлечения тарифов из базы данных
     * @return Мапа тарифов
     */
    private  Map<String, TariffStats> uploadTariff() {
        Map<String, TariffStats> tS = new HashMap<>();
        for (TariffStats elem : tariffStatsService.getAllTariffStats()) {
            tS.put(elem.getTariff_id(), elem);
        }
        return tS;
    }

    /**
     * Декодирование парамертов URL запроса
     * @param encodedString Закодированния строка
     * @return Декодированная строка
     */
    private static String decodeParam(String encodedString) {
        String decodedString = "";
        try {
            decodedString = URLDecoder.decode(encodedString, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decodedString;
    }

    /**
     * Подготовка JSON'a с номером абонента и стоимостью звонка
     * @param msisdn Номер абонента
     * @param price Стоимость звонка
     * @return JSON
     */
    private static String prepareJson(long msisdn, double price) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msisdn", msisdn);
        jsonObject.put("money", price);
        return jsonObject.toString();
    }

    /**
     * Округление переменных типа double до какого-то знака после запятой
     * @param value Переменная
     * @param places Количество знаков
     * @return Округленная переменная
     */
    public static double round(double value, int places) {
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    /**
     * Типа декапсуляция HTTP запроса. Проверка совпадения заголовка с заданным типа JWT токеном
     * @param head Заголовок
     * @return Булеан совпадения или несовпадения
     */
    private static boolean checkSignature(String head) {
        if (head != null) {
            return head.equals(BRT_SIGNATURE);
        }
        return false;
    }
}
