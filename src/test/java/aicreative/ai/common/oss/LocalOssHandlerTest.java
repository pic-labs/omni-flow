package aicreative.ai.common.oss;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class LocalOssHandlerTest {

    private LocalOssHandler localOssHandler;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        localOssHandler = new LocalOssHandler(new ObjectMapper());
    }

    @Test
    public void getForObject_TxtFile_ReturnsObject() throws IOException {
        Path tempFile = Files.createFile(tempDir.resolve("test.txt"));
        String txtContent = "GOOD TEST.";

        Files.writeString(tempFile, txtContent);
        log.info("TempFile: {}", tempFile);
        String rtn = localOssHandler.getForObject(tempFile.toString(), String.class);

        assertEquals(txtContent, rtn);
    }
}
