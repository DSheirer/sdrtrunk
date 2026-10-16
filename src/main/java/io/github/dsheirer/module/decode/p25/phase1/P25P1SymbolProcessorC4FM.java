package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.dsp.c4fm.C4FMSymbolProcessor;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorFactory;

/**
 * P25 Phase 1 C4FM implementation of the symbol processor.
 */
public class P25P1SymbolProcessorC4FM extends C4FMSymbolProcessor
{
    private static final int MAX_MESSAGE_DIBIT_LENGTH = P25P1DataUnitID.PACKET_DATA_UNIT_BLOCK_5.getElapsedDibitLength();

    //TODO: adjust each of these values to the P25 waveform
    private static final int EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT = 600;
    private static final int EQUALIZER_SKIP_TRAINING_FRAME_COUNT = 5;
    private static final float SAMPLE_EQUALIZER_GAIN_MINIMUM = 4.0f;
    private static final float SAMPLE_EQUALIZER_GAIN_MAXIMUM = 6.2f;
    private static final float SYNC_THRESHOLD_DETECTED = 80;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 100;
    private static final float SYNC_THRESHOLD_EQUALIZED = 100;

    /**
     * Constructs an instance
     *
     * @param feedbackDecoder parent decoder
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public P25P1SymbolProcessorC4FM(FeedbackDecoder feedbackDecoder, C4FMMessageFramer messageFramer)
    {
        super(feedbackDecoder, messageFramer, P25P1SoftSyncDetectorFactory.getDetector(),
                P25P1SoftSyncDetectorFactory.getDetector());
    }

    /**
     * Dynamic balance tracking (ie decision directed PLL adjustments) is enabled for this decoder.
     */
    @Override
    public boolean isDynamicBalanceTrackingRequired()
    {
        return true;
    }

    @Override
    public int getMaxMessageDibitLength()
    {
        return MAX_MESSAGE_DIBIT_LENGTH;
    }

    @Override
    public int getEqualizerTrainingPeriodDurationSymbolCount()
    {
        return EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT;
    }

    @Override
    public int getEqualizerSkipTrainingFrameCount()
    {
        return EQUALIZER_SKIP_TRAINING_FRAME_COUNT;
    }

    @Override
    public float getSampleGainMinimum()
    {
        return SAMPLE_EQUALIZER_GAIN_MINIMUM;
    }

    @Override
    public float getSampleGainMaximum()
    {
        return SAMPLE_EQUALIZER_GAIN_MAXIMUM;
    }

    @Override
    public float getSyncThresholdDetected()
    {
        return SYNC_THRESHOLD_DETECTED;
    }

    @Override
    public float getSyncThresholdOptimized()
    {
        return SYNC_THRESHOLD_OPTIMIZED;
    }

    @Override
    public float getSyncThresholdEqualized()
    {
        return SYNC_THRESHOLD_EQUALIZED;
    }
}
