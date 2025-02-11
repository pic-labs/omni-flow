package aicreative.ai.dataplane.proxy.huoshan.tts.async.model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HuoshanTtsAsyncTaskRepository extends CrudRepository<HuoshanTtsAsyncTask, String> {
    List<HuoshanTtsAsyncTask> findAllByTaskType(String taskType);
}
