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
package io.github.dsheirer.instrument.gui.viewer;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;

public class ViewerDesktop extends BorderPane
{
    private PlaybackController mPlaybackController;
    private VBox mChartBox;
    private ComplexSampleLineChart mSampleLineChart;
    private ComplexPhaseLineChart mPhaseLineChart;

    public ViewerDesktop()
    {
        setBottom(getPlaybackController());
        getPlaybackController().addListener(getSampleLineChart());
        getPlaybackController().addListener(getPhaseLineChart());
        setCenter(getChartBox());
    }

    public void load(File file)
    {
        close();
        getPlaybackController().load(file);
    }

    public void close()
    {
        getPlaybackController().close();
    }

    private PlaybackController getPlaybackController()
    {
        if(mPlaybackController == null)
        {
            mPlaybackController = new PlaybackController();
        }

        return mPlaybackController;
    }

    private VBox getChartBox()
    {
        if(mChartBox == null)
        {
            mChartBox = new VBox();
            mChartBox.getChildren().addAll(getSampleLineChart(), getPhaseLineChart());
        }

        return mChartBox;
    }

    private ComplexSampleLineChart getSampleLineChart()
    {
        if(mSampleLineChart == null)
        {
            mSampleLineChart = new ComplexSampleLineChart(40);
        }

        return mSampleLineChart;
    }

    private ComplexPhaseLineChart getPhaseLineChart()
    {
        if(mPhaseLineChart == null)
        {
            mPhaseLineChart = new ComplexPhaseLineChart(50);
        }

        return mPhaseLineChart;
    }
}
