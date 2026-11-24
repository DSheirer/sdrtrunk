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

package io.github.dsheirer.health;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps how well each standard channel and tuner is heard, for node health reports, and every 10 seconds a snapshot
 * of the totals (HealthHistory), from which a report for any window of the last half hour is the difference of two.
 *
 * One per application: channels' HealthMonitors and traffic channel managers write to it, and services' reporters
 * (the Broadcastify Calls broadcaster) read from it.  Snapshots start when the first reader asks for them.
 */
public class HealthRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthRegistry.class);
    private static final HealthRegistry INSTANCE = new HealthRegistry();

    /** How often the totals are kept. */
    public static final long SNAPSHOT_INTERVAL_MS = 10_000;
    /** A tuner counts as running for at most this long between two snapshots (a stall isn't running time). */
    private static final long MAX_SNAPSHOT_GAP_MS = 15_000;

    private final long mSince = System.currentTimeMillis();
    private final Map<Integer,ChannelHealth> mChannels = new ConcurrentHashMap<>();
    private final Map<String,Double> mTunerSeconds = new HashMap<>();
    private final HealthHistory mHistory = new HealthHistory();
    private TunerManager mTunerManager;
    private ScheduledFuture<?> mSnapshotFuture;
    private long mLastSnapshot;

    private HealthRegistry()
    {
    }

    public static HealthRegistry getInstance()
    {
        return INSTANCE;
    }

    /**
     * Tuners to report on (their running time and frequency error).
     */
    public synchronized void setTunerManager(TunerManager tunerManager)
    {
        mTunerManager = tunerManager;
    }

    /**
     * The totals of a standard channel, made on first use.
     */
    public ChannelHealth channel(Channel channel)
    {
        return mChannels.computeIfAbsent(channel.getChannelID(), id -> {
            String mode = channel.getDecodeConfiguration() != null ?
                HealthMonitor.conventionalMode(channel.getDecodeConfiguration().getDecoderType()) : null;
            ChannelHealth health = new ChannelHealth(new ChannelHealth.ChannelInfo(id, channel.getName(),
                channel.getSystem(), channel.getSite(), channel.getAliasListName(), mode));

            if(channel.getSourceConfiguration() instanceof SourceConfigTuner tuner)
            {
                health.setFrequency(tuner.getFrequency());
            }
            else if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency multiple &&
                multiple.getFrequencies() != null && !multiple.getFrequencies().isEmpty())
            {
                health.setFrequency(multiple.getFrequencies().getFirst());
            }

            return health;
        });
    }

    /**
     * Calls announced by a standard channel's control channel, and their outcomes.  Asking for them says they're
     * counted: the channel's sites are reported from now on.
     */
    public CallTally calls(Channel channel)
    {
        ChannelHealth health = channel(channel);
        health.setCallsCounted();
        return health.getCalls();
    }

    /**
     * The snapshots of the last half hour.  The first call starts taking them.
     */
    public synchronized HealthHistory getHistory()
    {
        if(mSnapshotFuture == null)
        {
            snapshot();
            mSnapshotFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::snapshot, SNAPSHOT_INTERVAL_MS,
                SNAPSHOT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        return mHistory;
    }

    /**
     * Takes a snapshot of every channel's and tuner's totals into the history.
     */
    synchronized void snapshot()
    {
        try
        {
            long now = System.currentTimeMillis();
            List<ChannelHealth.Totals> channels = new ArrayList<>();

            for(ChannelHealth health: mChannels.values())
            {
                channels.add(health.totals(now));
            }

            mHistory.add(new HealthSnapshot(now, mSince, channels, radios(now)));
            mLastSnapshot = now;
        }
        catch(Throwable t)
        {
            LOGGER.error("Error taking a node health snapshot", t);
        }
    }

    /**
     * Tuners in use: name, kind, seconds running since the start, and frequency error left after correction.
     */
    private List<HealthSnapshot.RadioTotals> radios(long now)
    {
        List<HealthSnapshot.RadioTotals> radios = new ArrayList<>();

        if(mTunerManager == null)
        {
            return radios;
        }

        double elapsed = mLastSnapshot > 0 ? Math.min(now - mLastSnapshot, MAX_SNAPSHOT_GAP_MS) / 1000.0 : 0.0;

        for(DiscoveredTuner discovered: mTunerManager.getAvailableTuners())
        {
            if(!discovered.hasTuner())
            {
                continue;
            }

            Tuner tuner = discovered.getTuner();
            TunerController controller = tuner.getTunerController();
            String name = tuner.getPreferredName();
            double seconds = mTunerSeconds.merge(name, elapsed, Double::sum);
            Double ppm = controller != null && controller.hasMeasuredFrequencyError() ?
                Math.round(controller.getPPMFrequencyError() * 100.0) / 100.0 : null;
            radios.add(new HealthSnapshot.RadioTotals(name, kind(tuner.getTunerClass()), seconds, ppm));
        }

        return radios;
    }

    /**
     * A tuner's kind, as a node health report names it.
     */
    static String kind(TunerClass tunerClass)
    {
        if(tunerClass == null)
        {
            return "unknown";
        }

        return switch(tunerClass)
        {
            case AIRSPY -> "airspy";
            case AIRSPY_HF -> "airspyhf";
            case BLADERF -> "bladerf";
            case FUNCUBE_DONGLE_PRO, FUNCUBE_DONGLE_PRO_PLUS -> "funcube";
            case HACKRF -> "hackrf";
            case HYDRASDR -> "hydrasdr";
            case LIMESDR -> "limesdr";
            case RTL2832 -> "rtlsdr";
            case RSP -> "sdrplay";
            case RECORDING_TUNER -> "recording";
            case NETWORK_TUNER -> "network";
            default -> tunerClass.name().toLowerCase();
        };
    }
}
