package aicreative.ai.dataplane.task.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface TaskDORepository
    extends CrudRepository<TaskDO, String>, ListPagingAndSortingRepository<TaskDO, String> {}
