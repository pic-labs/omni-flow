package aicreative.ai.dataplane.proxy.huoshan.tts.async.schedule;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HuoshanTtsAsyncQueryResponse {

    private String task_id;
    private Integer task_status;
    private Integer text_length;
    private Long url_expire_time;
    private String audio_url;
    private Integer code;
    private String message;

    public Status getStatus() {
        return Status.from(task_status);
    }

    @Getter
    @AllArgsConstructor
    public enum Status {
        Success(1), Pending(0), Fail(2), Unknown(-1),
        ;

        private final int value;

        public static Status from(Integer taskStatus) {
            if (taskStatus == null) {
                return Unknown;
            }
            for (Status status : Status.values()) {
                if (status.value == taskStatus) {
                    return status;
                }
            }
            return Unknown;
        }
    }
}
