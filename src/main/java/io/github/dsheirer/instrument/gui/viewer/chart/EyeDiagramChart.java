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

import io.github.dsheirer.dsp.psk.SymbolDecisionData2;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EyeDiagramChart extends LineChart implements Listener<SymbolDecisionData2>
{
    private final static Logger mLog = LoggerFactory.getLogger(EyeDiagramChart.class);

    private ObservableList<Series<Double,Double>> mData = FXCollections.observableArrayList();
    private int mSeriesCount;
    private int mSeriesPointer;
    private int mSeriesLength;

    public EyeDiagramChart(int seriesCount)
    {
        super(new NumberAxis("Symbol Timing", 1.0, 10.0, 1.0),
            new NumberAxis("Value", -1.25, 1.25, 0.25));

        mSeriesCount = seriesCount * 2;
        init();
    }

    private void init()
    {
        for(int x = 0; x < mSeriesCount; x++)
        {
            ObservableList<Data<Double,Double>> series = FXCollections.observableArrayList();
            mData.add(new Series<>(series));
        }

        setData(mData);
    }

    private ObservableList<Data<Double,Double>> getSeries(int series, int length)
    {
        ObservableList<Data<Double,Double>> seriesData = mData.get(series).getData();

        while(seriesData.size() > length)
        {
            seriesData.remove(0);
        }

        while(seriesData.size() < length)
        {
            seriesData.add(new Data<Double,Double>(0.0,0.0));
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

    @Override
    public void receive(SymbolDecisionData2 symbolDecisionData)
    {
        Complex[] demodulated = symbolDecisionData.getDemodulated();

        int length = demodulated.length;

        checkChartLength(length);


        ObservableList<Data<Double,Double>> iSeries = getSeries(mSeriesPointer++, length + 1);

        if(mSeriesPointer >= mSeriesCount)
        {
            mSeriesPointer = 0;
        }

        ObservableList<Data<Double,Double>> qSeries = getSeries(mSeriesPointer++, length + 1);

        if(mSeriesPointer >= mSeriesCount)
        {
            mSeriesPointer = 0;
        }

        for(int x = 0; x < length; x++)
        {
            demodulated[x].normalize();
            Data<Double,Double> iDataPoint = iSeries.get(x);
            iDataPoint.setXValue((double)x + 1 - symbolDecisionData.getSamplingPoint());
            double iValue = demodulated[x].inphase();
            iDataPoint.setYValue(iValue);

            Data<Double,Double> qDataPoint = qSeries.get(x);
            qDataPoint.setXValue((double)x + 1 - symbolDecisionData.getSamplingPoint());
            double qValue = demodulated[x].quadrature();
            qDataPoint.setYValue(qValue);
        }
    }
}
