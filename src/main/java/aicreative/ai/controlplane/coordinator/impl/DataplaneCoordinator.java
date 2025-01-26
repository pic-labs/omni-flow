package aicreative.ai.controlplane.coordinator.impl;

import aicreative.ai.controlplane.api.gateway.ApiGateway;
import aicreative.ai.controlplane.coordinator.Coordinator;
import aicreative.ai.controlplane.coordinator.model.CpRequestDO;
import aicreative.ai.controlplane.coordinator.model.CpRequestRepository;
import aicreative.ai.dataplane.task.TaskHandler;
import aicreative.ai.dataplane.task.enums.TaskType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Data
@Slf4j
public final class DataplaneCoordinator implements Coordinator {

    private final ApiGateway apiGateway;

    private final TaskHandler taskHandler;

    private final CpRequestRepository cpRequestRepository;

    private final Lock localLock = new ReentrantLock(true);

    public DataplaneCoordinator(ApiGateway apiGateway, TaskHandler taskHandler, CpRequestRepository cpRequestRepository) {
        this.apiGateway = apiGateway;
        this.taskHandler = taskHandler;
        this.cpRequestRepository = cpRequestRepository;
    }

    @Override
    public String addTask(String functionName, String kindId, Object param, Map<String, String> ext) {
        final String taskId = UUID.randomUUID().toString();
        // todo add exception handle
        var future = CompletableFuture.runAsync(() -> execute(taskId, functionName, kindId, param, ext));
        return taskId;
    }

    private void execute(String taskId, String functionName, String kindId, Object param, Map<String, String> ext) {
        final TaskType taskType = TaskType.fromName(functionName);
        Assert.notNull(taskType, String.format("TaskId:%s task type not exists", taskId));

        cpRequestRepository.save(CpRequestDO.builder().taskId(taskId).functionName(functionName).kindId(kindId).param(param).build());

        localLock.lock();
        try {
            var taskInfo = new TaskHandler.TaskInfo();
            taskInfo.setTaskId(taskId);
            taskInfo.setTaskType(taskType);
            taskInfo.setParam(param);
            taskInfo.setExt(ext);
            taskHandler.addTask(taskInfo);
        } finally {
            localLock.unlock();
        }
    }
}
