package aicreative.ai.dataplane.proxy.coze.schedule;

import aicreative.ai.dataplane.proxy.coze.schedule.CozeResponse.Status;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Data
@Component
public class CozeHandler {

    private final String cozeRequestUrl = "https://api.coze.cn/v1/workflow/run";
    private final String cozeQueryResultUrl = "https://api.coze.cn/v1/workflows/{workflow_id}/run_histories/{execute_id}";

    @Value("${proxy.coze.api.token}")
    private String cozeApiToken;

    private final RestTemplate restTemplate;

    public CozeHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String request(String workflowId, Object param) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", String.format("Bearer %s", cozeApiToken));

        Map<String, Object> body = new HashMap<>();
        body.put("workflow_id", workflowId);
        body.put("parameters", param);
        body.put("is_async", true);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<RequestResponse> response = restTemplate.postForEntity(cozeRequestUrl, requestEntity, RequestResponse.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(String.format("Failed to request coze workflow. Status code: %s", response.getStatusCode()));
        }
        RequestResponse responseBody = Objects.requireNonNull(response.getBody());
        if (!Objects.equals(responseBody.getCode(), 0)) {
            throw new RuntimeException(String.format("Failed to request coze workflow. Return code: %s", responseBody.getCode()));
        }
        return responseBody.getExecuteId();
    }

    public CozeResponse queryResult(String workflowId, String executeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", String.format("Bearer %s", cozeApiToken));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = cozeQueryResultUrl.replace("{workflow_id}", workflowId).replace("{execute_id}", executeId);

        ResponseEntity<QueryResponse> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, QueryResponse.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(String.format("Failed to query coze workflow result. Status code: %s", response.getStatusCode()));
        }
        QueryResponse responseBody = Objects.requireNonNull(response.getBody());
        if (!Objects.equals(responseBody.getCode(), 0)) {
            throw new RuntimeException(String.format("Failed to query coze workflow result. Return code: %s, %s", responseBody.getCode(), responseBody.getMsg()));
        }
        if (CollectionUtils.isEmpty(responseBody.getData())) {
            return CozeResponse.builder().status(Status.Running).build();
        }
        return convertCozeResponse(responseBody.getData().getFirst());
    }

    private CozeResponse convertCozeResponse(DataResponse resp) {
        if (StringUtils.equals(resp.getExecuteStatus(), "Running")) {
            return CozeResponse.builder().status(Status.Running).build();
        }
        if (StringUtils.equals(resp.getExecuteStatus(), "Failed")) {
            return CozeResponse.builder().status(Status.Failed).message(resp.getErrorMessage()).build();
        }
        Assert.state(StringUtils.equals(resp.getExecuteStatus(), "Success"), "Invalid execute status");
        return CozeResponse.builder().status(Status.Success).output(resp.getOutput()).build();
    }

    @Data
    public static class RequestResponse {

        @JsonProperty("code")
        private int code;

        @JsonProperty("debug_url")
        private String debugUrl;

        @JsonProperty("execute_id")
        private String executeId;

        @JsonProperty("msg")
        private String msg;
    }

    @Data
    public static class QueryResponse {

        @JsonProperty("code")
        private int code;

        @JsonProperty("msg")
        private String msg;

        @JsonProperty("data")
        private List<DataResponse> data;
    }

    @Data
    public static class DataResponse {
        @JsonProperty("execute_status")
        private String executeStatus;
        @JsonProperty("error_message")
        private String errorMessage;
        @JsonProperty("output")
        private String output;
    }
}
