package aicreative.ai.controlplane.api.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import aicreative.ai.controlplane.api.kinduser.KindUserDO;
import aicreative.ai.controlplane.api.kinduser.KindUserRepository;
import aicreative.ai.controlplane.kindcontroller.base.KindDefinition;
import aicreative.ai.controlplane.kindcontroller.base.KindDefinitionHelper;
import aicreative.ai.controlplane.kindcontroller.base.KindLockHelper;
import aicreative.ai.controlplane.kindcontroller.mq.KindMessageSender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Component
@Data
@Slf4j
@AllArgsConstructor
public class ApiGateway {

    private final KindDefinitionHelper kindDefinitionHelper;

    private final KindMessageSender kindMessageSender;

    private final KindLockHelper kindLockHelper;

    private final KindUserRepository kindUserRepository;

    private final ObjectMapper objectMapper;

    public String createKindDefinition(final String kindDefinitionStr) {
        final KindDefinition kind = kindDefinitionHelper.parseKindFromString(kindDefinitionStr);
        return this.createKindDefinition(kind);
    }

    public String createKindDefinition(final KindDefinition kindDefinition) {

        if (Objects.isNull(kindDefinition) || Objects.isNull(kindDefinition.getMetadata())) {
            throw new IllegalArgumentException("KindDefinition or Metadata is null");
        }

        final boolean uidNotExist = !StringUtils.hasText(kindDefinition.getMetadata().getId());
        if (uidNotExist) {
            kindDefinition.getMetadata().setId(UUID.randomUUID().toString());
        }
        if (!StringUtils.hasText(kindDefinition.getMetadata().getCreateTime())) {
            kindDefinition.getMetadata().setCreateTime(LocalDateTime.now().format(ISO_LOCAL_DATE_TIME));
        }

        kindDefinitionHelper.writeKindToStorage(kindDefinition);
        kindUserRepository.save(KindUserDO.of(kindDefinition));
        kindMessageSender.sendKindMessage(kindDefinition, "Create", "Init Kind");

        return kindDefinition.getMetadata().getId();
    }

    public void deleteKindDefinition(final String kindId) {
        kindDefinitionHelper.deleteKindFromStorage(kindId);
        kindUserRepository.deleteById(kindId);
    }

    public <T extends KindDefinition> Optional<T> getKindDefinition(final String id) {
        return kindDefinitionHelper.getKindFromStorage(id);
    }

    public List<KindDefinition> getKindDefinitions(final List<String> kindIds) {
        return kindDefinitionHelper.getKindsFromStorage(kindIds);
    }

    /**
     * @return Whether it was updated. true: kind was updated; False: kind is consistent with the database and does not need to be updated.
     */
    public boolean updateKindDefinition(final KindDefinition def, final String operation) {
        final Optional<KindDefinition> old = kindDefinitionHelper.getKindFromStorage(def.getMetadata().getId());
        Assert.state(old.isPresent(), String.format("%s none exist.", def.getMetadata().getId()));

        final boolean needUpdate = !Objects.equals(old.get(), def);
        if (needUpdate) {
            kindDefinitionHelper.writeKindToStorage(def);
            kindMessageSender.sendKindMessage(def, operation, extraInfos(def, old.get()));
        }
        return needUpdate;
    }

    /**
     * @return Whether it was updated. true: kind was updated; False: kind is consistent with the database and does not need to be updated.
     */
    public boolean updateDefinitionWithLock(final KindDefinitionUpdater updater, final String kindId, final String operation) {
        final String randomValue = UUID.randomUUID().toString();
        try {
            kindLockHelper.lock(kindId, randomValue);
            log.trace("Got KindLock for Updater.");
            final Optional<KindDefinition> o = kindDefinitionHelper.getKindFromStorage(kindId);
            if (o.isEmpty()) {
                log.warn("Kind not exist.");
                return false;
            }
            final KindDefinition definition = o.get();
            updater.update(definition);
            log.debug("Update. Operation: {}", operation);
            return updateKindDefinition(definition, operation);
        } finally {
            log.trace("Release KindLock for Updater.");
            kindLockHelper.unlock(kindId, randomValue);
        }
    }

    public interface KindDefinitionUpdater {
        void update(KindDefinition definition);
    }

    private String extraInfos(KindDefinition def, KindDefinition old) {
        final List<KindDefinition.Diff> diff = def.diff(old);
        if (CollectionUtils.isEmpty(diff)) {
            return "";
        }
        String diffPaths = diff.stream().map(KindDefinition.Diff::getPath).collect(Collectors.joining(","));
        try {
            if (log.isTraceEnabled()) {
                String truncate = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(diff);
                return String.format("%s, Detail: %s", diffPaths, truncate);
            }
            if (log.isDebugEnabled()) {
                String truncate = StringUtils.truncate(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(diff), 1000);
                return String.format("%s, Detail: %s", diffPaths, truncate);
            }
            return String.format("%s, Detail: %s", diffPaths, StringUtils.truncate(objectMapper.writeValueAsString(diff)));
        } catch (JsonProcessingException e) {
            return diffPaths;
        }
    }
}