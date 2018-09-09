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

import io.github.dsheirer.buffer.DoubleCircularBuffer;
import io.github.dsheirer.sample.Listener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoubleLineChart extends LineChart implements Listener<Double>
{
    private final static Logger mLog = LoggerFactory.getLogger(DoubleLineChart.class);

    private DoubleCircularBuffer mDoubleCircularBuffer;
    private ObservableList<Data<Integer,Double>> mPhaseValues = FXCollections.observableArrayList();

    public DoubleLineChart(String seriesLabel, double min, double max, double tick, int length)
    {
        super(new NumberAxis(1, length, 2), new NumberAxis(min, max, tick));

        Series<Integer,Double> phaseSeries = new Series<>(seriesLabel, mPhaseValues);
        ObservableList<Series<Integer,Double>> observableList = FXCollections.observableArrayList(phaseSeries);

        setData(observableList);
        init(length);
    }

    /**
     * Sets the minimum displayable value for the y axis
     * @param min
     */
    public void setMin(double min)
    {
        ((ValueAxis)getYAxis()).setLowerBound(min);
    }

    /**
     * Sets the maximum displayable value for the y axis
     * @param max
     */
    public void setMax(double max)
    {
        ((ValueAxis)getYAxis()).setUpperBound(max);
    }

    private void init(int length)
    {
        mDoubleCircularBuffer = new DoubleCircularBuffer(length);

        for(int x = 1; x <= length; x++)
        {
            mPhaseValues.add(new Data(x, 0.0));
        }
    }

    @Override
    public void receive(Double phase)
    {
        mDoubleCircularBuffer.put(phase);

        updateChart();
    }

    private void updateChart()
    {
        double[] values = mDoubleCircularBuffer.getAll();

        for(int x = 0; x < values.length; x++)
        {
            mPhaseValues.get(x).setYValue(values[x]);
        }
    }
}
