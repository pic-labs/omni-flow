package aicreative.ai.dataplane.proxy.coze.model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CozeTaskRepository extends CrudRepository<CozeTask, String> {
    List<CozeTask> findAllByTaskType(String taskType);
}
