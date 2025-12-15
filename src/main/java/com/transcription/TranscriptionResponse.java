package com.transcription;

public class TranscriptionResponse {
    private final boolean success;
    private final String originalText;
    private final String srtOriginal;
    private final String translatedText;
    private final String srtTranslated;
    private final String errorMessage;

    private TranscriptionResponse(boolean success, String originalText, String srtOriginal,
                                  String translatedText, String srtTranslated, String errorMessage) {
        this.success = success;
        this.originalText = originalText;
        this.srtOriginal = srtOriginal;
        this.translatedText = translatedText;
        this.srtTranslated = srtTranslated;
        this.errorMessage = errorMessage;
    }

    /**
     * Create a successful response with transcription data
     * @param originalText The transcribed text in the original language
     * @param srtOriginal The SRT subtitle file with original text
     * @param translatedText The translated text (can be null if no translation was requested)
     * @param srtTranslated The SRT subtitle file with translated text (can be null)
     * @return A successful TranscriptionResponse
     */
    public static TranscriptionResponse success(String originalText, String srtOriginal,
                                                String translatedText, String srtTranslated) {
        return new TranscriptionResponse(true, originalText, srtOriginal,
                translatedText, srtTranslated, null);
    }

    /**
     * Create an error response
     * @param errorMessage Description of the error that occurred
     * @return An error TranscriptionResponse
     */
    public static TranscriptionResponse error(String errorMessage) {
        return new TranscriptionResponse(false, null, null, null, null, errorMessage);
    }

    /**
     * Check if the response indicates success
     * @return true if the request was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the transcribed text in the original language
     * @return The original text, or null if the request failed
     */
    public String getOriginalText() {
        return originalText;
    }

    /**
     * Get the SRT subtitle file for the original transcription
     * @return The SRT content, or null if the request failed
     */
    public String getSrtOriginal() {
        return srtOriginal;
    }

    /**
     * Get the translated transcription text
     * @return The translated text, or null if no translation was requested or request failed
     */
    public String getTranslatedText() {
        return translatedText;
    }

    /**
     * Get the SRT subtitle file for the translated transcription
     * @return The translated SRT content, or null if no translation was requested or request failed
     */
    public String getSrtTranslated() {
        return srtTranslated;
    }

    /**
     * Get the error message if the request failed
     * @return The error message, or null if the request was successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "TranscriptionResponse{" +
                "success=" + success +
                ", hasOriginalText=" + (originalText != null) +
                ", hasSrtOriginal=" + (srtOriginal != null) +
                ", hasTranslatedText=" + (translatedText != null) +
                ", hasSrtTranslated=" + (srtTranslated != null) +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}