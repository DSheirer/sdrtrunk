/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.gui.instrument.chart;

import io.github.dsheirer.dsp.psk.SymbolDecisionData;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EyeDiagramChart extends LineChart implements Listener<SymbolDecisionData>
{
    private final static Logger mLog = LoggerFactory.getLogger(EyeDiagramChart.class);

    private ObservableList<Series<Double,Float>> mData = FXCollections.observableArrayList();
    private int mSeriesCount;
    private int mSeriesPointer;
    private int mSeriesLength;

    public EyeDiagramChart(int seriesCount, String legend)
    {
        super(new NumberAxis("Sample Point = 3", -1.0, 7.0, 1.0),
            new NumberAxis("Radians", -5.0, 5.0, 0.5));

        mSeriesCount = seriesCount;
        init();
    }

    private void init()
    {
        for(int x = 0; x < mSeriesCount; x++)
        {
            ObservableList<Data<Double,Double>> series = FXCollections.observableArrayList();
            mData.add(new Series(series));
        }

        setData(mData);
    }

    private ObservableList<Data<Double,Float>> getSeries(int series, int length)
    {
        ObservableList<Data<Double,Float>> seriesData = mData.get(series).getData();

        while(seriesData.size() > length)
        {
            seriesData.remove(0);
        }

        while(seriesData.size() < length)
        {
            seriesData.add(new Data<>(0.0,0.0f));
        }

        return seriesData;
    }

    private void checkChartLength(int length)
    {
        if(mSeriesLength != length)
        {
            ((NumberAxis)getXAxis()).setUpperBound(length);
            mSeriesLength = length;
        }
    }

    private float[] conditionData(Complex[] samples)
    {
        Float previousAngle = null;
        float[] corrected = new float[samples.length];

        int phaseRolloverIndex = -1;

        for(int x = 0; x < samples.length; x++)
        {
            corrected[x] = samples[x].angle();

            if(x > 0 && (FastMath.abs(corrected[x] - corrected[x - 1]) > FastMath.PI))
            {
                phaseRolloverIndex = x;
            }
        }

        if(phaseRolloverIndex >= 0)
        {
            for(int x = 0; x < phaseRolloverIndex; x++)
            {
                if(corrected[x] > 0)
                {
                    corrected[x] -= 2 * FastMath.PI;
                }
                else
                {
                    corrected[x] += 2 * FastMath.PI;
                }
            }
        }

        return corrected;
    }

    @Override
    public void receive(SymbolDecisionData symbolDecisionData)
    {
        Complex[] samples = symbolDecisionData.getSamples();
        int length = samples.length;

        ObservableList<Data<Double,Float>> iSeries = getSeries(mSeriesPointer++, length);
//        checkChartLength(length);

        if(mSeriesPointer >= mSeriesCount)
        {
            mSeriesPointer = 0;
        }

        float[] angles = conditionData(samples);

        for(int x = 0; x < samples.length; x++)
        {
            Data<Double,Float> iDataPoint = iSeries.get(x);
            Complex sample = samples[x];
            iDataPoint.setXValue((double)x - symbolDecisionData.getSamplingPoint());
            iDataPoint.setYValue(angles[x]);
        }
    }
}
