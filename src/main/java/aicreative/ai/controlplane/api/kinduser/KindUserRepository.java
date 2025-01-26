package aicreative.ai.controlplane.api.kinduser;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

import java.util.List;

public interface KindUserRepository extends CrudRepository<KindUserDO, String>, ListPagingAndSortingRepository<KindUserDO, String>{

    List<KindUserDO> findAllByUid(String uid, Pageable pageable);

}
