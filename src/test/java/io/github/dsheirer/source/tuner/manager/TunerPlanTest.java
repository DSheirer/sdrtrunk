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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class TunerPlanTest
{
    private static final int HALF = 1_000_000;
    private static final int BANDWIDTH = 12_500;
    private static final long MIN = 24_000_000;
    private static final long MAX = 1_766_000_000;

    private static TunerPlan.Tuner tuner(String id, long center, int middle, boolean movable, long... channels)
    {
        List<TunerPlan.Channel> list = java.util.Arrays.stream(channels)
            .mapToObj(frequency -> new TunerPlan.Channel(frequency, BANDWIDTH, movable)).toList();
        return new TunerPlan.Tuner(id, center, HALF, middle, MIN, MAX, list, movable);
    }

    private static TunerPlan.Learned learned(long frequency, long weight)
    {
        return new TunerPlan.Learned(frequency, BANDWIDTH, weight, false);
    }

    private static TunerPlan.Learned control(long frequency, long weight)
    {
        return new TunerPlan.Learned(frequency, BANDWIDTH, weight, true);
    }

    private static TunerPlan.Placement placement(List<TunerPlan.Placement> placements, String id)
    {
        return placements.stream().filter(placement -> placement.id().equals(id)).findFirst().orElseThrow();
    }

    private static boolean inWindow(long center, long frequency, int middle)
    {
        return inWindow(center, frequency, middle, HALF);
    }

    private static boolean inProtectedWindow(long center, long frequency, int middle)
    {
        return inWindow(center, frequency, middle, (long)(HALF * ChannelSourceManager.ANCHORED_USABLE_FRACTION));
    }

    private static boolean inWindow(long center, long frequency, int middle, long half)
    {
        long min = frequency - BANDWIDTH / 2;
        long max = frequency + BANDWIDTH / 2;
        boolean inside = min >= center - half && max <= center + half;
        return inside && (middle == 0 || max < center - middle || min > center + middle);
    }

    @Test
    void controlTunerMovesToCoverTrafficAndKeepsControlChannel()
    {
        long control = 771_000_000;
        List<TunerPlan.Learned> learned = List.of(learned(772_600_000, 50), learned(772_650_000, 30),
            learned(769_000_000, 1));

        TunerPlan.Placement placement = placement(TunerPlan.plan(List.of(tuner("a", control, 0, true, control)),
            learned), "a");

        assertTrue(placement.isMove());
        assertTrue(inProtectedWindow(placement.center(), control, 0));
        assertTrue(inWindow(placement.center(), 772_600_000, 0));
        assertTrue(inWindow(placement.center(), 772_650_000, 0));
        assertEquals(80, placement.weight());
    }

    @Test
    void settledPlanDoesNotMoveAgain()
    {
        long control = 771_000_000;
        List<TunerPlan.Learned> learned = List.of(learned(772_600_000, 50), learned(770_100_000, 20));

        TunerPlan.Placement first = placement(TunerPlan.plan(List.of(tuner("a", control, 0, true, control)),
            learned), "a");
        TunerPlan.Placement second = placement(TunerPlan.plan(List.of(tuner("a", first.center(), 0, true, control)),
            learned), "a");

        assertFalse(second.isMove());
    }

    @Test
    void idleTunerCoversWhatTheControlTunerCannotReach()
    {
        long control = 855_000_000;
        List<TunerPlan.Learned> learned = List.of(learned(855_500_000, 40), learned(772_600_000, 25),
            learned(772_900_000, 10));

        List<TunerPlan.Placement> placements = TunerPlan.plan(List.of(tuner("idle", 100_000_000, 0, true),
            tuner("control", control, 0, true, control)), learned);

        assertEquals(40, placement(placements, "control").weight());
        TunerPlan.Placement idle = placement(placements, "idle");
        assertTrue(inWindow(idle.center(), 772_600_000, 0));
        assertTrue(inWindow(idle.center(), 772_900_000, 0));
        assertEquals(35, idle.weight());
    }

    @Test
    void busyTunerKeepsItsCenterAndOthersSkipWhatItCovers()
    {
        List<TunerPlan.Learned> learned = List.of(learned(855_200_000, 40), learned(860_000_000, 10));

        List<TunerPlan.Placement> placements = TunerPlan.plan(List.of(tuner("idle", 100_000_000, 0, true),
            tuner("busy", 855_000_000, 0, false, 855_200_000)), learned);

        assertFalse(placement(placements, "busy").isMove());
        TunerPlan.Placement idle = placement(placements, "idle");
        assertTrue(inWindow(idle.center(), 860_000_000, 0));
        assertEquals(10, idle.weight());
    }

    @Test
    void plannedCenterKeepsChannelsOutOfTheDcRegion()
    {
        int middle = 20_000;
        long control = 771_000_000;
        List<TunerPlan.Learned> learned = List.of(learned(771_400_000, 20), learned(771_800_000, 20));

        TunerPlan.Placement placement = placement(TunerPlan.plan(List.of(tuner("a", 770_500_000, middle, true,
            control)), learned), "a");

        assertTrue(inProtectedWindow(placement.center(), control, middle));
        assertTrue(inWindow(placement.center(), 771_400_000, middle));
        assertTrue(inWindow(placement.center(), 771_800_000, middle));
        assertEquals(40, placement.weight());
    }

    @Test
    void logKeepsOneEntryPerFrequencyAndSurvivesRestart() throws Exception
    {
        Path path = Files.createTempDirectory("tuner-plan").resolve("tuner_traffic_frequencies.json");
        long now = System.currentTimeMillis();

        TrafficFrequencyLog log = new TrafficFrequencyLog(path);
        log.record("cuyahoga", 772_600_000, BANDWIDTH, now);
        log.record("cuyahoga", 772_600_000, BANDWIDTH, now);
        log.record("cuyahoga", 770_100_000, BANDWIDTH, now - TimeUnit.DAYS.toMillis(20));
        log.record("medina", 773_000_000, BANDWIDTH, now);
        log.save();

        TrafficFrequencyLog reloaded = new TrafficFrequencyLog(path);
        reloaded.load();
        List<TunerPlan.Learned> learned = reloaded.getLearned(now, Set.of("cuyahoga"));

        assertEquals(1, learned.size());
        assertEquals(772_600_000, learned.get(0).frequency());
        assertEquals(2, learned.get(0).weight());
    }

    @Test
    void startupUsesControlChannelsRunningAtTheEndOfTheLastSession()
    {
        long now = System.currentTimeMillis();
        TrafficFrequencyLog log = new TrafficFrequencyLog(null);
        log.recordControl("cuyahoga", 771_000_000, BANDWIDTH, now);
        log.recordControl("summit", 855_000_000, BANDWIDTH, now - TimeUnit.MINUTES.toMillis(2));
        log.recordControl("medina", 857_000_000, BANDWIDTH, now - TimeUnit.HOURS.toMillis(2));

        Map<String, TrafficFrequencyLog.Entry> controls = log.getLastSessionControls();

        assertEquals(Set.of("cuyahoga", "summit"), controls.keySet());
        assertEquals(771_000_000, controls.get("cuyahoga").frequency);
    }

    @Test
    void startupPlanCoversEveryControlChannelBeforeTraffic()
    {
        long controlWeight = 1_000;
        List<TunerPlan.Learned> learned = List.of(control(771_000_000, controlWeight),
            control(855_000_000, controlWeight), learned(772_500_000, 300), learned(860_000_000, 500));

        List<TunerPlan.Placement> placements = TunerPlan.plan(List.of(tuner("a", 100_000_000, 0, true),
            tuner("b", 100_000_000, 0, true)), learned);

        long covered = placements.stream().mapToLong(TunerPlan.Placement::weight).sum() / controlWeight;
        assertEquals(2, covered);
        assertTrue(placements.stream().anyMatch(placement -> inWindow(placement.center(), 771_000_000, 0) &&
            inWindow(placement.center(), 772_500_000, 0)));
    }

    @Test
    void controlChannelAtTheBandEdgeMovesInward()
    {
        long control = 774_581_250;
        long center = control - HALF + BANDWIDTH;

        TunerPlan.Placement placement = placement(TunerPlan.plan(List.of(tuner("a", center, 0, true, control)),
            List.of()), "a");

        assertFalse(placement.currentProtected());
        assertTrue(placement.isMove());
        assertTrue(inProtectedWindow(placement.center(), control, 0));
    }

    @Test
    void startupSpreadsControlChannelsTooWideForOneProtectedWindow()
    {
        long controlWeight = 1_000;
        long[] controls = {772_593_750, 773_831_250, 774_581_250};
        List<TunerPlan.Learned> learned = java.util.Arrays.stream(controls)
            .mapToObj(frequency -> control(frequency, controlWeight)).toList();

        List<TunerPlan.Placement> placements = TunerPlan.plan(List.of(tuner("a", 100_000_000, 0, true),
            tuner("b", 100_000_000, 0, true)), learned);

        assertEquals(3, placements.stream().mapToLong(TunerPlan.Placement::weight).sum() / controlWeight);

        for(long frequency: controls)
        {
            assertTrue(placements.stream().anyMatch(placement -> inProtectedWindow(placement.center(), frequency, 0)));
        }
    }

    @Test
    void alternateControlFrequencyLandsOnTheSameTuner()
    {
        long controlWeight = 10_000;
        List<TunerPlan.Learned> learned = List.of(control(774_581_250, controlWeight), control(774_081_250, 51),
            learned(775_200_000, 50), learned(773_300_000, 20));

        TunerPlan.Placement placement = placement(TunerPlan.plan(List.of(tuner("a", 100_000_000, 0, true)), learned),
            "a");

        assertTrue(inProtectedWindow(placement.center(), 774_581_250, 0));
        assertTrue(inProtectedWindow(placement.center(), 774_081_250, 0));
    }

    @Test
    void logListsEveryControlFrequencyForTheOwners()
    {
        long now = System.currentTimeMillis();
        TrafficFrequencyLog log = new TrafficFrequencyLog(null);
        log.recordControl("medina", 774_581_250, BANDWIDTH, now);
        log.recordControl("medina", 774_081_250, BANDWIDTH, now - TimeUnit.HOURS.toMillis(3));
        log.recordControl("summit", 858_512_500, BANDWIDTH, now);
        log.record("medina", 773_000_000, BANDWIDTH, now);

        assertEquals(Set.of(774_581_250L, 774_081_250L), log.getControls(Set.of("medina")).stream()
            .map(entry -> entry.frequency).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void startupKeepsEachChannelsFrequenciesOnOneTuner()
    {
        long control = 100_000;
        long alternate = 10;
        List<TunerPlan.Learned> learned = List.of(
            new TunerPlan.Learned(772_593_750, BANDWIDTH, control, true, "marshaville"),
            new TunerPlan.Learned(771_831_250, BANDWIDTH, control, true, "parma"),
            new TunerPlan.Learned(771_193_750, BANDWIDTH, alternate, true, "parma"),
            new TunerPlan.Learned(771_506_250, BANDWIDTH, alternate, true, "parma"),
            new TunerPlan.Learned(774_081_250, BANDWIDTH, control, true, "medina"),
            new TunerPlan.Learned(774_581_250, BANDWIDTH, alternate, true, "medina"),
            new TunerPlan.Learned(773_831_250, BANDWIDTH, control, true, "cuyahoga"),
            new TunerPlan.Learned(774_281_250, BANDWIDTH, alternate, true, "cuyahoga"),
            new TunerPlan.Learned(774_531_250, BANDWIDTH, alternate, true, "cuyahoga"),
            new TunerPlan.Learned(774_781_250, BANDWIDTH, alternate, true, "cuyahoga"),
            new TunerPlan.Learned(855_962_500, BANDWIDTH, control, true, "gcrcn"));

        List<TunerPlan.Placement> placements = TunerPlan.plan(List.of(tuner("a", 100_000_000, 0, true),
            tuner("b", 100_000_000, 0, true), tuner("c", 100_000_000, 0, true), tuner("d", 100_000_000, 0, true)),
            learned);

        assertEquals(5, placements.stream().mapToLong(TunerPlan.Placement::weight).sum() / control);

        for(String group: List.of("marshaville", "parma", "medina", "cuyahoga", "gcrcn"))
        {
            List<Long> frequencies = learned.stream().filter(entry -> group.equals(entry.group()))
                .map(TunerPlan.Learned::frequency).toList();
            assertTrue(placements.stream().anyMatch(placement -> frequencies.stream()
                .allMatch(frequency -> inProtectedWindow(placement.center(), frequency, 0))), group);
        }
    }
}
