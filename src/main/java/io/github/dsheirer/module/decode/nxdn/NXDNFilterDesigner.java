package io.github.dsheirer.module.decode.nxdn;

/**
 * Designs an inverse sinc (x / sin(x)) FIR filter for NXDN 4FSK receivers.
 *
 * The NXDN transmitter applies a sinc response, sin(pi*f*T)/(pi*f*T), to the symbol stream along with the root
 * raised cosine filter. The receiver cascades this inverse sinc filter, H(f) = (pi*f*T)/sin(pi*f*T), with its own
 * root raised cosine filter to restore an overall raised cosine (Nyquist) response.
 *
 * The inverse sinc grows without bound as f approaches the symbol rate, so the response is only matched over the
 * occupied bandwidth of the signal, |f| <= (1 + alpha) * symbolRate / 2. Beyond that edge the cascaded RRC filter
 * suppresses the signal, so that region is lightly weighted toward a flat gain equal to the band-edge value, which
 * keeps the filter from boosting out-of-band noise.
 *
 * The taps are a weighted least-squares fit of a linear phase (symmetric, odd length) FIR filter to that target
 * response on a dense frequency grid.
 */
public class NXDNFilterDesigner
{
    /** NXDN root raised cosine roll-off factor. */
    public static final double NXDN_ALPHA = 0.2;

    /** Default filter length in symbol periods. */
    private static final int DEFAULT_SYMBOL_SPAN = 4;

    /** Least-squares weight applied beyond the band edge, relative to a weight of 1.0 in band. */
    private static final double STOPBAND_WEIGHT = 0.01;

    /** Frequency grid points per filter coefficient used for the least-squares fit. */
    private static final int GRID_DENSITY = 16;

    /**
     * Designs an inverse sinc filter using the NXDN roll-off factor (0.2) to define the matched bandwidth.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second (2400 for NXDN48, 4800 for NXDN96)
     * @return filter taps, odd length, linear phase, unity gain at DC
     */
    public static float[] design(double sampleRate, double symbolRate)
    {
        return design(sampleRate, symbolRate, NXDN_ALPHA, DEFAULT_SYMBOL_SPAN);
    }

