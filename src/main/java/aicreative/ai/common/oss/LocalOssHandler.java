package aicreative.ai.common.oss;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@AllArgsConstructor
public class LocalOssHandler implements OssHandler {

    private ObjectMapper objectMapper;

    @Override
    public URI uploadFile(String filePath) {
        String newPash = String.format("./tmp/%s", filePath.substring(filePath.lastIndexOf("/") + 1));
        try {
            FileCopyUtils.copy(new File(filePath), new File(newPash));
            return new URI(newPash);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T getForObject(String filePath, Class<T> clazz) {
        final StringBuilder content = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            content.append(reader.lines().collect(Collectors.joining(System.lineSeparator())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (clazz == String.class) {
            return (T) content.toString();
        }
        try {
            return objectMapper.readValue(content.toString(), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
