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

import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.PolarChart;
import eu.hansolo.fx.charts.XYPane;
import eu.hansolo.fx.charts.data.XYChartItem;
import eu.hansolo.fx.charts.series.XYSeries;
import io.github.dsheirer.buffer.ComplexCircularBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSamples;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleXYChart extends PolarChart<XYChartItem> implements Listener<ComplexSamples>
{
    private static ObservableList<XYChartItem> sChartItemList = FXCollections.observableArrayList();

    static
    {
        sChartItemList.add(new XYChartItem(1, 1));
        sChartItemList.add(new XYChartItem(-1, -1));
    }

    private final static Logger mLog = LoggerFactory.getLogger(SampleXYChart.class);
    private ComplexCircularBuffer mCircularBuffer;
    private int mSampleCount;

    public SampleXYChart(int sampleCount, String title)
    {
        super(new XYPane<>(new XYSeries<>(sChartItemList, ChartType.POLAR)));
        mSampleCount = sampleCount;
        mCircularBuffer = new ComplexCircularBuffer(mSampleCount);

        getXYPane().setLowerBoundX(0);
        getXYPane().setUpperBoundX(1.0);
        getXYPane().setLowerBoundY(0);
        getXYPane().setUpperBoundY(1.0);
    }

    private void init()
    {
    }

    @Override
    public void receive(ComplexSamples buffer)
    {
        for(int x = 0; x < buffer.i().length; x++)
        {
            mCircularBuffer.put(new Complex(buffer.i()[x], buffer.q()[x]));
        }

        Complex[] complexSamples = mCircularBuffer.getAll();

        XYSeries<XYChartItem> series = getXYPane().getListOfSeries().get(0);
        series.setStroke(Color.BLUE);

        series.getItems().clear();

        for (Complex sample : complexSamples) {
            series.getItems().add(new XYChartItem(sample.polarAngleDegrees(), sample.magnitude(), Color.BLUE));
        }

        refresh();
    }
}
