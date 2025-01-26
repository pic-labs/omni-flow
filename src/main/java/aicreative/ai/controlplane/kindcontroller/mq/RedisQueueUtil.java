package aicreative.ai.controlplane.kindcontroller.mq;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
public class RedisQueueUtil {

    private final StringRedisTemplate redisTemplate;

    public boolean queueExist(String queueName) {
        return redisTemplate.hasKey(queueName);
    }

    public void createGroup(String queueName, String group) {
        redisTemplate.opsForStream().createGroup(queueName, group);
    }

    public RecordId enqueue(String queueName, Map<String, String> value) {
        return redisTemplate.opsForStream().add(queueName, value);
    }

    public void ack(String queueName, String group, String... recordIds) {
        redisTemplate.opsForStream().acknowledge(queueName, group, recordIds);
    }

    public void delete(String queueName, String... recordIds) {
        redisTemplate.opsForStream().delete(queueName, recordIds);
    }
}


