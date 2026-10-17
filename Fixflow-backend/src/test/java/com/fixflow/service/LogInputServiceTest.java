package com.fixflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fixflow.exception.InvalidLogException;
import com.fixflow.service.LogInputService.PreparedUpload;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class LogInputServiceTest {

    private final LogInputService service = new LogInputService(1);

    @Test
    void rejectsEmptyPastedContent() {
        InvalidLogException ex = assertThrows(InvalidLogException.class,
                () -> service.preparePastedContent("   \n  "));

        assertEquals("EMPTY_INPUT", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void normalizesLineEndings() {
        assertEquals("a\nb\nc", service.preparePastedContent("a\r\nb\rc"));
    }

    @Test
    void stripsTerminalColourCodes() {
        assertEquals("ERROR failed", service.preparePastedContent("\u001B[31mERROR\u001B[0m failed"));
    }

    @Test
    void rejectsOversizedPaste() {
        String huge = "a".repeat(1024 * 1024 + 1);

        InvalidLogException ex = assertThrows(InvalidLogException.class, () -> service.preparePastedContent(huge));

        assertEquals("INPUT_TOO_LARGE", ex.getCode());
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.getStatus());
    }

    @Test
    void rejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "app.exe", "application/octet-stream",
                "abc".getBytes(StandardCharsets.UTF_8));

        InvalidLogException ex = assertThrows(InvalidLogException.class, () -> service.prepareUpload(file));

        assertEquals("UNSUPPORTED_FILE_TYPE", ex.getCode());
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getStatus());
    }

    @Test
    void rejectsBinaryFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "data.log", "text/plain",
                new byte[] {0x50, 0x00, 0x01, 0x02});

        InvalidLogException ex = assertThrows(InvalidLogException.class, () -> service.prepareUpload(file));

        assertEquals("BINARY_FILE", ex.getCode());
    }

    @Test
    void rejectsEmptyFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.log", "text/plain", new byte[0]);

        InvalidLogException ex = assertThrows(InvalidLogException.class, () -> service.prepareUpload(file));

        assertEquals("EMPTY_INPUT", ex.getCode());
    }

    @Test
    void rejectsOversizedUploads() {
        MockMultipartFile file = new MockMultipartFile("file", "big.log", "text/plain",
                new byte[1024 * 1024 + 1]);

        InvalidLogException ex = assertThrows(InvalidLogException.class, () -> service.prepareUpload(file));

        assertEquals("INPUT_TOO_LARGE", ex.getCode());
    }

    @Test
    void acceptsLogFileAndSanitizesFileName() {
        MockMultipartFile file = new MockMultipartFile("file", "..\\..\\evil name?.log", "text/plain",
                "java.lang.NullPointerException".getBytes(StandardCharsets.UTF_8));

        PreparedUpload upload = service.prepareUpload(file);

        assertEquals("evil name_.log", upload.fileName());
        assertEquals("java.lang.NullPointerException", upload.content());
    }
}