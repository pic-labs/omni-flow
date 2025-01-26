package aicreative.ai.controlplane.kindcontroller.flow.definition.spel;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Slf4j
public class Fn {
    public static Map<String, Object> findMapByKey(List<Map<String, Object>> list, String key, Object value) {
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }
        for (Map<String, Object> map : list) {
            if (Objects.equals(map.get(key), value)) {
                return map;
            }
        }
        return new HashMap<>();
    }

    public static List<Map<String, Object>> string2Map(List<String> list, String key) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String s : list) {
            Map<String, Object> map = new HashMap<>();
            map.put(key, s);
            result.add(map);
        }
        return result;
    }

    public static List<Map<String, Object>> mergeListByIndex(List<Map<String, Object>> list1, List<Map<String, Object>> list2) {
        if (CollectionUtils.isEmpty(list1)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        //已 list1 为基准， 合并list2
        for (int i = 0; i < list1.size(); i++) {
            if (Objects.isNull(list1.get(i))) {
                continue;
            }
            Map<String, Object> map1 = new HashMap<>(list1.get(i));
            Map<String, Object> map2 = null;
            if (i < list2.size() && Objects.nonNull(list2.get(i))) {
                map2 = new HashMap<>(list2.get(i));
                map1.putAll(map2);
            }
            result.add(map1);
        }
        return result;
    }

    public static <T> T toJavaObject(String json, Class<T> clazz) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("toJavaObject error, json: {}, clazz: {}", json, clazz.getName(), e);
            throw new RuntimeException("toJavaObject error", e);
        }
    }

    public static boolean allStringBlank(String... str) {
        if (Objects.isNull(str)) {
            return true;
        }
        return Arrays.stream(str).allMatch(StringUtils::isBlank);
    }

    public static boolean allStringNotBlank(String... str) {
        if (Objects.isNull(str)) {
            return true;
        }
        return Arrays.stream(str).allMatch(StringUtils::isNotBlank);
    }

    public static boolean anyStringBlank(String... str) {
        if (Objects.isNull(str)) {
            return false;
        }
        return Arrays.stream(str).anyMatch(StringUtils::isBlank);
    }
}
