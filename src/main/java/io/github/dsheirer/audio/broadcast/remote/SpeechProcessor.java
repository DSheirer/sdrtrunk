package io.github.dsheirer.audio.broadcast.remote;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Pluggable local or hosted completed-call speech processor. */
public interface SpeechProcessor
{
    CompletableFuture<CallTranscription> process(Path audioPath);
}
