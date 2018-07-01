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
package io.github.dsheirer.gui.instrument.chart;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AFSK1200ZeroCrossingErrorDetectorChart extends LineChart
{
    private final static Logger mLog = LoggerFactory.getLogger(AFSK1200ZeroCrossingErrorDetectorChart.class);

    private ObservableList<Data<Number,Number>> mCurrentSamples = FXCollections.observableArrayList();
    private Series<Number,Number> mCurrentSampleSeries = new Series<>("Samples", mCurrentSamples);

    private ObservableList<Data<Number,Number>> mPreviousSamples = FXCollections.observableArrayList();
    private Series<Number,Number> mPreviousSampleSeries = new Series<>("Samples", mPreviousSamples);

    private ObservableList<Data<Number,Number>> mDetected = FXCollections.observableArrayList();
    private Series<Number,Number> mDetectedSeries = new Series<>("Detected", mDetected);

    private IInstrumentedAFSK1200Decoder mIInstrumentedAFSK1200Decoder;

    public AFSK1200ZeroCrossingErrorDetectorChart(IInstrumentedAFSK1200Decoder decoder, int length)
    {
        super(new NumberAxis("Samples", 0, length, 5),
            new NumberAxis("Value", -1.0, 1.0, 0.25));

        ObservableList<Series> observableList = FXCollections.observableArrayList(mCurrentSampleSeries,
            mPreviousSampleSeries, mDetectedSeries);
        setData(observableList);

        mIInstrumentedAFSK1200Decoder = decoder;
        decoder.getAFSK1200Decoder().getErrorDetector().timingError.addListener(new ErrorChangeListener());
        decoder.getBufferCountProperty().addListener(new ChangeListener()
        {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue)
            {
                updateBuffer();
            }
        });

        for(int x = 0; x < length; x++)
        {
            Data<Number,Number> sample = new Data<>(x, 0.0f);
            mCurrentSamples.add(sample);
            Data<Number,Number> sample2 = new Data<>(x, 0.0f);
            mPreviousSamples.add(sample2);
        }

        Data<Number,Number> detected1 = new Data<>(5, -0.25f);
        mDetected.add(detected1);
        Data<Number,Number> detected2 = new Data<>(5, -0.75f);
        mDetected.add(detected2);
    }

    private void updateBuffer()
    {
        boolean[] samples = mIInstrumentedAFSK1200Decoder.getAFSK1200Decoder().getErrorDetector().getBuffer();

        for(int x = 0; x < samples.length; x++)
        {
            Data<Number,Number> sample = mCurrentSamples.get(x);
            sample.setYValue(samples[x] ? 0.7f : 0.3f);
        }
    }

    public class ErrorChangeListener implements ChangeListener
    {
        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue)
        {
            float error = mIInstrumentedAFSK1200Decoder.getAFSK1200Decoder().getErrorDetector().getError();

            boolean[] samples = mIInstrumentedAFSK1200Decoder.getAFSK1200Decoder().getErrorDetector().getBuffer();

            for(int x = 0; x < samples.length; x++)
            {
                Data<Number,Number> sample = mPreviousSamples.get(x);
                sample.setYValue(samples[x] ? -0.4f : -0.6f);
            }

            if(error == 0.0f)
            {
                Data<Number,Number> detected1 = mDetected.get(0);
                detected1.setXValue(0.0f);
                Data<Number,Number> detected2 = mDetected.get(1);
                detected2.setXValue(0.0f);
            }
            else
            {
                Data<Number,Number> detected1 = mDetected.get(0);
                detected1.setXValue(mIInstrumentedAFSK1200Decoder.getAFSK1200Decoder().getErrorDetector().getDetectedZeroCrossing());
                Data<Number,Number> detected2 = mDetected.get(1);
                detected2.setXValue(mIInstrumentedAFSK1200Decoder.getAFSK1200Decoder().getErrorDetector().getDetectedZeroCrossing());
            }
        }
    }
}
