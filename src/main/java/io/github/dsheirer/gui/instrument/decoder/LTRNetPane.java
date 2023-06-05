/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.gui.instrument.decoder;

import io.github.dsheirer.gui.instrument.chart.LTRNetSampleBufferChart;
import io.github.dsheirer.gui.instrument.chart.RealSampleLineChart;
import io.github.dsheirer.gui.instrument.chart.ZeroCrossingErrorDetectorChart;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.ltrnet.DecodeConfigLTRNet;
import io.github.dsheirer.module.decode.ltrnet.LTRNetDecoderInstrumented;
import io.github.dsheirer.sample.Listener;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LTRNetPane extends RealDecoderPane
{
    private final static Logger mLog = LoggerFactory.getLogger(LTRNetPane.class);

    private HBox mSampleChartBox;
    private RealSampleLineChart mRealSampleLineChart;
    private HBox mSymbolDecoderBox;
    private LTRNetSampleBufferChart mLTRNetSampleBufferChart;
    private ZeroCrossingErrorDetectorChart mZeroCrossingErrorDetectorChart;

    private LTRNetDecoderInstrumented mDecoder;

    public LTRNetPane()
    {
        super(DecoderType.LTR_NET);
        init();
    }

    private void init()
    {
        addListener(getDecoder());
        addListener(getSampleLineChart());

        HBox.setHgrow(getSampleChartBox(), Priority.ALWAYS);
        HBox.setHgrow(getSymbolDecoderBox(), Priority.ALWAYS);

        VBox.setVgrow(getSampleChartBox(), Priority.ALWAYS);
        VBox.setVgrow(getSymbolDecoderBox(), Priority.ALWAYS);

        getSampleChartBox().setMaxWidth(Double.MAX_VALUE);
        getSymbolDecoderBox().setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(getSampleChartBox(), getSymbolDecoderBox());

    }

    private LTRNetDecoderInstrumented getDecoder()
    {
        if(mDecoder == null)
        {
            DecodeConfigLTRNet config = new DecodeConfigLTRNet();
            config.setMessageDirection(MessageDirection.OSW);
            mDecoder = new LTRNetDecoderInstrumented(config);
            mDecoder.setMessageListener(new Listener<IMessage>()
            {
                @Override
                public void receive(IMessage message)
                {
                    mLog.debug(message.toString());
                }
            });
        }

        return mDecoder;
    }

    private HBox getSampleChartBox()
    {
        if(mSampleChartBox == null)
        {
            mSampleChartBox = new HBox();
            mSampleChartBox.setMaxHeight(Double.MAX_VALUE);

            getSampleLineChart().setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(getSampleLineChart(), Priority.ALWAYS);

            mSampleChartBox.getChildren().addAll(getSampleLineChart());
        }

        return mSampleChartBox;
    }

    private RealSampleLineChart getSampleLineChart()
    {
        if(mRealSampleLineChart == null)
        {
            mRealSampleLineChart = new RealSampleLineChart(500, (8000.0 / 300.0));
        }

        return mRealSampleLineChart;
    }

    private HBox getSymbolDecoderBox()
    {
        if(mSymbolDecoderBox == null)
        {
            mSymbolDecoderBox = new HBox();
            mSymbolDecoderBox.setMaxWidth(Double.MAX_VALUE);

            getSampleBufferChart().setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(getSampleBufferChart(), Priority.ALWAYS);

            mSampleChartBox.getChildren().addAll(getSampleBufferChart(), getZeroCrossingErrorDetectorChart());
        }

        return mSymbolDecoderBox;
    }

    private LTRNetSampleBufferChart getSampleBufferChart()
    {
        if(mLTRNetSampleBufferChart == null)
        {
            mLTRNetSampleBufferChart = new LTRNetSampleBufferChart(getDecoder(), 106);
        }

        return mLTRNetSampleBufferChart;
    }

    private ZeroCrossingErrorDetectorChart getZeroCrossingErrorDetectorChart()
    {
        if(mZeroCrossingErrorDetectorChart == null)
        {
//            mZeroCrossingErrorDetectorChart = new ZeroCrossingErrorDetectorChart(getDecoder(),
//                    getDecoder().getLTRDecoder().getErrorDetector().getBuffer().length);
        }

        return mZeroCrossingErrorDetectorChart;
    }
}
