package aicreative.ai.controlplane.kindcontroller.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Data
@Slf4j
public class KindDefinition implements Serializable {

    private String apiVersion;
    private String kind;
    protected Metadata metadata;
    protected Spec spec;
    protected Status status;

    public KindFunctionTask getFunctionTaskOrCreateOne(final String functionName, final String taskId) {
        if (Objects.isNull(this.getStatus().getFunction())) {
            this.getStatus().setFunction(new HashMap<>());
        }
        if (Objects.isNull(this.getStatus().getFunction().get(functionName))) {
            final var nkf = new KindDefinition.KindFunction();
            nkf.setTasks(new ArrayList<>());
            this.getStatus().getFunction().put(functionName, nkf);
        }
        if (CollectionUtils.isEmpty(this.getStatus().getFunction().get(functionName).getTasks())) {
            final var nkf = new ArrayList<KindDefinition.KindFunctionTask>();
            this.getStatus().getFunction().get(functionName).setTasks(nkf);
        }
        var tasks = this.getStatus().getFunction().get(functionName).getTasks();
        final Optional<KindDefinition.KindFunctionTask> taskOptional = tasks.stream()
                .filter(t -> Objects.equals(taskId, t.getTaskId())).findFirst();
        if (taskOptional.isPresent()) {
            return taskOptional.get();
        }
        final var t = new KindDefinition.KindFunctionTask();
        t.setTaskId(taskId);
        t.setTraces(new ArrayList<>());
        tasks.add(t);
        return t;
    }

    public List<KindFunctionTask> getAllFunctionTasks(final String functionName) {
        if (Objects.isNull(this.getStatus())) {
            return List.of();
        }
        if (CollectionUtils.isEmpty(this.getStatus().getFunction())) {
            return List.of();
        }
        final KindFunction f = this.getStatus().getFunction().get(functionName);
        if (Objects.isNull(f)) {
            return List.of();
        }
        return CollectionUtils.isEmpty(f.getTasks())? List.of(): f.getTasks();
    }

    public Optional<KindFunctionTask> getFunctionTask(final String functionName, final String taskId) {
        if (Objects.isNull(this.getStatus())) {
            return Optional.empty();
        }
        if (CollectionUtils.isEmpty(this.getStatus().getFunction())) {
            return Optional.empty();
        }
        final KindFunction f = this.getStatus().getFunction().get(functionName);
        if (Objects.isNull(f)) {
            return Optional.empty();
        }
        if (CollectionUtils.isEmpty(f.getTasks())) {
            return Optional.empty();
        }
        return f.getTasks().stream().filter(t-> Objects.equals(t.getTaskId(), taskId)).findFirst();
    }

    public List<Diff> diff(KindDefinition old) {
        return new Differ().diff(old, this);
    }

    public void writeLog(DateLog dateLog) {
        Assert.notNull(this.getStatus(), "Kind Status is null");
        if (CollectionUtils.isEmpty(this.getStatus().getLogs())) {
            this.getStatus().setLogs(new ArrayList<>());
        }
        this.getStatus().getLogs().add(dateLog);
    }

    @Data
    public static class Metadata implements Serializable {
        private String id;
        private String name;
        private String createTime;
        private Map<String, String> labels;
    }

    @Data
    public static class Spec implements Serializable {
    }

    @Data
    public static class Status implements Serializable {

        private PhaseEnum phase;
        private Map<String, KindFunction> function;
        private List<DateLog> logs;

        public void initialize() {
            if (Objects.isNull(this.getPhase())) {
                this.setPhase(PhaseEnum.PENDING);
            }
            if (CollectionUtils.isEmpty(this.getFunction())) {
                this.setFunction(new HashMap<>());
            }
            if (Objects.isNull(this.getLogs())) {
                this.setLogs(new ArrayList<>());
            }
        }

        public void phaseUpdate(PhaseEnum phaseEnum) {
            this.setPhase(phaseEnum);
            this.getLogs().add(new DateLog(String.format("Phase:%s", phaseEnum)));
        }

    }

    public enum PhaseEnum {
        PENDING,
        RUNNING,
        SUCCEED,
        FAILED
    }

    @Data
    public static class KindFunction implements Serializable {
        private List<KindFunctionTask> tasks;
    }

    @Data
    public static class KindFunctionTask implements Serializable {
        private String taskId;
        private KindFunctionTaskStatusEnum status;
        private Map<String, Object> result;
        private List<DateLog> traces;
    }

    @Data
    @AllArgsConstructor
    public static class DateLog {

        private String date;

        private String log;

        public DateLog(final String log) {
            this.date = LocalDateTime.now().format(ISO_LOCAL_DATE_TIME);
            this.log = log;
        }

    }

    public enum KindFunctionTaskStatusEnum {
        RUNNING,
        SUCCEED,
        FAILED;
    }

    @Data
    @AllArgsConstructor
    public static class Diff {
        private String path;
        private Object nv;
        private Object ov;
    }

    protected static class Differ {
        protected List<Diff> diffs;

        public List<Diff> diff(KindDefinition o, KindDefinition n) {
            if (Objects.isNull(o) || Objects.isNull(n)) {
                return List.of();
            }

            diffs = new ArrayList<>();
            compareBasicFields(o, n);
            compareMetadata(o, n);
            compareSpec(o, n);
            compareStatus(o, n);
            return diffs;
        }

        private void compareBasicFields(KindDefinition o, KindDefinition n) {
            compareField("apiVersion", o.getApiVersion(), n.getApiVersion());
            compareField("kind", o.getKind(), n.getKind());
        }

        private void compareMetadata(KindDefinition o, KindDefinition n) {
            if (Objects.isNull(o.getMetadata()) || Objects.isNull(n.getMetadata())) {
                compareField("metadata", o.getMetadata(), n.getMetadata());
                return;
            }
            compareField("metadata.id", o.getMetadata().getId(), n.getMetadata().getId());
            compareField("metadata.name", o.getMetadata().getName(), n.getMetadata().getName());
        }

        private void compareSpec(KindDefinition o, KindDefinition n) {
            if (Objects.isNull(o.getSpec()) || Objects.isNull(n.getSpec())) {
                compareField("spec", o.getSpec(), n.getSpec());
            }
        }

        protected void compareStatus(KindDefinition o, KindDefinition n) {
            if (Objects.isNull(o.getStatus()) || Objects.isNull(n.getStatus())) {
                compareField("status", o.getStatus(), n.getStatus());
                return;
            }

            compareField("status.phase", o.getStatus().getPhase(), n.getStatus().getPhase());
            compareFunctions(o.getStatus().getFunction(),n.getStatus().getFunction());
        }

        protected void compareField(String path, Object oldValue, Object newValue) {
            if (!Objects.equals(oldValue, newValue)) {
                diffs.add(new Diff(path, newValue, oldValue));
            }
        }

        protected void compareFunctions(Map<String, KindFunction> o, Map<String, KindFunction> n) {
            if (Objects.isNull(o) || Objects.isNull(n)) {
                compareField("function", o, n);
                return;
            }
            var keys = SetUtils.union(o.keySet(), n.keySet());
            keys.forEach(k -> {
                compareField( String.format("function.%s", k), o.get(k), n.get(k));
            });
        }

    }
}