    /**
     * Designs an inverse sinc filter.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second
     * @param alpha roll-off factor of the companion RRC filter, defining the band edge (1 + alpha) * symbolRate / 2
     * @param symbolSpan filter length in symbol periods
     * @return filter taps, odd length, linear phase, unity gain at DC
     */
    public static float[] design(double sampleRate, double symbolRate, double alpha, int symbolSpan)
    {
        if(sampleRate <= 0 || symbolRate <= 0)
        {
            throw new IllegalArgumentException("Sample rate and symbol rate must be positive");
        }

        if(alpha < 0 || alpha > 1)
        {
            throw new IllegalArgumentException("Alpha must be in the range 0 - 1");
        }

        if(symbolSpan < 1)
        {
            throw new IllegalArgumentException("Symbol span must be at least 1");
        }

        double nyquist = sampleRate / 2.0;
        double bandEdge = (1.0 + alpha) * symbolRate / 2.0;

        if(bandEdge >= nyquist)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate +
                "] is too low for symbol rate [" + symbolRate + "] - band edge exceeds Nyquist");
        }

        //Odd tap count so the filter has an integer group delay of (length - 1) / 2 samples
        int length = (int)Math.ceil(symbolSpan * sampleRate / symbolRate);
        if(length % 2 == 0)
        {
            length++;
        }
        length = Math.max(length, 9);

        int middle = (length - 1) / 2;
        int coefficientCount = middle + 1;
        double symbolPeriod = 1.0 / symbolRate;
        double edgeGain = inverseSinc(bandEdge * symbolPeriod);

        //A symmetric filter has the zero-phase response A(f) = c[0] + sum(m=1..middle) c[m] * 2cos(2*pi*f*m/fs)
        //where c[m] = h[middle - m] = h[middle + m]. Accumulate the weighted normal equations (B'WB)c = B'Wd over a
        //dense frequency grid from DC to Nyquist.
        double[][] normal = new double[coefficientCount][coefficientCount];
        double[] rhs = new double[coefficientCount];
        double[] basis = new double[coefficientCount];
        int gridSize = GRID_DENSITY * coefficientCount;

        for(int g = 0; g <= gridSize; g++)
        {
            double frequency = nyquist * g / gridSize;
            boolean inBand = frequency <= bandEdge;
            double desired = inBand ? inverseSinc(frequency * symbolPeriod) : edgeGain;
            double weight = inBand ? 1.0 : STOPBAND_WEIGHT;

            basis[0] = 1.0;
            for(int m = 1; m < coefficientCount; m++)
            {
                basis[m] = 2.0 * Math.cos(2.0 * Math.PI * frequency * m / sampleRate);
            }

            for(int i = 0; i < coefficientCount; i++)
            {
                double weighted = weight * basis[i];
                rhs[i] += weighted * desired;

                for(int j = i; j < coefficientCount; j++)
                {
                    normal[i][j] += weighted * basis[j];
                }
            }
        }

        //Fill in the lower triangle of the symmetric normal matrix
        for(int i = 0; i < coefficientCount; i++)
        {
            for(int j = 0; j < i; j++)
            {
                normal[i][j] = normal[j][i];
            }
        }

        double[] coefficients = solve(normal, rhs);

        //Expand to the full symmetric impulse response and normalize to unity gain at DC
        double[] taps = new double[length];
        double sum = 0.0;

        for(int m = 0; m < coefficientCount; m++)
        {
            taps[middle - m] = coefficients[m];
            taps[middle + m] = coefficients[m];
        }

        for(double tap : taps)
        {
            sum += tap;
        }

        float[] output = new float[length];
        for(int n = 0; n < length; n++)
        {
            output[n] = (float)(taps[n] / sum);
        }

        return output;
    }

    /** Default root raised cosine filter length in symbol periods for the combined NXDN receive filter. */
    private static final int DEFAULT_RRC_SYMBOL_SPAN = 8;

    /**
     * Designs the complete NXDN receive filter: a root raised cosine filter (alpha 0.2) convolved with the inverse
     * sinc filter. Cascaded with the NXDN transmit filter (RRC and sinc), the overall response is a raised cosine
     * Nyquist filter with no intersymbol interference at the symbol sampling instants.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second (2400 for NXDN48, 4800 for NXDN96)
     * @return filter taps, odd length, linear phase, unity gain at DC
     */
    public static float[] designNXDN(double sampleRate, double symbolRate)
    {
        return designNXDN(sampleRate, symbolRate, DEFAULT_RRC_SYMBOL_SPAN, DEFAULT_SYMBOL_SPAN);
    }

    /**
     * Designs the complete NXDN receive filter: a root raised cosine filter (alpha 0.2) convolved with the inverse
     * sinc filter.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second
     * @param rrcSymbolSpan root raised cosine filter length in symbol periods
     * @param inverseSincSymbolSpan inverse sinc filter length in symbol periods
     * @return filter taps, odd length (sum of both filter lengths - 1), linear phase, unity gain at DC
     */
    public static float[] designNXDN(double sampleRate, double symbolRate, int rrcSymbolSpan, int inverseSincSymbolSpan)
    {
        double[] rrc = designRootRaisedCosine(sampleRate, symbolRate, NXDN_ALPHA, rrcSymbolSpan);
        float[] inverseSinc = design(sampleRate, symbolRate, NXDN_ALPHA, inverseSincSymbolSpan);

        //Full linear convolution - both filters are odd length and symmetric, so the result is too
        double[] combined = new double[rrc.length + inverseSinc.length - 1];

        for(int i = 0; i < rrc.length; i++)
        {
            for(int j = 0; j < inverseSinc.length; j++)
            {
                combined[i + j] += rrc[i] * inverseSinc[j];
            }
        }

        //Normalize to unity gain at DC so the discriminator output keeps its deviation scaling
        double sum = 0.0;
        for(double tap : combined)
        {
            sum += tap;
        }

        float[] output = new float[combined.length];
        for(int n = 0; n < combined.length; n++)
        {
            output[n] = (float)(combined[n] / sum);
        }

        return output;
    }

    /**
     * Designs a root raised cosine filter from the closed form impulse response, normalized to unity gain at DC.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second
     * @param alpha roll-off factor (0 - 1)
     * @param symbolSpan filter length in symbol periods
     * @return filter taps, odd length, linear phase
     */
    public static double[] designRootRaisedCosine(double sampleRate, double symbolRate, double alpha, int symbolSpan)
    {
        if(sampleRate <= 0 || symbolRate <= 0)
        {
            throw new IllegalArgumentException("Sample rate and symbol rate must be positive");
        }

        if(alpha <= 0 || alpha > 1)
        {
            throw new IllegalArgumentException("Alpha must be in the range (0 - 1]");
        }

        if(symbolSpan < 1)
        {
            throw new IllegalArgumentException("Symbol span must be at least 1");
        }

        int length = (int)Math.ceil(symbolSpan * sampleRate / symbolRate);
        if(length % 2 == 0)
        {
            length++;
        }

        int middle = (length - 1) / 2;
        double[] taps = new double[length];
        double sum = 0.0;

        for(int n = 0; n < length; n++)
        {
            //Time in symbol periods relative to the filter center
            double t = (n - middle) * symbolRate / sampleRate;
            double value;

            if(Math.abs(t) < 1e-9)
            {
                value = 1.0 - alpha + 4.0 * alpha / Math.PI;
            }
            else if(Math.abs(Math.abs(t) - 1.0 / (4.0 * alpha)) < 1e-9)
            {
                //Limit at the singularity t = +/- T / (4 * alpha)
                value = (alpha / Math.sqrt(2.0)) *
                    ((1.0 + 2.0 / Math.PI) * Math.sin(Math.PI / (4.0 * alpha)) +
                     (1.0 - 2.0 / Math.PI) * Math.cos(Math.PI / (4.0 * alpha)));
            }
            else
            {
                double numerator = Math.sin(Math.PI * t * (1.0 - alpha)) +
                    4.0 * alpha * t * Math.cos(Math.PI * t * (1.0 + alpha));
                double denominator = Math.PI * t * (1.0 - Math.pow(4.0 * alpha * t, 2.0));
                value = numerator / denominator;
            }

            taps[n] = value;
            sum += value;
        }

        for(int n = 0; n < length; n++)
        {
            taps[n] /= sum;
        }

        return taps;
    }

    /**
     * Inverse sinc: x / sin(x) where x = pi * f * T.
     *
     * @param normalizedFrequency f * T (frequency divided by the symbol rate)
     */
    private static double inverseSinc(double normalizedFrequency)
    {
        if(normalizedFrequency == 0.0)
        {
            return 1.0;
        }

        double x = Math.PI * normalizedFrequency;
        return x / Math.sin(x);
    }

    /**
     * Solves Ax = b using Gaussian elimination with partial pivoting. A and b are modified.
     */
    private static double[] solve(double[][] a, double[] b)
    {
        int n = b.length;

        for(int column = 0; column < n; column++)
        {
            int pivot = column;
            for(int row = column + 1; row < n; row++)
            {
                if(Math.abs(a[row][column]) > Math.abs(a[pivot][column]))
                {
                    pivot = row;
                }
            }

            double[] swapRow = a[column];
            a[column] = a[pivot];
            a[pivot] = swapRow;
            double swapValue = b[column];
            b[column] = b[pivot];
            b[pivot] = swapValue;

            for(int row = column + 1; row < n; row++)
            {
                double factor = a[row][column] / a[column][column];
                b[row] -= factor * b[column];

                for(int k = column; k < n; k++)
                {
                    a[row][k] -= factor * a[column][k];
                }
            }
        }

        double[] x = new double[n];
        for(int row = n - 1; row >= 0; row--)
        {
            double sum = b[row];
            for(int k = row + 1; k < n; k++)
            {
                sum -= a[row][k] * x[k];
            }
            x[row] = sum / a[row][row];
        }

        return x;
    }
}
