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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecodedSymbolChart extends LineChart implements Listener<Boolean>
{
    private final static Logger mLog = LoggerFactory.getLogger(DecodedSymbolChart.class);

    private ObservableList<Data<Number,Number>> mSymbols = FXCollections.observableArrayList();
    private Series<Number,Number> mSymbolSeries = new Series<>("Samples", mSymbols);
    private boolean[] mSymbolBuffer;
    private int mSymbolBufferPointer;

    public DecodedSymbolChart(int length)
    {
        super(new NumberAxis("Symbols", 0, length, 5),
            new NumberAxis("Value", -1.0, 1.0, 0.25));

        ObservableList<Series> observableList = FXCollections.observableArrayList(mSymbolSeries);
        setData(observableList);

        for(int x = 0; x < length; x++)
        {
            Data<Number,Number> symbol = new Data<>(x, -0.5f);
            mSymbols.add(symbol);
        }

        mSymbolBuffer = new boolean[length];
    }

    @Override
    public void receive(Boolean symbol)
    {
        mSymbolBuffer[mSymbolBufferPointer++] = symbol;
        mSymbolBufferPointer %= mSymbolBuffer.length;

        update();
    }

    private void update()
    {
        for(int x = 0; x < mSymbolBuffer.length; x++)
        {
            Data<Number,Number> symbol = mSymbols.get(x);

            int offset = x + mSymbolBufferPointer;
            offset %= mSymbolBuffer.length;

            symbol.setYValue(mSymbolBuffer[offset] ? 0.5f : -0.5f);
        }
    }
}
