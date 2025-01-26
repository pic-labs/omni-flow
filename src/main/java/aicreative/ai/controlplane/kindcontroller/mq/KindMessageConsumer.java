package aicreative.ai.controlplane.kindcontroller.mq;

import aicreative.ai.controlplane.kindcontroller.base.KindController;
import aicreative.ai.controlplane.kindcontroller.base.KindLockHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Configuration
@Slf4j
@AllArgsConstructor
public class KindMessageConsumer {

    private RedisQueueUtil redisQueueUtil;

    private List<KindController> controllers;

    private KindLockHelper kindLockHelper;

    static final String KIND_QUEUE_NAME = "cp-queue-kind";
    private final static String KIND_CONSUMER_GROUP = "kind-group";
    private final static String KIND_CONSUMER_NAME = "kind-consumer";

    @Bean
    public Subscription kindSubscription(RedisConnectionFactory factory) {

        // init queue
        if (!redisQueueUtil.queueExist(KIND_QUEUE_NAME)) {
            final Map<String, String> map = new HashMap<>(1);
            map.put("field", "value");
            final RecordId result = redisQueueUtil.enqueue(KIND_QUEUE_NAME, map);
            redisQueueUtil.createGroup(KIND_QUEUE_NAME, KIND_CONSUMER_GROUP);
            redisQueueUtil.delete(KIND_QUEUE_NAME, result.getValue());
            log.info("Initialize Queue:{} and ConsumerGroup:{} success.", KIND_QUEUE_NAME, KIND_CONSUMER_GROUP);
        }

        // create container
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setDaemon(true);
        executor.setThreadNamePrefix("kind-consumer-");
        executor.initialize();

        var containerOptions =
                StreamMessageListenerContainer
                        .StreamMessageListenerContainerOptions
                        .builder()
                        .batchSize(1)
                        .executor(executor)
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();
        var container = StreamMessageListenerContainer.create(factory, containerOptions);

        // start listener
        var listener = new KindStreamListener(redisQueueUtil, controllers, kindLockHelper);

        var streamRequest = StreamMessageListenerContainer.StreamReadRequest
                .builder(StreamOffset.create(KIND_QUEUE_NAME, ReadOffset.lastConsumed()))
                .consumer(Consumer.from(KIND_CONSUMER_GROUP, KIND_CONSUMER_NAME))
                .cancelOnError(t -> {
                    try {
                        TimeUnit.SECONDS.sleep(5L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return false;
                })
                .autoAcknowledge(true)
                .build();
        Subscription subscription = container.register(streamRequest, listener);
        container.start();
        return subscription;
    }

    @Slf4j
    @Data
    @RequiredArgsConstructor
    public static class KindStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

        private final RedisQueueUtil redisQueueUtil;

        private final List<KindController> controllers;

        private final KindLockHelper kindLockHelper;

        @Override
        public void onMessage(MapRecord<String, String, String> message) {

            final String queueName = message.getStream();
            final RecordId recordId = message.getId();
            final Map<String, String> m = message.getValue();

            log.debug("Receive KIND message. RecordId: {}", recordId);

            // process logic
            final KindMessageSender.KindMessageBody messageBody = new KindMessageSender.KindMessageBody(m);
            final KindController.Request request = new KindController.Request(queueName,
                    recordId.getValue(), messageBody.getKindType(), messageBody.getKindId(),
                    messageBody.getOperation(), messageBody.getExtra());
            for (final KindController c : controllers) {
                final String randomValue = UUID.randomUUID().toString();
                try {
                    kindLockHelper.lock(messageBody.getKindId(), randomValue);
                    log.trace("Got controller KindLock.");
                    reconcile(c, request);
                } finally {
                    log.trace("Release controller KindLock.");
                    kindLockHelper.unlock(messageBody.getKindId(), randomValue);
                }
            }

            redisQueueUtil.delete(queueName, recordId.getValue());
        }

        private void reconcile(final KindController c, final KindController.Request request) {
            try {
                c.reconcile(request);
            } catch (Throwable e) {
                log.error("Controller error. {}", e.getMessage(), e);
            }
        }
    }

}


