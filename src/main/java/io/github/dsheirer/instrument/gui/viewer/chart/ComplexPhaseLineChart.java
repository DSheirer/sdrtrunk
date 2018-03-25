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

import io.github.dsheirer.buffer.ComplexCircularBuffer;
import io.github.dsheirer.dsp.gain.ComplexGain;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.complex.Complex;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexPhaseLineChart extends LineChart implements Listener<ReusableComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexPhaseLineChart.class);

    private static final Complex ANGLE_OFFSET_45_DEGREES = Complex.fromAngle(Math.PI / 4.0);
    private ComplexCircularBuffer mComplexCircularBuffer;
    private ComplexGain mComplexGain = new ComplexGain(600.0f);
    private ObservableList<Data<Integer,Float>> mPhaseValues = FXCollections.observableArrayList();
    private IntegerProperty mLengthProperty = new SimpleIntegerProperty(40);

    public ComplexPhaseLineChart(int length)
    {
        super(new NumberAxis("Time", 1, length - 10, 2),
            new NumberAxis("Phase", -Math.PI, Math.PI, Math.PI / 2.0));

        Series<Integer,Float> phaseSeries = new Series<>("Phase", mPhaseValues);
        ObservableList<Series<Integer,Float>> observableList = FXCollections.observableArrayList(phaseSeries);

        setData(observableList);
        init(length);
    }

    private void init(int length)
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
        length().setValue(length);

        for(int x = 10; x <= length; x++)
        {
            mPhaseValues.add(new Data(x - 10, 0.0f));
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
    public void receive(ReusableComplexBuffer complexBuffer)
    {
        float[] samples = complexBuffer.getSamples();

        Complex sample;

        for(int x = 0; x < samples.length; x += 2)
        {
            sample = new Complex(samples[x], samples[x + 1]);
            Complex copy = sample.copy();
            mComplexGain.apply(copy);
            mComplexCircularBuffer.put(copy);
        }

        complexBuffer.decrementUserCount();

        updateChart();
    }

    private void updateChart()
    {
        Complex[] samples = mComplexCircularBuffer.getAll();

        for(int x = 10; x < samples.length; x++)
        {
            Complex previous = samples[x - 10].copy();
            Complex sample = samples[x].copy();

            if(previous != null && sample != null)
            {
//                previous.normalize();
//                sample.normalize();
                previous.add(ANGLE_OFFSET_45_DEGREES);
                sample.multiply(previous.conjugate());
//                sample.normalize();
                mPhaseValues.get(x - 9).setYValue(sample.angle());
            }
        }
    }
}
