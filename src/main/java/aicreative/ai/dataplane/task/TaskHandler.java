package aicreative.ai.dataplane.task;

import aicreative.ai.controlplane.coordinator.ResultReceiver;
import aicreative.ai.dataplane.task.config.TaskExecStrategyConfig;
import aicreative.ai.dataplane.task.enums.TaskFailType;
import aicreative.ai.dataplane.task.enums.TaskStatus;
import aicreative.ai.dataplane.task.enums.TaskType;
import aicreative.ai.dataplane.task.repository.TaskDO;
import aicreative.ai.dataplane.task.repository.TaskDORepository;
import aicreative.ai.dataplane.task.validator.TaskResultValidator;
import aicreative.ai.dataplane.task.validator.TaskResultValidatorManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TaskHandler {
    @Resource
    private TaskQueue taskQueue;
    @Resource
    private TaskDORepository taskDORepository;
    @Lazy
    @Resource
    private ResultReceiver cpReceiver;
    @Resource
    private TaskExecStrategyConfig execStrategyConfig;
    @Resource
    private TaskResultValidatorManager taskResultValidatorManager;
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;


    public String addTask(TaskInfo taskInfo) {
        TaskDO taskDO = TaskDO.newTask(taskInfo.getTaskId(), taskInfo.getTaskType(), taskInfo.getExt(), taskInfo.getParam(), this.getTaskPriority(false));
        taskDORepository.save(taskDO);
        taskQueue.addPendingTask(taskDO.getTaskId(), taskDO.getTaskType(), taskDO.getPriority());
        log.info("Add task success, taskId:{},taskType:{}", taskDO.getTaskId(), taskDO.getTaskType());
        return taskDO.getTaskId();
    }

    private void addRetryTask(TaskDO task, TaskResult taskResult) {
        task.exec2Pending(taskResult.getFailType(), taskResult.getResult(), this.getTaskPriority(true));
        taskDORepository.save(task);
        taskQueue.addPendingTask(task.getTaskId(), task.getTaskType(), task.getPriority());
        log.info("Task fail, add retry task, taskResult:{}", taskResult);
    }

    public TaskInfo fetchTask(TaskType taskType) {
        String taskId = taskQueue.getPendingTask(taskType);
        if (StringUtils.isBlank(taskId)) {
            return null;
        }
        Optional<TaskDO> taskDOOpt = taskDORepository.findById(taskId);
        if (taskDOOpt.isEmpty()) {
            return null;
        }
        TaskDO task = taskDOOpt.get();
        task.execute();
        taskDORepository.save(task);
        taskQueue.addExecTask(taskId, taskType);
        taskQueue.removePendingTask(taskId, taskType);
        cpReceiver.updateTask(taskId, TaskStatus.EXECUTING, null, null);
        log.info("Fetch task, taskId:{},taskType:{}", task.getTaskId(), task.getTaskType());
        return TaskInfo.convertFromTaskDO(task);
    }

    public void processTaskResult(TaskResult taskResult) {
        if (!taskQueue.isInExecQueue(taskResult.getTaskType(), taskResult.getTaskId())) {
            log.info("Task not found in exec queue, taskResult:{}", taskResult);
            return;
        }
        Optional<TaskDO> taskOpt = taskDORepository.findById(taskResult.getTaskId());
        if (taskOpt.isEmpty()) {
            log.info("Task not found in db, taskResult:{}", taskResult);
            return;
        }
        TaskDO task = taskOpt.get();
        if (!taskResult.isSuccess()) {
            processTaskFail(task, taskResult);
        } else {
            processTaskSuccess(task, taskResult);
        }
        taskQueue.removeExecTask(task.getTaskId(), task.getTaskType());
    }

    public Set<String> getAllExecTaskByType(TaskType taskType) {
        return taskQueue.getAllExecTask(taskType);
    }

    private void processTaskSuccess(TaskDO task, TaskResult taskResult) {
        TaskResultValidator taskResultValidator = taskResultValidatorManager.getValidator(task.getTaskType());
        if (Objects.isNull(taskResultValidator) ||
                taskResultValidator.isTaskResultValid(task, taskResult.getResult())) {
            task.success(taskResult.getResult());
            taskDORepository.save(task);
            cpReceiver.updateTask(task.getTaskId(), TaskStatus.SUCCEED, null, taskResult.getResult());
            log.info("Task success, taskResult:{}", taskResult);
            return;
        }
        processTaskFail(task, TaskResult.fail(task.getTaskId(), task.getTaskType(), TaskFailType.RESULT_VALIDATE_FAIL));
    }

    private void processTaskFail(TaskDO task, TaskResult taskResult) {
        if (task.canRetry(execStrategyConfig.getStrategyConfig(task.getTaskType()).getRetryCnt())) {
            addRetryTask(task, taskResult);
            return;
        }
        task.fail(taskResult.getFailType(), taskResult.getResult());
        taskDORepository.save(task);
        cpReceiver.updateTask(task.getTaskId(), TaskStatus.FAILED,
                Optional.of(taskResult.getFailType()).map(Enum::name).orElse(null), taskResult.getResult());
        log.info("Task fail, execCnt:{}, taskResult:{}", task.getExecCnt(), taskResult);
    }

    private boolean taskCanRetry(TaskDO taskDO) {
        TaskExecStrategyConfig.StrategyConfig config = execStrategyConfig.getStrategyConfig(taskDO.getTaskType());
        return taskDO.getExecCnt() < config.getRetryCnt() + 1;
    }

    private long getTaskPriority(boolean isRetryTask) {
        String retryPriority = isRetryTask ? "1" : "2";
        String profilePriority = "2";
        if (StringUtils.contains(activeProfile, "product")) {
            profilePriority = "1";
        }
        return Long.parseLong(profilePriority + retryPriority + System.currentTimeMillis());
    }

    @Data
    public static class TaskInfo {
        private String taskId;
        private TaskType taskType;
        private Object param;
        private Map<String, String> ext;

        public static TaskInfo convertFromTaskDO(TaskDO taskDO) {
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setTaskId(taskDO.getTaskId());
            taskInfo.setTaskType(taskDO.getTaskType());
            Object paramObj;
            try {
                paramObj = new ObjectMapper().readValue(taskDO.getParam(), Object.class);
            } catch (JsonProcessingException e) {
                log.warn("ConvertFromTaskDO parse param failed, param:{}", taskDO.getParam());
                throw new RuntimeException("convertFromTaskDO parse param failed");
            }
            taskInfo.setParam(paramObj);
            taskInfo.setExt(taskDO.getExt());
            return taskInfo;
        }
    }

    @Data
    @ToString
    public static class TaskResult {
        private String taskId;
        private TaskType taskType;
        private boolean success;
        @ToString.Exclude
        private Object result;
        private TaskFailType failType;

        public static TaskResult fail(String taskId, TaskType taskType, TaskFailType taskFailType) {
            TaskResult taskResult = new TaskResult();
            taskResult.setTaskId(taskId);
            taskResult.setTaskType(taskType);
            taskResult.setSuccess(false);
            taskResult.setFailType(taskFailType);
            return taskResult;
        }

        public static TaskResult success(String taskId, TaskType taskType, String result) {
            TaskResult taskResult = new TaskResult();
            taskResult.setTaskId(taskId);
            taskResult.setTaskType(taskType);
            taskResult.setSuccess(true);
            taskResult.setResult(result);
            return taskResult;
        }
    }
}
