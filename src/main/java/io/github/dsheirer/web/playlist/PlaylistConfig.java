/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.web.playlist;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.action.AliasAction;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.web.api.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Editable representations of channels and aliases, and their versions.  The nested configurations use the field
 * names of the playlist file (see {@link PlaylistJson}).
 */
public final class PlaylistConfig
{
    private PlaylistConfig()
    {
    }

    /**
     * A channel's configuration.  On create and update the id is ignored.
     * @param autoStart start the channel when sdrtrunk starts
     * @param autoStartOrder order among the auto-start channels, or null
     * @param source source configuration, for example {"type": "sourceConfigTuner", "frequency": 453850000}
     * @param decode decoder configuration, for example {"type": "decodeConfigNBFM", "bandwidth": "BW_12_5"}
     * @param auxDecode auxiliary decoders, or null for none
     * @param eventLog event log configuration, or null for none
     * @param record recording configuration, or null for none
     */
    public record ChannelConfig(Integer id, String name, String system, String site, String aliasList,
                                Boolean autoStart, Integer autoStartOrder, Boolean autoAddTalkerAliases,
                                JsonNode source, JsonNode decode, JsonNode auxDecode, JsonNode eventLog,
                                JsonNode record)
    {
        public static ChannelConfig of(Channel channel)
        {
            return new ChannelConfig(channel.getChannelID(), channel.getName(), channel.getSystem(),
                channel.getSite(), channel.getAliasListName(), channel.isAutoStart(), channel.getAutoStartOrder(),
                channel.isAutoAddTalkerAliases(), PlaylistJson.toTree(channel.getSourceConfiguration()),
                PlaylistJson.toTree(channel.getDecodeConfiguration()),
                PlaylistJson.toTree(channel.getAuxDecodeConfiguration()),
                PlaylistJson.toTree(channel.getEventLogConfiguration()),
                PlaylistJson.toTree(channel.getRecordConfiguration()));
        }

        /**
         * Validates and parses this configuration.
         * @throws ApiException 400 listing every invalid field
         */
        Parsed parse()
        {
            List<ApiException.FieldError> errors = new ArrayList<>();

            if(name == null || name.isBlank())
            {
                errors.add(new ApiException.FieldError("name", "is required"));
            }

            if(autoStartOrder != null && autoStartOrder < 0)
            {
                errors.add(new ApiException.FieldError("autoStartOrder", "must be 0 or more"));
            }

            if(source == null || source.isNull())
            {
                errors.add(new ApiException.FieldError("source", "is required"));
            }

            if(decode == null || decode.isNull())
            {
                errors.add(new ApiException.FieldError("decode", "is required"));
            }

            SourceConfiguration sourceConfiguration = parse(source, SourceConfiguration.class, "source", errors);
            DecodeConfiguration decodeConfiguration = parse(decode, DecodeConfiguration.class, "decode", errors);
            AuxDecodeConfiguration aux = parse(auxDecode, AuxDecodeConfiguration.class, "auxDecode", errors);
            EventLogConfiguration log = parse(eventLog, EventLogConfiguration.class, "eventLog", errors);
            RecordConfiguration rec = parse(record, RecordConfiguration.class, "record", errors);

            if(!errors.isEmpty())
            {
                throw ApiException.invalid(errors);
            }

            return new Parsed(this, sourceConfiguration, decodeConfiguration,
                aux != null ? aux : new AuxDecodeConfiguration(), log != null ? log : new EventLogConfiguration(),
                rec != null ? rec : new RecordConfiguration());
        }

        private static <T> T parse(JsonNode node, Class<T> type, String field, List<ApiException.FieldError> errors)
        {
            try
            {
                return PlaylistJson.fromTree(node, type, field);
            }
            catch(ApiException e)
            {
                errors.addAll(e.getErrors());
                return null;
            }
        }
    }

    /**
     * Parsed channel configuration, ready to apply
     */
    record Parsed(ChannelConfig config, SourceConfiguration source, DecodeConfiguration decode,
                  AuxDecodeConfiguration auxDecode, EventLogConfiguration eventLog, RecordConfiguration record)
    {
        /**
         * Copies the configuration to the channel.
         */
        void applyTo(Channel channel)
        {
            channel.setName(config.name().trim());
            channel.setSystem(blankToNull(config.system()));
            channel.setSite(blankToNull(config.site()));
            channel.setAliasListName(blankToNull(config.aliasList()));
            channel.setAutoStart(config.autoStart() != null && config.autoStart());
            channel.setAutoStartOrder(config.autoStartOrder() == null || config.autoStartOrder() < 1 ? null :
                config.autoStartOrder());
            channel.setAutoAddTalkerAliases(config.autoAddTalkerAliases() != null && config.autoAddTalkerAliases());
            channel.setSourceConfiguration(source);
            channel.setDecodeConfiguration(decode);
            channel.setAuxDecodeConfiguration(auxDecode);
            channel.setEventLogConfiguration(eventLog);
            channel.setRecordConfiguration(record);
        }
    }

