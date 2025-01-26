package aicreative.ai.dataplane.proxy.coze.schedule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CozeResponse {

    private Status status;
    private String message;
    private String output;

    public enum Status {
        Success,
        Failed,
        Running
    }
}
