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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates a center frequency for each tuner so the tuners cover as much of the learned traffic channel activity
 * as possible while every channel already on a tuner stays within that tuner's usable bandwidth.
 *
 * Tuners that can't move keep their current center and cover what they already can.  Movable tuners carrying
 * control or conventional channels are placed next, then idle tuners take whatever remains uncovered.  Anchored
 * channels and learned control frequencies must stay within the protected inner share of a tuner's bandwidth.
 */
public class TunerPlan
{
    /**
     * A channel currently sourced by a tuner.
     *
     * @param anchored for control and conventional channels, which stay away from the band edges
     */
    public record Channel(long frequency, int bandwidth, boolean anchored)
    {
        long min()
        {
            return frequency - (bandwidth / 2);
        }

        long max()
        {
            return frequency + (bandwidth / 2);
        }
    }

    /**
     * A learned frequency, weighted by how often it carries calls.
     *
     * @param control for a control channel, which stays away from the band edges
     * @param group for the frequencies of one multi-frequency control channel, which count only when the tuner covers
     * all of them, so the channel can rotate without changing tuners.  Null for no group.
     */
    public record Learned(long frequency, int bandwidth, long weight, boolean control, String group)
    {
        public Learned(long frequency, int bandwidth, long weight, boolean control)
        {
            this(frequency, bandwidth, weight, control, null);
        }

        long min()
        {
            return frequency - (bandwidth / 2);
        }

        long max()
        {
            return frequency + (bandwidth / 2);
        }
    }

    /**
     * A tuner as seen by the planner.
     *
     * @param id unique to this plan
     * @param center current center frequency
     * @param usableHalfBandwidth usable bandwidth either side of center
     * @param middleHalfBandwidth unusable DC region either side of center, or 0
     * @param minimumFrequency lowest tunable frequency
     * @param maximumFrequency highest tunable frequency
     * @param channels currently sourced by the tuner, which must stay in range
     * @param movable false when the tuner must keep its current center
     */
    public record Tuner(String id, long center, int usableHalfBandwidth, int middleHalfBandwidth,
                        long minimumFrequency, long maximumFrequency, List<Channel> channels, boolean movable)
    {
    }

    /**
     * Planned center for a tuner, with the learned weight it covers there and at its current center.
     *
     * @param currentProtected false when an anchored channel sits outside the protected share at the current center
     */
    public record Placement(String id, long center, long currentCenter, long weight, long currentWeight,
                            boolean currentProtected)
    {
        public boolean isMove()
        {
            return center != currentCenter;
        }
    }

    private TunerPlan()
    {
    }

    public static List<Placement> plan(List<Tuner> tuners, List<Learned> learned)
    {
        List<Tuner> ordered = new ArrayList<>(tuners);
        ordered.sort(Comparator.comparingInt(TunerPlan::rank));

        Set<Long> covered = new HashSet<>();
        List<Placement> placements = new ArrayList<>();
        Map<String, List<Learned>> groups = new HashMap<>();

        for(Learned entry: learned)
        {
            if(entry.group() != null)
            {
                groups.computeIfAbsent(entry.group(), key -> new ArrayList<>()).add(entry);
            }
        }

        for(Tuner tuner: ordered)
        {
            long currentWeight = weight(tuner, tuner.center(), learned, covered, groups);
            boolean currentProtected = fits(tuner, tuner.center());
            long best = tuner.center();
            long bestWeight = currentProtected ? currentWeight : -1;

            if(tuner.movable())
            {
                for(long candidate: candidates(tuner, learned))
                {
                    if(!fits(tuner, candidate))
                    {
                        continue;
                    }

                    long weight = weight(tuner, candidate, learned, covered, groups);

                    if(weight > bestWeight || (weight == bestWeight &&
                        Math.abs(candidate - tuner.center()) < Math.abs(best - tuner.center())))
                    {
                        best = candidate;
                        bestWeight = weight;
                    }
                }
            }

            if(bestWeight < 0)
            {
                bestWeight = currentWeight;
            }

            for(Learned entry: learned)
            {
                if(counts(tuner, best, entry, groups))
                {
                    covered.add(entry.frequency());
                }
            }

            placements.add(new Placement(tuner.id(), best, tuner.center(), bestWeight, currentWeight, currentProtected));
        }

        return placements;
    }

