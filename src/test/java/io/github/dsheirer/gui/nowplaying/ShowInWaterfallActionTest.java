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
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.source.config.SourceConfigRecording;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.tuner.TunerEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShowInWaterfallActionTest
{
    @Test
    public void tunedBandwidthCoversFrequenciesWithinHalfTheSampleRate()
    {
        //Tuner at 855 MHz with a 10 MHz sample rate spans 850 - 860 MHz
        assertTrue(ShowInWaterfallAction.covers(855_000_000L, 10_000_000.0, 855_000_000L));
        assertTrue(ShowInWaterfallAction.covers(855_000_000L, 10_000_000.0, 850_000_000L));
        assertTrue(ShowInWaterfallAction.covers(855_000_000L, 10_000_000.0, 860_000_000L));
        assertFalse(ShowInWaterfallAction.covers(855_000_000L, 10_000_000.0, 849_999_999L));
        assertFalse(ShowInWaterfallAction.covers(855_000_000L, 10_000_000.0, 860_000_001L));
    }

    @Test
    public void onlyChannelsWithATunerSourceAreSupported()
    {
        Channel tuned = new Channel("tuned");
        tuned.setSourceConfiguration(new SourceConfigTuner());
        assertTrue(ShowInWaterfallAction.isSupported(tuned));

        Channel multiple = new Channel("multiple");
        multiple.setSourceConfiguration(new SourceConfigTunerMultipleFrequency());
        assertTrue(ShowInWaterfallAction.isSupported(multiple));

        Channel other = new Channel("other");
        other.setSourceConfiguration(new SourceConfigRecording());
        assertFalse(ShowInWaterfallAction.isSupported(other));
    }

    @Test
    public void tunerEventCarriesTheCenterFrequencyOnlyWhenGiven()
    {
        TunerEvent plain = new TunerEvent(null, TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY);
        assertFalse(plain.hasCenterFrequency());

        TunerEvent centred = new TunerEvent(null, TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY, 855_237_500L);
        assertTrue(centred.hasCenterFrequency());
        assertEquals(855_237_500L, centred.getCenterFrequency());
    }
}
