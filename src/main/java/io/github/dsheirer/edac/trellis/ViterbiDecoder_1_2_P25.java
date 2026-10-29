/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.edac.trellis;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;

import java.util.List;

/**
 * Viterbi decoder for APCO-25 1/2 rate Trellis Coded Modulation (TCM) encoded messages.
 */
public class ViterbiDecoder_1_2_P25 extends ViterbiDecoder
{
    /**
     * Maximum per-symbol error magnitude (radians) applied to the soft decision branch metric.  FM discriminator noise
     * is impulsive (clicks), and an unbounded squared error lets a single corrupted symbol dominate the path metric.
     * Noise injection testing against a P25 control channel recording found a peak at 1.75 - 2.0 radians.  Noise
     * injection testing against P25 LSM recordings (phase wrapped soft symbols) also found 1.75 performed best.
     */
    private static final double SOFT_SYMBOL_ERROR_CLIP = 1.75;
    /**
     * Per-symbol error magnitude clip applied when the branch metric of phase wrapped soft symbols (ie P25 LSM) is
     * weighted by symbol reliabilities.  The reliability weighting de-emphasizes the corrupted (low envelope) symbols
     * that the clip otherwise protects against, so the weighted metric doesn't clip (PI is the maximum phase wrapped
     * error).  Noise injection testing against P25 LSM recordings found that no clip produced 1.6-7.0% more valid
     * messages than the 1.75 clip.  Soft symbols that aren't phase wrapped (ie C4FM) can have errors beyond PI from FM
     * discriminator clicks, so the weighted metric for those uses the SOFT_SYMBOL_ERROR_CLIP, which testing against a
     * P25 C4FM recording found 4.5% better than no clip.
     */
    private static final double WEIGHTED_PHASE_WRAPPED_SOFT_SYMBOL_ERROR_CLIP = Math.PI;
    private static final double TWO_PI = 2.0 * Math.PI;
    public ViterbiDecoder_1_2_P25()
    {
        super(2,4);
    }

    /**
     * Decodes a 1/2 rate trellis coded modulation (TCM) encoded P25 binary message containing 196 bits that have
     * already been deinterleaved.
     *
     * @param encodedMessage to decode that has already been deinterleaved.
     * @return decoded message
     */
    public CorrectedBinaryMessage decode(BinaryMessage encodedMessage)
    {
        int[] symbols = getSymbols(encodedMessage);

        Path mostLikelyPath = decode(symbols);

        return getMessage(mostLikelyPath);
    }

    /**
     * Soft decision Viterbi decoding of a deinterleaved 1/2 rate TCM encoded P25 block.  Each 4-bit trellis output
     * value is transmitted as two dibits/symbols, so the branch metric is the squared Euclidean distance between the
     * two received soft symbols and the ideal symbol phases for the branch output value.
     *
     * The corrected bit count on the returned message is the Hamming distance between the hard decisions of the
     * received soft symbols and the re-encoded output of the decoded path, consistent with hard decision decoding.
     *
     * @param softSymbols 98 deinterleaved soft symbols (radians, ideal values +/- PI/4 and +/- 3*PI/4).
     * @return decoded 96-bit message
     */
    public CorrectedBinaryMessage decodeSoft(float[] softSymbols)
    {
        return decodeSoft(softSymbols, false);
    }

    /**
     * Soft decision Viterbi decoding of a deinterleaved 1/2 rate TCM encoded P25 block.  See decodeSoft(float[]).
     *
     * @param softSymbols 98 deinterleaved soft symbols (radians, ideal values +/- PI/4 and +/- 3*PI/4).
     * @param phaseWrapped true when the soft symbols are phase wrapped to +/- PI (ie differentially demodulated P25 LSM)
     * where the soft symbol error is measured around the circle so that a soft symbol near +/- PI is near both the
     * +3 and -3 symbols.  False for C4FM where soft symbols are not wrapped and a soft symbol beyond +/- PI favors the
     * outer symbol of the same polarity.
     * @return decoded 96-bit message
     */
    public CorrectedBinaryMessage decodeSoft(float[] softSymbols, boolean phaseWrapped)
    {
        return decodeSoft(softSymbols, null, phaseWrapped);
    }

