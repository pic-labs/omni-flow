package aicreative.ai.controlplane.kindcontroller.flow;

import aicreative.ai.controlplane.api.gateway.ApiGateway;
import aicreative.ai.controlplane.kindcontroller.base.KindController;
import aicreative.ai.controlplane.kindcontroller.base.KindDefinition;
import aicreative.ai.controlplane.kindcontroller.flow.definition.FlowKindDefinition;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@AllArgsConstructor
public class FlowKindController implements KindController {

    private ApiGateway apiGateway;

    private ApplicationContext applicationContext;

    @Override
    public void reconcile(final Request request) {
        var sw = new StopWatch(String.format("FlowReconcile_%s", request.getRecordId()));
        log.info(">>> {}, {}, {}, {}, Diff: {}", request.getRecordId(), request.getKindId(), request.getKindType(), request.getOperation(),
                StringUtils.defaultIfBlank(request.getExtra(), "").replaceAll(", Detail:[\\s\\S]*", ""));
        boolean kindUpdated = false;
        try {
            sw.start("FetchKind");
            final Optional<KindDefinition> d = apiGateway.getKindDefinition(request.getKindId());
            if (d.isEmpty()) {
                throw new IllegalArgumentException(String.format("FlowKindDefinition not exist. KindId: %s", request.getKindId()));
            }
            if (!FlowKindDefinition.kindExists(d.get().getKind())) {
                return;
            }
            sw.stop();

            final FlowKindDefinition def = (FlowKindDefinition) d.get();
            if (Objects.isNull(def.getStatus())) {
                def.initializeStatus();
                def.writeLog(new KindDefinition.DateLog("InitStatus"));
            }

            if (def.getStatus().getPhase() == KindDefinition.PhaseEnum.SUCCEED || def.getStatus().getPhase() == KindDefinition.PhaseEnum.FAILED) {
                log.debug("Flow kind is {}, Skip.", def.getStatus().getPhase());
                return;
            }

            if (def.getStatus().getPhase() == KindDefinition.PhaseEnum.PENDING) {
                log.debug("Flow update Kind phase to RUNNING.");
                def.getStatus().phaseUpdate(KindDefinition.PhaseEnum.RUNNING);
            }

            if (def.getStatus().getPhase() == KindDefinition.PhaseEnum.RUNNING) {
                final FlowExecutor flowExecutor = applicationContext.getBean(FlowExecutor.class);
                flowExecutor.init(def);
                flowExecutor.executeFlow();
                computeKindPhase(def);
            }

            sw.start("updateKind");
            kindUpdated = updateKindIfChanged(def);
            sw.stop();
        } finally {
            log.info("<<< {},{},{},{}. RT: {}ms", request.getRecordId(), request.getKindId(), request.getKindType(), kindUpdated ? "KindUpdated" : "None", sw.getTotalTimeMillis());
            log.trace(sw.prettyPrint(TimeUnit.MILLISECONDS));
        }
    }

    private void computeKindPhase(final FlowKindDefinition def) {
        var status = def.getStatus();
        if (status.running()) {
            return;
        }
        if (status.succeed()) {
            def.getStatus().phaseUpdate(KindDefinition.PhaseEnum.SUCCEED);
            return;
        }
        if (status.failed()) {
            def.getStatus().phaseUpdate(KindDefinition.PhaseEnum.FAILED);
        }
    }


    private boolean updateKindIfChanged(final FlowKindDefinition def) {
        return apiGateway.updateKindDefinition(def, "Update");
    }

    @Override
    public KindController.KindType kindType() {
        return new KindController.KindType("Flow", FlowKindDefinition.class);
    }
}