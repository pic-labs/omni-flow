package aicreative.ai.controlplane.kindcontroller.base;

import aicreative.ai.controlplane.kindcontroller.flow.FlowKindController;
import aicreative.ai.controlplane.kindcontroller.flow.definition.FlowKindDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.*;

@Data
@Component
@AllArgsConstructor
@Slf4j
public class KindDefinitionHelper {

    private static final String KIND_STORAGE_PREFIX = "cp-kind";

    private final StringRedisTemplate template;

    private final ObjectMapper objectMapper;

    private final ApplicationContext applicationContext;

    public <T extends KindDefinition> void writeKindToStorage(T baseDefinition) {
        final String key = storageKey(baseDefinition.getMetadata().getId());
        final String dump;
        try {
            dump = objectMapper.writeValueAsString(baseDefinition);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        template.opsForValue().set(key, dump);
    }

    public <T extends KindDefinition> Optional<T> getKindFromStorage(final String id) {
        final String s = template.opsForValue().get(storageKey(id));
        if (!StringUtils.hasText(s)) {
            return Optional.empty();
        }
        return Optional.of(this.parseKindFromString(s));
    }

    public <T extends KindDefinition> List<T> getKindsFromStorage(List<String> kindIds) {
        if (CollectionUtils.isEmpty(kindIds)) {
            return List.of();
        }
        final List<String> storageKeys = kindIds.stream().map(this::storageKey).toList();
        final List<String> kinds = template.opsForValue().multiGet(storageKeys);
        if (CollectionUtils.isEmpty(kinds)) {
            return List.of();
        }
        List<T> list = new ArrayList<>();
        for (String def : kinds) {
            if (!StringUtils.hasText(def)) {
                continue;
            }
            T t = this.parseKindFromString(def);
            list.add(t);
        }
        return list;
    }

    public <T extends KindDefinition> T parseKindFromString(final String defStr) {

        final KindDefinition def;
        try {
            def = objectMapper.readValue(defStr, KindDefinition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Illegal definition.", e);
        }

        final Map<String, KindController> controllers = applicationContext.getBeansOfType(KindController.class);
        final Optional<KindController.KindType> kind;
        if (FlowKindDefinition.kindExists(def.getKind())) {
            kind = Optional.of(applicationContext.getBean(FlowKindController.class).kindType());
        } else {
            kind = controllers.values().stream()
                    .filter(d -> Objects.equals(d.kindType().getName(), def.getKind()))
                    .findFirst()
                    .map(KindController::kindType);
        }
        if (kind.isEmpty()) {
            throw new IllegalArgumentException("Invalid KIND type.");
        }

        try {
            return objectMapper.readValue(defStr, new TypeReference<>() {
                @Override
                public Type getType() {
                    return kind.get().getTypeClass();
                }
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid KIND definition.", e);
        }
    }

    public void deleteKindFromStorage(final String id) {
        var key = storageKey(id);
        template.delete(key);
    }

    private String storageKey(String id) {
        return String.format("%s:%s", KIND_STORAGE_PREFIX, id);
    }

}
