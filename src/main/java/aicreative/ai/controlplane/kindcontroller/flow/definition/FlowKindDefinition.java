package aicreative.ai.controlplane.kindcontroller.flow.definition;

import aicreative.ai.controlplane.kindcontroller.base.KindDefinition;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.*;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@Slf4j
public class FlowKindDefinition extends KindDefinition {

    protected Spec spec;
    protected Status status;

    public void initializeStatus() {
        if (!Objects.isNull(this.getStatus())) {
            return;
        }
        this.status = new Status();
        this.status.initialize(this.getKind());
    }

    public List<Diff> diff(KindDefinition old) {
        return new Differ().diff(old, this);
    }

    public Optional<ConditionStatus> findFlowCondition(String flowId) {
        if (Objects.isNull(flowId)) {
            return Optional.empty();
        }
        if (Objects.isNull(this.getStatus())
                || Objects.isNull(this.getStatus().getCondition())) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.getStatus().getCondition().get(flowId));
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Spec extends KindDefinition.Spec {
        private Map<String, Object> param;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @JsonPropertyOrder({"phase", "condition", "context"})
    public static class Status extends KindDefinition.Status {
        private Map<String, ConditionStatus> condition;
        private Map<String, Object> context;
        private Map<String, Space> spaces;

        public void initialize(String kind) {
            super.initialize();
            FlowConfig flowConfig = getFlowConfig(kind);
            if (Objects.isNull(condition)) {
                this.condition = new LinkedHashMap<>();
                flowConfig.getFlows().forEach(flow -> this.condition.put(flow.getFlowId(), ConditionStatus.PENDING));
            }
            if (Objects.isNull(context)) {
                this.context = new LinkedHashMap<>();
                flowConfig.getFlows().forEach(flow -> this.context.put(flow.getFlowId(), new HashMap<>()));
            }
            if (Objects.isNull(spaces)) {
                this.spaces = new LinkedHashMap<>();
                flowConfig.getFlows().forEach(flow -> this.spaces.put(flow.getFlowId(), new Space()));
            }
        }

        public boolean succeed() {
            return MapUtils.isNotEmpty(this.condition) && this.condition.values().stream().allMatch(status -> status == ConditionStatus.SUCCEED);
        }

        public boolean failed() {
            return MapUtils.isNotEmpty(this.condition) && this.condition.values().stream().anyMatch(status -> status == ConditionStatus.FAILED);
        }

        public boolean running() {
            return MapUtils.isNotEmpty(this.condition) && this.condition.values().stream().anyMatch(status -> status == ConditionStatus.RUNNING);
        }

        public void updateFlowCondition(String flowId, ConditionStatus conditionStatus) {
            this.getCondition().put(flowId, conditionStatus);
            if (CollectionUtils.isEmpty(this.getLogs())) {
                this.setLogs(new ArrayList<>());
            }
            this.getLogs().add(new DateLog(String.format("%s:%s", flowId, conditionStatus)));
        }
    }

    @Data
    public static class Space {
        private List<String> taskIds = new ArrayList<>();
        private BatchBizId batchBizId = new BatchBizId();
    }

    @Data
    public static class BatchBizId {
        private String name;
        private List<Object> bizIds;
    }

    public enum ConditionStatus {
        PENDING,
        RUNNING,
        SUCCEED,
        FAILED
    }

    protected static class Differ extends KindDefinition.Differ {
        @Override
        protected void compareStatus(KindDefinition o, KindDefinition n) {

            if (Objects.isNull(o.getStatus()) || Objects.isNull(n.getStatus())) {
                compareField("status", o.getStatus(), n.getStatus());
                return;
            }
            compareField("status.phase", o.getStatus().getPhase(), n.getStatus().getPhase());

            FlowKindDefinition o1 = (FlowKindDefinition) o;
            FlowKindDefinition n1 = (FlowKindDefinition) n;
            super.compareFunctions(o.getStatus().getFunction(), n.getStatus().getFunction());
            compareConditions(o1.getStatus().getCondition(), n1.getStatus().getCondition());
            compareContext(o1.getStatus().getContext(), n1.getStatus().getContext());
            compareSpaces(o1.getStatus().getSpaces(), n1.getStatus().getSpaces());
        }

        private void compareSpaces(Map<String, Space> o, Map<String, Space> n) {
            if (Objects.isNull(o) || Objects.isNull(n)) {
                compareField("status.spaces", o, n);
                return;
            }
            var keys = SetUtils.union(o.keySet(), n.keySet());
            keys.forEach(k -> compareField(String.format("spaces.%s", k), o.get(k), n.get(k)));
        }

        private void compareContext(Map<String, Object> o, Map<String, Object> n) {
            if (Objects.isNull(o) || Objects.isNull(n)) {
                compareField("status.context", o, n);
                return;
            }
            var keys = SetUtils.union(o.keySet(), n.keySet());
            keys.forEach(k -> compareField(String.format("context.%s", k), o.get(k), n.get(k)));
        }

        private void compareConditions(Map<String, ConditionStatus> o, Map<String, ConditionStatus> n) {
            if (Objects.isNull(o) || Objects.isNull(n)) {
                compareField("status.condition", o, n);
                return;
            }
            var keys = SetUtils.union(o.keySet(), n.keySet());
            keys.forEach(k -> compareField(String.format("condition.%s", k), o.get(k), n.get(k)));
        }
    }

    public static FlowConfig getFlowConfig(String kind) {
        Resource resource = new ClassPathResource(getFlowConfigPath(kind));
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(resource.getInputStream(), FlowConfig.class);
        } catch (IOException e) {
            log.warn("Failed to load flow configuration, kind:{}", kind, e);
            throw new RuntimeException(e);
        }
    }

    public static boolean kindExists(String kind) {
        return new ClassPathResource(getFlowConfigPath(kind)).exists();
    }

    private static String getFlowConfigPath(String kind) {
        return "flow/" + kind + ".yaml";
    }
}
