package io.github.dsheirer.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebTranscriptionServiceTest
{
    @Test
    void normalizesScannerNumbersAndPhonetics()
    {
        assertEquals("Unit AB plate 7ABC123", WebTranscriptionService.normalize(
            "Unit Adam Boy plate 7 ABC 123"));
        assertEquals("XY", WebTranscriptionService.normalize("X-ray Yellow"));
    }

    @Test
    void redactsSensitiveInformation()
    {
        String redacted = WebTranscriptionService.redact(
            "DOB 01/02/1980, phone 312-555-1212, SSN 123-45-6789 at 1234 Main Street");
        assertFalse(redacted.contains("1980"));
        assertFalse(redacted.contains("555"));
        assertFalse(redacted.contains("6789"));
        assertFalse(redacted.contains("Main Street"));
    }

    @Test
    void rejectsShellCommandInsteadOfLoggingAnErrorForEveryCall()
    {
        WebTranscriptionService.ExecutableStatus status = WebTranscriptionService.inspectExecutable(
            "realpath \"$HOME/whisper-env/bin/whisper\"whisper");

        assertFalse(status.available());
        assertEquals("Enter only the Whisper executable path, not a shell command", status.message());
    }

    @Test
    void acceptsAnExecutableWithAFullPath(@TempDir Path temporaryDirectory) throws Exception
    {
        Path executable = Files.writeString(temporaryDirectory.resolve("whisper-test"), "test");
        executable.toFile().setExecutable(true);
        assumeTrue(Files.isExecutable(executable));

        WebTranscriptionService.ExecutableStatus status =
            WebTranscriptionService.inspectExecutable(executable.toString());

        assertTrue(status.available());
        assertEquals(executable.toAbsolutePath().normalize().toString(), status.resolvedExecutable());
    }
}
