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
import io.github.dsheirer.gui.instrument.chart.SamplesPerSymbolChart;
import io.github.dsheirer.gui.instrument.chart.SymbolChart;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.p25.P25DecoderLSMInstrumented;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableBufferBroadcaster;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25Phase1LSMPane extends ComplexDecoderPane
{
    private final static Logger mLog = LoggerFactory.getLogger(P25Phase1LSMPane.class);

    private HBox mTopChartBox;
    private ComplexSampleLineChart mDifferentialDemodulatedSamplesChartBox;
    private EyeDiagramChart mEyeDiagramChart;
    private HBox mBottomChartBox;
    private SymbolChart mSymbolConstellationChart;
    private PhaseLineChart mPLLPhaseErrorLineChart;
    private DoubleLineChart mPLLFrequencyLineChart;
    private SamplesPerSymbolChart mSamplesPerSymbolLineChart;
    private ReusableBufferBroadcaster mFilteredBufferBroadcaster = new ReusableBufferBroadcaster();
    private P25DecoderLSMInstrumented mDecoder = new P25DecoderLSMInstrumented(null);

    public P25Phase1LSMPane()
    {
        super(DecoderType.P25_PHASE1);
        init();
    }

    private void init()
    {
        addListener(getDecoder());

        getDecoder().setFilteredBufferListener(mFilteredBufferBroadcaster);
        getDecoder().setComplexSymbolListener(getSymbolConstellationChart());
        getDecoder().setPLLPhaseErrorListener(getPLLPhaseErrorLineChart());
        getDecoder().setPLLFrequencyListener(getPLLFrequencyLineChart());
        getDecoder().setSymbolDecisionDataListener(getEyeDiagramChart());
        getDecoder().setSamplesPerSymbolListener(getSamplesPerSymbolLineChart());
        mFilteredBufferBroadcaster.addListener(getDifferentialDemodulatedSamplesChartBox());

        HBox.setHgrow(getTopChartBox(), Priority.ALWAYS);
        HBox.setHgrow(getBottomChartBox(), Priority.ALWAYS);
        getChildren().addAll(getTopChartBox(), getBottomChartBox());

        mDecoder.setMessageListener(new Listener<Message>()
        {
            @Override
            public void receive(Message message)
            {
                mLog.debug(message.getMessage());
            }
        });

    }

    @Override
    public void setSampleRate(double sampleRate)
    {
        mLog.debug("Configuring for sample rate: " + sampleRate);

        mDecoder.setSampleRate(sampleRate);
        double samplesPerSymbol = sampleRate / 4800.0;

        getDifferentialDemodulatedSamplesChartBox().setSamplesPerSymbol((int)samplesPerSymbol);
        getSamplesPerSymbolLineChart().setSamplesPerSymbol(samplesPerSymbol);
    }

    private P25DecoderLSMInstrumented getDecoder()
    {
        return mDecoder;
    }

    private SymbolChart getSymbolConstellationChart()
    {
        if(mSymbolConstellationChart == null)
        {
            mSymbolConstellationChart = new SymbolChart(10);
        }

        return mSymbolConstellationChart;
    }

    private HBox getBottomChartBox()
    {
        if(mBottomChartBox == null)
        {
            mBottomChartBox = new HBox();
            mBottomChartBox.setMaxHeight(Double.MAX_VALUE);
            getSymbolConstellationChart().setMaxWidth(Double.MAX_VALUE);
            getPLLPhaseErrorLineChart().setMaxWidth(Double.MAX_VALUE);
            getPLLFrequencyLineChart().setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(getSymbolConstellationChart(), Priority.ALWAYS);
            HBox.setHgrow(getPLLPhaseErrorLineChart(), Priority.ALWAYS);
            HBox.setHgrow(getPLLFrequencyLineChart(), Priority.ALWAYS);
            mBottomChartBox.getChildren().addAll(getSymbolConstellationChart(), getPLLPhaseErrorLineChart(),
                getPLLFrequencyLineChart());
        }

        return mBottomChartBox;
    }

    private HBox getTopChartBox()
    {
        if(mTopChartBox == null)
        {
            mTopChartBox = new HBox();
            mTopChartBox.setMaxHeight(Double.MAX_VALUE);
            getDifferentialDemodulatedSamplesChartBox().setMaxWidth(Double.MAX_VALUE);
            getEyeDiagramChart().setMaxWidth(Double.MAX_VALUE);
            getSamplesPerSymbolLineChart().setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(getDifferentialDemodulatedSamplesChartBox(), Priority.ALWAYS);
            HBox.setHgrow(getEyeDiagramChart(), Priority.ALWAYS);
            HBox.setHgrow(getSamplesPerSymbolLineChart(), Priority.ALWAYS);
            mTopChartBox.getChildren().addAll(getDifferentialDemodulatedSamplesChartBox(), getEyeDiagramChart(),
                getSamplesPerSymbolLineChart());
        }

        return mTopChartBox;
    }

    private ComplexSampleLineChart getDifferentialDemodulatedSamplesChartBox()
    {
        if(mDifferentialDemodulatedSamplesChartBox == null)
        {
            mDifferentialDemodulatedSamplesChartBox = new ComplexSampleLineChart(100, 10);
        }

        return mDifferentialDemodulatedSamplesChartBox;
    }

    private EyeDiagramChart getEyeDiagramChart()
    {
        if(mEyeDiagramChart == null)
        {
            mEyeDiagramChart = new EyeDiagramChart(10, "Symbol/Eye Diagram");
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
            mPLLFrequencyLineChart = new DoubleLineChart( "PLL Frequency", -2500, 2500, 200, 40);
        }

        return mPLLFrequencyLineChart;
    }

    private SamplesPerSymbolChart getSamplesPerSymbolLineChart()
    {
        if(mSamplesPerSymbolLineChart == null)
        {
            mSamplesPerSymbolLineChart = new SamplesPerSymbolChart(40, 6.25);
        }

        return mSamplesPerSymbolLineChart;
    }
}
