/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import io.github.dsheirer.buffer.ComplexCircularBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSampleListener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexSampleLineChart extends LineChart implements Listener<ComplexSamples>, ComplexSampleListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexSampleLineChart.class);

    private ComplexCircularBuffer mComplexCircularBuffer;
    private ComplexCircularBuffer mDemodulationCircularBuffer;
    private ObservableList<XYChart.Data<Integer,Float>> mISamples = FXCollections.observableArrayList();
    private ObservableList<XYChart.Data<Integer,Float>> mQSamples = FXCollections.observableArrayList();
    private IntegerProperty mLengthProperty = new SimpleIntegerProperty(40);

    public ComplexSampleLineChart(String label, int length, int samplesPerSymbol)
    {
        super(new NumberAxis(label, 0, length, 10),
            new NumberAxis("Value", -1.0, 1.0, 0.25));

        LineChart.Series<Integer,Float> iSampleSeries = new LineChart.Series<>("Inphase", mISamples);
        LineChart.Series<Integer,Float> qSampleSeries = new LineChart.Series<>("Quadrature", mQSamples);
        ObservableList<XYChart.Series<Integer,Float>> observableList =
            FXCollections.observableArrayList(iSampleSeries, qSampleSeries);

        setData(observableList);
        init(length, samplesPerSymbol);
    }

    public void setSamplesPerSymbol(int samplesPerSymbol)
    {
        mDemodulationCircularBuffer = new ComplexCircularBuffer(samplesPerSymbol);
    }

    private void init(int length, int samplesPerSymbol)
    {
        mLengthProperty.addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                mComplexCircularBuffer = new ComplexCircularBuffer(newValue.intValue());
            }
        });
        mComplexCircularBuffer = new ComplexCircularBuffer(length);
        mDemodulationCircularBuffer = new ComplexCircularBuffer(samplesPerSymbol);
        length().setValue(length);

        for(int x = 1; x <= length; x++)
        {
            mISamples.add(new Data(x, 0.0f));
            mQSamples.add(new Data(x, 0.0f));
        }
    }

    /**
     * Length of the x-axis for displaying samples
     */
    public IntegerProperty length()
    {
        return mLengthProperty;
    }

    @Override
    public void receive(ComplexSamples samples)
    {
        for(int x = 0; x < samples.i().length; x++)
        {
            receive(samples.i()[x], samples.q()[x]);
        }
    }

    @Override
    public void receive(float i, float q)
    {
        Complex sample = new Complex(i,q);
        sample.normalize();
        Complex previous = mDemodulationCircularBuffer.get(sample.copy());
        sample.multiply(previous.conjugate());
        mComplexCircularBuffer.put(sample);
        updateChart();
    }

    private void updateChart()
    {
        Complex[] samples = mComplexCircularBuffer.getAll();

        for(int x = 0; x < samples.length; x++)
        {
            Complex sample = samples[x];

            if(sample != null)
            {
                sample.normalize();
                mISamples.get(x).setYValue(sample.inphase());
                mQSamples.get(x).setYValue(sample.quadrature());
            }
        }
    }
}