    /**
     * Soft decision Viterbi decoding of a deinterleaved 1/2 rate TCM encoded P25 block with each symbol's branch metric
     * weighted by the symbol's reliability (ie the inverse of the soft symbol noise variance).  See decodeSoft(float[]).
     *
     * The weights only need to be proportional to the symbol reliabilities, since they're normalized to unity mean
     * across the block.
     *
     * @param softSymbols 98 deinterleaved soft symbols (radians, ideal values +/- PI/4 and +/- 3*PI/4).
     * @param reliabilities for each deinterleaved soft symbol, or null to decode without weighting.
     * @param phaseWrapped true when the soft symbols are phase wrapped to +/- PI (ie differentially demodulated P25 LSM)
     * @return decoded 96-bit message
     */
    public CorrectedBinaryMessage decodeSoft(float[] softSymbols, float[] reliabilities, boolean phaseWrapped)
    {
        if(softSymbols == null || softSymbols.length % 2 != 0 || softSymbols.length < 4)
        {
            throw new IllegalArgumentException("Soft symbols must contain an integral number of symbol pairs");
        }

        double[] weights = getWeights(reliabilities, softSymbols.length);
        double clip = weights != null && phaseWrapped ? WEIGHTED_PHASE_WRAPPED_SOFT_SYMBOL_ERROR_CLIP :
                SOFT_SYMBOL_ERROR_CLIP;

        int steps = softSymbols.length / 2;
        int states = getInputValueCount();
        double[] metrics = new double[states];
        double[] nextMetrics = new double[states];
        int[][] previousState = new int[steps][states];
        java.util.Arrays.fill(metrics, Double.MAX_VALUE);
        metrics[0] = 0.0; //Encoder starts in state 0

        //Branch metric for each of the 16 possible 4-bit output values at the current step
        double[] branchMetrics = new double[getOutputValueCount()];

        for(int step = 0; step < steps; step++)
        {
            float first = softSymbols[2 * step];
            float second = softSymbols[2 * step + 1];

            for(int output = 0; output < branchMetrics.length; output++)
            {
                double errorFirst = Math.min(getError(first, getIdealPhase(output >> 2), phaseWrapped), clip);
                double errorSecond = Math.min(getError(second, getIdealPhase(output & 3), phaseWrapped), clip);

                if(weights != null)
                {
                    branchMetrics[output] = weights[2 * step] * errorFirst * errorFirst +
                            weights[2 * step + 1] * errorSecond * errorSecond;
                }
                else
                {
                    branchMetrics[output] = errorFirst * errorFirst + errorSecond * errorSecond;
                }
            }

            java.util.Arrays.fill(nextMetrics, Double.MAX_VALUE);

            //The final step is the flushing step where the encoder input is zero
            int inputCount = (step == steps - 1) ? 1 : states;

            for(int state = 0; state < states; state++)
            {
                if(metrics[state] == Double.MAX_VALUE)
                {
                    continue;
                }

                for(int input = 0; input < inputCount; input++)
                {
                    double metric = metrics[state] + branchMetrics[P25_1_2_Node.TRANSITION_MATRIX[state][input]];

                    if(metric < nextMetrics[input])
                    {
                        nextMetrics[input] = metric;
                        previousState[step][input] = state;
                    }
                }
            }

            double[] swap = metrics;
            metrics = nextMetrics;
            nextMetrics = swap;
        }

        //Trace back from the final (flushed) state 0.  The state after each step equals that step's input value.
        int[] inputs = new int[steps];
        int state = 0;

        for(int step = steps - 1; step >= 0; step--)
        {
            inputs[step] = state;
            state = previousState[step][state];
        }

        //Message bits are the inputs for all but the final flushing step
        CorrectedBinaryMessage message = new CorrectedBinaryMessage((steps - 1) * getInputBitLength());
        int bitErrors = 0;
        int previous = 0;

        for(int step = 0; step < steps; step++)
        {
            int input = inputs[step];

            if(step < steps - 1)
            {
                int offset = step * getInputBitLength();

                if((input & 2) == 2)
                {
                    message.set(offset);
                }

                if((input & 1) == 1)
                {
                    message.set(offset + 1);
                }
            }

            int output = P25_1_2_Node.TRANSITION_MATRIX[previous][input];
            int received = (getDibitValue(softSymbols[2 * step]) << 2) | getDibitValue(softSymbols[2 * step + 1]);
            bitErrors += P25_1_2_Node.HAMMING_ERROR_COUNT[output ^ received];
            previous = input;
        }

        message.setCorrectedBitCount(bitErrors);
        return message;
    }

    /**
     * Absolute error between the soft symbol and the ideal symbol phase.
     * @param softSymbol received (radians)
     * @param idealPhase of the candidate symbol (radians)
     * @param phaseWrapped to measure the error around the circle (+/- PI)
     * @return absolute error in radians
     */
    private static double getError(float softSymbol, float idealPhase, boolean phaseWrapped)
    {
        double error = softSymbol - idealPhase;

        if(phaseWrapped)
        {
            error -= TWO_PI * Math.round(error / TWO_PI);
        }

        return Math.abs(error);
    }

