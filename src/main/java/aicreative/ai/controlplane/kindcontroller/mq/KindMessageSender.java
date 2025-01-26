package aicreative.ai.controlplane.kindcontroller.mq;

import aicreative.ai.controlplane.kindcontroller.base.KindDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static aicreative.ai.controlplane.kindcontroller.mq.KindMessageConsumer.KIND_QUEUE_NAME;

@Slf4j
@AllArgsConstructor
@Component
public class KindMessageSender {

    private RedisQueueUtil redisQueueUtil;

    public void sendKindMessage(final KindDefinition def, final String operation, final String extras) {

        final KindMessageBody msg = new KindMessageBody(operation, def.getKind(), def.getMetadata().getId(), extras);
        final Map<String, String> msgMap = msg.toMap();
        final RecordId recordId = redisQueueUtil.enqueue(KIND_QUEUE_NAME, msgMap);
        log.info("Send KIND message. RecordId: {}, KindType: {}, KindId: {}, Operation: {}, Diff: {}",
                recordId.getValue(), msg.getKindType(), msg.getKindId(), msg.getOperation(), msg.getExtra());
    }

    @Data
    @AllArgsConstructor
    public static class KindMessageBody {
        private String operation;
        private String kindType;
        private String kindId;
        private String extra;

        public KindMessageBody(final Map<String, String> m) {
            this.operation = m.get("operation");
            this.kindType = m.get("kindType");
            this.kindId = m.get("kindId");
            this.extra = m.get("extra");
        }

        public Map<String, String> toMap() {
            final Map<String, String> m = new HashMap<>();
            m.put("operation", this.operation);
            m.put("kindType", this.kindType);
            m.put("kindId", this.kindId);
            m.put("extra", this.extra);
            return m;
        }
    }
}
