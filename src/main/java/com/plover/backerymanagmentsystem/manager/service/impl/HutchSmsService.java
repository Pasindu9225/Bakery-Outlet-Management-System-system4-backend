package com.plover.backerymanagmentsystem.manager.service.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class HutchSmsService {

    @Value("${hutch.sms.username:randiwijesinghe92@gmail.com}")
    private String username;

    @Value("${hutch.sms.password:qt%^81PW}")
    private String password;

    @Value("${hutch.sms.loginUrl:https://bsms.hutch.lk/api/login}")
    private String loginUrl;

    @Value("${hutch.sms.sendUrl:https://bsms.hutch.lk/api/sendsms}")
    private String sendUrl;

    @Value("${hutch.sms.mask:CakeRepublc}")
    private String defaultMask;

    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedAccessToken = null;
    private long tokenExpiryTimeMs = 0;

    /**
     * Authenticates with Hutch OAuth 2.0 Login API to obtain a Bearer Access Token.
     */
    public synchronized String getAccessToken() {
        // Return cached token if valid (valid for 24 hours, refresh 10 minutes early)
        if (cachedAccessToken != null && System.currentTimeMillis() < (tokenExpiryTimeMs - 600000)) {
            return cachedAccessToken;
        }

        try {
            log.info("Requesting fresh Hutch OAuth 2.0 Access Token for username: {}", username);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-VERSION", "v1");
            headers.set("Accept", "*/*");

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("username", username);
            requestBody.put("password", password);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<HutchTokenResponse> response = restTemplate.postForEntity(loginUrl, entity, HutchTokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getAccessToken() != null) {
                cachedAccessToken = response.getBody().getAccessToken();
                // Valid for 24 hours (86,400,000 ms)
                tokenExpiryTimeMs = System.currentTimeMillis() + 86400000L;
                log.info("Successfully obtained Hutch Access Token.");
                return cachedAccessToken;
            } else {
                log.error("Failed to authenticate with Hutch SMS Gateway. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error during Hutch OAuth 2.0 Login: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Dispatches an SMS using Hutch OAuth 2.0 Send SMS API.
     */
    public boolean sendSms(String phoneNumber, String messageText) {
        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                log.warn("Cannot send SMS: Phone number is empty.");
                return false;
            }

            // Format Sri Lankan phone number (e.g. 0740561822 -> 94740561822)
            String cleanNumber = phoneNumber.trim();
            String formattedNumber = cleanNumber;
            if (cleanNumber.startsWith("0")) {
                formattedNumber = "94" + cleanNumber.substring(1);
            } else if (!cleanNumber.startsWith("94") && cleanNumber.length() == 9) {
                formattedNumber = "94" + cleanNumber;
            }

            String token = getAccessToken();
            if (token == null) {
                log.error("Unable to dispatch SMS: Could not acquire valid Hutch Access Token.");
                return false;
            }

            log.info("Dispatching Hutch OAuth 2.0 SMS to destination {}: {}", formattedNumber, messageText);

            boolean success = executeSendSms(token, formattedNumber, messageText);

            // If 401 Unauthorized or expired token, retry once with fresh login token
            if (!success) {
                log.warn("Retrying Hutch SMS dispatch with refreshed Access Token...");
                this.cachedAccessToken = null;
                token = getAccessToken();
                if (token != null) {
                    success = executeSendSms(token, formattedNumber, messageText);
                }
            }

            return success;
        } catch (Exception e) {
            log.error("Failed to dispatch SMS via Hutch Gateway to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    private boolean executeSendSms(String token, String formattedNumber, String messageText) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-VERSION", "v1");
            headers.set("Accept", "*/*");
            headers.set("Authorization", "Bearer " + token);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("campaignName", "OTP Verification");
            requestBody.put("mask", defaultMask);
            requestBody.put("numbers", formattedNumber);
            requestBody.put("content", messageText);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(sendUrl, entity, String.class);

            log.info("Hutch Send SMS API Response: {} - {}", response.getStatusCode(), response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error executing Hutch Send SMS HTTP POST: {}", e.getMessage());
            return false;
        }
    }

    @Data
    public static class HutchTokenResponse {
        private String accessToken;
        private String refreshToken;
    }
}
