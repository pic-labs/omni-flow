package aicreative.ai.controlplane.coordinator.impl;

import aicreative.ai.controlplane.api.gateway.ApiGateway;
import aicreative.ai.controlplane.kindcontroller.base.KindDefinition;
import aicreative.ai.controlplane.coordinator.ResultReceiver;
import aicreative.ai.controlplane.coordinator.model.CpRequestDO;
import aicreative.ai.controlplane.coordinator.model.CpRequestRepository;
import aicreative.ai.dataplane.task.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
@AllArgsConstructor
public class DataplaneResultReceiver implements ResultReceiver {

    private final ApiGateway apiGateway;

    private final CpRequestRepository cpRequestRepository;

    @Override
    public void updateTask(String taskId, TaskStatus status, String message, Object result) {

        Assert.notNull(taskId, "TaskId not exist.");
        Optional<CpRequestDO> cpRequestDOOptional = cpRequestRepository.findById(taskId);
        if (cpRequestDOOptional.isEmpty()) {
            log.warn("TaskId:{} not found associated KindId", taskId);
            return;
        }

        var dpDo = cpRequestDOOptional.get();
        final String operationNote = String.format("Function_%s_%s_%s", status, dpDo.getFunctionName(), taskId);

        var kindTaskStatus = toKindTaskStatus(status);
        apiGateway.updateDefinitionWithLock(definition -> {
            if (Objects.isNull(definition.getStatus())) {
                throw new IllegalStateException(String.format("Status is null. Definition: %s", definition));
            }
            final KindDefinition.KindFunctionTask t = definition.getFunctionTaskOrCreateOne(dpDo.getFunctionName(), taskId);
            t.setStatus(kindTaskStatus);
            try {
                t.setResult((Map<String, Object>)result);
            } catch (Exception e) {
                // ignore
            }
            t.getTraces().add(new KindDefinition.DateLog(kindTaskStatus.name()));
        }, dpDo.getKindId(), operationNote);
    }

    private KindDefinition.KindFunctionTaskStatusEnum toKindTaskStatus(TaskStatus status) {
        switch(status) {
            case SUCCEED -> {
                return KindDefinition.KindFunctionTaskStatusEnum.SUCCEED;
            }
            case FAILED -> {
                return KindDefinition.KindFunctionTaskStatusEnum.FAILED;
            }
        }
        return KindDefinition.KindFunctionTaskStatusEnum.RUNNING;
    }
}
