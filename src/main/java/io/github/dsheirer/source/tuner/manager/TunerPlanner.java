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
package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.util.ThreadPool;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Learns the traffic channel frequencies in use and periodically moves tuners toward the center frequencies that
 * cover them, so traffic channels can be sourced without retuning a tuner in the middle of a call.
 *
 * Idle tuners move whenever the plan improves their coverage.  A tuner carrying a control or conventional channel
 * moves only for a clear improvement and no more than once per interval, since each move briefly interrupts it.
 * Tuners carrying a traffic channel are left alone until the call ends.
 */
public class TunerPlanner
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerPlanner.class);
    private static final long UPDATE_INTERVAL_SECONDS = 30;
    private static final long SAVE_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    private static final long ANCHORED_MOVE_INTERVAL = TimeUnit.MINUTES.toMillis(10);
    private static final double ANCHORED_MOVE_GAIN = 1.25;
    private static final double ANCHORED_MOVE_MINIMUM_SHARE = 0.02;

    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private final TrafficFrequencyLog mTrafficFrequencyLog;
    private final Map<DiscoveredTuner, Long> mLastAnchoredMove = new HashMap<>();
    private ScheduledFuture<?> mUpdateFuture;
    private long mLastSave = System.currentTimeMillis();

    public TunerPlanner(DiscoveredTunerModel discoveredTunerModel, Path logPath)
    {
        mDiscoveredTunerModel = discoveredTunerModel;
        mTrafficFrequencyLog = new TrafficFrequencyLog(logPath);
    }

    public void start()
    {
        mTrafficFrequencyLog.load();
        applyStartupPlan();

        if(mUpdateFuture == null)
        {
            mUpdateFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::update, UPDATE_INTERVAL_SECONDS,
                UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    public void stop()
    {
        if(mUpdateFuture != null)
        {
            mUpdateFuture.cancel(false);
            mUpdateFuture = null;
        }

        mTrafficFrequencyLog.save();
    }

    /**
     * Records a traffic channel request for the learned frequency list.
     */
    public void record(TunerChannel tunerChannel)
    {
        if(tunerChannel.getOwner() != null)
        {
            mTrafficFrequencyLog.record(tunerChannel.getOwner(), tunerChannel.getFrequency(),
                tunerChannel.getBandwidth(), System.currentTimeMillis());
        }
    }

    /**
     * Adds the owners' other known control frequencies, so a multi-frequency channel that rotates can stay on its
     * tuner.  Each counts for a little more than the busiest traffic frequency.
     */
    private void addAlternateControls(List<TunerPlan.Learned> learned, Collection<String> owners, Set<Long> active)
    {
        long weight = learned.stream().mapToLong(TunerPlan.Learned::weight).max().orElse(0) + 1;

        for(TrafficFrequencyLog.Entry control: mTrafficFrequencyLog.getControls(owners))
        {
            if(!active.contains(control.frequency))
            {
                learned.add(new TunerPlan.Learned(control.frequency, control.bandwidth, weight, true, control.owner));
            }
        }
    }

    /**
     * Positions the idle tuners before any channel starts, using the control channels that were running at the end
     * of the last session, their alternate frequencies and the traffic frequencies they granted, so each control
     * channel starts on a tuner that already covers it and its traffic.
     */
    private void applyStartupPlan()
    {
        try
        {
            Map<String, TrafficFrequencyLog.Entry> controls = mTrafficFrequencyLog.getLastSessionControls();

            if(controls.isEmpty())
            {
                return;
            }

            List<TunerPlan.Learned> learned = new ArrayList<>(mTrafficFrequencyLog.getLearned(System.currentTimeMillis(),
                controls.keySet()));
            Set<Long> active = new HashSet<>();
            controls.values().forEach(control -> active.add(control.frequency));
            addAlternateControls(learned, controls.keySet(), active);
            long controlWeight = learned.stream().mapToLong(TunerPlan.Learned::weight).sum() + 1;

            for(TrafficFrequencyLog.Entry control: controls.values())
            {
                learned.add(new TunerPlan.Learned(control.frequency, control.bandwidth, controlWeight, true,
                    control.owner));
            }

            Map<String, DiscoveredTuner> tunersById = new HashMap<>();
            List<TunerPlan.Tuner> tuners = new ArrayList<>();

            for(DiscoveredTuner discoveredTuner: mDiscoveredTunerModel.getAvailableTuners())
            {
                if(!discoveredTuner.hasTuner() || !discoveredTuner.getTuner().getChannelSourceManager().canRecenter() ||
                    discoveredTuner.getTuner().getChannelSourceManager().getTunerChannelCount() > 0)
                {
                    continue;
                }

                TunerController controller = discoveredTuner.getTuner().getTunerController();
                String id = String.valueOf(tuners.size());
                tunersById.put(id, discoveredTuner);
                tuners.add(new TunerPlan.Tuner(id, controller.getFrequency(), controller.getUsableHalfBandwidth(),
                    controller.getMiddleUnusableHalfBandwidth(), controller.getMinimumFrequency(),
                    controller.getMaximumFrequency(), List.of(), true));
            }

            int controlsCovered = 0;

            for(TunerPlan.Placement placement: TunerPlan.plan(tuners, learned))
            {
                DiscoveredTuner discoveredTuner = tunersById.get(placement.id());
                controlsCovered += (int)(placement.weight() / controlWeight);

                if(placement.weight() > 0 && discoveredTuner.getTuner().getChannelSourceManager()
                    .recenter(placement.center()))
                {
                    mLog.info("Tuner plan: startup placed [" + discoveredTuner.getTuner().getPreferredName() +
                        "] at " + placement.center() + " - " + (placement.weight() / controlWeight) +
                        " control channels and " + (placement.weight() % controlWeight) + " weight of alternates and learned calls");
                }
            }

            mLog.info("Tuner plan: startup plan covers " + controlsCovered + " of " + controls.size() +
                " control channels from the last session");
        }
        catch(Throwable t)
        {
            mLog.error("Error applying startup tuner plan", t);
        }
    }

    private void update()
    {
        try
        {
            long now = System.currentTimeMillis();

            if(now - mLastSave >= SAVE_INTERVAL)
            {
                mTrafficFrequencyLog.save();
                mLastSave = now;
            }

            Set<String> owners = new HashSet<>();
            Set<Long> active = new HashSet<>();

            for(DiscoveredTuner discoveredTuner: mDiscoveredTunerModel.getAvailableTuners())
            {
                if(discoveredTuner.hasTuner())
                {
                    for(TunerChannel tunerChannel: discoveredTuner.getTuner().getChannelSourceManager().getTunerChannels())
                    {
                        if(tunerChannel.isAnchored() && tunerChannel.getOwner() != null)
                        {
                            owners.add(tunerChannel.getOwner());
                            active.add(tunerChannel.getFrequency());
                            mTrafficFrequencyLog.recordControl(tunerChannel.getOwner(), tunerChannel.getFrequency(),
                                tunerChannel.getBandwidth(), now);
                        }
                    }
                }
            }

            List<TunerPlan.Learned> learned = new ArrayList<>(mTrafficFrequencyLog.getLearned(now, owners));
            addAlternateControls(learned, owners, active);
            long totalWeight = learned.stream().mapToLong(TunerPlan.Learned::weight).sum();
            Map<String, DiscoveredTuner> tunersById = new HashMap<>();
            List<TunerPlan.Tuner> tuners = new ArrayList<>();

            for(DiscoveredTuner discoveredTuner: mDiscoveredTunerModel.getAvailableTuners())
            {
                if(!discoveredTuner.hasTuner() || !discoveredTuner.getTuner().getChannelSourceManager().canRecenter())
                {
                    continue;
                }

                TunerController controller = discoveredTuner.getTuner().getTunerController();
                SortedSet<TunerChannel> tunerChannels = discoveredTuner.getTuner().getChannelSourceManager().getTunerChannels();
                List<TunerPlan.Channel> channels = new ArrayList<>();
                boolean movable = true;

                for(TunerChannel tunerChannel: tunerChannels)
                {
                    channels.add(new TunerPlan.Channel(tunerChannel.getFrequency(), tunerChannel.getBandwidth(),
                        tunerChannel.isAnchored()));
                    movable &= tunerChannel.isAnchored();
                }

                String id = String.valueOf(tuners.size());
                tunersById.put(id, discoveredTuner);
                tuners.add(new TunerPlan.Tuner(id, controller.getFrequency(), controller.getUsableHalfBandwidth(),
                    controller.getMiddleUnusableHalfBandwidth(), controller.getMinimumFrequency(),
                    controller.getMaximumFrequency(), channels, movable));
            }

            for(TunerPlan.Placement placement: TunerPlan.plan(tuners, learned))
            {
                boolean offEdge = !placement.currentProtected();

                if(!placement.isMove() || (!offEdge && placement.weight() <= placement.currentWeight()))
                {
                    continue;
                }

                DiscoveredTuner discoveredTuner = tunersById.get(placement.id());
                ChannelSourceManager manager = discoveredTuner.getTuner().getChannelSourceManager();
                boolean anchored = manager.getTunerChannelCount() > 0;

                if(anchored)
                {
                    Long lastMove = mLastAnchoredMove.get(discoveredTuner);

                    if(lastMove != null && now - lastMove < ANCHORED_MOVE_INTERVAL)
                    {
                        continue;
                    }

                    long gain = placement.weight() - placement.currentWeight();

                    if(!offEdge && (placement.weight() < placement.currentWeight() * ANCHORED_MOVE_GAIN ||
                        gain < totalWeight * ANCHORED_MOVE_MINIMUM_SHARE))
                    {
                        continue;
                    }
                }

                if(manager.recenter(placement.center()))
                {
                    if(anchored)
                    {
                        mLastAnchoredMove.put(discoveredTuner, now);
                    }

                    mLog.info("Tuner plan: moved [" + discoveredTuner.getTuner().getPreferredName() + "]" +
                        (anchored ? " carrying a control channel" : "") +
                        (offEdge && anchored ? " off the band edge" : "") + " from " + placement.currentCenter() +
                        " to " + placement.center() + " - covers " + placement.weight() + " of " + totalWeight +
                        " learned calls (was " + placement.currentWeight() + "), " + learned.size() +
                        " frequencies known");
                }
            }
        }
        catch(Throwable t)
        {
            mLog.error("Error updating tuner plan", t);
        }
    }
}
