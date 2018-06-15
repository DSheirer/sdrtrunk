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

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SymbolChart extends ScatterChart implements Listener<Complex>
{
    private final static Logger mLog = LoggerFactory.getLogger(SymbolChart.class);

    private ObservableList<ScatterChart.Data> mCurrentPoints = FXCollections.observableArrayList();
//    private ObservableList<ScatterChart.Data> mPreviousPoints = FXCollections.observableArrayList();
//    private ObservableList<ScatterChart.Data> mOldestPoints = FXCollections.observableArrayList();
    private int mDataPointer;
    private int mHistory;

    /**
     * Symbol chart for plotting a series of constellation symbols over time.  Maintains three history sets for
     * current, previous, and oldest symbols.
     *
     * @param history size for each of the history sets
     */
    public SymbolChart(int history)
    {
        super(new NumberAxis("I", -1.1, 1.1, 0.2),
            new NumberAxis("Q", -1.1, 1.1, 0.2));

        mHistory = history;

        for(int x = 0; x < mHistory; x++)
        {
            mCurrentPoints.add(new XYChart.Data<>(0.0, 0.0));
//            mPreviousPoints.add(new XYChart.Data<>(0.0, 0.0));
//            mOldestPoints.add(new XYChart.Data<>(0.0, 0.0));
        }

        ObservableList<XYChart.Series> data = FXCollections.observableArrayList();
        data.add(new ScatterChart.Series("Constellation", mCurrentPoints));
//        data.add(new ScatterChart.Series("Previous", mPreviousPoints));
//        data.add(new ScatterChart.Series("Oldest", mOldestPoints));

        this.setData(data);
    }

    @Override
    public void receive(Complex complex)
    {
        //Transfer previous to oldest
//        XYChart.Data oldest = mOldestPoints.get(mDataPointer);
//        XYChart.Data previous = mPreviousPoints.get(mDataPointer);
        XYChart.Data current = mCurrentPoints.get(mDataPointer);

//        oldest.setXValue(previous.getXValue());
//        oldest.setYValue(previous.getYValue());
//
//        previous.setXValue(current.getXValue());
//        previous.setYValue(current.getYValue());

        current.setXValue(complex.inphase());
        current.setYValue(complex.quadrature());

        mDataPointer++;

        if(mDataPointer >= mHistory)
        {
            mDataPointer = 0;
        }
    }
}
