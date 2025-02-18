package aicreative.ai.dataplane.proxy.huoshan.tts.async.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HuoshanTtsAsyncSubmitResponse {
    private String task_id;
    private Integer task_status;
    private Integer text_length;
}
