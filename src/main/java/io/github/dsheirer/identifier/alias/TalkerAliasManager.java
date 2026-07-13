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

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.preference.application.ApplicationPreference;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Talker alias cache manager.  Collects observed talker aliases and inserts them into an identifier collection when
 * the corresponding radio is active in a FROM role.
 *
 * This implementation is thread safe and is intended to be used across control and traffic channels.
 */
public class TalkerAliasManager
{
    private static final Logger mLog = LoggerFactory.getLogger(TalkerAliasManager.class);
    private static final Map<Path,Object> EXPORT_LOCKS = new ConcurrentHashMap<>();
    private Map<Integer,TalkerAliasIdentifier> mAliasMap = new ConcurrentHashMap<>();
    private final Path mExportFile;
    private volatile boolean mAliasExportEnabled;

    /**
     * Constructs an instance
     */
    public TalkerAliasManager()
    {
        mExportFile = null;
        mAliasExportEnabled = false;
    }

    /**
     * Constructs an instance that exports newly observed and changed aliases to a CSV file.
     * @param exportDirectory parent directory for the CSV file
     * @param sourceName system or channel name used for the file name
     */
    public TalkerAliasManager(Path exportDirectory, String sourceName)
    {
        this(exportDirectory, sourceName, ApplicationPreference.readTalkerAliasCsvExportEnabled());
    }

    /**
     * Constructs an instance that exports newly observed and changed aliases to a CSV file when enabled.
     *
     * @param exportDirectory parent directory for the CSV file
     * @param sourceName system or channel name used for the file name
     * @param aliasExportEnabled true to initialize the CSV export file
     */
    TalkerAliasManager(Path exportDirectory, String sourceName, boolean aliasExportEnabled)
    {
        mExportFile = exportDirectory.resolve(safeFileName(sourceName) + ".csv");

        if(aliasExportEnabled)
        {
            initializeAliasExport();
        }
    }

    /**
     * Updates the alias for the radio identifier
     *
     * @param identifier to update
     * @param alias to assign to the radio identifier
     */
    public void update(RadioIdentifier identifier, TalkerAliasIdentifier alias)
    {
        if(identifier.getRole() == Role.FROM)
        {
            TalkerAliasIdentifier previous = mAliasMap.put(identifier.getValue(), alias);

            if(identifier.getValue() > 0 && mAliasExportEnabled && !Objects.equals(alias, previous))
            {
                int radio = identifier.getValue();
                String aliasText = alias.toString().replaceFirst("^TA-", "");
                ThreadPool.CACHED.execute(() -> appendCsv(radio, aliasText));
            }
        }
    }

    void appendCsv(int radio, String alias)
    {
        if(!mAliasExportEnabled)
        {
            return;
        }

        synchronized(EXPORT_LOCKS.computeIfAbsent(mExportFile, ignored -> new Object()))
        {
            if(!mAliasExportEnabled)
            {
                return;
            }

            try
            {
                Files.writeString(mExportFile, radio + "," + csv(alias) + '\n', StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
            }
            catch(IOException ioe)
            {
                mAliasExportEnabled = false;
                mLog.warn("Unable to export talker alias CSV [{}]", mExportFile, ioe);
            }
        }
    }

    /**
     * Initializes the alias export file once so that later alias updates do not repeatedly attempt a failed file
     * creation.
     */
    private void initializeAliasExport()
    {
        synchronized(EXPORT_LOCKS.computeIfAbsent(mExportFile, ignored -> new Object()))
        {
            try
            {
                Files.createDirectories(mExportFile.getParent());

                if(Files.notExists(mExportFile) || Files.size(mExportFile) == 0)
                {
                    Files.writeString(mExportFile, "radio,alias\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                }

                mAliasExportEnabled = true;
            }
            catch(IOException ioe)
            {
                mLog.warn("Unable to initialize talker alias CSV export [{}]", mExportFile, ioe);
            }
        }
    }

    static String csv(String value)
    {
        String escaped = value == null ? "" : value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    static String safeFileName(String value)
    {
        String safe = value == null ? "Talker Aliases" : value.replaceFirst("^T-", "")
            .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        return safe.isEmpty() ? "Talker Aliases" : safe;
    }

    /**
     * Indicates if an alias exists for the identifier
     * @param radioIdentifier to test
     * @return true if an alias exists.
     */
    public boolean hasAlias(RadioIdentifier radioIdentifier)
    {
        return mAliasMap.containsKey(radioIdentifier.getValue());
    }

    /**
     * Enriches the immutable identifier collection by detecting a radio identifier with the FROM role, lookup a
     * matching alias, and insert the alias into a new mutable identifier collection.
     * @param originalIC to enrich
     * @return enriched identifier collection or the original identifier collection if we don't have an alias.
     */
    public synchronized IdentifierCollection enrich(IdentifierCollection originalIC)
    {
        Identifier fromRadio = originalIC.getFromIdentifier();

        if(fromRadio instanceof RadioIdentifier rid)
        {
            TalkerAliasIdentifier alias = mAliasMap.get(rid.getValue());

            if(alias != null)
            {
                MutableIdentifierCollection enrichedIC = new MutableIdentifierCollection(originalIC.getIdentifiers());
                enrichedIC.update(alias);
                return enrichedIC;
            }
        }

        return originalIC;
    }

    /**
     * Enriches the mutable identifier collection by detecting a radio identifier with the FROM role, lookup a
     * matching alias, and insert the alias into the mutable identifier collection argument.
     * @param mic to enrich
     */
    public synchronized void enrichMutable(MutableIdentifierCollection mic)
    {
        Identifier fromRadio = mic.getFromIdentifier();

        if(fromRadio instanceof RadioIdentifier rid)
        {
            TalkerAliasIdentifier alias = mAliasMap.get(rid.getValue());

            if(alias != null)
            {
                mic.update(alias);
            }
        }
    }

    /**
     * Creates a summary listing of talker aliases
     * @return summary.
     */
    public synchronized String getAliasSummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Active System Radio Aliases\n");
        sb.append("  Radio\tTalker Alias (TA-)\n");
        List<Integer> radios = new ArrayList<>(mAliasMap.keySet());

        if(radios.size() > 0)
        {
            Collections.sort(radios);
            for(Integer radio : radios)
            {
                sb.append("  ").append(radio);
                sb.append("\t").append(mAliasMap.get(radio));
                sb.append("\n");
            }
        }
        else
        {
            sb.append("  None\n");
        }

        return sb.toString();
    }
}
