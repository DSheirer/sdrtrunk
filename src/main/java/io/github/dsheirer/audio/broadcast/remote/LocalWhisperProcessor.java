package io.github.dsheirer.audio.broadcast.remote;

import io.github.dsheirer.util.ThreadPool;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Local whisper.cpp command-line provider. */
public class LocalWhisperProcessor implements SpeechProcessor
{
    private final String mExecutable;
    private final String mModel;
    private final boolean mTranslateToEnglish;
    private final Duration mTimeout;

    public LocalWhisperProcessor(String executable, String model, boolean translateToEnglish, Duration timeout)
    {
        mExecutable = executable;
        mModel = model;
        mTranslateToEnglish = translateToEnglish;
        mTimeout = timeout;
    }

    @Override
    public CompletableFuture<CallTranscription> process(Path audioPath)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            Path outputBase = null;
            try
            {
                outputBase = Files.createTempFile("sdrtrunk-whisper-", "");
                Files.deleteIfExists(outputBase);
                List<String> command = new ArrayList<>(List.of(mExecutable, "-m", mModel, "-f",
                    audioPath.toString(), "-otxt", "-of", outputBase.toString(), "-np"));
                if(mTranslateToEnglish)
                {
                    command.add("-tr");
                }
                Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                boolean finished = process.waitFor(mTimeout.toMillis(), TimeUnit.MILLISECONDS);
                if(!finished)
                {
                    process.destroyForcibly();
                    throw new IllegalStateException("Local Whisper processing timed out");
                }
                if(process.exitValue() != 0)
                {
                    String details = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    throw new IllegalStateException("Local Whisper failed: " + details);
                }
                Path output = Path.of(outputBase + ".txt");
                String text = Files.readString(output, StandardCharsets.UTF_8).trim();
                Files.deleteIfExists(output);
                return new CallTranscription("local", Path.of(mModel).getFileName().toString(), text,
                    mTranslateToEnglish);
            }
            catch(Exception e)
            {
                throw new IllegalStateException("Local Whisper processing failed", e);
            }
            finally
            {
                if(outputBase != null)
                {
                    try { Files.deleteIfExists(outputBase); } catch(Exception ignored) {}
                }
            }
        }, ThreadPool.CACHED);
    }
}
