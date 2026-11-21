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

package io.github.dsheirer.service.radioreference;

import com.google.common.annotations.VisibleForTesting;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasListConfiguration;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceDecoder;
import io.github.dsheirer.gui.playlist.radioreference.TalkgroupEncryption;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.radioreference.AliasListUpdateInterval;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.RadioReferenceService;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.rrapi.type.Talkgroup;
import io.github.dsheirer.rrapi.type.TalkgroupCategory;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps alias lists that track a Radio Reference system up to date.  Checks each tracked system on the interval from
 * the user's Radio Reference preferences, records the system's Radio Reference last updated timestamp, and when the
 * alias list is set to auto-update and the system changed, adds an alias for each new talkgroup and updates the name
 * and group of aliases for talkgroups that changed on Radio Reference since the last synchronization.
 *
 * Aliases for talkgroups that are removed from Radio Reference are left in place, and alias settings other than the
 * name and group (color, icon, priority, record, stream, etc.) are never changed.
 */
public class AliasListSynchronizer
{
    private static final Logger mLog = LoggerFactory.getLogger(AliasListSynchronizer.class);
    private static final long CHECK_PERIOD_MINUTES = 30;
    private static final long INITIAL_DELAY_MINUTES = 2;
    private static final long RETRY_AFTER_FAILURE_MS = TimeUnit.HOURS.toMillis(1);

    private final AliasModel mAliasModel;
    private final RadioReference mRadioReference;
    private final UserPreferences mUserPreferences;
    private final Object mSynchronizationLock = new Object();
    private final Map<Integer,Long> mFailedAttempts = new HashMap<>();
    private RadioReferenceDecoder mRadioReferenceDecoder;
    private ScheduledFuture<?> mScheduledCheck;

    /**
     * Constructs an instance
     * @param aliasModel containing aliases and alias list configurations
     * @param radioReference for access to the web service
     * @param userPreferences for the update interval and encrypted talkgroup import preferences
     */
    public AliasListSynchronizer(AliasModel aliasModel, RadioReference radioReference, UserPreferences userPreferences)
    {
        mAliasModel = aliasModel;
        mRadioReference = radioReference;
        mUserPreferences = userPreferences;
    }

