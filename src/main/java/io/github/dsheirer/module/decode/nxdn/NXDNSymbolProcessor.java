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
}
