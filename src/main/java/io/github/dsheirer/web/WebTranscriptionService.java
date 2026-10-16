package io.github.dsheirer.web;

import io.github.dsheirer.gui.SDRTrunk;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Background OpenAI Whisper CLI processing for completed web-console calls. */
public class WebTranscriptionService
{
    private static final Logger mLog = LoggerFactory.getLogger(WebTranscriptionService.class);
    public static final String DEFAULT_PROMPT = "Police radio transcript. Unit numbers: Adam-12, Lincoln-42, " +
        "5-21, 3-14. CAD numbers: 2024-001234. License plates: 7ABC123. Addresses: 1234 Main Street. " +
        "10-codes: 10-4, 10-97, 10-8, 10-7. Phonetic: Adam, Boy, Charles, David, Edward, Frank, George, " +
        "Henry, Ida, John, King, Lincoln, Mary, Nora, Ocean, Paul, Queen, Robert, Sam, Tom, Union, Victor, " +
        "William, X-ray, Yellow, Zebra. Signal codes, badge numbers, and radio callsigns.";
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}[- ]?\\d{2}[- ]?\\d{4}\\b");
    private static final Pattern PHONE = Pattern.compile("\\b(?:\\+?1[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}\\b");
    private static final Pattern DOB = Pattern.compile("(?i)\\b(?:date of birth|DOB|D\\.O\\.B\\.?)\\s*[: ]?\\s*\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}");
    private static final Pattern ADDRESS = Pattern.compile("(?i)\\b\\d{1,6}\\s+(?:[NSEW]\\s+)?[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?\\s+(?:Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Boulevard|Blvd|Highway|Hwy|Lane|Ln|Way|Court|Ct|Circle|Cir|Place|Pl)\\b");
    private static final Map<String,String> PHONETIC = Map.ofEntries(
        Map.entry("adam", "A"), Map.entry("boy", "B"), Map.entry("charles", "C"), Map.entry("david", "D"),
        Map.entry("edward", "E"), Map.entry("frank", "F"), Map.entry("george", "G"), Map.entry("henry", "H"),
        Map.entry("ida", "I"), Map.entry("john", "J"), Map.entry("king", "K"), Map.entry("lincoln", "L"),
        Map.entry("mary", "M"), Map.entry("nora", "N"), Map.entry("ocean", "O"), Map.entry("paul", "P"),
        Map.entry("queen", "Q"), Map.entry("robert", "R"), Map.entry("sam", "S"), Map.entry("tom", "T"),
        Map.entry("union", "U"), Map.entry("victor", "V"), Map.entry("william", "W"), Map.entry("x-ray", "X"),
        Map.entry("yellow", "Y"), Map.entry("zebra", "Z"));
    private static final Pattern PHONETIC_RUN = Pattern.compile("(?i)\\b(?:" + String.join("|", PHONETIC.keySet()) +
        ")(?:[ ,.-]+(?:" + String.join("|", PHONETIC.keySet()) + "))+\\b");
    private static final Pattern PHONETIC_TOKEN = Pattern.compile("(?i)" + String.join("|", PHONETIC.keySet()));
    private final Preferences mPreferences = Preferences.userNodeForPackage(SDRTrunk.class);
    private final ConcurrentLinkedDeque<Map<String,Object>> mTranscripts = new ConcurrentLinkedDeque<>();
    private final Semaphore mWorker = new Semaphore(1);
    private final java.util.concurrent.ExecutorService mExecutor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    private final AtomicReference<String> mLastError = new AtomicReference<>("");
    private final AtomicReference<String> mLastLoggedProblem = new AtomicReference<>("");

    public Map<String,Object> settings()
    {
        boolean enabled = mPreferences.getBoolean("sdrtrunk.whisper.enabled", false);
        ExecutableStatus executableStatus = inspectExecutable(
            mPreferences.get("sdrtrunk.whisper.executable", "whisper"));
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("enabled", enabled);
        value.put("executable", mPreferences.get("sdrtrunk.whisper.executable", "whisper"));
        value.put("available", executableStatus.available());
        value.put("resolvedExecutable", executableStatus.resolvedExecutable());
        value.put("configurationError", executableStatus.available() ? "" : executableStatus.message());
        value.put("lastError", mLastError.get());
        value.put("status", !enabled ? "Disabled" : executableStatus.available() ?
            (mWorker.availablePermits() == 0 ? "Processing" : "Ready") : "Unavailable");
        value.put("model", mPreferences.get("sdrtrunk.whisper.model", "base.en"));
        value.put("language", mPreferences.get("sdrtrunk.whisper.language", "English"));
        value.put("task", mPreferences.get("sdrtrunk.whisper.task", "transcribe"));
        value.put("prompt", mPreferences.get("sdrtrunk.whisper.prompt", DEFAULT_PROMPT));
        value.put("normalize", mPreferences.getBoolean("sdrtrunk.whisper.normalize", true));
        value.put("redact", mPreferences.getBoolean("sdrtrunk.whisper.redact", false));
        value.put("timeoutSeconds", mPreferences.getInt("sdrtrunk.whisper.timeout", 180));
        value.put("city", mPreferences.get("sdrtrunk.whisper.city", ""));
        value.put("repository", "https://github.com/robgwi/whisper");
        value.put("busy", mWorker.availablePermits() == 0);
        return value;
    }

    public void update(Map<String,Object> values)
    {
        putBoolean(values, "enabled", "sdrtrunk.whisper.enabled");
        putBoolean(values, "normalize", "sdrtrunk.whisper.normalize");
        putBoolean(values, "redact", "sdrtrunk.whisper.redact");
        putString(values, "executable", "sdrtrunk.whisper.executable");
        putString(values, "model", "sdrtrunk.whisper.model");
        putString(values, "language", "sdrtrunk.whisper.language");
        putString(values, "task", "sdrtrunk.whisper.task");
        putString(values, "prompt", "sdrtrunk.whisper.prompt");
        putString(values, "city", "sdrtrunk.whisper.city");
        Object timeout = values.get("timeoutSeconds");
        if(timeout instanceof Number number) { mPreferences.putInt("sdrtrunk.whisper.timeout", Math.max(10, number.intValue())); }
        mLastError.set("");
        mLastLoggedProblem.set("");
    }

    private void putBoolean(Map<String,Object> values, String source, String key)
    { if(values.get(source) instanceof Boolean value) { mPreferences.putBoolean(key, value); } }
    private void putString(Map<String,Object> values, String source, String key)
    { if(values.get(source) instanceof String value) { mPreferences.put(key, value); } }

    public List<Map<String,Object>> transcripts() { return new ArrayList<>(mTranscripts); }

    public void submit(byte[] mp3, Map<String,Object> call)
    {
        if(!mPreferences.getBoolean("sdrtrunk.whisper.enabled", false)) { return; }
        ExecutableStatus executableStatus = inspectExecutable(
            mPreferences.get("sdrtrunk.whisper.executable", "whisper"));
        if(!executableStatus.available())
        {
            reportProblem(executableStatus.message(), null);
            return;
        }
        mExecutor.execute(() -> process(mp3, call, executableStatus.resolvedExecutable()));
    }

    private void process(byte[] mp3, Map<String,Object> call, String executable)
    {
        Path directory = null;
        mWorker.acquireUninterruptibly();
        try
        {
            directory = Files.createTempDirectory("sdrtrunk-whisper-");
            Path audio = directory.resolve("call.mp3");
            Files.write(audio, mp3);
            String model = mPreferences.get("sdrtrunk.whisper.model", "base.en");
            List<String> command = new ArrayList<>(List.of(executable, audio.toString(), "--model", model,
                "--output_format", "txt", "--output_dir", directory.toString(), "--language",
                mPreferences.get("sdrtrunk.whisper.language", "English"), "--task",
                mPreferences.get("sdrtrunk.whisper.task", "transcribe"), "--initial_prompt",
                mPreferences.get("sdrtrunk.whisper.prompt", DEFAULT_PROMPT)));
            Path processLog = directory.resolve("whisper.log");
            Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(processLog.toFile()).start();
            int timeout = mPreferences.getInt("sdrtrunk.whisper.timeout", 180);
            if(!process.waitFor(timeout, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("Whisper timed out"); }
            String output = Files.readString(processLog, StandardCharsets.UTF_8);
            if(process.exitValue() != 0) { throw new IllegalStateException(output); }
            String raw = Files.readString(directory.resolve("call.txt"), StandardCharsets.UTF_8).trim();
            String normalized = mPreferences.getBoolean("sdrtrunk.whisper.normalize", true) ? normalize(raw) : raw;
            Map<String,Object> item = new LinkedHashMap<>(call);
            item.put("time", Instant.now().toString());
            boolean redacted = mPreferences.getBoolean("sdrtrunk.whisper.redact", false);
            item.put("text", redacted ? redact(normalized) : normalized);
            if(!redacted) { item.put("raw", raw); }
            item.put("redacted", redacted);
            item.put("model", model);
            mTranscripts.addFirst(item);
            while(mTranscripts.size() > 200) { mTranscripts.pollLast(); }
            mLastError.set("");
            mLastLoggedProblem.set("");
        }
        catch(Exception e) { reportProblem(rootMessage(e), e); }
        finally
        {
            if(directory != null)
            {
                try(var files = Files.list(directory)) { files.forEach(path -> { try { Files.deleteIfExists(path); } catch(Exception ignored) {} }); }
                catch(Exception ignored) {}
                try { Files.deleteIfExists(directory); } catch(Exception ignored) {}
            }
            mWorker.release();
        }
    }

    private void reportProblem(String message, Exception exception)
    {
        String detail = message == null || message.isBlank() ? "Unknown Whisper error" : message;
        mLastError.set(detail);
        if(!detail.equals(mLastLoggedProblem.getAndSet(detail)))
        {
            if(exception == null)
            {
                mLog.warn("Background Whisper transcription is enabled but unavailable: {}", detail);
            }
            else
            {
                mLog.error("Background Whisper transcription failed: {}", detail, exception);
            }
        }
    }

    private static String rootMessage(Throwable throwable)
    {
        Throwable current = throwable;
        while(current.getCause() != null) { current = current.getCause(); }
        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }

    static ExecutableStatus inspectExecutable(String configuredExecutable)
    {
        String configured = configuredExecutable != null ? configuredExecutable.trim() : "";
        if(configured.isEmpty())
        {
            return new ExecutableStatus(false, "", "Whisper executable is not configured");
        }
        if(configured.startsWith("realpath ") || configured.startsWith("which ") || configured.contains("$(") ||
            configured.contains("&&") || configured.contains(";") || configured.contains("\n") ||
            configured.contains("\r"))
        {
            return new ExecutableStatus(false, "", "Enter only the Whisper executable path, not a shell command");
        }

        if((configured.startsWith("\"") && configured.endsWith("\"")) ||
            (configured.startsWith("'") && configured.endsWith("'")))
        {
            configured = configured.substring(1, configured.length() - 1);
        }
        String home = System.getProperty("user.home", "");
        if(configured.equals("~")) { configured = home; }
        else if(configured.startsWith("~/")) { configured = home + configured.substring(1); }
        configured = configured.replace("${HOME}", home).replace("$HOME", home);

        if(configured.contains("/") || configured.contains("\\"))
        {
            try
            {
                Path path = Path.of(configured).toAbsolutePath().normalize();
                if(!Files.isRegularFile(path))
                {
                    return new ExecutableStatus(false, path.toString(),
                        "Whisper executable was not found: " + path);
                }
                if(!Files.isExecutable(path))
                {
                    return new ExecutableStatus(false, path.toString(),
                        "Whisper executable is not executable: " + path);
                }
                return new ExecutableStatus(true, path.toString(), "Ready");
            }
            catch(Exception e)
            {
                return new ExecutableStatus(false, "", "Invalid Whisper executable path: " + configured);
            }
        }

        String pathValue = System.getenv("PATH");
        if(pathValue != null)
        {
            List<String> extensions = System.getProperty("os.name", "").toLowerCase().contains("win") ?
                Arrays.asList("", ".exe", ".cmd", ".bat") : List.of("");
            for(String directory: pathValue.split(Pattern.quote(System.getProperty("path.separator"))))
            {
                for(String extension: extensions)
                {
                    try
                    {
                        Path candidate = Path.of(directory, configured + extension).toAbsolutePath().normalize();
                        if(Files.isRegularFile(candidate) && Files.isExecutable(candidate))
                        {
                            return new ExecutableStatus(true, candidate.toString(), "Ready");
                        }
                    }
                    catch(Exception ignored) {}
                }
            }
        }
        return new ExecutableStatus(false, configured,
            "Whisper executable '" + configured + "' was not found on PATH");
    }

    record ExecutableStatus(boolean available, String resolvedExecutable, String message) {}

    static String normalize(String text)
    {
        Matcher phoneticMatcher = PHONETIC_RUN.matcher(text);
        StringBuilder phoneticResult = new StringBuilder();
        while(phoneticMatcher.find())
        {
            StringBuilder letters = new StringBuilder();
            Matcher tokenMatcher = PHONETIC_TOKEN.matcher(phoneticMatcher.group());
            while(tokenMatcher.find())
            { letters.append(PHONETIC.get(tokenMatcher.group().toLowerCase())); }
            phoneticMatcher.appendReplacement(phoneticResult, Matcher.quoteReplacement(letters.toString()));
        }
        phoneticMatcher.appendTail(phoneticResult);
        String value = phoneticResult.toString();
        for(int count = 6; count >= 3; count--)
        {
            StringBuilder pattern = new StringBuilder("\\b");
            StringBuilder replacement = new StringBuilder();
            for(int index = 1; index <= count; index++)
            { if(index > 1) { pattern.append("\\s+"); } pattern.append("(\\d)"); replacement.append("$").append(index); }
            pattern.append("\\b"); value = value.replaceAll(pattern.toString(), replacement.toString());
        }
        value = value.replaceAll("(?i)\\b([0-9])\\s*([A-Z]{2,3})\\s*([0-9]{3,4})\\b", "$1$2$3");
        return value.replaceAll("\\s{2,}", " ").trim();
    }

    static String redact(String text)
    {
        String value = SSN.matcher(text).replaceAll("[REDACTED:SSN]");
        value = PHONE.matcher(value).replaceAll("[REDACTED:PHONE]");
        value = DOB.matcher(value).replaceAll("[REDACTED:DOB]");
        return ADDRESS.matcher(value).replaceAll("[REDACTED:ADDRESS]");
    }
}
