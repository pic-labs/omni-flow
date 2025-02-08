package aicreative.ai.controlplane.api.controller;

import aicreative.ai.controlplane.api.common.Pagination;
import aicreative.ai.controlplane.api.common.Response;
import aicreative.ai.controlplane.api.common.Response.ResponseCode;
import aicreative.ai.controlplane.api.gateway.ApiGateway;
import aicreative.ai.controlplane.api.kinduser.KindUserDO;
import aicreative.ai.controlplane.api.kinduser.KindUserRepository;
import aicreative.ai.controlplane.kindcontroller.base.KindDefinition;
import aicreative.ai.controlplane.kindcontroller.flow.definition.FlowKindDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/apis/create-controller/creator/v1")
public class CreatorController {

    private final KindUserRepository kindUserRepository;
    private final ApiGateway apiGateway;

    @PostMapping("/create-flow")
    public Response<Map<String, String>> createFlow(@RequestBody CreateFlowRequest request) {
        Assert.notNull(request, "Request not exist.");
        Assert.notNull(request.getKindType(), "Request.kindType not exist.");
        Assert.notNull(request.getUid(), "Request.uid not exist.");

        final FlowKindDefinition kind = new FlowKindDefinition();
        kind.setKind(request.getKindType());
        kind.setApiVersion("v1alpha1");

        final KindDefinition.Metadata metadata = new KindDefinition.Metadata();
        metadata.setName(String.format(request.getKindType() + "-%s", UUID.randomUUID()));
        metadata.setLabels(Map.of("uid", request.getUid()));
        kind.setMetadata(metadata);

        final FlowKindDefinition.Spec spec = new FlowKindDefinition.Spec();
        spec.setParam(request.getParam());
        kind.setSpec(spec);

        final String id = apiGateway.createKindDefinition(kind);
        return new Response<>(ResponseCode.Success, Map.of("id", id));
    }

    @GetMapping("/list")
    public Response<Pagination<KindSummary>> findAll(@RequestParam String uid,
                                                     @RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size) {

        Assert.notNull(uid, "Uid not exist.");

        if (page < 1) {
            page = 1;
        }
        // todo The number of records should be obtained using the JPA count method, which has not yet been successfully executed.
        int total = kindUserRepository.findAllByUid(uid, null).size();
        if (total == 0) {
            return new Response<>(ResponseCode.Success, Pagination.empty(page, size));
        }
        final List<KindUserDO> kindUsers = kindUserRepository.findAllByUid(uid,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")));

        // Query kind definitions
        final List<String> kindIds = kindUsers.stream().map(KindUserDO::getKindId).toList();
        final List<KindDefinition> kinds = apiGateway.getKindDefinitions(kindIds);
        final Map<String, KindDefinition> kindMap = kinds.stream()
                .collect(Collectors.toMap(k -> k.getMetadata().getId(), k -> k));

        // Convert to KindSummary
        List<KindSummary> summaries = new ArrayList<>();
        for (KindUserDO ku : kindUsers) {
            if (!kindMap.containsKey(ku.getKindId())) {
                continue;
            }
            summaries.add(new KindSummary(kindMap.get(ku.getKindId())));
        }
        return new Response<>(ResponseCode.Success, Pagination.of(summaries, total, page, size));
    }

    @GetMapping("/detail")
    public Response<KindDetail> findKind(@RequestParam String kindId) {
        Assert.notNull(kindId, "KindId not exist.");
        final Optional<KindDefinition> kind = apiGateway.getKindDefinition(kindId);
        return kind.map(definition -> new Response<>(ResponseCode.Success, new KindDetail(definition)))
                .orElseGet(() -> new Response<>(ResponseCode.IllegalParam, "KindId not exist.", null));
    }

    @Data
    public static class CreateFlowRequest {
        private String uid;
        private String kindType;
        private Map<String, Object> param;
    }

    @Data
    public static class KindSummary {
        private String id;
        private String kind;
        private KindDefinition.PhaseEnum phase;
        private String createTime;
        private String input;
        private String videoUrl;

        public KindSummary(KindDefinition def) {
            this.id = def.getMetadata().getId();
            this.kind = def.getKind();
            if (Objects.nonNull(def.getStatus())) {
                this.phase = def.getStatus().getPhase();
            }
            if (Objects.nonNull(def.getMetadata())) {
                this.createTime = def.getMetadata().getCreateTime();
                this.id = def.getMetadata().getId();
            }
        }
    }

    @Data
    public static class KindDetail {
        private KindDefinition.Metadata metadata;
        private KindDefinition.Spec spec;
        private StatusSummary status;

        public KindDetail(KindDefinition kind) {
            this.metadata = kind.getMetadata();
            this.status = new StatusSummary(kind.getStatus());
            this.spec = kind.getSpec();
        }

        @Data
        public static class StatusSummary {
            private KindDefinition.PhaseEnum phase;
            private Map<String, FlowKindDefinition.ConditionStatus> condition;

            public StatusSummary(final KindDefinition.Status status) {
                if (Objects.isNull(status)) {
                    return;
                }
                this.phase = status.getPhase();
                if (status instanceof FlowKindDefinition.Status sstatus) {
                    this.condition = sstatus.getCondition();
                }
            }
        }
    }

}