    /**
     * An alias.  On create and update the id is ignored.
     * @param list alias list that the alias belongs to
     * @param color as #rrggbb
     * @param streamTalkgroupAlias talkgroup to stream the alias as, or null
     * @param ids alias identifiers, for example [{"type": "talkgroup", "value": 1201, "protocol": "APCO25"},
     * {"type": "priority", "priority": 2}, {"type": "record"}]
     * @param actions alias actions, for example [{"type": "beepAction", ...}]
     */
    public record AliasConfig(Long id, String name, String list, String group, String color, String iconName,
                              Integer streamTalkgroupAlias, JsonNode ids, JsonNode actions)
    {
        public static AliasConfig of(long id, Alias alias)
        {
            return new AliasConfig(id, alias.getName(), alias.getAliasListName(), alias.getGroup(),
                String.format(Locale.ROOT, "#%06x", alias.getColor() & 0xFFFFFF), alias.getIconName(),
                alias.getStreamTalkgroupAlias() != null ? alias.getStreamTalkgroupAlias().getValue() : null,
                PlaylistJson.toTree(new ArrayList<>(alias.getAliasIdentifiers())),
                PlaylistJson.toTree(new ArrayList<>(alias.getAliasActions())));
        }

        /**
         * Validates and copies this configuration to the alias.
         * @throws ApiException 400 listing every invalid field
         */
        void applyTo(Alias alias)
        {
            List<ApiException.FieldError> errors = new ArrayList<>();

            if(name == null || name.isBlank())
            {
                errors.add(new ApiException.FieldError("name", "is required"));
            }

            if(list == null || list.isBlank())
            {
                errors.add(new ApiException.FieldError("list", "is required"));
            }

            Integer parsedColor = null;

            if(color != null && !color.isBlank())
            {
                try
                {
                    parsedColor = 0xFF000000 | Integer.parseInt(color.trim().replace("#", ""), 16);
                }
                catch(NumberFormatException nfe)
                {
                    errors.add(new ApiException.FieldError("color", "must be #rrggbb"));
                }
            }

            List<AliasID> aliasIds = parseList(ids, AliasID[].class, "ids", errors);
            List<AliasAction> aliasActions = parseList(actions, AliasAction[].class, "actions", errors);

            if(!errors.isEmpty())
            {
                throw ApiException.invalid(errors);
            }

            alias.setName(name.trim());
            alias.setAliasListName(list.trim());
            alias.setGroup(blankToNull(group));

            if(parsedColor != null)
            {
                alias.setColor(parsedColor);
            }

            alias.setIconName(blankToNull(iconName));

            alias.setStreamTalkgroupAlias(streamTalkgroupAlias != null ?
                new io.github.dsheirer.alias.id.talkgroup.StreamAsTalkgroup(streamTalkgroupAlias) : null);

            alias.setAliasIdentifiers(aliasIds);
            alias.setAliasActions(aliasActions);
        }

        private static <T> List<T> parseList(JsonNode node, Class<T[]> type, String field,
                                             List<ApiException.FieldError> errors)
        {
            try
            {
                T[] parsed = PlaylistJson.fromTree(node, type, field);
                return parsed == null ? new ArrayList<>() : new ArrayList<>(List.of(parsed));
            }
            catch(ApiException e)
            {
                errors.addAll(e.getErrors());
                return new ArrayList<>();
            }
        }
    }

    /**
     * Version (ETag) of the channel's configuration.  The desktop channel editor uses this to notice edits made
     * elsewhere.
     */
    public static String version(Channel channel)
    {
        return PlaylistJson.version(ChannelConfig.of(channel));
    }

    /**
     * Version (ETag) of the alias.  The id isn't part of the version, so the desktop alias editor can compute it.
     */
    public static String version(Alias alias)
    {
        return PlaylistJson.version(AliasConfig.of(0, alias));
    }

    private static String blankToNull(String text)
    {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
