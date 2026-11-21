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
package io.github.dsheirer.record;

import io.github.dsheirer.call.CallManager;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.util.StringUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the subfolder of the recordings folder for a call recording, from the folder layout preference.
 */
public final class RecordingFolders
{
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH");
    private static final int MAX_FOLDER_NAME_LENGTH = 64;
    static final String UNKNOWN_SYSTEM = "Unknown-System";
    static final String UNKNOWN_SITE = "Unknown-Site";
    static final String UNKNOWN_CHANNEL = "Unknown-Channel";
    static final String UNKNOWN_TALKGROUP = "Unknown";

    private RecordingFolders()
    {
    }

    /**
     * Folder names of a call, extracted from its identifiers
     * @param system name from the channel configuration
     * @param site name from the channel configuration
     * @param channel name from the channel configuration
     * @param talkgroup folder name of the TO talkgroup or radio (see {@link #getTalkgroupFolder})
     */
    public record CallFolders(String system, String site, String channel, String talkgroup)
    {
        /**
         * Extracts the folder names from the identifiers of a call
         */
        public static CallFolders from(IdentifierCollection identifiers)
        {
            if(identifiers == null)
            {
                return new CallFolders(null, null, null, null);
            }

            String channel = getConfiguration(identifiers, Form.CHANNEL);
            return new CallFolders(getConfiguration(identifiers, Form.SYSTEM), getConfiguration(identifiers, Form.SITE),
                channel, getTalkgroupFolder(identifiers, channel));
        }
    }

    /**
     * Folder for the recording, below the recordings folder.
     * @param root recordings folder
     * @param layout of the subfolders
     * @param hourFolder to add an hour folder (00 - 23) below the date folder
     * @param folders of the call
     * @param timestamp of the call start
     */
    public static Path getFolder(Path root, RecordingFolderLayout layout, boolean hourFolder, CallFolders folders,
                                 long timestamp)
    {
        Path folder = root;

        for(String name: getFolderNames(layout, hourFolder, folders, timestamp))
        {
            folder = folder.resolve(name);
        }

        return folder;
    }

    /**
     * Example recording folder path for the layout, for display in the preferences
     * @return e.g. Metro/2026-11-22/TG_1001
     */
    public static String getExample(RecordingFolderLayout layout, boolean hourFolder)
    {
        long time = LocalDateTime.of(2026, 11, 22, 14, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return String.join("/", getFolderNames(layout, hourFolder,
            new CallFolders("Metro", "Site 1", "Control A", "TG_1001"), time));
    }

    /**
     * Subfolder names for the recording, outermost first.
     */
    static List<String> getFolderNames(RecordingFolderLayout layout, boolean hourFolder, CallFolders folders,
                                       long timestamp)
    {
        List<String> names = new ArrayList<>();

        if(layout == null || layout == RecordingFolderLayout.NONE)
        {
            return names;
        }

        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        String date = DATE.format(time);
        String system = clean(folders.system(), UNKNOWN_SYSTEM);

        switch(layout)
        {
            case DATE -> names.add(date);
            case DATE_SYSTEM ->
            {
                names.add(date);
                names.add(system);
            }
            case SYSTEM_DATE ->
            {
                names.add(system);
                names.add(date);
            }
            case SYSTEM_SITE_DATE ->
            {
                names.add(system);
                names.add(clean(folders.site(), UNKNOWN_SITE));
                names.add(date);
            }
            case SYSTEM_CHANNEL_DATE ->
            {
                names.add(system);
                names.add(clean(folders.channel(), UNKNOWN_CHANNEL));
                names.add(date);
            }
            case SYSTEM_TALKGROUP_DATE ->
            {
                names.add(system);
                names.add(clean(folders.talkgroup(), UNKNOWN_TALKGROUP));
                names.add(date);
            }
            case SYSTEM_DATE_TALKGROUP ->
            {
                names.add(system);
                names.add(date);
                names.add(clean(folders.talkgroup(), UNKNOWN_TALKGROUP));
            }
            case TALKGROUP_DATE ->
            {
                names.add(clean(folders.talkgroup(), UNKNOWN_TALKGROUP));
                names.add(date);
            }
            default -> {}
        }

        //The hour folder goes directly below the date folder
        if(hourFolder)
        {
            names.add(names.indexOf(date) + 1, HOUR.format(time));
        }

        return names;
    }

    /**
     * Folder name for the talkgroup or radio of a call: the TO talkgroup (TG_1001), patch group (PATCH_500) or radio
     * of a private call (RADIO_1234), else the FROM radio (e.g. a conventional channel with radio IDs), else the
     * channel name (a conventional channel without identifiers), else null.  The plain identifier value is used, not
     * the alias name or the talkgroup display format, so that the folder doesn't change when they change.
     */
    static String getTalkgroupFolder(IdentifierCollection identifiers, String channel)
    {
        String to = getIdentifierFolder(identifiers.getToIdentifier());

        if(to != null)
        {
            return to;
        }

        String from = getIdentifierFolder(identifiers.getFromIdentifier());

        if(from != null)
        {
            return from;
        }

        return channel;
    }

    private static String getIdentifierFolder(Identifier identifier)
    {
        if(identifier == null || identifier.getValue() == null)
        {
            return null;
        }

        Long value = CallManager.getValue(identifier);

        return switch(identifier.getForm())
        {
            case TALKGROUP -> value != null ? "TG_" + value : null;
            case PATCH_GROUP -> value != null ? "PATCH_" + value : null;
            case RADIO -> value != null ? "RADIO_" + value : null;
            case TONE, TALKER_ALIAS -> null;
            default -> identifier.toString();
        };
    }

    private static String getConfiguration(IdentifierCollection identifiers, Form form)
    {
        Identifier identifier = identifiers.getIdentifier(IdentifierClass.CONFIGURATION, form, Role.ANY);
        return identifier != null && identifier.getValue() != null ? identifier.getValue().toString() : null;
    }

    /**
     * Folder name with illegal characters replaced, no leading or trailing dots or spaces (not allowed on Windows),
     * and a maximum length.  Reserved Windows device names get a trailing underscore.
     */
    static String clean(String name, String fallback)
    {
        if(name == null || name.isBlank())
        {
            return fallback;
        }

        String cleaned = StringUtils.replaceIllegalCharacters(name.trim());

        if(cleaned.length() > MAX_FOLDER_NAME_LENGTH)
        {
            cleaned = cleaned.substring(0, MAX_FOLDER_NAME_LENGTH);
        }

        cleaned = cleaned.replaceAll("^[.\\s]+|[.\\s]+$", "");

        if(cleaned.isEmpty())
        {
            return fallback;
        }

        if(cleaned.toUpperCase(Locale.ROOT).matches("CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9]"))
        {
            cleaned = cleaned + "_";
        }

        return cleaned;
    }
}
