package com.bootcamp_proj.bootcampproj.standalone_services;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
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
    private static final String ADMIN = "admin";
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
                                                  @RequestHeader(AUTH_HEADER) HttpHeaders head) {
        if (checkAdminAuthorization(head) || checkAbonentInBrt(head)) {
            String url = STARTER_URL + LIST_URL + URL_BREAK + msisdn;
            return sendRequestToBrt(url, HttpMethod.GET);
        } else {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/list")
    private ResponseEntity<String> getAllAbonentsInfo(@RequestHeader(AUTH_HEADER) HttpHeaders head) {
        if (checkAdminAuthorization(head)) {
            String url = STARTER_URL + LIST_URL;
            return sendRequestToBrt(url, HttpMethod.GET);
        } else {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/list/{msisdn}/pay")
    private ResponseEntity<String> abonentProceedPayment(@PathVariable String msisdn,
                                                         @RequestParam("money") String value,
                                                         @RequestHeader(AUTH_HEADER) HttpHeaders head) {
        if (checkAbonentInBrt(head)) {
            String url = STARTER_URL + LIST_URL + URL_BREAK + msisdn + PAY_URL + VALUE_PARAM + value;
            return sendRequestToBrt(url, HttpMethod.POST);
        } else {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/create/{msisdn}")
    private ResponseEntity<String> managerCreateNewAbonent(@PathVariable String msisdn,
                                                           @RequestParam("tariff-id") String tariffId,
                                                           @RequestBody RequestBody body,
                                                           @RequestHeader(AUTH_HEADER) HttpHeaders head) {
        if (checkAdminAuthorization(head)) {
            String url = STARTER_URL + CREATE_URL + URL_BREAK + msisdn + TARIFF_PARAM + tariffId;
            return sendRequestToBrt(url, HttpMethod.POST, body);
        } else {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/{msisdn}/change-tariff")
    private ResponseEntity<String> managerUpdateAbonentTariff(@PathVariable String msisdn,
                                                              @RequestParam("tariff-id") String tariffId,
                                                              @RequestHeader(AUTH_HEADER) HttpHeaders head) {
        if (checkAdminAuthorization(head)) {
            String url = STARTER_URL + URL_BREAK + msisdn + TARIFF_CHANGER_URL + TARIFF_PARAM + tariffId;
            return sendRequestToBrt(url, HttpMethod.POST);
        } else {
            return new ResponseEntity<>(DENY, HttpStatus.UNAUTHORIZED);
        }
    }

    private ResponseEntity<String> badResponseHandler(ResponseEntity<String> entity) {
        if (entity.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            return new ResponseEntity<>("Incorrect Request", HttpStatus.BAD_REQUEST);
        } else if (entity.getStatusCode().equals(HttpStatus.REQUEST_TIMEOUT)) {
            return new ResponseEntity<>("Service unavailable. Try Again Later", HttpStatus.BAD_REQUEST);
        } else {
            return entity;
        }
    }

    private boolean checkAdminAuthorization(HttpHeaders auth) {
        String[] arr = extractBasicHeader(auth);
        return arr[0].equals(ADMIN) && arr[1].equals(ADMIN);
    }

    private boolean checkAbonentInBrt(HttpHeaders header) {
        String url = STARTER_URL + URL_BREAK + CHECK_CONTAINMENT + MSISDN_PARAM + extractBasicHeader(header)[0];
        ResponseEntity<String> resp = sendRequestToBrt(url, HttpMethod.GET);
        return resp.getStatusCode().equals(HttpStatus.OK);
    }

    private ResponseEntity<String> sendRequestToBrt(String url, HttpMethod httpMethod) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CUSTOM_HEADER, CRM_SIGNATURE);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);
        logger.info("CRM Response From BRT: " + response.getBody());

        return badResponseHandler(response);
    }

    private ResponseEntity<String> sendRequestToBrt(String url, HttpMethod httpMethod, RequestBody body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CUSTOM_HEADER, CRM_SIGNATURE);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);
        logger.info("CRM Response From BRT: " + response.getBody());

        return badResponseHandler(response);
    }

    public String[] extractBasicHeader(HttpHeaders headers) {
        if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader.startsWith(BASIC)) {
                String base64Credentials = authHeader.substring(BASIC.length());
                return base64Credentials.split(COLON, 2);
            }
        } else {
            throw new IllegalArgumentException();
        }
        return null;
    }
}
