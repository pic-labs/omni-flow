package aicreative.ai.controlplane.coordinator;

import java.util.Map;

public interface Coordinator {

    String addTask(String functionName, String kindId, Object param, Map<String, String> ext);

}
