package aicreative.ai.controlplane.kindcontroller.flow;

import aicreative.ai.controlplane.coordinator.Coordinator;
import aicreative.ai.controlplane.kindcontroller.base.KindDefinition;
import aicreative.ai.controlplane.kindcontroller.flow.definition.FlowConfig;
import aicreative.ai.controlplane.kindcontroller.flow.definition.FlowKindDefinition;
import aicreative.ai.controlplane.kindcontroller.flow.definition.FlowKindDefinition.ConditionStatus;
import aicreative.ai.controlplane.kindcontroller.flow.definition.spel.SpelFunctionRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

@Component
@Slf4j
@Scope("prototype")
public class FlowExecutor {

    private final Coordinator coordinator;
    private final SpelFunctionRegistrar spelFunctionRegistrar;

    private FlowKindDefinition def;
    private FlowKindDefinition.Spec spec;
    private FlowKindDefinition.Status status;
    private Map<String, Object> context;
    private Map<String, FlowKindDefinition.Space> spaces;
    private static final String FUNCTION_START = "Start";
    private static final String FUNCTION_END = "End";

    public FlowExecutor(Coordinator coordinatorFacade, SpelFunctionRegistrar spelFunctionRegistrar) {
        this.coordinator = coordinatorFacade;
        this.spelFunctionRegistrar = spelFunctionRegistrar;
    }

    public void init(FlowKindDefinition def) {
        this.def = def;
        if (Objects.isNull(def.getStatus())) {
            throw new IllegalStateException("Flow Kind Status none exist.");
        }
        this.spec = def.getSpec();
        this.status = def.getStatus();
        this.context = def.getStatus().getContext();
        this.spaces = def.getStatus().getSpaces();
    }

    public void executeFlow() {
        FlowConfig flowConfig = FlowKindDefinition.getFlowConfig(this.def.getKind());

        for (FlowConfig.FlowStep step : flowConfig.getFlows()) {
            try {
                executeStep(step);
            } catch (Exception e) {
                status.updateFlowCondition(step.getFlowId(), ConditionStatus.FAILED);
                status.phaseUpdate(KindDefinition.PhaseEnum.FAILED);
                log.warn("FlowExecutor executeStep error, flowId:{}", step.getFlowId(), e);
            }
        }
    }

