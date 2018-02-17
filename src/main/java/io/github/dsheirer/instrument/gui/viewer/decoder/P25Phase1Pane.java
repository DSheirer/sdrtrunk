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
package io.github.dsheirer.instrument.gui.viewer.decoder;

import io.github.dsheirer.instrument.gui.viewer.chart.ComplexPhaseLineChart;
import io.github.dsheirer.instrument.gui.viewer.chart.ComplexSampleLineChart;
import io.github.dsheirer.instrument.gui.viewer.chart.PhaseLineChart;
import io.github.dsheirer.instrument.gui.viewer.chart.SymbolChart;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.P25_C4FMDecoder2;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25Phase1Pane extends DecoderPane
{
    private final static Logger mLog = LoggerFactory.getLogger(P25Phase1Pane.class);

    private HBox mSampleChartBox;
    private ComplexSampleLineChart mSampleLineChart;
    private ComplexPhaseLineChart mPhaseLineChart;
    private HBox mDecoderChartBox;
    private SymbolChart mSymbolChart;
    private PhaseLineChart mPLLPhaseErrorLineChart;
    private P25_C4FMDecoder2 mDecoder = new P25_C4FMDecoder2(null);

    public P25Phase1Pane()
    {
        super(DecoderType.P25_PHASE1);
        init();
    }

    private void init()
    {
        addListener(getSampleLineChart());
        addListener(getPhaseLineChart());
        addListener(getDecoder());

        getDecoder().setSymbolListener(getSymbolChart());
        getDecoder().setPLLPhaseErrorListener(getPLLPhaseErrorLineChart());

        getChildren().addAll(getSampleChartBox(), getDecoderChartBox());
    }

    @Override
    public void setSampleRate(double sampleRate)
    {
        mLog.debug("Configuring for sample rate: " + sampleRate);

        mDecoder.setSampleRate(sampleRate);
    }

    private P25_C4FMDecoder2 getDecoder()
    {
        return mDecoder;
    }

    private SymbolChart getSymbolChart()
    {
        if(mSymbolChart == null)
        {
            mSymbolChart = new SymbolChart(20);
        }

        return mSymbolChart;
    }

    private HBox getDecoderChartBox()
    {
        if(mDecoderChartBox == null)
        {
            mDecoderChartBox = new HBox();
            getSymbolChart().setMaxWidth(Double.MAX_VALUE);
            getPLLPhaseErrorLineChart().setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(getSymbolChart(), Priority.ALWAYS);
            HBox.setHgrow(getPLLPhaseErrorLineChart(), Priority.ALWAYS);
            mDecoderChartBox.getChildren().addAll(getSymbolChart(), getPLLPhaseErrorLineChart());
        }

        return mDecoderChartBox;
    }

    private HBox getSampleChartBox()
    {
        if(mSampleChartBox == null)
        {
            mSampleChartBox = new HBox();
            getSampleLineChart().setMaxWidth(Double.MAX_VALUE);
            getPhaseLineChart().setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(getSampleLineChart(), Priority.ALWAYS);
            HBox.setHgrow(getPhaseLineChart(), Priority.ALWAYS);
            mSampleChartBox.getChildren().addAll(getSampleLineChart(), getPhaseLineChart());
        }

        return mSampleChartBox;
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

    private PhaseLineChart getPLLPhaseErrorLineChart()
    {
        if(mPLLPhaseErrorLineChart == null)
        {
            mPLLPhaseErrorLineChart = new PhaseLineChart(80);
        }

        return mPLLPhaseErrorLineChart;
    }
}
