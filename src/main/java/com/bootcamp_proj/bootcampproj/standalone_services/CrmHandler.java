package com.bootcamp_proj.bootcampproj.standalone_services;

import jakarta.websocket.server.PathParam;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.logging.Logger;

@Service
@EnableAsync
@RequestMapping("/abonents")
@RestController
public class CrmHandler {
    private static final String STARTER_URL = "http://localhost:8082/api/brt";
    private static final String LIST_URL = "/list";
    private static final String PAY_URL = "/pay";
    private static final String TARIFF_CHANGER_URL = "/change-tariff";
    private static final String CREATE_URL = "/create";
    private static final String URL_BREAK = "/";
    private static final String COLON = ":";
    private static final String DENY = "AccessProhibited";
    private static final String CUSTOM_HEADER = "Custom-Header";
    private static final String CRM_SIGNATURE = "CRM-Signature";
    private static final String ADMIN = "admin:admin";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BASIC = "Basic ";
    private static final String VALUE_PARAM = "?value=";
    private static final String CHECK_CONTAINMENT = "/check-containment";
    private static final String MSISDN_PARAM = "?msisdn=";
    private static final String TARIFF_PARAM = "?tariff-id=";

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = Logger.getLogger(CrmHandler.class.getName());

    @GetMapping("/list/{msisdn}")
    private ResponseEntity<String> getAbonentInfo(@PathVariable String msisdn,
                                                  @RequestHeader(AUTH_HEADER) String head) {
        if (checkAdminAuthorization(head) || checkAbonentInBrt(head)) {
            String url = STARTER_URL + LIST_URL + URL_BREAK + msisdn;
            return sendRequestToBrt(url, HttpMethod.GET);
        }
        return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/list")
    private ResponseEntity<String> getAllAbonentsInfo(@RequestHeader(AUTH_HEADER) String head) {
        if (checkAdminAuthorization(head)) {
            String url = STARTER_URL + LIST_URL;
            return sendRequestToBrt(url, HttpMethod.GET);
        }
        return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/list/{msisdn}/pay")
    private ResponseEntity<String> abonentProceedPayment(@PathVariable String msisdn,
                                                         @RequestParam("value") String value,
                                                         @RequestHeader(AUTH_HEADER) String head) {
        if (checkAbonentInBrt(head)) {
            String url = STARTER_URL + LIST_URL + URL_BREAK + msisdn + PAY_URL + VALUE_PARAM + value;
            return sendRequestToBrt(url, HttpMethod.POST);
        }
        return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/create/{msisdn}")
    private ResponseEntity<String> managerCreateNewAbonent(@PathVariable String msisdn,
                                                           @RequestParam("tariff-id") String tariffId,
                                                           @RequestHeader(AUTH_HEADER) String head) {
        if (checkAdminAuthorization(head)) {
            String url = STARTER_URL + CREATE_URL + URL_BREAK + msisdn + TARIFF_PARAM + tariffId;
            return sendRequestToBrt(url, HttpMethod.POST);
        }
        return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/{msisdn}/change-tariff")
    private ResponseEntity<String> managerUpdateAbonentTariff(@PathVariable String msisdn,
                                                              @RequestParam("tariff-id") String tariffId,
                                                              @RequestHeader(AUTH_HEADER) String head) {
        if (checkAdminAuthorization(head)) {
            String url = STARTER_URL + URL_BREAK + msisdn + TARIFF_CHANGER_URL + TARIFF_PARAM + tariffId;
            return sendRequestToBrt(url, HttpMethod.POST);
        }
        return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
    }

    private boolean checkAdminAuthorization(String auth) {
        //auth = decodeFromAuthHeader(auth);
        if (auth.equals(ADMIN)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkAbonentInBrt(String msisdn) {
        //msisdn = decodeFromAuthHeader(msisdn);
        String url = STARTER_URL + URL_BREAK + CHECK_CONTAINMENT + MSISDN_PARAM + extractSubstring(msisdn);
        ResponseEntity<String> resp = sendRequestToBrt(url, HttpMethod.GET);
        if (resp.getStatusCode().equals(HttpStatus.OK)) {
            return true;
        }
        return false;
    }

    private ResponseEntity<String> sendRequestToBrt(String url, HttpMethod httpMethod) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CUSTOM_HEADER, CRM_SIGNATURE);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);
        logger.info("Response: " + response.getBody());

        return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
    }

    private String decodeFromAuthHeader(String in) {
        if (in != null && in.startsWith(BASIC)) {
            in = in.substring(BASIC.length());
        }

        byte[] bytes;
        try {
            bytes = Base64.decodeBase64(in);
        } catch (Exception e) {
            bytes = new byte[0];
        }
        return new String(bytes);
    }

    public static String extractSubstring(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        int index = input.indexOf(COLON);
        if (index != -1) {
            return input.substring(0, index);
        } else {
            return input;
        }
    }
}
