package io.github.dsheirer.audio.broadcast.remote;

import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Hosted OpenAI whisper-1 transcription or English translation provider. */
public class OpenAiWhisperProcessor implements SpeechProcessor
{
    private static final URI TRANSCRIPTIONS = URI.create("https://api.openai.com/v1/audio/transcriptions");
    private static final URI TRANSLATIONS = URI.create("https://api.openai.com/v1/audio/translations");
    private final HttpClient mClient;
    private final String mApiKeyEnvironmentVariable;
    private final boolean mTranslateToEnglish;
    private final Duration mTimeout;

    public OpenAiWhisperProcessor(String apiKeyEnvironmentVariable, boolean translateToEnglish, Duration timeout)
    {
        mApiKeyEnvironmentVariable = apiKeyEnvironmentVariable;
        mTranslateToEnglish = translateToEnglish;
        mTimeout = timeout;
        mClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public CompletableFuture<CallTranscription> process(Path audioPath)
    {
        String apiKey = System.getenv(mApiKeyEnvironmentVariable);
        if(apiKey == null || apiKey.isBlank())
        {
            return CompletableFuture.failedFuture(new IllegalStateException(
                "OpenAI API key environment variable is not set: " + mApiKeyEnvironmentVariable));
        }

        try
        {
            MultipartBody body = new MultipartBody()
                .text("model", "whisper-1")
                .text("response_format", "json")
                .file("file", audioPath.getFileName().toString(), "audio/mpeg", Files.readAllBytes(audioPath));
            HttpRequest request = HttpRequest.newBuilder(mTranslateToEnglish ? TRANSLATIONS : TRANSCRIPTIONS)
                .timeout(mTimeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + body.boundary())
                .POST(body.publisher())
                .build();

            return mClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response ->
            {
                if(response.statusCode() < 200 || response.statusCode() >= 300)
                {
                    throw new IllegalStateException("OpenAI audio request failed with HTTP " + response.statusCode());
                }
                String text = JsonParser.parseString(response.body()).getAsJsonObject().get("text").getAsString();
                return new CallTranscription("openai", "whisper-1", text, mTranslateToEnglish);
            });
        }
        catch(Exception e)
        {
            return CompletableFuture.failedFuture(e);
        }
    }
}
