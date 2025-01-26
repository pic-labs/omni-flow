package aicreative.ai.controlplane.api.kinduser;

import aicreative.ai.controlplane.kindcontroller.base.KindDefinition;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.Objects;

@RedisHash("cp-kind-user")
@Data
public class KindUserDO {

    @Id
    private String kindId;
    @Indexed
    private String uid;
    private String kindType;
    private String createTime;

    public static KindUserDO of(KindDefinition def) {
        KindUserDO k = new KindUserDO();
        k.setKindType(def.getKind());
        if (Objects.isNull(def.getMetadata())){
            return k;
        }
        k.setKindId(def.getMetadata().getId());
        k.setCreateTime(def.getMetadata().getCreateTime());
        if (Objects.isNull(def.getMetadata().getLabels())){
            return k;
        }
        k.setUid(def.getMetadata().getLabels().get("uid"));
        return k;
    }
}
