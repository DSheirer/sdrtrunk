/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.gui.instrument.chart;

import io.github.dsheirer.module.decode.ltrnet.LTRNetDecoderInstrumented;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZeroCrossingErrorDetectorChart extends LineChart
{
    private final static Logger mLog = LoggerFactory.getLogger(ZeroCrossingErrorDetectorChart.class);

    private ObservableList<Data<Number,Number>> mCurrentSamples = FXCollections.observableArrayList();
    private Series<Number,Number> mCurrentSampleSeries = new Series<>("Samples", mCurrentSamples);

    private ObservableList<Data<Number,Number>> mPreviousSamples = FXCollections.observableArrayList();
    private Series<Number,Number> mPreviousSampleSeries = new Series<>("Samples", mPreviousSamples);

    private ObservableList<Data<Number,Number>> mIdeal = FXCollections.observableArrayList();
    private Series<Number,Number> mIdealSeries = new Series<>("Ideal", mIdeal);

    private ObservableList<Data<Number,Number>> mDetected = FXCollections.observableArrayList();
    private Series<Number,Number> mDetectedSeries = new Series<>("Detected", mDetected);

    private LTRNetDecoderInstrumented mLTRNetDecoderInstrumented;

    public ZeroCrossingErrorDetectorChart(LTRNetDecoderInstrumented decoder, int length)
    {
        super(new NumberAxis("Samples", 0, length, 5),
            new NumberAxis("Value", -1.0, 1.0, 0.25));

        ObservableList<Series> observableList = FXCollections.observableArrayList(mCurrentSampleSeries,
            mPreviousSampleSeries, mIdealSeries, mDetectedSeries);
        setData(observableList);

        mLTRNetDecoderInstrumented = decoder;
//        decoder.getLTRDecoder().getErrorDetector().timingError.addListener(new ErrorChangeListener());
        decoder.bufferCount.addListener(new ChangeListener()
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

        Data<Number,Number> ideal1 = new Data<>(5, -0.1f);
        mIdeal.add(ideal1);
        Data<Number,Number> ideal2 = new Data<>(5, -0.9f);
        mIdeal.add(ideal2);

        Data<Number,Number> detected1 = new Data<>(5, -0.25f);
        mDetected.add(detected1);
        Data<Number,Number> detected2 = new Data<>(5, -0.75f);
        mDetected.add(detected2);
    }

    private void updateBuffer()
    {
//        boolean[] samples = mLTRNetDecoderInstrumented.getLTRDecoder().getErrorDetector().getBuffer();
//
//        for(int x = 0; x < samples.length; x++)
//        {
//            Data<Number,Number> sample = mCurrentSamples.get(x);
//            sample.setYValue(samples[x] ? 0.7f : 0.3f);
//        }
    }

    public class ErrorChangeListener implements ChangeListener
    {
        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue)
        {
//            float error = mLTRNetDecoderInstrumented.getLTRDecoder().getErrorDetector().getError();
//
//            boolean[] samples = mLTRNetDecoderInstrumented.getLTRDecoder().getErrorDetector().getBuffer();
//
//            for(int x = 0; x < samples.length; x++)
//            {
//                Data<Number,Number> sample = mPreviousSamples.get(x);
//                sample.setYValue(samples[x] ? -0.4f : -0.6f);
//            }
//
//            Data<Number,Number> ideal1 = mIdeal.get(0);
//            ideal1.setXValue(mLTRNetDecoderInstrumented.getLTRDecoder().getErrorDetector().getZeroCrossingIdeal());
//            Data<Number,Number> ideal2 = mIdeal.get(1);
//            ideal2.setXValue(mLTRNetDecoderInstrumented.getLTRDecoder().getErrorDetector().getZeroCrossingIdeal());
//
//            if(error == 0.0f)
//            {
//                Data<Number,Number> detected1 = mDetected.get(0);
//                detected1.setXValue(0.0f);
//                Data<Number,Number> detected2 = mDetected.get(1);
//                detected2.setXValue(0.0f);
//            }
//            else
//            {
//                Data<Number,Number> detected1 = mDetected.get(0);
//                detected1.setXValue(mLTRNetDecoderInstrumented.getLTRDecoder().getErrorDetector().getDetectedZeroCrossing());
//                Data<Number,Number> detected2 = mDetected.get(1);
//                detected2.setXValue(mLTRNetDecoderInstrumented.getLTRDecoder().getErrorDetector().getDetectedZeroCrossing());
//            }
        }
    }
}
