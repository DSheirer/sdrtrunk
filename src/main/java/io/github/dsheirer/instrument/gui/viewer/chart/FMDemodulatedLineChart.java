/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.instrument.gui.viewer.chart;

import io.github.dsheirer.buffer.DoubleCircularBuffer;
import io.github.dsheirer.dsp.fm.FMDemodulator;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.complex.Complex;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FMDemodulatedLineChart extends LineChart implements Listener<ReusableComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(FMDemodulatedLineChart.class);

    private DoubleCircularBuffer mDoubleCircularBuffer;
    private int mSamplesPerSymbol;
    private int mTwiceSamplesPerSymbol;
    private int mSeriesCount;
    private ObservableList<Series<Integer,Double>> mData = FXCollections.observableArrayList();
    private Complex mPreviousSample = new Complex(0,0);
    private int mSeriesPointer;
    private int mSamplePointer;

    public FMDemodulatedLineChart(int samplesPerSymbol, int seriesCount)
    {
        super(new NumberAxis("Time", 1, samplesPerSymbol * 2, 2),
            new NumberAxis("Demodulated", -1.0, 1.0, 0.25));

        mSamplesPerSymbol = samplesPerSymbol;
        mTwiceSamplesPerSymbol = mSamplesPerSymbol * 2;
        mSeriesCount = seriesCount;

        init();
    }

    private void init()
    {
        int seriesLength = 2 * mSamplesPerSymbol;
        mDoubleCircularBuffer = new DoubleCircularBuffer(seriesLength * mSeriesCount);

        for(int x = 0; x < mSeriesCount; x++)
        {
            ObservableList<Data<Integer,Double>> series = FXCollections.observableArrayList();

            for(int y = 0; y < seriesLength; y++)
            {
                series.add(new Data<Integer,Double>(y + 1, 0.0));
            }

            mData.add(new Series<>(series));
        }

        setData(mData);
    }

    @Override
    public void receive(ReusableComplexBuffer complexBuffer)
    {
        float[] samples = complexBuffer.getSamples();

        Complex sample;

        for(int x = 0; x < samples.length; x += 2)
        {
            sample = new Complex(samples[x], samples[x + 1]);
            double demodulated = FMDemodulator.demodulate(mPreviousSample, sample);
            demodulated *= 2.0;

            if(demodulated > 1.0)
            {
                demodulated = 1.0;
            }
            else if(demodulated < -1.0)
            {
                demodulated = -1.0;
            }
            mDoubleCircularBuffer.put(demodulated);
            mPreviousSample = sample;
            updateChart(demodulated);
        }

        complexBuffer.decrementUserCount();
    }

    private void updateChart(double value)
    {
        Series<Integer,Double> series = mData.get(mSeriesPointer);
        Data data = series.getData().get(mSamplePointer++);
        data.setYValue(value);

        if(mSamplePointer >= mTwiceSamplesPerSymbol)
        {
            mSamplePointer = 0;
            mSeriesPointer++;

            if(mSeriesPointer >= mSeriesCount)
            {
                mSeriesPointer = 0;
            }
        }
    }
}
