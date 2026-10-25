package io.github.dsheirer.dsp.c4fm;

import io.github.dsheirer.dsp.symbol.Dibit;

/**
 * C4FM message framer base class.
 */
public abstract class C4FMMessageFramer
{
    /**
     * Process the symbol
     * @param symbol in radians
     * @return true if the framer emits a valid message that includes this symbol, so that corrective measurement
     * can be sent to a feedback decoder.
     */
    public abstract boolean process(Dibit symbol);

    /**
     * Process the symbol decision along with the soft symbol it was decided from.  Framers that can use soft symbols
     * (ie soft decision decoding) override this method.  Default implementation discards the soft symbol.
     * @param symbol decision
     * @param softSymbol equalized soft symbol in radians
     * @return true if the framer emits a valid message that includes this symbol.
     */
    public boolean process(Dibit symbol, float softSymbol)
    {
        return process(symbol);
    }

    /**
     * A sync pattern is detected
     */
    public abstract void syncDetected();
}
