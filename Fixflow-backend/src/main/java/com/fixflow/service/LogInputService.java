package com.fixflow.service;

import com.fixflow.exception.InvalidLogException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LogInputService {

    public record PreparedUpload(String content, String fileName) {
    }

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("log", "txt");
    private static final int MAX_FILE_NAME_LENGTH = 100;
    private static final int BINARY_SAMPLE_SIZE = 8192;

    // Terminal colour codes, with or without the leading ESC character (copied console text often loses it).
    private static final Pattern TERMINAL_CODES =
            Pattern.compile("\\u001B\\[[0-9;]*[A-Za-z]|\\[\\d{1,2}(?:;\\d{1,2})*m");
    private static final Pattern UNSAFE_FILE_CHARS = Pattern.compile("[^A-Za-z0-9._\\- ]");

    private final int maxUploadMb;
    private final long maxBytes;

    public LogInputService(@Value("${app.max-upload-mb:1}") int maxUploadMb) {
        this.maxUploadMb = maxUploadMb;
        this.maxBytes = maxUploadMb * 1024L * 1024L;
    }

    public String preparePastedContent(String content) {
        if (content == null || content.isBlank()) {
            throw emptyInput();
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw tooLarge();
        }
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            throw emptyInput();
        }
        return normalized;
    }

    public PreparedUpload prepareUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw emptyInput();
        }
        String fileName = sanitizeFileName(file.getOriginalFilename());
        if (fileName.isBlank()) {
            throw new InvalidLogException("INVALID_FILE_NAME", "The file name is not valid.", HttpStatus.BAD_REQUEST);
        }
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidLogException("UNSUPPORTED_FILE_TYPE",
                    "Only .log and .txt files are supported.", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        if (file.getSize() > maxBytes) {
            throw tooLarge();
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidLogException("UNREADABLE_FILE", "The file could not be read.", HttpStatus.BAD_REQUEST);
        }
        if (looksBinary(bytes)) {
            throw new InvalidLogException("BINARY_FILE",
                    "The file looks like a binary file. Please upload a plain-text log.", HttpStatus.BAD_REQUEST);
        }
        String normalized = normalize(new String(bytes, StandardCharsets.UTF_8));
        if (normalized.isBlank()) {
            throw emptyInput();
        }
        return new PreparedUpload(normalized, fileName);
    }

    private String normalize(String raw) {
        String text = TERMINAL_CODES.matcher(raw).replaceAll("");
        return text.replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "");
    }

    private String sanitizeFileName(String original) {
        if (original == null) {
            return "";
        }
        String name = original;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = UNSAFE_FILE_CHARS.matcher(name).replaceAll("_").strip();
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            name = name.substring(name.length() - MAX_FILE_NAME_LENGTH);
        }
        return name;
    }

    private boolean looksBinary(byte[] bytes) {
        int sample = Math.min(bytes.length, BINARY_SAMPLE_SIZE);
        int controlCount = 0;
        for (int i = 0; i < sample; i++) {
            int b = bytes[i] & 0xFF;
            if (b == 0) {
                return true;
            }
            if (b < 0x20 && b != '\n' && b != '\r' && b != '\t' && b != 0x1B) {
                controlCount++;
            }
        }
        return sample > 0 && controlCount > sample * 0.02;
    }

    private InvalidLogException emptyInput() {
        return new InvalidLogException("EMPTY_INPUT", "Log content must not be empty.", HttpStatus.BAD_REQUEST);
    }

    private InvalidLogException tooLarge() {
        return new InvalidLogException("INPUT_TOO_LARGE",
                "Input is larger than the " + maxUploadMb + " MB limit.", HttpStatus.PAYLOAD_TOO_LARGE);
    }
}