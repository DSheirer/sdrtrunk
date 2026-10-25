package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.dsp.c4fm.C4FMSymbolProcessor;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.NXDNSyncDetectorFactory;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;

/**
 * NXDN implementation of the symbol processor.
 */
public class NXDNSymbolProcessor extends C4FMSymbolProcessor
{
    private static final int MAX_MESSAGE_DIBIT_LENGTH = 192;
    private static final int EQUALIZER_TRAINING_PERIOD_SYMBOL_COUNT = 192 * 3;
    private static final int EQUALIZER_SKIP_TRAINING_FRAME_COUNT = 10;
    //Equalizer span in symbols (14x T/2 taps).  Testing against NXDN recordings: 7 yields the most valid messages on
    //low SNR and mistuned recordings compared to the default sync-length span of 9 symbols.
    private static final int EQUALIZER_SYMBOL_LENGTH = 7;
    private static final float SYNC_THRESHOLD_DETECTED = 37;
    private static final float SYNC_THRESHOLD_OPTIMIZED = 40;
    private static final float SYNC_THRESHOLD_EQUALIZED = 40;
    private final TransmissionMode mTransmissionMode;

    /**
     * Constructs an instance
     *
     * @param feedbackDecoder parent decoder
     * @param messageFramer to receive symbol decisions (dibits) and sync notifications.
     * @param transmissionMode to determine the symbol rate and symbol deviation.
     */
    public NXDNSymbolProcessor(FeedbackDecoder feedbackDecoder, C4FMMessageFramer messageFramer,
                               TransmissionMode transmissionMode)
    {
        super(feedbackDecoder, messageFramer, NXDNSyncDetectorFactory.getStandardDetector(),
                NXDNSyncDetectorFactory.getStandardDetector());
        mTransmissionMode = transmissionMode;
    }

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
    public int getEqualizerSymbolLength()
    {
        return EQUALIZER_SYMBOL_LENGTH;
    }

    @Override
    public int getEqualizerSkipTrainingFrameCount()
    {
        return EQUALIZER_SKIP_TRAINING_FRAME_COUNT;
    }

    @Override
    public int getSymbolRate()
    {
        return mTransmissionMode.getSymbolRate();
    }

    @Override
    public double getOuterSymbolDeviation()
    {
        return mTransmissionMode.getOuterSymbolDeviation();
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
