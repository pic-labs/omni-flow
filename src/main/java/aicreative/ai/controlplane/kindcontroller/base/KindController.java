package aicreative.ai.controlplane.kindcontroller.base;


import lombok.AllArgsConstructor;
import lombok.Data;

public interface KindController {

    void reconcile(Request request);

    KindType kindType();

    @Data
    @AllArgsConstructor
    class Request {
        private String queueName;
        private String recordId;
        private String kindType;
        private String kindId;
        private String operation;
        private String extra;
    }

    @Data
    @AllArgsConstructor
    class KindType {
        private String name;
        private Class<? extends KindDefinition> typeClass;
    }
}
