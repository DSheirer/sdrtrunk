package io.github.dsheirer.audio.broadcast.remote;

/** Result of optional speech processing for a completed call. */
public record CallTranscription(String provider, String model, String text, boolean translatedToEnglish)
{
    public static CallTranscription none()
    {
        return new CallTranscription("none", "", "", false);
    }
}