    private static int rank(Tuner tuner)
    {
        if(!tuner.movable())
        {
            return 0;
        }

        return tuner.channels().isEmpty() ? 2 : 1;
    }

    /**
     * Centers that put a channel or learned frequency right at a usable edge or right beside the DC region.  The best
     * center for any set of frequencies is always one of these, so there's no need to sweep every hertz.
     */
    private static Set<Long> candidates(Tuner tuner, List<Learned> learned)
    {
        Set<Long> candidates = new LinkedHashSet<>();
        candidates.add(tuner.center());

        for(Channel channel: tuner.channels())
        {
            addCandidates(tuner, channel.min(), channel.max(), channel.anchored(), candidates);
        }

        for(Learned entry: learned)
        {
            addCandidates(tuner, entry.min(), entry.max(), entry.control(), candidates);
        }

        return candidates;
    }

    private static void addCandidates(Tuner tuner, long min, long max, boolean protect, Set<Long> candidates)
    {
        long halfBandwidth = halfBandwidth(tuner, protect);
        candidates.add(min + halfBandwidth);
        candidates.add(max - halfBandwidth);

        if(tuner.middleHalfBandwidth() > 0)
        {
            candidates.add(max + tuner.middleHalfBandwidth() + 1);
            candidates.add(min - tuner.middleHalfBandwidth() - 1);
        }
    }

    private static boolean fits(Tuner tuner, long center)
    {
        if(center < tuner.minimumFrequency() || center > tuner.maximumFrequency())
        {
            return false;
        }

        for(Channel channel: tuner.channels())
        {
            if(!inWindow(tuner, center, channel.min(), channel.max(), channel.anchored()))
            {
                return false;
            }
        }

        return true;
    }

    private static boolean covers(Tuner tuner, long center, Learned entry)
    {
        return entry.min() >= tuner.minimumFrequency() && entry.max() <= tuner.maximumFrequency() &&
            inWindow(tuner, center, entry.min(), entry.max(), entry.control());
    }

    /**
     * True when the tuner covers the entry and, for a grouped entry, every frequency in its group.  A group too wide
     * for any one tuner counts each frequency on its own.
     */
    private static boolean counts(Tuner tuner, long center, Learned entry, Map<String, List<Learned>> groups)
    {
        if(!covers(tuner, center, entry))
        {
            return false;
        }

        List<Learned> group = entry.group() == null ? null : groups.get(entry.group());

        if(group == null || group.size() < 2)
        {
            return true;
        }

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for(Learned member: group)
        {
            min = Math.min(min, member.min());
            max = Math.max(max, member.max());
        }

        if(max - min > 2 * halfBandwidth(tuner, true))
        {
            return true;
        }

        for(Learned member: group)
        {
            if(!covers(tuner, center, member))
            {
                return false;
            }
        }

        return true;
    }

    private static long halfBandwidth(Tuner tuner, boolean protect)
    {
        return protect ? (long)(tuner.usableHalfBandwidth() * ChannelSourceManager.ANCHORED_USABLE_FRACTION) :
            tuner.usableHalfBandwidth();
    }

    private static boolean inWindow(Tuner tuner, long center, long min, long max, boolean protect)
    {
        long halfBandwidth = halfBandwidth(tuner, protect);

        if(min < center - halfBandwidth || max > center + halfBandwidth)
        {
            return false;
        }

        if(tuner.middleHalfBandwidth() > 0)
        {
            return max < center - tuner.middleHalfBandwidth() || min > center + tuner.middleHalfBandwidth();
        }

        return true;
    }

    private static long weight(Tuner tuner, long center, List<Learned> learned, Set<Long> covered,
                               Map<String, List<Learned>> groups)
    {
        long weight = 0;

        for(Learned entry: learned)
        {
            if(!covered.contains(entry.frequency()) && counts(tuner, center, entry, groups))
            {
                weight += entry.weight();
            }
        }

        return weight;
    }
}