    private void executeStep(FlowConfig.FlowStep step) {
        var conditionOptional = def.findFlowCondition(step.getFlowId());
        Assert.state(conditionOptional.isPresent(), String.format("Flow:%s condition absent.", step.getFlowId()));

        var condition = conditionOptional.get();
        if (isExecuted(condition)) {
            log.debug("FlowStep Executed:KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
            return;
        }
        if (dependsNotSucceed(step)) {
            log.debug("FlowStep PreConditionNotSucceed,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
            return;
        }
        if (condition == ConditionStatus.PENDING) {
            processPending(step);
            return;
        }
        processTaskResult(step);
    }

    private void processTaskResult(FlowConfig.FlowStep step) {
        List<String> taskIds = spaces.get(step.getFlowId()).getTaskIds();
        if (CollectionUtils.isEmpty(taskIds)) {
            log.debug("FlowStep Process TaskResult, Task Not Exist,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
            return;
        }
        List<Object> result = new ArrayList<>();
        for (String taskId : taskIds) {
            Optional<KindDefinition.KindFunctionTask> taskOptional = def.getFunctionTask(step.getFunction(), taskId);
            if (taskOptional.isEmpty()) {
                log.debug("FlowStep Process TaskResult, Can not find task,KindId:{}, FlowId:{},taskId:{}", def.getMetadata().getId(), step.getFlowId(), taskId);
                return;
            }
            final KindDefinition.KindFunctionTask functionTask = taskOptional.get();
            if (Objects.equals(functionTask.getStatus(), KindDefinition.KindFunctionTaskStatusEnum.FAILED)) {
                status.updateFlowCondition(step.getFlowId(), ConditionStatus.FAILED);
                log.debug("FlowStep Process TaskResult, Task Failed,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
                return;
            }
            if (Objects.equals(functionTask.getStatus(), KindDefinition.KindFunctionTaskStatusEnum.RUNNING)) {
                log.debug("FlowStep Process TaskResult, Task Running,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
                return;
            }
            result.add(functionTask.getResult());
            //bizId与task结果关联，方便后续FlowStep使用
            if (Objects.nonNull(step.getBatchField())) {
                functionTask.getResult().put(spaces.get(step.getFlowId()).getBatchBizId().getName(),
                        spaces.get(step.getFlowId()).getBatchBizId().getBizIds().get(taskIds.indexOf(taskId)));
            }
        }
        status.updateFlowCondition(step.getFlowId(), ConditionStatus.SUCCEED);
        context.put(step.getFlowId(), isBatchFlowStep(step) ? result : result.getFirst());
        log.debug("FlowStep Process TaskResult, Task Succeed,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
    }

    @SuppressWarnings("unchecked")
    private void processPending(FlowConfig.FlowStep step) {
        if (StringUtils.equals(step.getFunction(), FUNCTION_START)) {
            context.put(step.getFlowId(), getStartInputParams(step.getInputs(), spec.getParam()));
            status.updateFlowCondition(step.getFlowId(), ConditionStatus.SUCCEED);
            log.debug("FlowStep StartFunction Process Pending End,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
            return;
        }
        if (StringUtils.equals(step.getFunction(), FUNCTION_END)) {
            context.put(step.getFlowId(), getInputParams(step.getInputs()));
            status.updateFlowCondition(step.getFlowId(), ConditionStatus.SUCCEED);
            log.debug("FlowStep EndFunction Process Pending End,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
            return;
        }
        if (isBatchFlowStep(step)) {
            processBatchFlowPending(step);
            log.debug("FlowStep Process Pending End,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
            return;
        }
        Object param = getInputParams(step.getInputs());
        final String taskId = coordinator.addTask(step.getFunction(), def.getMetadata().getId(), param, step.getExt());
        spaces.get(step.getFlowId()).getTaskIds().add(taskId);
        status.updateFlowCondition(step.getFlowId(), ConditionStatus.RUNNING);
        log.debug("FlowStep Process Pending End,KindId:{}, FlowId:{}", def.getMetadata().getId(), step.getFlowId());
    }

    @SuppressWarnings("unchecked")
    private void processBatchFlowPending(FlowConfig.FlowStep step) {
        Map<String, Object> paramMap = (Map<String, Object>) getInputParams(step.getInputs());
        List<Object> batchParams = (List<Object>) paramMap.get(step.getBatchField());
        if (CollectionUtils.isEmpty(batchParams)) {
            status.updateFlowCondition(step.getFlowId(), ConditionStatus.SUCCEED);
            return;
        }
        batchParams.forEach(batchParam -> {
            Map<String, Object> singleParamMap = new HashMap<>();
            if (paramMap.size() > 1) {
                singleParamMap.putAll(paramMap);
                singleParamMap.put(step.getBatchField(), batchParam);
            } else {
                singleParamMap.putAll((Map<String, Object>) batchParam);
            }
            final String taskId = coordinator.addTask(step.getFunction(), def.getMetadata().getId(), singleParamMap, step.getExt());
            spaces.get(step.getFlowId()).getTaskIds().add(taskId);
        });
        if (Objects.nonNull(step.getBatchBizId())) {
            spaces.get(step.getFlowId()).getBatchBizId().setName(step.getBatchBizId().getName());
            spaces.get(step.getFlowId()).getBatchBizId().setBizIds((List<Object>) getValue(step.getBatchBizId().getValue()));
        }
        status.updateFlowCondition(step.getFlowId(), ConditionStatus.RUNNING);
    }

    private boolean isBatchFlowStep(FlowConfig.FlowStep step) {
        return StringUtils.isNotBlank(step.getBatchField());
    }

    private Object getStartInputParams(List<FlowConfig.InputConfig> inputConfigs, Map<String, Object> specParam) {
        if (CollectionUtils.isEmpty(inputConfigs)) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        inputConfigs.forEach(inputConfig -> params.put(inputConfig.getName(), specParam.get(inputConfig.getName())));
        return params;
    }

    private boolean dependsNotSucceed(FlowConfig.FlowStep step) {
        List<String> dependFlowIds = step.getDepends();
        if (CollectionUtils.isEmpty(dependFlowIds)) {
            return false;
        }
        return !dependFlowIds.stream().allMatch(flowId -> def.findFlowCondition(flowId).orElse(ConditionStatus.FAILED) == ConditionStatus.SUCCEED);
    }

    private boolean isExecuted(ConditionStatus condition) {
        return condition == ConditionStatus.FAILED || condition == ConditionStatus.SUCCEED;
    }

    private Object getInputParams(List<FlowConfig.InputConfig> inputConfigs) {
        if (CollectionUtils.isEmpty(inputConfigs)) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        inputConfigs.forEach(inputConfig -> params.put(inputConfig.getName(), getValue(inputConfig.getValue())));
        return params;
    }

    private Object getValue(FlowConfig.Value value) {
        return switch (value.getType()) {
            case LITERAL -> getLiteralValue(value);
            case SPEL -> getSpelValue(value);
        };
    }

    private Object getSpelValue(FlowConfig.Value value) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext sec = new StandardEvaluationContext();
        spelFunctionRegistrar.registerFunctions(sec);
        sec.setVariables(context);
        return parser.parseExpression(value.getContent()).getValue(sec);
    }

    private Object getLiteralValue(FlowConfig.Value value) {
        return value.getContent();
    }
}