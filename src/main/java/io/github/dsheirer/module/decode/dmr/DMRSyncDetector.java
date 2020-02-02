package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.QPSKCarrierLock;
import org.apache.commons.lang3.Validate;

/**
 * Detector for DMR sync patterns that also includes support for detecting PLL phase lock errors.  There are
 * two primary use cases for this class:
 *
 * The first is to detect a sync pattern from a stream of dibits that are processed via the add(dibit) method.  After
 * each dibit is added, the sync pattern can be inspected by either the hasSync() method or the getSyncPattern()
 * method.  The maxStreamBitErrors construction parameter specifies how many bit errors can be present when matching
 * patterns to the dibit stream.
 *
 * The second use case is to set a candidate sync pattern value using the setCurrentSyncValue() method.  You can then
 * use the hasSync() and getSyncPatter() methods to determine if the set value matches a valid sync pattern.  The
 * maxExplicitBitErrors specifies how many bit errors can be present when matching patterns to the explicitly set
 * sync value.
 *
 * Note: sync pattern matching maximum bit errors uses a hard-coded value for PLL misalignment sync detection since
 * this threshold should be relatively high to avoid false triggering and once detected, a PLL should be immediately
 * commanded to correct the issue.
 */
public class DMRSyncDetector
{
    private static long SYNC_MASK = 0xFFFFFFFFFFFFl;
    private static final int MAX_PATTERN_BIT_ERROR_PLL_MISALIGNMENT = 1;
    private int mMaxStreamBitErrors;
    private int mMaxExplicitBitErrors;
    private long mCurrentSyncValue;
    private long mErrorPattern;
    private int mPatternMatchBitErrorCount;
    private DMRSyncPattern mSyncPattern = DMRSyncPattern.UNKNOWN;
    private QPSKCarrierLock mCarrierLock = QPSKCarrierLock.NORMAL;

    /**
     * Constructs an instance
     * @param maxStreamBitErrors allowed for matching sync patterns when using the dibit streaming use case.
     * @param maxExplicitBitErrors allows for matching sync patterns when using the explicitly set sync value use case.
     */
    public DMRSyncDetector(int maxStreamBitErrors, int maxExplicitBitErrors)
    {
        Validate.inclusiveBetween(0, 24, maxStreamBitErrors,
            "Max (allowable) stream bit errors for sync match must be between 0 and 24");
        Validate.inclusiveBetween(0, 24, maxExplicitBitErrors,
            "Max (allowable) explicit bit errors for sync match must be between 0 and 24");
        mMaxStreamBitErrors = maxStreamBitErrors;
        mMaxExplicitBitErrors = maxExplicitBitErrors;
    }

    /**
     * Sync pattern for the current sync value.
     */
    public DMRSyncPattern getSyncPattern()
    {
        return mSyncPattern;
    }

    /**
     * Indicates if there is a sync pattern in the currently set sync value that was either explicitly set or was
     * aggregated from a stream of dibits.  When this method returns true, the sync pattern can be accessed via the
     * getSyncPattern() method.
     */
    public boolean hasSync()
    {
        return getSyncPattern() != DMRSyncPattern.UNKNOWN;
    }

    /**
     * Carrier lock for the QPSK PLL to detect when the PLL has locked to the signal at +/- 90 degrees or 180 degrees.
     * @return carrier lock value.
     */
    public QPSKCarrierLock getCarrierLock()
    {
        return mCarrierLock;
    }

    /**
     * Indicates if the detected sync pattern has normal QPSK carrier lock.
     */
    public boolean hasNormalCarrierLock()
    {
        return getCarrierLock() == QPSKCarrierLock.NORMAL;
    }

    /**
     * Number of bit errors that were present when matching the current sync value to the current sync pattern.
     */
    public int getPatternMatchBitErrorCount()
    {
        return mPatternMatchBitErrorCount;
    }

    /**
     * Processes the streaming dibit by left shifting the current sync value and adding the dibit onto the end and then
     * test for sync pattern match.
     * @param dibit to process and test for sync pattern.
     */
    public void add(Dibit dibit)
    {
        mCurrentSyncValue = Long.rotateLeft(mCurrentSyncValue, 2);
        mCurrentSyncValue &= SYNC_MASK;
        mCurrentSyncValue += dibit.getValue();
        checkSync(mMaxStreamBitErrors);
    }

    /**
     * Explicitly sets the argument as the current sync value and tests for sync pattern match.
     * @param value to load as the current sync value and test
     */
    public void setCurrentSyncValue(long value)
    {
        mCurrentSyncValue = value;
        checkSync(mMaxExplicitBitErrors);
    }

    /**
     * Checks the current sync value against each of the sync patterns to determine if the value matches a pattern.
     */
    private void checkSync(int maxBitErrors)
    {
        for(DMRSyncPattern pattern: DMRSyncPattern.SYNC_PATTERNS)
        {
            mErrorPattern = mCurrentSyncValue ^ pattern.getPattern();

            if(mErrorPattern == 0)
            {
                mSyncPattern = pattern;
                mCarrierLock = QPSKCarrierLock.NORMAL;
                mPatternMatchBitErrorCount = 0;
                return;
            }

            mPatternMatchBitErrorCount = Long.bitCount(mErrorPattern);

            if(mPatternMatchBitErrorCount <= maxBitErrors)
            {
                mSyncPattern = pattern;
                mCarrierLock = QPSKCarrierLock.NORMAL;
                return;
            }


            //For PLL mis-aligned lock patterns, reduce the pattern bit error match threshold
            mPatternMatchBitErrorCount = Long.bitCount(mCurrentSyncValue ^ pattern.getPlus90Pattern());

            if(mPatternMatchBitErrorCount <= MAX_PATTERN_BIT_ERROR_PLL_MISALIGNMENT)
            {
                mSyncPattern = pattern;
                mCarrierLock = QPSKCarrierLock.PLUS_90;
                return;
            }

            mPatternMatchBitErrorCount = Long.bitCount(mCurrentSyncValue ^ pattern.getMinus90Pattern());

            if(mPatternMatchBitErrorCount <= MAX_PATTERN_BIT_ERROR_PLL_MISALIGNMENT)
            {
                mSyncPattern = pattern;
                mCarrierLock = QPSKCarrierLock.MINUS_90;
                return;
            }

            mPatternMatchBitErrorCount = Long.bitCount(mCurrentSyncValue ^ pattern.getInvertedPattern());

            if(mPatternMatchBitErrorCount <= MAX_PATTERN_BIT_ERROR_PLL_MISALIGNMENT)
            {
                mSyncPattern = pattern;
                mCarrierLock = QPSKCarrierLock.INVERTED;
                return;
            }
        }

        mSyncPattern = DMRSyncPattern.UNKNOWN;
        mCarrierLock = QPSKCarrierLock.NORMAL;
        mPatternMatchBitErrorCount = 0;
    }
}
