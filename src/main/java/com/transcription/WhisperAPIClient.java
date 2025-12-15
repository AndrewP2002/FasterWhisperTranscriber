package com.transcription;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WhisperAPIClient {

    private static final String TRANSCRIBE_ENDPOINT = "/api/transcribe";
    private static final String BOUNDARY = "----" + UUID.randomUUID().toString();
    private final String serverUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public WhisperAPIClient(String serverUrl) {
        this.serverUrl = serverUrl.replaceAll("/$", ""); // Remove trailing slash if present
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Transcribe audio file using the remote API
     * @param filePath Path to the audio file
     * @param sourceLanguage Source language code (e.g., "en", "uk")
     * @param model Model to use (e.g., "base", "small", "medium", "large")
     * @param translate Whether to translate the transcription
     * @param targetLanguage Target language for translation (can be null if translate=false)
     * @return TranscriptionResponse object containing the results or error information
     */
    public TranscriptionResponse transcribeAudio(String filePath, String sourceLanguage,
                                                 String model, boolean translate,
                                                 String targetLanguage) {
        try {
            //Read the audio file into memory
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String fileName = Paths.get(filePath).getFileName().toString();
            //Build the multipart request body (Now returns BodyPublisher, not String)
            HttpRequest.BodyPublisher bodyPublisher = buildMultipartBody(fileContent, fileName,
                    sourceLanguage, model,
                    translate, targetLanguage);
            //Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + TRANSCRIBE_ENDPOINT))
                    .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                    .timeout(Duration.ofMinutes(10))
                    .POST(bodyPublisher) // Використовуємо binary publisher
                    .build();
            System.out.println("Sending request to: " + serverUrl + TRANSCRIBE_ENDPOINT);
            //Send the request and get the response
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return parseResponse(response);
        } catch (IOException | InterruptedException e) {
            System.err.println("Communication error: " + e.getMessage());
            return TranscriptionResponse.error("Failed to communicate with server: " + e.getMessage());
        }
    }

    /**
     * Build a multipart/form-data request body
     * @param fileContent The binary content of the audio file
     * @param fileName The name of the file
     * @param sourceLanguage Source language code
     * @param model Model name
     * @param translate Whether translation is needed
     * @param targetLanguage Target language (can be null)
     * @return The multipart request body as a String
     */
    private HttpRequest.BodyPublisher buildMultipartBody(byte[] fileContent, String fileName,
                                                         String sourceLanguage, String model,
                                                         boolean translate, String targetLanguage) {
        //List to hold all parts of the body (headers + binary data + footers)
        List<byte[]> byteArrays = new ArrayList<>();
        //separator with newLine
        byte[] separator = ("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] newLine = "\r\n".getBytes(StandardCharsets.UTF_8);

        //Add File
        byteArrays.add(separator);
        String fileHeader = "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n";
        byteArrays.add(fileHeader.getBytes(StandardCharsets.UTF_8));
        byteArrays.add(fileContent); // ВАЖЛИВО: додаємо "сирі" байти файлу без конвертації в String
        byteArrays.add(newLine);

        //Add Language Field
        addTextField(byteArrays, "language", sourceLanguage, separator, newLine);

        // Add Model Field
        addTextField(byteArrays, "model", model, separator, newLine);

        //Add Translate check
        addTextField(byteArrays, "translate", translate ? "true" : "false", separator, newLine);

        //Add Target Language (if needed)
        if (translate && targetLanguage != null) {
            addTextField(byteArrays, "target_language", targetLanguage, separator, newLine);
        }

        //Final Boundary
        byteArrays.add(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));

        //Compile all byte arrays into a single BodyPublisher
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    //Helper method to add simple text fields
    private void addTextField(List<byte[]> byteArrays, String name, String value,
                              byte[] separator, byte[] newLine) {
        byteArrays.add(separator);
        String header = "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n";
        byteArrays.add(header.getBytes(StandardCharsets.UTF_8));
        byteArrays.add(value.getBytes(StandardCharsets.UTF_8));
        byteArrays.add(newLine);
    }

    /**
     * Parse the API response and create a TranscriptionResponse object
     * @param response The HTTP response from the server
     * @return TranscriptionResponse object with parsed data or error information
     */
    private TranscriptionResponse parseResponse(HttpResponse<String> response) {
        try {
            //Check HTTP status code
            if (response.statusCode() != 200) {
                return TranscriptionResponse.error("Server error: HTTP " + response.statusCode());
            }
            //Parse JSON response
            JsonNode jsonResponse = objectMapper.readTree(response.body());
            //Check if there's an error in the response
            if (jsonResponse.has("error")) {
                String errorMsg = jsonResponse.get("error").asText();
                return TranscriptionResponse.error("Server error: " + errorMsg);
            }
            //Extract fields from response
            String originalText = jsonResponse.has("original_text") ?
                    jsonResponse.get("original_text").asText() : "";
            String srtOriginal = jsonResponse.has("srt_original") ?
                    jsonResponse.get("srt_original").asText() : "";
            String translatedText = null;
            String srtTranslated = null;
            if (jsonResponse.has("translated_text")) {
                translatedText = jsonResponse.get("translated_text").asText();
            }
            if (jsonResponse.has("srt_translated")) {
                srtTranslated = jsonResponse.get("srt_translated").asText();
            }
            return TranscriptionResponse.success(originalText, srtOriginal,
                    translatedText, srtTranslated);
        } catch (Exception e) {
            System.err.println("Response parsing error: " + e.getMessage());
            return TranscriptionResponse.error("Failed to parse server response: " + e.getMessage());
        }
    }
}