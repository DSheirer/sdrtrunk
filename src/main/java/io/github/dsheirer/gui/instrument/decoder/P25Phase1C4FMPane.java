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
package io.github.dsheirer.gui.instrument.decoder;

import io.github.dsheirer.gui.instrument.chart.ComplexSampleLineChart;
import io.github.dsheirer.gui.instrument.chart.DoubleLineChart;
import io.github.dsheirer.gui.instrument.chart.EyeDiagramChart;
import io.github.dsheirer.gui.instrument.chart.PhaseLineChart;
import io.github.dsheirer.gui.instrument.chart.SymbolChart;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.P25DecoderC4FMInstrumented;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableBufferBroadcaster;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25Phase1C4FMPane extends ComplexDecoderPane
{
    private final static Logger mLog = LoggerFactory.getLogger(P25Phase1C4FMPane.class);

    private HBox mSampleChartBox;
    private ComplexSampleLineChart mSampleLineChart;
    private EyeDiagramChart mEyeDiagramChart;
    private HBox mDecoderChartBox;
    private SymbolChart mSymbolChart;
    private PhaseLineChart mPLLPhaseErrorLineChart;
    private DoubleLineChart mPLLFrequencyLineChart;
    private DoubleLineChart mSamplesPerSymbolLineChart;
    private ReusableBufferBroadcaster mFilteredBufferBroadcaster = new ReusableBufferBroadcaster();
    private P25DecoderC4FMInstrumented mDecoder = new P25DecoderC4FMInstrumented();

    public P25Phase1C4FMPane()
    {
        super(DecoderType.P25_PHASE1);
        init();
    }

    private void init()
    {
        addListener(getDecoder());

        getDecoder().setFilteredBufferListener(mFilteredBufferBroadcaster);
        getDecoder().setComplexSymbolListener(getSymbolChart());
        getDecoder().setPLLPhaseErrorListener(getPLLPhaseErrorLineChart());
        getDecoder().setPLLFrequencyListener(getPLLFrequencyLineChart());
        getDecoder().setSymbolDecisionDataListener(getEyeDiagramChart());
        getDecoder().setSamplesPerSymbolListener(getSamplesPerSymbolLineChart());
        mFilteredBufferBroadcaster.addListener(getSampleLineChart());

        HBox.setHgrow(getSampleChartBox(), Priority.ALWAYS);
        HBox.setHgrow(getDecoderChartBox(), Priority.ALWAYS);
        getChildren().addAll(getSampleChartBox(), getDecoderChartBox());

        mDecoder.setMessageListener(new Listener<IMessage>()
        {
            @Override
            public void receive(IMessage message)
            {
                mLog.debug(message.toString());
            }
        });

    }

    @Override
    public void setSampleRate(double sampleRate)
    {
        mLog.debug("Configuring for sample rate: " + sampleRate);

        mDecoder.setSampleRate(sampleRate);
        double samplesPerSymbol = sampleRate / 4800.0;

        getSampleLineChart().setSamplesPerSymbol((int) samplesPerSymbol);
    }

    private P25DecoderC4FMInstrumented getDecoder()
    {
        return mDecoder;
    }

    private SymbolChart getSymbolChart()
    {
        if(mSymbolChart == null)
        {
            mSymbolChart = new SymbolChart(10);
        }

        return mSymbolChart;
    }

    private HBox getDecoderChartBox()
    {
        if(mDecoderChartBox == null)
        {
            mDecoderChartBox = new HBox();
            mDecoderChartBox.setMaxHeight(Double.MAX_VALUE);
            getSymbolChart().setMaxWidth(Double.MAX_VALUE);
            getPLLPhaseErrorLineChart().setMaxWidth(Double.MAX_VALUE);
            getPLLFrequencyLineChart().setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(getSymbolChart(), Priority.ALWAYS);
            HBox.setHgrow(getPLLPhaseErrorLineChart(), Priority.ALWAYS);
            HBox.setHgrow(getPLLFrequencyLineChart(), Priority.ALWAYS);
            mDecoderChartBox.getChildren().addAll(getSymbolChart(), getPLLPhaseErrorLineChart(),
                    getPLLFrequencyLineChart());
        }

        return mDecoderChartBox;
    }

    private HBox getSampleChartBox()
    {
        if(mSampleChartBox == null)
        {
            mSampleChartBox = new HBox();
            mSampleChartBox.setMaxHeight(Double.MAX_VALUE);
            getSampleLineChart().setMaxWidth(Double.MAX_VALUE);
            getEyeDiagramChart().setMaxWidth(Double.MAX_VALUE);
            getSamplesPerSymbolLineChart().setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(getSampleLineChart(), Priority.ALWAYS);
            HBox.setHgrow(getEyeDiagramChart(), Priority.ALWAYS);
            HBox.setHgrow(getSamplesPerSymbolLineChart(), Priority.ALWAYS);
            mSampleChartBox.getChildren().addAll(getSampleLineChart(), getEyeDiagramChart(),
                    getSamplesPerSymbolLineChart());
        }

        return mSampleChartBox;
    }

    private ComplexSampleLineChart getSampleLineChart()
    {
        if(mSampleLineChart == null)
        {
            mSampleLineChart = new ComplexSampleLineChart(100, 10);
        }

        return mSampleLineChart;
    }

    private EyeDiagramChart getEyeDiagramChart()
    {
        if(mEyeDiagramChart == null)
        {
            mEyeDiagramChart = new EyeDiagramChart(10, "Symbol: 3-4");
        }

        return mEyeDiagramChart;
    }

    private PhaseLineChart getPLLPhaseErrorLineChart()
    {
        if(mPLLPhaseErrorLineChart == null)
        {
            mPLLPhaseErrorLineChart = new PhaseLineChart(40);
        }

        return mPLLPhaseErrorLineChart;
    }

    private DoubleLineChart getPLLFrequencyLineChart()
    {
        if(mPLLFrequencyLineChart == null)
        {
            mPLLFrequencyLineChart = new DoubleLineChart("PLL Frequency", -500, 500, 50, 40);
        }

        return mPLLFrequencyLineChart;
    }

    private DoubleLineChart getSamplesPerSymbolLineChart()
    {
        if(mSamplesPerSymbolLineChart == null)
        {
            mSamplesPerSymbolLineChart = new DoubleLineChart("Sample Point", 9.5, 11.5, 0.1, 40);
        }

        return mSamplesPerSymbolLineChart;
    }
}
