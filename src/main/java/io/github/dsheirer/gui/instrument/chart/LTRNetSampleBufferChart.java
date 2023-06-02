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
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LTRNetSampleBufferChart extends LineChart
{
    private final static Logger mLog = LoggerFactory.getLogger(LTRNetSampleBufferChart.class);
    private ObservableList<Data<Integer,Float>> mSamples = FXCollections.observableArrayList();
    private Series<Integer,Float> mSampleSeries = new Series<>("Samples", mSamples);

    private ObservableList<Data<Number,Number>> mSamplePoints = FXCollections.observableArrayList();
    private Series<Number,Number> mSamplePointSeries = new Series<>("Pointers", mSamplePoints);

    private ObservableList<Data<Number,Number>> mSymbolSamples = FXCollections.observableArrayList();
    private Series<Number,Number> mSymbolSamplesSeries = new Series<>("Next Symbol Samples", mSymbolSamples);

    private ObservableList<Data<Number,Number>> mSymbol = FXCollections.observableArrayList();
    private Series<Number,Number> mSymbolSeries = new Series<>("Previous Symbol", mSymbol);

    private ObservableList<Data<Number,Number>> mZeroCrossing = FXCollections.observableArrayList();
    private Series<Number,Number> mZeroCrossingSeries = new Series<>("Zero Crossing", mZeroCrossing);

    private LTRNetDecoderInstrumented mLTRNetDecoderInstrumented;

    public LTRNetSampleBufferChart(LTRNetDecoderInstrumented decoder, int length)
    {
        super(new NumberAxis("Samples", 0, length, 5),
            new NumberAxis("Value", -1.0, 1.0, 0.25));

        ObservableList<Series> observableList = FXCollections.observableArrayList(mSampleSeries, mSamplePointSeries,
            mSymbolSamplesSeries, mSymbolSeries, mZeroCrossingSeries);

        setData(observableList);

        mLTRNetDecoderInstrumented = decoder;
        decoder.bufferCount.addListener(new BufferChangeListener());
//        decoder.getLTRDecoder().getErrorDetector().timingError.addListener(new ErrorChangeListener());
    }

    public void setBuffer(boolean[] buffer)
    {
        while(mSamples.size() < buffer.length)
        {
            Data<Integer,Float> sample = new Data<>(mSamples.size(), 0.0f);
            mSamples.add(sample);

            //Turn off the data point marker symbols - yes I know this is inefficient
            for(Data data: mSampleSeries.getData())
            {
                StackPane stackPane = (StackPane)data.getNode();
                stackPane.setVisible(false);
            }
        }

        for(int x = 0; x < buffer.length; x++)
        {
            Data<Integer,Float> sample = mSamples.get(x);

            sample.setYValue(buffer[x] ? 0.5f : -0.5f);
        }
    }

    public class BufferChangeListener implements ChangeListener
    {
        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue)
        {
//            setBuffer(mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getDelayLine());

            while(mSymbol.size() < 2)
            {
                Data<Number,Number> sample = new Data<>(0.0, -0.55);
                mSymbol.add(sample);
            }

            while(mZeroCrossing.size() < 2)
            {
                Data<Number,Number> sample = new Data<>(0.0, -0.65);
                mZeroCrossing.add(sample);
            }

            while(mSamplePoints.size() < 2)
            {
                Data<Number,Number> sample = new Data<>(0.0, -0.75);
                mSamplePoints.add(sample);
            }

            while(mSymbolSamples.size() < 2)
            {
                Data<Number,Number> sample = new Data<>(0.0, -0.85);
                mSymbolSamples.add(sample);
            }

//            int pointer1 = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getDelayLinePointer();
//            int pointer2 = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getDelayLineSecondPointer();

//            mSamplePoints.get(0).setXValue(pointer1);
//            mSamplePoints.get(1).setXValue(pointer2);

//            float samplesRemaining = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getMidSymbolSamplingPoint();
//            float samplesPerSymbol = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getSamplesPerSymbol();

//            float start = pointer1 + samplesRemaining;
//            float end = start + samplesPerSymbol;

//            mSymbolSamples.get(0).setXValue(start);
//            mSymbolSamples.get(1).setXValue(end);

//            boolean symbol = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getLastSymbol();
//            int symbolStart = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getSymbolStart();
//            int symbolEnd = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getSymbolEnd();

//            mSymbol.get(0).setXValue(symbolStart);
//            mSymbol.get(0).setYValue(symbol ? 0.55f : -0.55f);
//            mSymbol.get(1).setXValue(symbolEnd);
//            mSymbol.get(1).setYValue(symbol ? 0.55f : -0.55f);
        }
    }

    public class ErrorChangeListener implements ChangeListener
    {
        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue)
        {
            while(mZeroCrossing.size() < 2)
            {
                Data<Number,Number> sample = new Data<>(0.0, -0.65);
                mZeroCrossing.add(sample);
            }

//            int pointer1 = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getDelayLinePointer();
//            int pointer2 = mLTRNetDecoderInstrumented.getLTRDecoder().getSampleBuffer().getDelayLineSecondPointer();

//            float zeroCrossingIdeal = mLTRNetDecoderInstrumented.getLTRDecoder().getErrorDetector().getZeroCrossingIdeal();
//            float detectedZeroCrossing = zeroCrossingIdeal - ((Number)newValue).floatValue();
//
//            int reference = (zeroCrossingIdeal < pointer1 || detectedZeroCrossing < pointer1 ? pointer2 : pointer1);
//            reference--;
//
//            float start = reference - zeroCrossingIdeal;
//            float end = reference - detectedZeroCrossing;
//
//            mZeroCrossing.get(0).setXValue(start);
//            mZeroCrossing.get(1).setXValue(end);
        }
    }
}
