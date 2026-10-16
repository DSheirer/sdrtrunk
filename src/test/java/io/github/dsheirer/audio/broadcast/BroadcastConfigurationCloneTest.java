package io.github.dsheirer.audio.broadcast;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallConfiguration;
import io.github.dsheirer.audio.broadcast.broadcastify.TalkerAliasSource;
import io.github.dsheirer.audio.broadcast.openmhz.OpenMHzConfiguration;
import io.github.dsheirer.playlist.PlaylistV2;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BroadcastConfigurationCloneTest
{
    @Test
    void broadcastifyCallCloneKeepsConnectionSettingsAndStartsDisabled()
    {
        BroadcastifyCallConfiguration source = new BroadcastifyCallConfiguration();
        source.setName("County Calls");
        source.setHost("https://example.test/calls");
        source.setSystemID(42);
        source.setApiKey("0123456789abcdef");
        source.setMaximumRecordingAge(123_000);
        source.setTestEnabled(true);
        source.setTestInterval(30);
        source.setTalkerAliasSource(TalkerAliasSource.PLAYLIST);
        source.setStoreTalkerAliases(true);
        source.setEnabled(true);

        BroadcastifyCallConfiguration copy = (BroadcastifyCallConfiguration)source.copyOfDisabled();

        assertEquals(source.getName(), copy.getName());
        assertEquals(source.getHost(), copy.getHost());
        assertEquals(source.getSystemID(), copy.getSystemID());
        assertEquals(source.getApiKey(), copy.getApiKey());
        assertEquals(source.getMaximumRecordingAge(), copy.getMaximumRecordingAge());
        assertEquals(source.getTestInterval(), copy.getTestInterval());
        assertEquals(TalkerAliasSource.PLAYLIST, copy.getTalkerAliasSource());
        assertEquals(source.isStoreTalkerAliases(), copy.isStoreTalkerAliases());
        assertFalse(copy.isEnabled());
    }

    @Test
    void openMHzCloneKeepsConnectionSettingsAndStartsDisabled()
    {
        OpenMHzConfiguration source = new OpenMHzConfiguration();
        source.setName("Metro");
        source.setHost("https://api.example.test");
        source.setSystemName("metro-system");
        source.setApiKey("long-api-key");
        source.setMaximumRecordingAge(456_000);
        source.setEnabled(true);

        OpenMHzConfiguration copy = (OpenMHzConfiguration)source.copyOfDisabled();

        assertEquals(source.getName(), copy.getName());
        assertEquals(source.getHost(), copy.getHost());
        assertEquals(source.getSystemName(), copy.getSystemName());
        assertEquals(source.getApiKey(), copy.getApiKey());
        assertEquals(source.getMaximumRecordingAge(), copy.getMaximumRecordingAge());
        assertFalse(copy.isEnabled());
    }

    @Test
    void broadcastifyTalkerAliasSettingsPersistInPlaylist() throws Exception
    {
        BroadcastifyCallConfiguration configuration = new BroadcastifyCallConfiguration();
        configuration.setName("County Calls");
        configuration.setHost("https://example.test/calls");
        configuration.setSystemID(42);
        configuration.setApiKey("key");
        configuration.setTalkerAliasSource(TalkerAliasSource.PLAYLIST);
        configuration.setStoreTalkerAliases(true);
        PlaylistV2 playlist = new PlaylistV2();
        playlist.setBroadcastConfigurations(List.of(configuration));
        JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(false);
        ObjectMapper mapper = new XmlMapper(module)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String xml = mapper.writeValueAsString(playlist);
        PlaylistV2 restored = mapper.readValue(xml, PlaylistV2.class);
        BroadcastifyCallConfiguration restoredConfiguration = (BroadcastifyCallConfiguration)
            restored.getBroadcastConfigurations().getFirst();

        assertEquals(TalkerAliasSource.PLAYLIST, restoredConfiguration.getTalkerAliasSource());
        assertTrue(restoredConfiguration.isStoreTalkerAliases());
    }
}
