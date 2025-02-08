package aicreative.ai.dataplane.proxy.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Data
@Component
public class DeepSeekHandler {
    @Value("${proxy.deep-seek.api.key}")
    private String deepSeekApiKey;
    @Resource
    private ObjectMapper objectMapper;
    private final String deepSeekUrl = "https://api.deepseek.com/chat/completions";
    private final RestTemplate restTemplate = new RestTemplate();

    public Object request(String taskId, Object param) {
        // Build request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + deepSeekApiKey);

        DeepSeekRequest request;
        try {
            request = objectMapper.readValue(objectMapper.writeValueAsString(param), DeepSeekRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("deepseek parse param error,taskId:{}", taskId, e);
            throw new RuntimeException("deepseek parse param error");
        }
        ResponseEntity<DeepSeekResponse> respEntity;
        try {
            respEntity = restTemplate.exchange(deepSeekUrl, HttpMethod.POST,
                    new HttpEntity<>(request, headers), DeepSeekResponse.class);
        } catch (Exception e) {
            log.warn("deepseek request error,taskId:{}", taskId, e);
            throw new RuntimeException("deepseek request error");
        }
        if (!respEntity.getStatusCode().is2xxSuccessful()) {
            log.warn("deepseek request error, code not 200, taskId:{},code:{},resp:{}", taskId, respEntity.getStatusCode(), respEntity);
            throw new RuntimeException("deepseek request error, code not 200");
        }
        DeepSeekResponse deepSeekResponse = respEntity.getBody();
        if (deepSeekResponse == null || deepSeekResponse.getChoices().isEmpty()) {
            log.warn("deepseek request error, no content, taskId:{},resp:{}", taskId, respEntity);
            throw new RuntimeException("deepseek request error, no content");
        }
        return getContentFromResponse(deepSeekResponse);
    }

    private Object getContentFromResponse(DeepSeekResponse deepSeekResponse) {
        String content = deepSeekResponse.getChoices().getFirst().getMessage().getContent();
        try {
            return objectMapper.readValue(content, Object.class);
        } catch (JsonProcessingException e) {
            return content;
        }
    }

    /**
     * <pre>
     * {
     *   "messages": [
     *     {
     *       "content": "You are a helpful assistant",
     *       "role": "system"
     *     },
     *     {
     *       "content": "Hi",
     *       "role": "user"
     *     }
     *   ],
     *   "model": "deepseek-chat",
     *   "frequency_penalty": 0,
     *   "max_tokens": 2048,
     *   "presence_penalty": 0,
     *   "response_format": {
     *     "type": "text"
     *   },
     *   "stop": null,
     *   "stream": false,
     *   "stream_options": null,
     *   "temperature": 1,
     *   "top_p": 1,
     *   "tools": null,
     *   "tool_choice": "none",
     *   "logprobs": false,
     *   "top_logprobs": null
     * }
     * </pre>
     */
    @Data
    public static class DeepSeekRequest {
        /**
         * A list of messages comprising the conversation so far.
         * required
         */
        private List<ReqMessage> messages;
        /**
         * ID of the model to use. [deepseek-chat, deepseek-reasoner]
         * required
         */
        private String model;
        /**
         * Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
         * default:0
         */
        private Double frequency_penalty;
        /**
         * Integer between 1 and 8192. The maximum number of tokens that can be generated in the chat completion.
         * <p>
         * The total length of input tokens and generated tokens is limited by the model's context length.
         * <p>
         * If max_tokens is not specified, the default value 4096 is used.
         */
        private Integer max_tokens;
        /**
         * Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.
         * default:0
         */
        private Double presence_penalty;
        /**
         * An object specifying the format that the model must output. Setting to { "type": "json_object" } enables JSON Output, which guarantees the message the model generates is valid JSON.
         */
        private ResponseFormat response_format;
        /**
         * What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.
         * <p>
         * We generally recommend altering this or top_p but not both.
         * default:1
         */
        private Double temperature;
        /**
         * An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.
         * <p>
         * We generally recommend altering this or temperature but not both.
         * <p>
         * default:1
         */
        private Double top_p;
    }

    @Data
    public static class ReqMessage {
        /**
         * The role of the messages author
         * required
         */
        private String role;
        /**
         * The contents of the message
         * required
         */
        private String content;
        /**
         * An optional name for the participant. Provides the model information to differentiate between participants of the same role.
         */
        private String name;
    }

    @Data
    public static class ResponseFormat {
        /**
         * [text, json_object]
         * default:text
         */
        private String type;
    }

    /**
     * <pre>
     * {
     *   "id": "82fa8e75-f6a7-4738-8db6-5b3069632771",
     *   "object": "chat.completion",
     *   "created": 1738838302,
     *   "model": "deepseek-chat",
     *   "choices": [
     *     {
     *       "index": 0,
     *       "message": {
     *         "role": "assistant",
     *         "content": "Hello! How can I assist you today? 😊"
     *       },
     *       "logprobs": null,
     *       "finish_reason": "stop"
     *     }
     *   ],
     *   "usage": {
     *     "prompt_tokens": 9,
     *     "completion_tokens": 11,
     *     "total_tokens": 20,
     *     "prompt_tokens_details": {
     *       "cached_tokens": 0
     *     },
     *     "prompt_cache_hit_tokens": 0,
     *     "prompt_cache_miss_tokens": 9
     *   },
     *   "system_fingerprint": "fp_3a5770e1b4"
     * }
     * </pre>
     */
    @Data
    public static class DeepSeekResponse {
        /**
         * A unique identifier for the chat completion.
         */
        private String id;
        /**
         * A list of chat completion choices.
         */
        private List<Choice> choices;
        /**
         * The Unix timestamp (in seconds) of when the chat completion was created.
         */
        private long created;
        /**
         * The model used for the chat completion.
         */
        private String model;
        /**
         * This fingerprint represents the backend configuration that the model runs with.
         */
        private String system_fingerprint;
        /**
         * The object type, which is always chat.completion.
         */
        private String object;
        /**
         * Usage statistics for the completion request.
         */
        private Object usage;
    }

    @Data
    public static class Choice {
        /**
         * The reason the model stopped generating tokens.
         * This will be "stop" if the model hit a natural stop point or a provided stop sequence,
         * "length" if the maximum number of tokens specified in the request was reached,
         * "content_filter" if content was omitted due to a flag from our content filters,
         * "tool_calls" if the model called a tool,
         * or "insufficient_system_resource" if the request is interrupted due to insufficient resource of the inference system.
         */
        private String finish_reason;
        /**
         * The index of the choice in the list of choices.
         */
        private int index;
        /**
         * A chat completion message generated by the model.
         */
        private RespMessage message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RespMessage {
        /**
         * The role of the author of this message.
         */
        private String role;
        /**
         * The contents of the message.
         */
        private String content;
        /**
         * For deepseek-reasoner model only. The reasoning contents of the assistant message, before the final answer.
         */
        private String reasoning_content;
    }
}
