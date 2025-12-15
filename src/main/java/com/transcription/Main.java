package com.transcription;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        //server url
        String serverUrl = "http://localhost:8080";
        WhisperAPIClient apiClient = new WhisperAPIClient(serverUrl);
        try {
            System.out.println("Starting transcription:");
            System.out.println();

            //Get input folder path
            System.out.print("Enter folder path (e.g., /path/to/folder/): ");
            String mainFolder = scanner.nextLine();

            //Validate that the folder exists
            if (!Files.exists(Paths.get(mainFolder))) {
                System.err.println("ERROR: Folder does not exist: " + mainFolder);
                return;
            }

            //Get audio file name
            System.out.print("Enter audio file name (e.g., audio.mp3): ");
            String fileName = scanner.nextLine();
            String filePath = mainFolder + (mainFolder.endsWith("/") ? "" : "/") + fileName;

            //Validate that the file exists
            if (!Files.exists(Paths.get(filePath))) {
                System.err.println("ERROR: File does not exist: " + filePath);
                return;
            }

            //Get language of the audio
            System.out.print("Enter source language (e.g., en, uk, fr): ");
            String sourceLanguage = scanner.nextLine();

            //Get the model to use
            System.out.print("Enter model (e.g., base, small, medium, large): ");
            String model = scanner.nextLine();

            //Ask if translation is needed
            System.out.print("Do you want to translate the transcription? (y/n): ");
            String translateResponse = scanner.nextLine();
            boolean needsTranslation = translateResponse.equalsIgnoreCase("y");
            String targetLanguage = null;
            if (needsTranslation) {
                System.out.print("Enter target language for translation (e.g., en, uk, fr): ");
                targetLanguage = scanner.nextLine();
            }

            //Call the API
            System.out.println("\nSending request to server...");
            TranscriptionResponse response = apiClient.transcribeAudio(
                    filePath,
                    sourceLanguage,
                    model,
                    needsTranslation,
                    targetLanguage
            );
            
            //Handle the response
            if (response.isSuccess()) {
                System.out.println("Transcription completed successfully!");
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                String originalSrtPath = mainFolder + "/" + baseName + ".srt";
                Files.writeString(Paths.get(originalSrtPath), response.getSrtOriginal());
                System.out.println("Original subtitles saved to: " + originalSrtPath);
                //Save translated transcription if applicable
                if (needsTranslation && response.getSrtTranslated() != null) {
                    String translatedSrtPath = mainFolder+ "/" + baseName + "_translated.srt";
                    Files.writeString(Paths.get(translatedSrtPath), response.getSrtTranslated());
                    System.out.println("Translated subtitles saved to: " + translatedSrtPath);
                }
            } else {
                System.err.println("\n✗ Error: " + response.getErrorMessage());
            }
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}