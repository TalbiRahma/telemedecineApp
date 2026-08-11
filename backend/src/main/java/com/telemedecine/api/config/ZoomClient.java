package com.telemedecine.api.config;

import com.telemedecine.api.model.ZoomMeetingInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ZoomClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${zoom.api.base-url}")
    private String baseUrl;

    @Value("${zoom.api.account-id}")
    private String accountId;

    @Value("${zoom.api.client-id}")
    private String clientId;

    @Value("${zoom.api.client-secret}")
    private String clientSecret;


    private String getAccessToken() {
        String url = "https://zoom.us/oauth/token?grant_type=account_credentials&account_id=" + accountId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to get Zoom access token");
        }

        return (String) response.getBody().get("access_token");
    }


    public ZoomMeetingInfo createMeeting(Long doctorId, String topic, java.time.LocalDateTime startTime, int durationMinutes) {
        String token = getAccessToken();
        String url = baseUrl + "/users/me/meetings";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = new HashMap<>();
        request.put("topic", topic != null ? topic : "Consultation Doctor #" + doctorId);
        request.put("type", 2);
        request.put("start_time", startTime.format(DateTimeFormatter.ISO_DATE_TIME));
        request.put("duration", durationMinutes);
        request.put("timezone", "Europe/Paris");

        Map<String, Object> settings = new HashMap<>();
        settings.put("join_before_host", false);
        settings.put("waiting_room", true);
        settings.put("host_video", true);
        settings.put("participant_video", true);

        request.put("settings", settings);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> data = response.getBody();
        if (data == null) throw new RuntimeException("Zoom API did not return meeting info");

        ZoomMeetingInfo info = new ZoomMeetingInfo();
        info.setId(String.valueOf(data.get("id")));
        info.setJoinUrl((String) data.get("join_url"));
        info.setStartUrl((String) data.get("start_url"));

        return info;
    }

    public void deleteMeeting(String meetingId) {
        String token = getAccessToken();
        String url = baseUrl + "/meetings/" + meetingId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
}