    /**
     * Starts periodic checks of tracked alias lists.
     */
    public synchronized void start()
    {
        if(mScheduledCheck == null)
        {
            mScheduledCheck = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::checkDueAliasLists,
                INITIAL_DELAY_MINUTES, CHECK_PERIOD_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * Stops periodic checks.
     */
    public synchronized void stop()
    {
        if(mScheduledCheck != null)
        {
            mScheduledCheck.cancel(false);
            mScheduledCheck = null;
        }
    }

    /**
     * Checks each tracked alias list whose last check is older than the preferred update interval.  Runs on a
     * scheduler thread.
     */
    private void checkDueAliasLists()
    {
        if(!mUserPreferences.getRadioReferencePreference().hasStoredCredentials())
        {
            return;
        }

        AliasListUpdateInterval interval = mUserPreferences.getRadioReferencePreference().getAliasListUpdateInterval();
        long now = java.lang.System.currentTimeMillis();

        for(AliasListConfiguration configuration: getTrackedConfigurations())
        {
            boolean due = now - configuration.getLastChecked() >= interval.getInterval().toMillis();

            Long failed;
            synchronized(mFailedAttempts)
            {
                failed = mFailedAttempts.get(configuration.getRadioReferenceSystemId());
            }

            boolean retryWait = failed != null && now - failed < RETRY_AFTER_FAILURE_MS;

            if(due && !retryWait)
            {
                try
                {
                    synchronize(configuration, false).join();
                }
                catch(Exception e)
                {
                    mLog.warn("Unable to check Radio Reference system [" + configuration.getRadioReferenceSystemId() +
                        "] for alias list [" + configuration.getName() + "] - " + getMessage(e));
                }
            }
        }
    }

    /**
     * Snapshot of the alias list configurations that track a Radio Reference system.
     */
    private List<AliasListConfiguration> getTrackedConfigurations()
    {
        List<AliasListConfiguration> tracked = new ArrayList<>();

        //The configuration list is modified on the FX thread, so retry on a concurrent change
        for(int attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                tracked.clear();

                for(AliasListConfiguration configuration: new ArrayList<>(mAliasModel.aliasListConfigurations()))
                {
                    if(configuration.isTrackingRadioReferenceSystem())
                    {
                        tracked.add(configuration);
                    }
                }

                return tracked;
            }
            catch(Exception e)
            {
                //Try again
            }
        }

        return tracked;
    }

    /**
     * Refreshes the alias list now: reads the current system and talkgroups from Radio Reference and adds or updates
     * aliases, regardless of the alias list's auto-update setting.
     * @param configuration of a tracked alias list
     * @return future result, completed after the aliases are updated on the FX thread
     */
    public CompletableFuture<Result> refresh(AliasListConfiguration configuration)
    {
        return synchronize(configuration, true);
    }

    /**
     * Checks the tracked system and, when forced or when the alias list auto-updates and the system changed,
     * synchronizes the talkgroup aliases.
     * @param configuration of the tracked alias list
     * @param force true to synchronize regardless of the auto-update setting or the last updated timestamps
     * @return future result
     */
    private CompletableFuture<Result> synchronize(AliasListConfiguration configuration, boolean force)
    {
        CompletableFuture<Result> future = new CompletableFuture<>();

        if(configuration == null || !configuration.isTrackingRadioReferenceSystem())
        {
            future.completeExceptionally(new IllegalArgumentException("Alias list doesn't track a Radio Reference system"));
            return future;
        }

        ThreadPool.CACHED.execute(() -> {
            //One synchronization at a time so that a manual refresh and a scheduled check can't both add aliases
            synchronized(mSynchronizationLock)
            {
                int systemId = configuration.getRadioReferenceSystemId();

                try
                {
                    RadioReferenceService service = mRadioReference.getService();

                    if(service instanceof CachingRadioReferenceService caching)
                    {
                        caching.invalidateSystem(systemId);
                    }

                    SystemInformation systemInformation = service.getSystemInformation(systemId);

                    if(systemInformation == null)
                    {
                        throw new RadioReferenceException("Radio Reference didn't provide system [" + systemId + "]");
                    }

                    long lastUpdated = getTime(systemInformation.getLastUpdated());
                    boolean changed = lastUpdated > configuration.getSynchronizedLastUpdated();
                    boolean synchronize = force || (configuration.isAutoUpdate() && changed);

                    List<Talkgroup> talkgroups = null;
                    Map<Integer,String> categoryNames = new HashMap<>();
                    System system = null;
                    RadioReferenceDecoder decoder = null;

                    if(synchronize)
                    {
                        decoder = getRadioReferenceDecoder();
                        system = toSystem(systemId, systemInformation);
                        talkgroups = service.getTalkgroups(systemId);

                        List<TalkgroupCategory> categories = service.getTalkgroupCategories(systemId);

                        if(categories != null)
                        {
                            for(TalkgroupCategory category: categories)
                            {
                                categoryNames.put(category.getTalkgroupCategoryId(), category.getName());
                            }
                        }
                    }

                    synchronized(mFailedAttempts)
                    {
                        mFailedAttempts.remove(systemId);
                    }

                    List<Talkgroup> talkgroupsToApply = talkgroups;
                    System systemToApply = system;
                    RadioReferenceDecoder decoderToApply = decoder;
                    String systemName = systemInformation.getName();

                    runOnFxThread(() -> {
                        try
                        {
                            future.complete(apply(configuration, systemId, systemName, lastUpdated, talkgroupsToApply,
                                categoryNames, systemToApply, decoderToApply));
                        }
                        catch(Throwable t)
                        {
                            future.completeExceptionally(t);
                        }
                    });
                }
                catch(Throwable t)
                {
                    synchronized(mFailedAttempts)
                    {
                        mFailedAttempts.put(systemId, java.lang.System.currentTimeMillis());
                    }

                    future.completeExceptionally(t);
                }
            }
        });

        return future;
    }

    /**
     * Records the check and applies the talkgroups to the alias list.  Runs on the FX thread.
     * @param talkgroups to apply, or null when only recording the check
     */
    @VisibleForTesting
    Result apply(AliasListConfiguration configuration, int systemId, String systemName, long lastUpdated,
                         List<Talkgroup> talkgroups, Map<Integer,String> categoryNames, System system,
                         RadioReferenceDecoder decoder)
    {
        //The user may have deleted the alias list or stopped tracking while the request was in progress
        if(!mAliasModel.aliasListConfigurations().contains(configuration) || !configuration.isTracking(systemId))
        {
            return new Result(false, 0, 0, lastUpdated);
        }

        configuration.setLastChecked(java.lang.System.currentTimeMillis());
        configuration.setRadioReferenceLastUpdated(lastUpdated);

        if(systemName != null && !systemName.isEmpty())
        {
            configuration.setRadioReferenceSystemName(systemName);
        }

        if(talkgroups == null)
        {
            return new Result(false, 0, 0, lastUpdated);
        }

        String aliasListName = configuration.getName();
        long previousSynchronization = configuration.getSynchronizedLastUpdated();
        boolean encryptedDoNotMonitor = mUserPreferences.getRadioReferencePreference().isEncryptedTalkgroupDoNotMonitor();

        List<Alias> listAliases = new ArrayList<>();

        for(Alias alias: mAliasModel.getAliases())
        {
            if(aliasListName.equals(alias.getAliasListName()))
            {
                listAliases.add(alias);
            }
        }

        List<Alias> created = new ArrayList<>();
        int updated = 0;

        for(Talkgroup talkgroup: talkgroups)
        {
            io.github.dsheirer.alias.id.talkgroup.Talkgroup aliasId = decoder.getTalkgroupAliasId(talkgroup, system);
            String group = categoryNames.get(talkgroup.getTalkgroupCategoryId());
            List<Alias> matches = getMatches(listAliases, aliasId);

            if(matches.isEmpty())
            {
                Alias alias = decoder.createAlias(talkgroup, system, aliasListName, group);

                if(encryptedDoNotMonitor &&
                    TalkgroupEncryption.lookup(talkgroup.getEncryptionState()) == TalkgroupEncryption.FULL)
                {
                    alias.addAliasID(new io.github.dsheirer.alias.id.priority.Priority(
                        io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR));
                }

                created.add(alias);
                listAliases.add(alias);
            }
            //Only update aliases for talkgroups that changed on Radio Reference after the previous synchronization, so
            //that the first synchronization and unchanged talkgroups keep any names the user has edited.
            else if(previousSynchronization > 0 && getTime(talkgroup.getLastUpdated()) > previousSynchronization)
            {
                for(Alias alias: matches)
                {
                    boolean aliasUpdated = false;
                    String name = talkgroup.getAlphaTag();

                    if(name != null && !name.isEmpty() && !Objects.equals(name, alias.getName()))
                    {
                        alias.setName(name);
                        aliasUpdated = true;
                    }

                    if(group != null && !group.isEmpty() && !Objects.equals(group, alias.getGroup()))
                    {
                        alias.setGroup(group);
                        aliasUpdated = true;
                    }

                    if(aliasUpdated)
                    {
                        updated++;
                    }
                }
            }
        }

        if(!created.isEmpty())
        {
            mAliasModel.addAliases(created);
        }

        configuration.setSynchronizedLastUpdated(lastUpdated);

        mLog.info("Alias list [" + aliasListName + "] synchronized with Radio Reference system [" +
            configuration.getRadioReferenceSystemId() + "] - added [" + created.size() + "] updated [" + updated +
            "] aliases");

        return new Result(true, created.size(), updated, lastUpdated);
    }

    /**
     * Aliases with a talkgroup alias identifier that matches the talkgroup.
     */
    private static List<Alias> getMatches(List<Alias> aliases, io.github.dsheirer.alias.id.talkgroup.Talkgroup aliasId)
    {
        List<Alias> matches = new ArrayList<>();

        for(Alias alias: aliases)
        {
            for(AliasID id: alias.getAliasIdentifiers())
            {
                if(aliasId.matches(id))
                {
                    matches.add(alias);
                    break;
                }
            }
        }

        return matches;
    }

    /**
     * Decoder for talkgroup values and protocols, created on first use from the cached Radio Reference maps.
     */
    private RadioReferenceDecoder getRadioReferenceDecoder() throws RadioReferenceException
    {
        if(mRadioReferenceDecoder == null)
        {
            RadioReferenceService service = mRadioReference.getService();
            mRadioReferenceDecoder = new RadioReferenceDecoder(mUserPreferences, service.getTypesMap(),
                service.getFlavorsMap(), service.getVoicesMap(), service.getTagsMap());
        }

        return mRadioReferenceDecoder;
    }

    /**
     * Creates a system from the system information, for the decoder's protocol lookup.
     */
    private static System toSystem(int systemId, SystemInformation systemInformation)
    {
        System system = new System();
        system.setSystemId(systemId);
        system.setName(systemInformation.getName());
        system.setTypeId(systemInformation.getTypeId());
        system.setFlavorId(systemInformation.getFlavorId());
        system.setVoiceId(systemInformation.getVoiceId());
        system.setCity(systemInformation.getCity());
        system.setLastUpdated(systemInformation.getLastUpdated());
        return system;
    }

    private static long getTime(Date date)
    {
        return date != null ? date.getTime() : 0;
    }

    /**
     * Runs on the FX thread, or on this thread when the FX toolkit isn't running (headless).
     */
    private static void runOnFxThread(Runnable runnable)
    {
        if(Platform.isFxApplicationThread())
        {
            runnable.run();
            return;
        }

        try
        {
            Platform.runLater(runnable);
        }
        catch(IllegalStateException ise)
        {
            runnable.run();
        }
    }

    /**
     * Message for an exception, unwrapping completion exceptions.
     */
    public static String getMessage(Throwable throwable)
    {
        Throwable cause = throwable;

        while(cause.getCause() != null && (cause instanceof java.util.concurrent.CompletionException ||
            cause instanceof java.util.concurrent.ExecutionException))
        {
            cause = cause.getCause();
        }

        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    /**
     * Synchronization result
     * @param applied true if the talkgroups were applied to the alias list
     * @param added alias count
     * @param updated alias count
     * @param lastUpdated Radio Reference system last updated timestamp
     */
    public record Result(boolean applied, int added, int updated, long lastUpdated) {}
}
