package aicreative.ai.controlplane.kindcontroller.flow.definition;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FlowConfig {

    private List<FlowStep> flows;

    @Data
    public static class FlowStep {
        private String flowId;
        private String function;
        private String batchField;
        private BatchBizId batchBizId;
        private List<InputConfig> inputs;
        private List<String> depends;
        private Map<String, String> ext;
    }

    @Data
    public static class BatchBizId {
        private String name;
        private Value value;
    }

    @Data
    public static class InputConfig {
        private String name;
        private Value value;
        private String desc;
        private List<String> enums;
        private Boolean required;
    }

    public enum ValueType {
        LITERAL, SPEL
    }

    @Data
    public static class Value {
        private ValueType type; // literal, ref
        private String content;
    }
}
