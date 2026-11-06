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

import io.github.dsheirer.module.decode.DecodeQuality;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.util.ThreadPool;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the decode quality of every control and conventional channel every few minutes, with its position in its
 * tuner's band.
 */
public class DecodeQualityReporter
{
    private static final Logger mLog = LoggerFactory.getLogger(DecodeQualityReporter.class);
    private static final long REPORT_INTERVAL_SECONDS = TimeUnit.MINUTES.toSeconds(5);

    private final DiscoveredTunerModel mDiscoveredTunerModel;
    private final Map<String, DecodeQuality.Sample> mLastReport = new HashMap<>();
    private ScheduledFuture<?> mFuture;

    public DecodeQualityReporter(DiscoveredTunerModel discoveredTunerModel)
    {
        mDiscoveredTunerModel = discoveredTunerModel;
    }

    public void start()
    {
        if(mFuture == null)
        {
            mFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::report, REPORT_INTERVAL_SECONDS,
                REPORT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    public void stop()
    {
        if(mFuture != null)
        {
            mFuture.cancel(false);
            mFuture = null;
        }
    }

    private void report()
    {
        try
        {
            for(DiscoveredTuner discoveredTuner: mDiscoveredTunerModel.getAvailableTuners())
            {
                if(!discoveredTuner.hasTuner())
                {
                    continue;
                }

                TunerController controller = discoveredTuner.getTuner().getTunerController();
                long center = controller.getFrequency();

                for(TunerChannel tunerChannel: discoveredTuner.getTuner().getChannelSourceManager().getTunerChannels())
                {
                    if(!tunerChannel.isAnchored() || tunerChannel.getOwner() == null)
                    {
                        continue;
                    }

                    DecodeQuality.Sample current = DecodeQuality.get(tunerChannel.getOwner());
                    DecodeQuality.Sample previous = mLastReport.put(tunerChannel.getOwner(), current);

                    if(previous == null)
                    {
                        continue;
                    }

                    DecodeQuality.Sample sample = current.minus(previous);
                    Double level = DecodeQuality.getSignalLevel(tunerChannel.getOwner());
                    long offset = tunerChannel.getFrequency() - center;
                    double edge = Math.abs(offset) * 100.0 / controller.getUsableHalfBandwidth();
                    mLog.info(String.format("Decode quality: [%s] %d on [%s] at %+d kHz (%.0f%% to the edge) - " +
                        "%.1f valid/s, %d failed, %.2f corrected bits per message, %d sync loss bits, signal %s",
                        tunerChannel.getOwner(), tunerChannel.getFrequency(), discoveredTuner.getTuner().getPreferredName(),
                        offset / 1000, edge, sample.valid() / (double)REPORT_INTERVAL_SECONDS, sample.invalid(),
                        sample.correctedBitsPerMessage(), sample.syncLossBits(),
                        level == null || level.isInfinite() ? "unknown" : String.format("%.1f dB", level)));
                }
            }
        }
        catch(Throwable t)
        {
            mLog.error("Error reporting decode quality", t);
        }
    }
}
