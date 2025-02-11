package aicreative.ai.dataplane.proxy.huoshan.tts.async.schedule;

import aicreative.ai.dataplane.proxy.huoshan.tts.async.model.HuoshanTtsAsyncTask;
import aicreative.ai.dataplane.proxy.huoshan.tts.async.model.HuoshanTtsAsyncTaskRepository;
import aicreative.ai.dataplane.proxy.schedule.ResultHandler;
import aicreative.ai.dataplane.task.TaskHandler;
import aicreative.ai.dataplane.task.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@Data
@AllArgsConstructor
public class HuoshanTtsAsyncScheduledTask {

    private final TaskHandler taskHandler;
    private final ResultHandler resultHandler;
    private final HuoshanTtsAsyncHandler huoshanTtsAsyncHandler;
    private final HuoshanTtsAsyncTaskRepository huoshanTtsAsyncTaskRepository;

    private final List<String> taskTypes = List.of("HuoshanTtsAsync");

    @Scheduled(fixedDelay = 5 * 1000)
    public void triggerTask() {
        taskTypes.forEach(type -> {
            final TaskHandler.TaskInfo taskInfo = taskHandler.fetchTask(TaskType.valueOf(type));
            if (Objects.isNull(taskInfo)) {
                return;
            }
            doTask(type, taskInfo.getTaskId(), (Map<String, Object>) taskInfo.getParam());
        });
    }

    @Scheduled(fixedDelay = 5 * 1000)
    public void checkTaskResult() {
        taskTypes.forEach(this::doCheckTaskResult);
    }

    private void doTask(String type, String taskId, Map<String, Object> params) {
        try {
            HuoshanTtsAsyncSubmitResponse submitResponse = huoshanTtsAsyncHandler.request(params);

            final HuoshanTtsAsyncTask t = new HuoshanTtsAsyncTask();
            t.setTaskId(taskId);
            t.setTaskType(type);
            t.setExecutionId(submitResponse.getTask_id());
            t.setUseEmotionApi((int) params.get("use_emotion_api") == 1);
            huoshanTtsAsyncTaskRepository.save(t);
        } catch (Exception e) {
            log.warn("HuoshanTtsAsyncScheduledTask doTask FAILED, type: {}, taskId: {}", type, taskId, e);
            resultHandler.replyFailed(taskId, TaskType.fromName(type), e.getMessage());
        }
    }

    private void doCheckTaskResult(String type) {
        List<HuoshanTtsAsyncTask> tasks = huoshanTtsAsyncTaskRepository.findAllByTaskType(type);
        for (HuoshanTtsAsyncTask t : tasks) {
            HuoshanTtsAsyncQueryResponse response = huoshanTtsAsyncHandler.queryResult(t);
            switch (response.getStatus()) {
                case Success:
                    Map<Object, Object> result = new HashMap<>();
                    // todo audio_url expires in one hour, save to cloud
                    result.put("output", response.getAudio_url());
                    resultHandler.replySuccess(t.getTaskId(), TaskType.fromName(t.getTaskType()), result);
                    huoshanTtsAsyncTaskRepository.delete(t);
                    break;
                case Fail:
                    resultHandler.replyFailed(t.getTaskId(), TaskType.fromName(t.getTaskType()), response.getMessage());
                    huoshanTtsAsyncTaskRepository.delete(t);
                    break;
                default:
                    break;
            }
        }
    }
}
