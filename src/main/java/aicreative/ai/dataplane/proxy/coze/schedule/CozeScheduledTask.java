package aicreative.ai.dataplane.proxy.coze.schedule;

import aicreative.ai.dataplane.proxy.coze.model.CozeTask;
import aicreative.ai.dataplane.proxy.coze.model.CozeTaskRepository;
import aicreative.ai.dataplane.proxy.schedule.ResultHandler;
import aicreative.ai.dataplane.task.TaskHandler;
import aicreative.ai.dataplane.task.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@Data
@AllArgsConstructor
public class CozeScheduledTask {

    private final TaskHandler taskHandler;
    private final ResultHandler resultHandler;
    private final CozeHandler cozeHandler;
    private final CozeTaskRepository cozeTaskRepository;

    private final List<String> taskTypes = List.of("CozeWorkflow");

    @Scheduled(fixedDelay = 5000)
    public void triggerTask() {
        taskTypes.forEach(type -> {
            final TaskHandler.TaskInfo taskInfo = taskHandler.fetchTask(TaskType.valueOf(type));
            if (Objects.isNull(taskInfo)) {
                return;
            }
            String workflowId = MapUtils.getString(taskInfo.getExt(), "workflowId");
            doTask(type, workflowId, taskInfo.getTaskId(), taskInfo.getParam());
        });
    }

    @Scheduled(fixedDelay = 5000)
    public void checkTaskResult() {
        taskTypes.forEach(this::doCheckTaskResult);
    }

    private void doTask(String type, String workflowId, String taskId, Object param) {
        try {
            log.info("Request Coze, taskId:{}, workflowId:{}", taskId, workflowId);
            String executionId = cozeHandler.request(workflowId, param);

            final CozeTask ct = new CozeTask();
            ct.setTaskId(taskId);
            ct.setTaskType(type);
            ct.setWorkflowId(workflowId);
            ct.setExecutionId(executionId);
            cozeTaskRepository.save(ct);
        } catch (Exception e) {
            log.warn("Request Coze failed, taskId:{}, type:{}, workflowId:{}, param:{}", taskId, type, workflowId, param, e);
            resultHandler.replyFailed(taskId, TaskType.fromName(type), e.getMessage());
        }
    }

    private void doCheckTaskResult(String type) {
        List<CozeTask> tasks = cozeTaskRepository.findAllByTaskType(type);
        Iterator<CozeTask> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            CozeTask t = iterator.next();
            CozeResponse resp = cozeHandler.queryResult(t.getWorkflowId(), t.getExecutionId());
            switch (resp.getStatus()) {
                case Success:
                    resultHandler.replySuccess(t.getTaskId(), TaskType.fromName(t.getTaskType()), resp.getOutput());
                    cozeTaskRepository.delete(t);
                    break;
                case Failed:
                    resultHandler.replyFailed(t.getTaskId(), TaskType.fromName(t.getTaskType()), resp.getMessage());
                    cozeTaskRepository.delete(t);
                    break;
            }
        }
    }
}
