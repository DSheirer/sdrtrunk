package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.dsp.c4fm.C4FMSymbolProcessor;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSyncDetectorFactory;

/**
 * NXDN implementation of the symbol processor.
 */
public class NXDNSymbolProcessor extends C4FMSymbolProcessor
{
    private static final int MAX_MESSAGE_DIBIT_LENGTH = 192;
    private static final int EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT = 192 * 3;
    private static final int EQUALIZER_SKIP_TRAINING_FRAME_COUNT = 10;
    private static final float SAMPLE_EQUALIZER_GAIN_MINIMUM = 5.2f;
    private static final float SAMPLE_EQUALIZER_GAIN_MAXIMUM = 6.2f;
    private static final float SYNC_THRESHOLD_DETECTED = 37;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 40;
    private static final float SYNC_THRESHOLD_EQUALIZED = 40;

    /**
     * Constructs an instance
     *
     * @param feedbackDecoder parent decoder
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     */
    public NXDNSymbolProcessor(FeedbackDecoder feedbackDecoder, C4FMMessageFramer messageFramer)
    {
        super(feedbackDecoder, messageFramer, NXDNSyncDetectorFactory.getStandardDetector(),
                NXDNSyncDetectorFactory.getStandardDetector());
    }

    @Override
    public boolean isDynamicBalanceTrackingRequired()
    {
        return false;
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
