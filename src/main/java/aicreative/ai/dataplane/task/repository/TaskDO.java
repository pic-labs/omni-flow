package aicreative.ai.dataplane.task.repository;

import aicreative.ai.dataplane.task.enums.TaskFailType;
import aicreative.ai.dataplane.task.enums.TaskStatus;
import aicreative.ai.dataplane.task.enums.TaskType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Data
@Slf4j
@ToString
@RedisHash("dp-task:storage")
public class TaskDO {
    @Id
    private String taskId;
    private TaskType taskType;
    private TaskStatus taskStatus;
    private Map<String, String> ext;
    private String param;
    private String result;
    private Long priority;
    private Integer execCnt = 0;
    private String execStartTime;
    private String execEndTime;
    private String createTime;
    private TaskFailType failType;

    public static TaskDO newTask(String taskId, TaskType taskType, Map<String, String> ext, Object param, Long priority) {
        TaskDO taskDO = new TaskDO();
        taskId = StringUtils.isNotBlank(taskId) ? taskId : UUID.randomUUID().toString();
        taskDO.setTaskId(taskId);
        taskDO.setTaskType(taskType);
        String paramStr;
        try {
            paramStr = new ObjectMapper().writeValueAsString(param);
        } catch (JsonProcessingException e) {
            log.warn("TaskDO parse param failed, param:{}", param);
            throw new RuntimeException("taskDO parse param failed");
        }
        taskDO.setExt(ext);
        taskDO.setParam(paramStr);
        taskDO.setTaskStatus(TaskStatus.PENDING);
        taskDO.setPriority(priority);
        taskDO.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        return taskDO;
    }

    public void exec2Pending(TaskFailType taskFailType, Object result, Long priority) {
        this.setFailType(taskFailType);
        this.setExecEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        String resultStr;
        try {
            resultStr = new ObjectMapper().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.warn("TaskDO parse result failed, result:{}", result);
            throw new RuntimeException("taskDO parse result failed");
        }
        this.setResult(resultStr);
        this.setTaskStatus(TaskStatus.PENDING);
        this.setPriority(priority);
    }

    public void execute() {
        this.setTaskStatus(TaskStatus.EXECUTING);
        this.setExecCnt(this.getExecCnt() + 1);
        this.setExecStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        this.setExecEndTime(null);
    }

    public void fail(TaskFailType failType, Object result) {
        this.setTaskStatus(TaskStatus.FAILED);
        this.setExecEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        String resultStr;
        try {
            resultStr = new ObjectMapper().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.warn("TaskDO parse result failed, result:{}", result);
            throw new RuntimeException("taskDO parse result failed");
        }
        this.setResult(resultStr);
        this.setFailType(failType);
    }

    public void success(Object result) {
        this.setTaskStatus(TaskStatus.SUCCEED);
        this.setExecEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        String resultStr;
        try {
            resultStr = new ObjectMapper().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.warn("TaskDO parse result failed, result:{}", result);
            throw new RuntimeException("taskDO parse result failed");
        }
        this.setResult(resultStr);
        this.setFailType(null);
    }

    public boolean canRetry(Integer retryCnt) {
        return this.getExecCnt() < retryCnt + 1;
    }
}
