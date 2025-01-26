package aicreative.ai.controlplane.kindcontroller.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@AllArgsConstructor
@Component
public class KindLockHelper {

    private StringRedisTemplate stringRedisTemplate;

    private static final long KIND_LOCK_EXPIRATION_MILLISECONDS = 90_000L;
    private static final String KIND_LOCK_REDIS_KEY = "lock-kind";
    private final Lock localLock = new ReentrantLock(true);

    public void lock(String kindId, String randomValue) {
        while(true) {
            localLock.lock();
            try {
                final String key = obtainRedisKey(kindId);
                final Boolean s = stringRedisTemplate.opsForValue().setIfAbsent(key, randomValue, KIND_LOCK_EXPIRATION_MILLISECONDS, TimeUnit.MILLISECONDS);
                boolean success =Objects.requireNonNullElse(s, false);
                if (success) {
                    return;
                }
            } finally {
                localLock.unlock();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(1_000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void unlock(String kindId, String value) {
        localLock.lock();
        try {
            final String key = obtainRedisKey(kindId);
            String currentValue = stringRedisTemplate.opsForValue().get(key);
            if (value.equals(currentValue)) {
                stringRedisTemplate.delete(key);
            }
        } finally {
            localLock.unlock();
        }
    }

    private String obtainRedisKey(final String kindId) {
        return String.format("%s:%s", KIND_LOCK_REDIS_KEY, kindId);
    }
}
