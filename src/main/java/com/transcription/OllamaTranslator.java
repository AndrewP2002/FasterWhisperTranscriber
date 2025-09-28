package com.transcription;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OllamaTranslator {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "llama3.1:8b";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Regex pattern to match SRT subtitle entries
    private static final Pattern SRT_PATTERN = Pattern.compile(
            "^(\\d+)\\s*\\n([\\d:,]+\\s*-->\\s*[\\d:,]+)\\s*\\n((?:.*\\n?)*)(?=\\d+\\s*\\n[\\d:,]+\\s*-->|$)",
            Pattern.MULTILINE
    );

    public static void translateSubtitleFile(String inputPath, String outputPath, String targetLanguage)
            throws IOException, InterruptedException {
        // Step 1: Read and parse the subtitle file
        System.out.println("Reading subtitle file: " + inputPath);
        Path input = Paths.get(inputPath);
        if (!Files.exists(input)) {
            throw new IOException("Input file does not exist: " + inputPath);
        }
        String content = Files.readString(input);
        List<SubtitleEntry> subtitles = parseSubtitleFile(content);
        System.out.println("Parsed " + subtitles.size() + " subtitle entries");
        // Step 2: Translate each subtitle entry
        StringBuilder translatedSrt = new StringBuilder();
        for (int i = 0; i < subtitles.size(); i++) {
            SubtitleEntry entry = subtitles.get(i);
            System.out.println("Translating subtitle " + (i + 1) + "/" + subtitles.size() +
                    " (ID: " + entry.number + ")");
            try {
                String translatedText = translateText(entry.text, targetLanguage);

                // Reconstruct the SRT format
                translatedSrt.append(entry.number).append("\n");
                translatedSrt.append(entry.timestamp).append("\n");
                translatedSrt.append(translatedText).append("\n");
                translatedSrt.append("\n"); // Empty line between entries
                // Small delay to avoid overwhelming the API
                Thread.sleep(500);
            } catch (Exception e) {
                System.err.println("Failed to translate subtitle " + entry.number + ": " + e.getMessage());

                // Keep original format but mark as failed
                translatedSrt.append(entry.number).append("\n");
                translatedSrt.append(entry.timestamp).append("\n");
                translatedSrt.append("[TRANSLATION FAILED] ").append(entry.text).append("\n");
                translatedSrt.append("\n");
            }
        }
        // Step 3: Save the translated subtitle file
        System.out.println("Saving translated subtitles to: " + outputPath);
        Files.writeString(Paths.get(outputPath), translatedSrt.toString());
    }

    private static List<SubtitleEntry> parseSubtitleFile(String content) {
        List<SubtitleEntry> subtitles = new ArrayList<>();
        // Normalize line endings
        content = content.replace("\r\n", "\n").replace("\r", "\n");
        // Split by double newlines (subtitle separator)
        String[] blocks = content.split("\n\n");
        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;
            String[] lines = block.split("\n");
            if (lines.length >= 3) {
                try {
                    // First line: subtitle number
                    int number = Integer.parseInt(lines[0].trim());
                    // Second line: timestamp
                    String timestamp = lines[1].trim();
                    // Remaining lines: subtitle text
                    StringBuilder text = new StringBuilder();
                    for (int i = 2; i < lines.length; i++) {
                        if (i > 2) text.append("\n");
                        text.append(lines[i]);
                    }
                    subtitles.add(new SubtitleEntry(number, timestamp, text.toString()));
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid subtitle block: " + block.substring(0, Math.min(50, block.length())));
                }
            }
        }
        return subtitles;
    }

    private static String translateText(String text, String targetLanguage)
            throws IOException, InterruptedException {
        // Handle multi-line subtitle text
        String cleanText = text.trim();
        // Create the prompt - specifically for subtitles
        String prompt = String.format(
                "Translate the following subtitle text to %s. Preserve line breaks and formatting. Only return the translation:\n\n%s",
                targetLanguage, cleanText
        );
        // Create JSON request
        String jsonRequest = String.format("""
            {
                "model": "%s",
                "prompt": %s,
                "stream": false,
                "options": {
                    "temperature": 0.1,
                    "top_p": 0.9
                }
            }
            """, MODEL, objectMapper.writeValueAsString(prompt));
        // Send HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(3))
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP Error: " + response.statusCode() + " - " + response.body());
        }
        // Parse JSON response
        JsonNode jsonResponse = objectMapper.readTree(response.body());
        if (jsonResponse.has("error")) {
            throw new IOException("Ollama Error: " + jsonResponse.get("error").asText());
        }
        return jsonResponse.get("response").asText().trim();
    }

    // Inner class to represent a subtitle entry
    private static class SubtitleEntry {
        final int number;
        final String timestamp;
        final String text;
        SubtitleEntry(int number, String timestamp, String text) {
            this.number = number;
            this.timestamp = timestamp;
            this.text = text;
        }
        @Override
        public String toString() {
            return String.format("SubtitleEntry{number=%d, timestamp='%s', text='%s'}",
                    number, timestamp, text.replace("\n", "\\n"));
        }
    }
}