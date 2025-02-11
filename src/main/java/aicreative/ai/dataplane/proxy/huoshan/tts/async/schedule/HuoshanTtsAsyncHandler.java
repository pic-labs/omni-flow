package aicreative.ai.dataplane.proxy.huoshan.tts.async.schedule;

import aicreative.ai.dataplane.proxy.huoshan.tts.async.model.HuoshanTtsAsyncTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Data
@Component
public class HuoshanTtsAsyncHandler {

    private static final String SUBMIT_URL = "https://openspeech.bytedance.com/api/v1/tts_async/submit";
    private static final String EMOTION_SUBMIT_URL = "https://openspeech.bytedance.com/api/v1/tts_async_with_emotion/submit";
    private static final String QUERY_URL = "https://openspeech.bytedance.com/api/v1/tts_async/query?appid=%s&task_id=%s";
    private static final String EMOTION_QUERY_URL = "https://openspeech.bytedance.com/api/v1/tts_async_with_emotion/query?appid=%s&task_id=%s";
    private static final String RESOURCE_ID = "volc.tts_async.default";
    private static final String EMOTION_RESOURCE_ID = "volc.tts_async.emotion";

    @Value("${proxy.coze.api.app-id}")
    private String cozeApiAppId;
    @Value("${proxy.coze.api.token}")
    private String cozeApiToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public HuoshanTtsAsyncHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HuoshanTtsAsyncSubmitResponse request(Map<String, Object> params) {
        String url;
        ResponseEntity<String> response;
        String appid = cozeApiAppId;
        String token = cozeApiToken;
        String reqid = UUID.randomUUID().toString();

        // create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer; " + token);
        if ((int) params.get("use_emotion_api") == 1) {
            url = EMOTION_SUBMIT_URL;
            headers.set("Resource-Id", EMOTION_RESOURCE_ID);
        } else {
            url = SUBMIT_URL;
            headers.set("Resource-Id", RESOURCE_ID);
        }

        // create body
        params.put("appid", appid);
        params.put("reqid", reqid);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, headers);

        // send request
        response = restTemplate.postForEntity(url, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(String.format("Failed to request huoshanTts workflow. Status http_code: %s", response.getStatusCode()));
        }
        try {
            HuoshanTtsAsyncSubmitResponse responseBody = objectMapper.readValue(response.getBody(), HuoshanTtsAsyncSubmitResponse.class);
            if (Objects.equals(responseBody.getTask_status(), 2)) {
                throw new RuntimeException(String.format("Failed to request huoshanTts workflow. Return task_status: %s", responseBody.getTask_status()));
            }
            return responseBody;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public HuoshanTtsAsyncQueryResponse queryResult(HuoshanTtsAsyncTask t) {
        String url;
        String appid = cozeApiAppId;
        String token = cozeApiToken;

        // create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer; " + token);
        if (t.getUseEmotion()) {
            url = String.format(EMOTION_QUERY_URL, appid, t.getExecutionId());
            headers.set("Resource-Id", EMOTION_RESOURCE_ID);
        } else {
            url = String.format(QUERY_URL, appid, t.getExecutionId());
            headers.set("Resource-Id", RESOURCE_ID);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<HuoshanTtsAsyncQueryResponse> httpResponse = restTemplate.exchange(url, HttpMethod.GET, requestEntity, HuoshanTtsAsyncQueryResponse.class);
            return httpResponse.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to query huoshanTts workflow result.", e);
        }
    }
}