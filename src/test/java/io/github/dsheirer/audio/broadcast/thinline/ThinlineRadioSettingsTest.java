package io.github.dsheirer.audio.broadcast.thinline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xEmbeddedTuner;
import io.github.dsheirer.source.tuner.rtl.r8x.r820t.R820TTunerConfiguration;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ThinlineRadioSettingsTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode field(ArrayNode fields, String name)
    {
        for(JsonNode field: fields)
        {
            if(name.equals(field.path("name").asText()))
            {
                return field;
            }
        }
        return null;
    }

    @Test
    public void tunerSettingsDescribeAndApply() throws Exception
    {
        R820TTunerConfiguration configuration = new R820TTunerConfiguration("test");
        ArrayNode fields = ThinlineRadioSettings.describe(configuration, Set.of("uniqueID"), Set.of(), false);

        JsonNode gain = field(fields, "masterGain");
        assertEquals("enum", gain.path("kind").asText());
        assertTrue(gain.path("options").size() > 2);
        assertTrue(field(fields, "uniqueID").path("readOnly").asBoolean());
        assertEquals("boolean", field(fields, "biasT").path("kind").asText());
        assertEquals("number", field(fields, "frequencyCorrection").path("kind").asText());

        ObjectNode values = MAPPER.createObjectNode();
        values.put("masterGain", R8xEmbeddedTuner.MasterGain.MANUAL.name());
        values.put("frequencyCorrection", 1.5);
        values.put("biasT", true);
        values.put("uniqueID", "changed");
        ThinlineRadioSettings.apply(configuration, values, Set.of("uniqueID"), Set.of());

        assertEquals(R8xEmbeddedTuner.MasterGain.MANUAL, configuration.getMasterGain());
        assertEquals(1.5, configuration.getFrequencyCorrection());
        assertTrue(configuration.isBiasT());
        assertEquals("test", configuration.getUniqueID());
    }

    @Test
    public void streamSettingsDescribeAndApply() throws Exception
    {
        ThinlineRadioCoreConfiguration configuration = new ThinlineRadioCoreConfiguration();
        configuration.setName("Core");
        ArrayNode fields = ThinlineRadioSettings.describe(configuration, Set.of("name"), Set.of(), false);
        System.out.println("stream fields: " + fields);
        assertFalse(fields.isEmpty());

        ObjectNode values = MAPPER.createObjectNode();
        values.put("enabled", !configuration.isEnabled());
        ThinlineRadioSettings.apply(configuration, values, Set.of("name"), Set.of());
        assertEquals(values.path("enabled").asBoolean(), configuration.isEnabled());
    }

    @Test
    public void preferencePagesDescribe() throws Exception
    {
        io.github.dsheirer.preference.UserPreferences preferences = new io.github.dsheirer.preference.UserPreferences();
        for(java.lang.reflect.Method method: preferences.getClass().getMethods())
        {
            if(method.getParameterCount() == 0 &&
                io.github.dsheirer.preference.Preference.class.isAssignableFrom(method.getReturnType()))
            {
                Object preference = method.invoke(preferences);
                ArrayNode fields = ThinlineRadioSettings.describe(preference, Set.of(), Set.of("password"), true);
                StringBuilder names = new StringBuilder();
                fields.forEach(field -> names.append(field.path("name").asText()).append('=').append(field.path("kind").asText()).append(' '));
                System.out.println("preference " + preference.getClass().getSimpleName() + ": " + names);
            }
        }
    }

    @Test
    public void channelSettingsNested() throws Exception
    {
        Channel channel = new Channel("Control");
        channel.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(DecoderType.P25_PHASE1));
        ArrayNode fields = ThinlineRadioSettings.describe(channel, Set.of("channelID"), Set.of(), false);
        System.out.println("channel fields: " + fields);
        JsonNode decode = field(fields, "decodeConfiguration");
        assertEquals("object", decode.path("kind").asText());
        JsonNode loggers = field((ArrayNode)field(fields, "eventLogConfiguration").path("fields"), "loggers");
        assertEquals("enumList", loggers.path("kind").asText());
        assertTrue(loggers.path("options").size() > 1);

        ObjectNode eventLog = MAPPER.createObjectNode();
        eventLog.putArray("loggers").add("CALL_EVENT");
        ObjectNode nested = MAPPER.createObjectNode();
        nested.set("eventLogConfiguration", eventLog);
        ThinlineRadioSettings.apply(channel, nested, Set.of("channelID"), Set.of());
        assertEquals(1, channel.getEventLogConfiguration().getLoggers().size());

        ObjectNode values = MAPPER.createObjectNode();
        values.put("site", "Avalon");
        ThinlineRadioSettings.apply(channel, values, Set.of("channelID"), Set.of());
        assertEquals("Avalon", channel.getSite());
    }
}
