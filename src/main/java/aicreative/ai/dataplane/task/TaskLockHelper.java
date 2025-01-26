package aicreative.ai.dataplane.task;

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
public class TaskLockHelper {

    private StringRedisTemplate stringRedisTemplate;

    private static final long DEFAULT_LOCK_EXPIRATION_MILLISECONDS = 90_000L;
    private final Lock localLock = new ReentrantLock();

    public void lock(String lockName, String randomValue) {
        while (true) {
            localLock.lock();
            try {
                final Boolean s = stringRedisTemplate.opsForValue().setIfAbsent(lockName, randomValue, DEFAULT_LOCK_EXPIRATION_MILLISECONDS, TimeUnit.MILLISECONDS);
                boolean success = Objects.requireNonNullElse(s, false);
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

    public void unlock(String lockName, String value) {
        localLock.lock();
        try {
            String currentValue = stringRedisTemplate.opsForValue().get(lockName);
            if (value.equals(currentValue)) {
                stringRedisTemplate.delete(lockName);
            }
        } finally {
            localLock.unlock();
        }
    }
}
