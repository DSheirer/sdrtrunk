package io.github.dsheirer.channel.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignalLevelMonitorTest
{
    @Test
    void fullScaleIsZeroDb()
    {
        assertEquals(0.0, SignalLevelMonitor.toDb(1.0), 1e-9);
        assertEquals(-20.0, SignalLevelMonitor.toDb(0.01), 1e-9);
        assertTrue(Double.isInfinite(SignalLevelMonitor.toDb(0)));
    }

    @Test
    void reportsAveragePowerOncePerInterval() throws InterruptedException
    {
        List<Double> levels = new ArrayList<>();
        SignalLevelMonitor monitor = new SignalLevelMonitor(levels::add);
        float[] i = new float[1000];
        float[] q = new float[1000];
        Arrays.fill(i, 0.1f);

        monitor.getComplexSamplesListener().receive(new ComplexSamples(i, q, 0));
        assertTrue(levels.isEmpty());
        Thread.sleep(1050);
        monitor.getComplexSamplesListener().receive(new ComplexSamples(i, q, 0));

        assertEquals(1, levels.size());
        assertEquals(-20.0, levels.get(0), 0.01);
    }
}
