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

import javafx.collections.ObservableList;

public class SamplesPerSymbolChart extends DoubleLineChart
{
    private double mFractionalSamplesPerSymbol;
    private double mPreviousSamplesPerSymbol;

    public SamplesPerSymbolChart(int length, double samplesPerSymbol)
    {
        super("Samples Per Symbol", -1.0, 1.0, 0.1, length);
        setSamplesPerSymbol(samplesPerSymbol);
    }

    public void setSamplesPerSymbol(double samplesPerSymbol)
    {
        mFractionalSamplesPerSymbol = samplesPerSymbol - ((int)samplesPerSymbol);
        mPreviousSamplesPerSymbol = samplesPerSymbol;

        ObservableList<Series> seriesList = getData();
        seriesList.get(0).setName("Samples Per Symbol:" + samplesPerSymbol);
    }

    @Override
    public void receive(Double samplesPerSymbol)
    {
        double delta = samplesPerSymbol - mPreviousSamplesPerSymbol - mFractionalSamplesPerSymbol;

        while(delta < (-1.0 + mFractionalSamplesPerSymbol))
        {
            delta++;
        }

        while(delta > 1.0)
        {
            delta--;
        }

        super.receive(delta);

        mPreviousSamplesPerSymbol = samplesPerSymbol;
    }
}