    /**
     * Normalizes the symbol reliabilities to unity mean for use as branch metric weights.
     * @param reliabilities for each soft symbol, or null.
     * @param length of the soft symbol array.
     * @return weights or null when the reliabilities are unavailable or unusable (ie all zero).
     */
    private static double[] getWeights(float[] reliabilities, int length)
    {
        if(reliabilities == null || reliabilities.length != length)
        {
            return null;
        }

        double sum = 0.0;

        for(float reliability : reliabilities)
        {
            if(!(reliability >= 0.0f) || Float.isInfinite(reliability)) //Rejects NaN
            {
                return null;
            }

            sum += reliability;
        }

        if(sum <= 0.0)
        {
            return null;
        }

        double scale = length / sum;
        double[] weights = new double[length];

        for(int x = 0; x < length; x++)
        {
            weights[x] = reliabilities[x] * scale;
        }

        return weights;
    }

    /**
     * Ideal soft symbol phase for a 2-bit dibit value where the MSB is the first transmitted bit.
     */
    private static float getIdealPhase(int dibitValue)
    {
        return switch(dibitValue)
        {
            case 0 -> Dibit.D00_PLUS_1.getIdealPhase();
            case 1 -> Dibit.D01_PLUS_3.getIdealPhase();
            case 2 -> Dibit.D10_MINUS_1.getIdealPhase();
            default -> Dibit.D11_MINUS_3.getIdealPhase();
        };
    }

    /**
     * Hard decision 2-bit dibit value (MSB is the first transmitted bit) for the soft symbol.
     */
    private static int getDibitValue(float softSymbol)
    {
        Dibit dibit = Dibit.fromSample(softSymbol);
        return (dibit.getBit1() ? 2 : 0) | (dibit.getBit2() ? 1 : 0);
    }

    /**
     * Extracts the decoded/corrected message from the surviving path argument.
     *
     * @param path to extract a message from
     * @return corrected binary message.
     */
    private CorrectedBinaryMessage getMessage(Path path)
    {
        List<Node> mNodes = path.getNodes();

        //Each node contains an input value excluding the starting(0) and final flushing(0) nodes.
        CorrectedBinaryMessage message = new CorrectedBinaryMessage((mNodes.size() - 2) * getInputBitLength());

        if(mNodes.size() > 2)
        {
            for(int x = 1; x < mNodes.size() - 1; x++)
            {
                int inputValue = mNodes.get(x).getInputValue();

                int messageOffset = getInputBitLength() * (x - 1);

                if((inputValue & 2) == 2)
                {
                    message.set(messageOffset);
                }

                if((inputValue & 1) == 1)
                {
                    message.set(messageOffset + 1);
                }
            }
        }

        //Transfer the corrected error count to the message
        message.setCorrectedBitCount(path.getError());

        return message;
    }

    /**
     * Extracts (49) four-bit symbols (196 bits / 4) from the deinterleaved P25 binary message
     *
     * @param encodedMessage that deinterleaved and contains 196-bits of 1/2 TCM encoding
     * @return symbols
     */
    public int[] getSymbols(BinaryMessage encodedMessage)
    {
        //Ensure we have an integral number of transmitted symbols (nibbles) in the message
        if(encodedMessage.size() % getOutputBitLength() != 0)
        {
            throw new  IllegalArgumentException("Encoded message must contain an integral number of 4-bit symbols -" +
                " message size: " + encodedMessage.size());
        }

        int[] symbols = new int[encodedMessage.size() / getOutputBitLength()];

        int index = 0;
        int pointer = 0;

        while(index < encodedMessage.size())
        {
            int symbol = encodedMessage.getInt(index, index + (getOutputBitLength() - 1));
            symbols[pointer++] = symbol;
            index += getOutputBitLength();
        }

        return symbols;
    }

    /**
     * Creates a P25 node
     * @param inputValue for the specific trellis node
     * @param transmittedOutputValue the actual transmitted output value
     * @return created node
     */
    @Override
    protected Node createNode(int inputValue, int transmittedOutputValue)
    {
        return new P25_1_2_Node(inputValue, transmittedOutputValue);
    }

    /**
     * Creates a P25 flushing node.  P25 uses an input value of zero to flush the final output value out of the
     * encoder.
     *
     * @param transmittedOutputValue the actual transmitted output value
     * @return
     */
    @Override
    protected Node createFlushingNode(int transmittedOutputValue)
    {
        return new P25_1_2_Node(0, transmittedOutputValue);
    }

    /**
     * Creates a P25 starting node.  P25 uses a starting input value of zero for the initial node.
     */
    @Override
    protected Node createStartingNode()
    {
        return new P25_1_2_Node(0,0);
    }
}
