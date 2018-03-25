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
package io.github.dsheirer.spectrum;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelEventListener;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.dsp.filter.Filters;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.halfband.real.HalfBandFilter_RB_RB;
import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter.SmoothingType;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.sample.real.RealBuffer;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.ColorSettingMenuItem;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.spectrum.converter.DFTResultsConverter;
import io.github.dsheirer.spectrum.converter.RealDecibelConverter;
import io.github.dsheirer.spectrum.menu.AveragingItem;
import io.github.dsheirer.spectrum.menu.DFTSizeItem;
import io.github.dsheirer.spectrum.menu.FFTWindowTypeItem;
import io.github.dsheirer.spectrum.menu.FrameRateItem;
import io.github.dsheirer.spectrum.menu.SmoothingItem;
import io.github.dsheirer.spectrum.menu.SmoothingTypeItem;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChannelSpectrumPanel extends JPanel implements ChannelEventListener,
    Listener<RealBuffer>, SettingChangeListener, SpectralDisplayAdjuster
{
    private static final long serialVersionUID = 1L;

    private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.REAL);
    private DFTResultsConverter mDFTConverter = new RealDecibelConverter();
    private JLayeredPane mLayeredPane;
    private SpectrumPanel mSpectrumPanel;
    private ChannelOverlayPanel mOverlayPanel;
    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("ChannelSpectrumPanel");

    private Channel mCurrentChannel;

    private int mSampleBufferSize = 2400;

    private HalfBandFilter_RB_RB mDecimatingFilter = new HalfBandFilter_RB_RB(
        Filters.FIR_HALF_BAND_31T_ONE_EIGHTH_FCO.getCoefficients(), 1.0f, true);

    private AtomicBoolean mEnabled = new AtomicBoolean();

    private SettingsManager mSettingsManager;
    private ChannelProcessingManager mChannelProcessingManager;

    public ChannelSpectrumPanel(SettingsManager settingsManager,
                                ChannelProcessingManager channelProcessingManager)
    {
        mSettingsManager = settingsManager;
        mChannelProcessingManager = channelProcessingManager;

        if(mSettingsManager != null)
        {
            mSettingsManager.addListener(this);
        }

        mSpectrumPanel = new SpectrumPanel(mSettingsManager);
        mSpectrumPanel.setAveraging(1);

        mOverlayPanel = new ChannelOverlayPanel(mSettingsManager);
        mDFTProcessor.addConverter(mDFTConverter);
        mDFTConverter.addListener(mSpectrumPanel);

        /* Set the DFTProcessor to the decimated 24kHz sample rate */
        mDFTProcessor.process(SourceEvent.sampleRateChange(24000.0));

        initGui();
    }

    public void dispose()
    {
        setEnabled(false);

        mDFTProcessor.dispose();

        if(mSettingsManager != null)
        {
            mSettingsManager.removeListener(this);
        }

        mSettingsManager = null;
        mCurrentChannel = null;
        mDFTProcessor = null;
        mSpectrumPanel = null;
    }

    public void setFrameRate(int framesPerSecond)
    {
        mSampleBufferSize = (int)(48000 / framesPerSecond);

        mDFTProcessor.setFrameRate(framesPerSecond);
    }

    private void initGui()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ",
            "[grow,fill]",
            "[grow,fill]"));

        mLayeredPane = new JLayeredPane();
        mLayeredPane.addComponentListener(new ResizeListener());

        MouseEventProcessor mouser = new MouseEventProcessor();

        mOverlayPanel.addMouseListener(mouser);
        mOverlayPanel.addMouseMotionListener(mouser);

        mLayeredPane.add(mSpectrumPanel, new Integer(0), 0);
        mLayeredPane.add(mOverlayPanel, new Integer(1), 0);

        add(mLayeredPane);
    }

    public void setEnabled(boolean enabled)
    {
        if(enabled && mEnabled.compareAndSet(false, true))
        {
            start();
        }
        else if(!enabled && mEnabled.compareAndSet(true, false))
        {
            stop();
        }
    }

    @Override
    @SuppressWarnings("incomplete-switch")
    public void channelChanged(ChannelEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_SELECTION_CHANGE:
                //ChannelSelectionManager ensures that only 1 channel can be
                //selected and any previously selected channel will be first
                //deselected before we get a new selection event
                if(event.getChannel().isSelected())
                {
                    if(mCurrentChannel != null)
                    {
                        stop();
                        mCurrentChannel = null;
                    }

                    mCurrentChannel = event.getChannel();

                    if(mEnabled.get())
                    {
                        start();
                    }
                }
                else
                {
                    stop();
                    mCurrentChannel = null;
                }
                break;
            case NOTIFICATION_PROCESSING_STOP:
                if(event.getChannel() == mCurrentChannel)
                {
                    if(mEnabled.get())
                    {
                        stop();
                    }

                    mCurrentChannel = null;
                }
                break;
        }
    }

    private void start()
    {
        if(mEnabled.get() && mCurrentChannel != null && mCurrentChannel.isProcessing())
        {
            ProcessingChain processingChain = mChannelProcessingManager
                .getProcessingChain(mCurrentChannel);

            if(processingChain != null)
            {
                processingChain.addRealBufferListener(this);

                mDFTProcessor.start();
            }
        }
    }

    private void stop()
    {
        if(mCurrentChannel != null && mCurrentChannel.isProcessing())
        {
            ProcessingChain processingChain = mChannelProcessingManager
                .getProcessingChain(mCurrentChannel);

            if(processingChain != null)
            {
                processingChain.removeRealBufferListener(this);
            }
        }

        mDFTProcessor.stop();

        mSpectrumPanel.clearSpectrum();
    }

    @Override
    public void settingChanged(Setting setting)
    {
        if(mSpectrumPanel != null)
        {
            mSpectrumPanel.settingChanged(setting);
        }
        if(mOverlayPanel != null)
        {
            mOverlayPanel.settingChanged(setting);
        }
    }

    @Override
    public void settingDeleted(Setting setting)
    {
        if(mSpectrumPanel != null)
        {
            mSpectrumPanel.settingDeleted(setting);
        }

        if(mOverlayPanel != null)
        {
            mOverlayPanel.settingDeleted(setting);
        }
    }

    @Override
    public void receive(RealBuffer buffer)
    {
        RealBuffer decimated = mDecimatingFilter.filter(buffer);

        //Hack: we're placing real samples in a complex buffer that the DFT
        //processor is expecting.
        ReusableComplexBuffer reusableComplexBuffer = mReusableComplexBufferQueue.getBuffer(decimated.getSamples().length);
        reusableComplexBuffer.reloadFrom(decimated.getSamples(), System.currentTimeMillis());
        reusableComplexBuffer.incrementUserCount();
        mDFTProcessor.receive(reusableComplexBuffer);
    }

    /**
     * Monitors the sizing of the layered pane and resizes the spectrum and
     * channel panels whenever the layered pane is resized
     */
    public class ResizeListener implements ComponentListener
    {
        @Override
        public void componentResized(ComponentEvent e)
        {
            Component c = e.getComponent();

            mSpectrumPanel.setBounds(0, 0, c.getWidth(), c.getHeight());
            mOverlayPanel.setBounds(0, 0, c.getWidth(), c.getHeight());
        }

        @Override
        public void componentHidden(ComponentEvent arg0)
        {
        }

        @Override
        public void componentMoved(ComponentEvent arg0)
        {
        }

        @Override
        public void componentShown(ComponentEvent arg0)
        {
        }
    }

    /**
     * Mouse event handler for the channel panel.
     */
    public class MouseEventProcessor implements MouseMotionListener, MouseListener
    {
        @Override
        public void mouseMoved(MouseEvent event)
        {
            update(event);
        }

        @Override
        public void mouseDragged(MouseEvent event)
        {
            update(event);
        }

        private void update(MouseEvent event)
        {
            if(event.getComponent() == mOverlayPanel)
            {
                mOverlayPanel.setCursorLocation(event.getPoint());
            }
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            if(e.getComponent() == mOverlayPanel)
            {
                mOverlayPanel.setCursorVisible(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            mOverlayPanel.setCursorVisible(false);
        }

        /**
         * Displays the context menu.
         */
        @Override
        public void mouseClicked(MouseEvent event)
        {
            if(SwingUtilities.isRightMouseButton(event))
            {
                JPopupMenu contextMenu = new JPopupMenu();

                /**
                 * Color Menus
                 */
                JMenu colorMenu = new JMenu("Color");

                colorMenu.add(new ColorSettingMenuItem(mSettingsManager,
                    ColorSettingName.SPECTRUM_CURSOR));

                colorMenu.add(new ColorSettingMenuItem(mSettingsManager,
                    ColorSettingName.SPECTRUM_LINE));

                colorMenu.add(new ColorSettingMenuItem(mSettingsManager,
                    ColorSettingName.SPECTRUM_BACKGROUND));

                colorMenu.add(new ColorSettingMenuItem(mSettingsManager,
                    ColorSettingName.SPECTRUM_GRADIENT_BOTTOM));

                colorMenu.add(new ColorSettingMenuItem(mSettingsManager,
                    ColorSettingName.SPECTRUM_GRADIENT_TOP));

                contextMenu.add(colorMenu);

                /**
                 * Display items: fft and frame rate
                 */
                JMenu displayMenu = new JMenu("Display");
                contextMenu.add(displayMenu);

                /**
                 * Averaging menu
                 */
                JMenu averagingMenu = new JMenu("Averaging");
                averagingMenu.add(
                    new AveragingItem(ChannelSpectrumPanel.this, 2));
                displayMenu.add(averagingMenu);

                /**
                 * FFT width
                 */
                JMenu fftWidthMenu = new JMenu("FFT Width");
                displayMenu.add(fftWidthMenu);

                for(DFTSize width : DFTSize.values())
                {
                    fftWidthMenu.add(new DFTSizeItem(mDFTProcessor, width));
                }

                /**
                 * DFT Processor Frame Rate
                 */
                JMenu frameRateMenu = new JMenu("Frame Rate");
                displayMenu.add(frameRateMenu);

                frameRateMenu.add(new FrameRateItem(mDFTProcessor, 14));
                frameRateMenu.add(new FrameRateItem(mDFTProcessor, 16));
                frameRateMenu.add(new FrameRateItem(mDFTProcessor, 18));
                frameRateMenu.add(new FrameRateItem(mDFTProcessor, 20));
                frameRateMenu.add(new FrameRateItem(mDFTProcessor, 25));
                frameRateMenu.add(new FrameRateItem(mDFTProcessor, 30));
                frameRateMenu.add(new FrameRateItem(mDFTProcessor, 40));
                frameRateMenu.add(new FrameRateItem(mDFTProcessor, 50));

                /**
                 * FFT Window Type
                 */
                JMenu fftWindowType = new JMenu("Window Type");
                displayMenu.add(fftWindowType);

                for(WindowType type : WindowType.values())
                {
                    fftWindowType.add(
                        new FFTWindowTypeItem(mDFTProcessor, type));
                }

                /**
                 * Smoothing menu
                 */
                JMenu smoothingMenu = new JMenu("Smoothing");

                if(mSpectrumPanel.getSmoothingType() != SmoothingType.NONE)
                {
                    smoothingMenu.add(new SmoothingItem(ChannelSpectrumPanel.this, 5));
                    smoothingMenu.add(new JSeparator());
                }

                smoothingMenu.add(new SmoothingTypeItem(ChannelSpectrumPanel.this, SmoothingType.GAUSSIAN));
                smoothingMenu.add(new SmoothingTypeItem(ChannelSpectrumPanel.this, SmoothingType.TRIANGLE));
                smoothingMenu.add(new SmoothingTypeItem(ChannelSpectrumPanel.this, SmoothingType.RECTANGLE));
                smoothingMenu.add(new SmoothingTypeItem(ChannelSpectrumPanel.this, SmoothingType.NONE));

                displayMenu.add(smoothingMenu);

                if(contextMenu != null)
                {
                    contextMenu.show(mOverlayPanel,
                        event.getX(),
                        event.getY());
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
        }
    }

    @Override
    public int getAveraging()
    {
        return mSpectrumPanel.getAveraging();
    }

    @Override
    public void setAveraging(int averaging)
    {
        mSpectrumPanel.setAveraging(averaging);
    }

    public void setSampleSize(double sampleSize)
    {
        mSpectrumPanel.setSampleSize(sampleSize);
    }

    @Override
    public int getSmoothing()
    {
        return mSpectrumPanel.getSmoothing();
    }

    @Override
    public void setSmoothing(int smoothing)
    {
        mSpectrumPanel.setSmoothing(smoothing);
    }

    @Override
    public SmoothingType getSmoothingType()
    {
        return mSpectrumPanel.getSmoothingType();
    }

    @Override
    public void setSmoothingType(SmoothingType type)
    {
        mSpectrumPanel.setSmoothingType(type);
    }
}
