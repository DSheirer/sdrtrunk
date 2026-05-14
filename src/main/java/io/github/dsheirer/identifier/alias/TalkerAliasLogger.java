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
package io.github.dsheirer.identifier.alias;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists observed P25 talker aliases to a CSV file and bootstraps them back on startup.
 */
public class TalkerAliasLogger
{
    private static final Logger mLog = LoggerFactory.getLogger(TalkerAliasLogger.class);

    private final Path mLogDirectory;
    private final String mSystemName;
    private String mLastWrittenContent = null;

    /**
     * Constructs an instance.
     * @param logDirectory where the alias CSV file is stored
     * @param systemName used as the filename prefix
     */
    public TalkerAliasLogger(Path logDirectory, String systemName)
    {
        mLogDirectory = logDirectory;
        mSystemName = systemName;
    }

    /**
     * Called whenever the alias map changes. Writes updated aliases to disk if the content has changed.
     * @param aliases current alias map (radioId -> TalkerAliasIdentifier)
     */
    public void onAliasUpdate(Map<Integer, TalkerAliasIdentifier> aliases)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("RADIO_ID,TALKER_ALIAS\n");

        for(Map.Entry<Integer, TalkerAliasIdentifier> entry : aliases.entrySet())
        {
            String aliasText = entry.getValue().getValue();
            if(aliasText == null)
            {
                aliasText = "";
            }
            // Strip commas from alias text to keep CSV valid
            aliasText = aliasText.replace(",", "");
            sb.append(entry.getKey()).append(",").append(aliasText).append("\n");
        }

        String content = sb.toString();

        if(!content.equals(mLastWrittenContent))
        {
            Path aliasFile = mLogDirectory.resolve(mSystemName + "_talker_aliases.csv");
            try
            {
                Files.writeString(aliasFile, content, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                mLastWrittenContent = content;
            }
            catch(IOException e)
            {
                mLog.error("Error writing talker alias file [" + aliasFile + "]", e);
            }
        }
    }

    /**
     * Reads previously persisted aliases and preloads them into the alias manager.
     * @param manager to preload
     */
    public void bootstrap(TalkerAliasManager manager)
    {
        Path aliasFile = mLogDirectory.resolve(mSystemName + "_talker_aliases.csv");

        List<String> lines;
        try
        {
            lines = Files.readAllLines(aliasFile);
        }
        catch(NoSuchFileException e)
        {
            return;
        }
        catch(IOException e)
        {
            mLog.warn("Could not read talker alias bootstrap file [" + aliasFile + "]", e);
            return;
        }

        Map<Integer, TalkerAliasIdentifier> loaded = new HashMap<>();
        boolean firstLine = true;

        for(String line : lines)
        {
            if(firstLine)
            {
                firstLine = false;
                continue; // skip header
            }

            line = line.trim();
            if(line.isEmpty())
            {
                continue;
            }

            String[] parts = line.split(",", 2);
            if(parts.length < 2)
            {
                continue;
            }

            try
            {
                int radioId = Integer.parseInt(parts[0].trim());
                String aliasText = parts[1].trim();

                // Strip "TA-" prefix if present (stored by P25TalkerAliasIdentifier.toString())
                if(aliasText.startsWith("TA-"))
                {
                    aliasText = aliasText.substring(3);
                }

                P25TalkerAliasIdentifier identifier = P25TalkerAliasIdentifier.create(aliasText);
                loaded.put(radioId, identifier);
            }
            catch(NumberFormatException e)
            {
                mLog.debug("Skipping invalid line in talker alias file: " + line);
            }
        }

        if(!loaded.isEmpty())
        {
            manager.preload(loaded);
            mLog.info("Preloaded " + loaded.size() + " talker aliases for system [" + mSystemName + "]");
        }
    }
}
