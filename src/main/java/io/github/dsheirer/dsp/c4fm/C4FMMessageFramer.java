package io.github.dsheirer.dsp.c4fm;

import io.github.dsheirer.dsp.symbol.Dibit;

/**
 * C4FM message framer base class.
 */
public abstract class C4FMMessageFramer
{
    private static final float SOFT_SYMBOL_SCALE = (float)(4.0 / Math.PI); //Radians to symbol units (+/-1, +/-3)

    /**
     * Converts the soft symbol into a pair of soft bit values using max-log likelihood ratios for the Gray mapped
     * 4-level symbol (D01=+3, D00=+1, D10=-1, D11=-3), where a positive soft bit value favors a 1 bit.
     * @param softSymbol in radians
     * @param softBits array to receive the two soft bit values
     * @param index for the first bit (sign bit), followed by the second (magnitude) bit
     */
    protected static void setSoftBits(float softSymbol, float[] softBits, int index)
    {
        float u = softSymbol * SOFT_SYMBOL_SCALE;
        float magnitude = Math.abs(u);
        //First bit is 1 for negative symbols
        float favorZero = magnitude <= 2.0f ? 4.0f * u : 8.0f * u - 8.0f * Math.signum(u);
        softBits[index] = -favorZero;
        //Second bit is 1 for the outer (+/-3) symbols
        softBits[index + 1] = 4.0f * (magnitude - 2.0f);
    }

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
