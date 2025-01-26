package aicreative.ai.common.oss;

import java.net.URI;

public interface OssHandler {

    URI uploadFile(String filePath);

    <T> T getForObject(String url, Class<T> clazz);
}
