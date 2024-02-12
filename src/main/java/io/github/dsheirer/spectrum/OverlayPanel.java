/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.spectrum;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

public class OverlayPanel extends JPanel implements Listener<ChannelEvent>, ISourceEventProcessor, SettingChangeListener
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(OverlayPanel.class);
    private final DecimalFormat PPM_FORMATTER = new DecimalFormat( "#.0" );

    private final static RenderingHints RENDERING_HINTS = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

    static
    {
        RENDERING_HINTS.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private final static BasicStroke DASHED_STROKE = new BasicStroke(0.8f, BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER, 5.0f, new float[]{2.0f, 4.0f}, 0.0f);

    private static DecimalFormat CURSOR_FORMAT = new DecimalFormat("000.00000");
    private long mFrequency = 0;
    private int mBandwidth = 0;
    private Point mCursorLocation = new Point(0, 0);
    private boolean mCursorVisible = false;

    private DFTSize mDFTSize = DFTSize.FFT04096;
    private int mZoom = 0;
    private int mDFTZoomWindowOffset = 0;

    /**
     * Colors used by this component
     */
    private Color mColorChannelConfig;
    private Color mColorChannelConfigProcessing;
    private Color mColorChannelConfigSelected;
    private Color mColorSpectrumBackground;
    private Color mColorSpectrumCursor;
    private Color mColorSpectrumLine;

    //Currently visible/displayable channels
    private List<Channel> mVisibleChannels = new CopyOnWriteArrayList<>();
    private List<Channel> mTrafficChannels = new CopyOnWriteArrayList<>();

    private ChannelDisplay mChannelDisplay = ChannelDisplay.ALL;

    //Defines the offset at the bottom of the spectral display to account for
    //the frequency labels
    private double mSpectrumInset = 20.0d;
    private LabelSizeManager mLabelSizeMonitor = new LabelSizeManager();

    private SettingsManager mSettingsManager;
    private ChannelModel mChannelModel;
    private ChannelProcessingManager mChannelProcessingManager;

    /**
     * Translucent overlay panel for displaying channel configurations,
     * processing channels, selected channels, frequency labels and lines, and
     * a cursor with a frequency readout.
     */
    public OverlayPanel(SettingsManager settingsManager, ChannelModel channelModel, ChannelProcessingManager channelProcessingManager)
    {
        mSettingsManager = settingsManager;

        if(mSettingsManager != null)
        {
            mSettingsManager.addListener(this);
        }

        mChannelModel = channelModel;

        if(mChannelModel != null)
        {
            mChannelModel.addListener(this::receive);
        }

        mChannelProcessingManager = channelProcessingManager;

        if(mChannelProcessingManager != null)
        {
            mChannelProcessingManager.addChannelEventListener(this::receive);
        }

        addComponentListener(mLabelSizeMonitor);

        //Set the background transparent, so the spectrum display can be seen
        setOpaque(false);

        //Fetch color settings from settings manager
        setColors();
    }

    public void dispose()
    {
        if(mChannelModel != null)
        {
            mChannelModel.removeListener(this);
        }

        mChannelModel = null;

        mVisibleChannels.clear();

        if(mSettingsManager != null)
        {
            mSettingsManager.removeListener(this);
        }

        mSettingsManager = null;
    }

    /**
     * Sets/changes the DFT bin size
     */
    public void setDFTSize(DFTSize size)
    {
        mDFTSize = size;
    }

    public ChannelDisplay getChannelDisplay()
    {
        return mChannelDisplay;
    }

    public void setChannelDisplay(ChannelDisplay display)
    {
        mChannelDisplay = display;
    }

    public void setCursorLocation(Point point)
    {
        mCursorLocation = point;

        repaint();
    }

    public void setCursorVisible(boolean visible)
    {
        mCursorVisible = visible;

        repaint();
    }

    /**
     * Sets the current zoom level (2^zoom)
     *
     * 0 	No Zoom
     * 1	2x Zoom
     * 2	4x Zoom
     * 3	8x Zoom
     * 4	16x Zoom
     * 5	32x Zoom
     * 6	64x Zoom
     *
     * @param zoom level, 0 - 6.
     */
    public void setZoom(int zoom)
    {
        mZoom = zoom;

        mLabelSizeMonitor.update();
    }

    public void setZoomWindowOffset(int offset)
    {
        mDFTZoomWindowOffset = offset;
    }

    /**
     * Fetches the color settings from the settings manager
     */
    private void setColors()
    {
        mColorChannelConfig = getColor(ColorSettingName.CHANNEL_CONFIG);
        mColorChannelConfigProcessing = getColor(ColorSettingName.CHANNEL_CONFIG_PROCESSING);
        mColorChannelConfigSelected = getColor(ColorSettingName.CHANNEL_CONFIG_SELECTED);
        mColorSpectrumCursor = getColor(ColorSettingName.SPECTRUM_CURSOR);
        mColorSpectrumLine = getColor(ColorSettingName.SPECTRUM_LINE);
        mColorSpectrumBackground = getColor(ColorSettingName.SPECTRUM_BACKGROUND);
    }

    /**
     * Fetches a named color setting from the settings manager.  If the setting
     * doesn't exist, creates the setting using the defaultColor
     */
    private Color getColor(ColorSettingName name)
    {
        ColorSetting setting = mSettingsManager.getColorSetting(name);

        return setting.getColor();
    }

    /**
     * Monitors for setting changes.  Colors can be changed by external actions
     * and will automatically update in this class
     */
    @Override
    public void settingChanged(Setting setting)
    {
        if(setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting)setting;

            switch(colorSetting.getColorSettingName())
            {
                case CHANNEL_CONFIG:
                    mColorChannelConfig = colorSetting.getColor();
                    break;
                case CHANNEL_CONFIG_PROCESSING:
                    mColorChannelConfigProcessing = colorSetting.getColor();
                    break;
                case CHANNEL_CONFIG_SELECTED:
                    mColorChannelConfigSelected = colorSetting.getColor();
                    break;
                case SPECTRUM_BACKGROUND:
                    mColorSpectrumBackground = colorSetting.getColor();
                    break;
                case SPECTRUM_CURSOR:
                    mColorSpectrumCursor = colorSetting.getColor();
                    break;
                case SPECTRUM_LINE:
                    mColorSpectrumLine = colorSetting.getColor();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Renders the channel configs, lines, labels, and cursor
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D graphics = (Graphics2D)g;
        graphics.setBackground(mColorSpectrumBackground);

        graphics.setRenderingHints(RENDERING_HINTS);

        drawFrequencies(graphics);
        drawChannels(graphics);
        drawCursor(graphics);
    }

    /**
     * Draws a cursor on the panel, whenever the mouse is hovering over the
     * panel
     */
    private void drawCursor(Graphics2D graphics)
    {
        if(mCursorVisible)
        {
            drawFrequencyLine(graphics, mCursorLocation.x, mColorSpectrumCursor);

            String frequency = CURSOR_FORMAT.format(getFrequencyFromAxis(mCursorLocation.getX()) / 1E6D);

            FontMetrics fontMetrics = graphics.getFontMetrics(this.getFont());

            Rectangle2D rect = fontMetrics.getStringBounds(frequency, graphics);

            if(mCursorLocation.y > rect.getHeight())
            {
                graphics.drawString(frequency, mCursorLocation.x + 5, mCursorLocation.y);
            }

            if(mZoom != 0)
            {
                graphics.drawString("Zoom: " + (int)FastMath.pow(2.0, mZoom) + "x", mCursorLocation.x + 17,
                    mCursorLocation.y + 11);
            }
        }
    }

    /**
     * Draws the frequency lines and labels every 10kHz
     */
    private void drawFrequencies(Graphics2D graphics)
    {
        Stroke currentStroke = graphics.getStroke();

        long minFrequency = getMinDisplayFrequency();
        long maxFrequency = getMaxDisplayFrequency();

        //Frequency increments for label and tick spacing
        int label = mLabelSizeMonitor.getLabelIncrement(graphics);
        int major = mLabelSizeMonitor.getMajorTickIncrement(graphics);
        int minor = mLabelSizeMonitor.getMinorTickIncrement(graphics);

        //Avoid divide by zero error
        if(minor == 0)
        {
            minor = 1;
        }
        if(label == 0)
        {
            label = 1;
        }

        //Adjust the start frequency to a multiple of the minor tick spacing
        long frequency = minFrequency - (minFrequency % minor);

        while(frequency < maxFrequency)
        {
            if(frequency % label == 0)
            {
                drawFrequencyLineAndLabel(graphics, frequency);
            }
            else if(frequency % major == 0)
            {
                drawTickLine(graphics, frequency, true);
            }
            else
            {
                drawTickLine(graphics, frequency, false);
            }

            frequency += minor;
        }
    }

    /**
     * Draws a vertical line and a corresponding frequency label at the bottom
     */
    private void drawFrequencyLineAndLabel(Graphics2D graphics, long frequency)
    {
        double xAxis = getAxisFromFrequency(frequency);

        drawFrequencyLine(graphics, xAxis, mColorSpectrumLine);

        drawTickLine(graphics, frequency, false);

        graphics.setColor(mColorSpectrumLine);

        drawFrequencyLabel(graphics, xAxis, frequency);
    }

    /**
     * Draws a vertical line at the xaxis
     */
    private void drawTickLine(Graphics2D graphics, long frequency, boolean major)
    {
        graphics.setColor(mColorSpectrumLine);

        double xAxis = getAxisFromFrequency(frequency);

        double start = getSize().getHeight() - mSpectrumInset;
        double end = start + (major ? 9.0d : 3.0d);

        graphics.draw(new Line2D.Double(xAxis, start, xAxis, end));
    }


    /**
     * Draws a vertical line at the xaxis
     */
    private void drawFrequencyLine(Graphics2D graphics, double xaxis, Color color)
    {
        graphics.setColor(color);

        graphics.draw(new Line2D.Double(xaxis, 0.0d, xaxis, getSize().getHeight() - mSpectrumInset));
    }

    /**
     * Draws a vertical line at the xaxis
     */
    private void drawChannelCenterLine(Graphics2D graphics, double xaxis)
    {
        double height = getSize().getHeight() - mSpectrumInset;

        graphics.setColor(Color.LIGHT_GRAY);

        graphics.draw(new Line2D.Double(xaxis, height * 0.65d, xaxis, height - 1.0d));
    }

    /**
     * Draws the Automatic Frequency Control (AFC) channel center offset
     */
    private void drawAFC(Graphics2D graphics, double frequencyAxis, double errorAxis, double bandwidth,
                         int correction, long frequency)
    {
        double height = getSize().getHeight() - mSpectrumInset;
        double verticalAxisTop = height * 0.88d;
        double verticalAxisBottom = height * 0.98d;

        double halfBandwidth = bandwidth / 2.0;
        double errorEdgeStart = errorAxis - halfBandwidth;
        double errorEdgeStop = errorAxis + halfBandwidth;

        graphics.setColor(Color.YELLOW);

        //Horizontal line connecting frequency and error line
        graphics.draw(new Line2D.Double(errorEdgeStart, verticalAxisBottom, errorEdgeStop, verticalAxisBottom));

        //Vertical band edge lines
        graphics.draw(new Line2D.Double(errorEdgeStart, verticalAxisTop, errorEdgeStart, verticalAxisBottom));
        graphics.draw(new Line2D.Double(errorEdgeStop, verticalAxisTop, errorEdgeStop, verticalAxisBottom));

        double ppm = (double)correction / ((double)frequency / 1E6d);

        String label = "PPM " + PPM_FORMATTER.format(ppm) ;

        FontMetrics fontMetrics = graphics.getFontMetrics(this.getFont());

        Rectangle2D rect = fontMetrics.getStringBounds(label, graphics);

        //Only render the correction value label if the spacing is large enough
        if(rect.getWidth() <= bandwidth && rect.getHeight() * 5 <= height)
        {
            graphics.drawString(label, (float)(errorEdgeStart + 1.0), (float)(verticalAxisBottom - 2.0));
        }
    }

    /**
     * Returns the x-axis value corresponding to the frequency
     */
    private double getAxisFromFrequency(long frequency)
    {
        double screenWidth = (double)getSize().getWidth();

        double pixelsPerBin = screenWidth / (double)mDFTSize.getSize();

        double pixelOffsetToMinDisplayFrequency = pixelsPerBin * 2.0d;

        //Calculate frequency offset from the min frequency
        double frequencyOffset = (double)(frequency - getMinDisplayFrequency());

        //Determine ratio of frequency offset to overall bandwidth
        double ratio = frequencyOffset / (double)getDisplayBandwidth();

        //Apply the ratio to the screen width minus 1 bin width
        double screenOffset = screenWidth * ratio;

        return pixelOffsetToMinDisplayFrequency + screenOffset;
    }

    /**
     * Returns the frequency corresponding to the x-axis value using the current
     * zoom level.
     */
    public long getFrequencyFromAxis(double xAxis)
    {
        double width = getSize().getWidth();

        double offset = xAxis / width;

        long frequency = getMinDisplayFrequency() + FastMath.round((double)getDisplayBandwidth() * offset);

        if(frequency > (getMaxFrequency()))
        {
            frequency = getMaxFrequency();
        }

        return frequency;
    }

    /**
     * Draws a frequency label at the x-axis position, at the bottom of the panel
     */
    private void drawFrequencyLabel(Graphics2D graphics, double xaxis, long frequency)
    {
        String label = mLabelSizeMonitor.format(frequency);

        FontMetrics fontMetrics = graphics.getFontMetrics(this.getFont());

        Rectangle2D rect = fontMetrics.getStringBounds(label, graphics);

        float xOffset = (float)rect.getWidth() / 2;

        graphics.drawString(label, (float)(xaxis - xOffset), (float)(getSize().getHeight() - 2.0f));
    }


    /**
     * Draws visible channel configs as translucent shaded frequency regions
     */
    private void drawChannels(Graphics2D graphics)
    {
        for(Channel channel : mVisibleChannels)
        {
            if(mChannelDisplay == ChannelDisplay.ALL || (mChannelDisplay == ChannelDisplay.ENABLED && channel.isProcessing()))
            {
                List<TunerChannel> tunerChannels = channel.getTunerChannels();

                for(TunerChannel tunerChannel: tunerChannels)
                {
                    if(tunerChannel.overlaps(getMinDisplayFrequency(), getMaxDisplayFrequency()))
                    {
                        //Choose the correct background color to use
                        if(channel.isSelected())
                        {
                            graphics.setColor(mColorChannelConfigSelected);
                        }
                        else if(channel.isProcessing())
                        {
                            graphics.setColor(mColorChannelConfigProcessing);
                        }
                        else
                        {
                            graphics.setColor(mColorChannelConfig);
                        }

                        double xAxis = getAxisFromFrequency(tunerChannel.getFrequency());
                        double width = (double)(tunerChannel.getBandwidth()) / (double)getDisplayBandwidth() * getSize().getWidth();

                        Rectangle2D.Double box = new Rectangle2D.Double(xAxis - (width / 2.0d), 0.0d, width,
                                getSize().getHeight() - mSpectrumInset);

                        //Fill the box with the correct color
                        graphics.fill(box);
                        graphics.draw(box);

                        //Change to the line color to render the channel name, etc.
                        graphics.setColor(mColorSpectrumLine);

                        //Draw the labels starting at yAxis position 0
                        double yAxis = 0;

                        //Draw the system label and adjust the y-axis position
                        String system = channel.hasSystem() ? channel.getSystem() : " ";

                        yAxis += drawLabel(graphics, system, this.getFont(), xAxis, yAxis, width);

                        //Draw the site label and adjust the y-axis position
                        String site = channel.hasSite() ? channel.getSite() : " ";

                        yAxis += drawLabel(graphics, site, this.getFont(), xAxis, yAxis, width);

                        //Draw the channel label and adjust the y-axis position
                        yAxis += drawLabel(graphics, channel.getName(), this.getFont(), xAxis, yAxis, width);

                        //Draw the decoder label
                        drawLabel(graphics, channel.getDecodeConfiguration().getDecoderType().getShortDisplayString(),
                                this.getFont(), xAxis, yAxis, width);
                        long frequency = tunerChannel.getFrequency();
                        double frequencyAxis = getAxisFromFrequency(frequency);
                        drawChannelCenterLine(graphics, frequencyAxis);

                        /* Draw Automatic Frequency Control line */
                        int correction = channel.getChannelFrequencyCorrection();

                        if(correction != 0)
                        {
                            long error = frequency + correction;
                            drawAFC(graphics, frequencyAxis, getAxisFromFrequency(error), width, correction,
                                    tunerChannel.getFrequency());
                        }
                    }
                }
            }
        }
    }


    /**
     * Draws a textual label at the x/y position, clipping the end of the text
     * to fit within the maxwidth value.
     *
     * @return height of the drawn label
     */
    private double drawLabel(Graphics2D graphics, String text, Font font, double x, double baseY, double maxWidth)
    {
        FontMetrics fontMetrics = graphics.getFontMetrics(font);

        if(text == null || text.isEmpty())
        {
            return 0;
        }

        Rectangle2D label = fontMetrics.getStringBounds(text, graphics);

        double offset = label.getWidth() / 2.0d;
        double y = baseY + label.getHeight();

        /**
         * If the label is wider than the max width, left justify the text and
         * clip the end of it
         */
        if(offset > (maxWidth / 2.0d))
        {
            label.setRect(x - (maxWidth / 2.0d), y - label.getHeight(), maxWidth, label.getHeight());

            graphics.setClip(label);

            graphics.drawString(text, (float)(x - (maxWidth / 2.0d)), (float)y);

            graphics.setClip(null);
        }
        else
        {
            graphics.drawString(text, (float)(x - offset), (float)y);
        }

        return label.getHeight();
    }

    /**
     * Frequency change event handler
     */
    @Override
    public void process(SourceEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                mBandwidth = event.getValue().intValue();
                mLabelSizeMonitor.update();
                break;
            case NOTIFICATION_FREQUENCY_CHANGE:
                mFrequency = event.getValue().longValue();
                mLabelSizeMonitor.update();
                break;
            default:
                break;
        }

        /**
         * Reset the visible channels list
         */
        mVisibleChannels.clear();
        mVisibleChannels.addAll(mChannelModel.getChannelsInFrequencyRange(getMinFrequency(), getMaxFrequency()));

        for(Channel trafficChannel: mTrafficChannels)
        {
            if(trafficChannel.isWithin(getMinFrequency(), getMaxFrequency()))
            {
                mVisibleChannels.add(trafficChannel);
            }
        }
    }

    /**
     * Channel change event handler
     */
    @Override
    public void receive(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        switch(event.getEvent())
        {
            case NOTIFICATION_ADD:
            case NOTIFICATION_PROCESSING_START:
                if(channel.getChannelType() == ChannelType.TRAFFIC && !mTrafficChannels.contains(channel))
                {
                    mTrafficChannels.add(channel);
                }
                if(!mVisibleChannels.contains(channel) && channel.isWithin(getMinFrequency(), getMaxFrequency()))
                {
                    mVisibleChannels.add(channel);
                }
                break;
            case NOTIFICATION_DELETE:
                mVisibleChannels.remove(channel);
                break;
            case NOTIFICATION_PROCESSING_STOP:
            case NOTIFICATION_PROCESSING_START_REJECTED:
                if(channel.getChannelType() == ChannelType.TRAFFIC)
                {
                    mVisibleChannels.remove(channel);
                    mTrafficChannels.remove(channel);
                }
                break;
            case NOTIFICATION_CONFIGURATION_CHANGE:
                if(mVisibleChannels.contains(channel) && !channel.isWithin(getMinFrequency(), getMaxFrequency()))
                {
                    mVisibleChannels.remove(channel);
                }

                if(!mVisibleChannels.contains(channel) && channel.isWithin(getMinFrequency(), getMaxFrequency()))
                {
                    mVisibleChannels.add(channel);
                }
                break;
            default:
                break;
        }

        repaint();
    }

    public int getBandwidth()
    {
        return mBandwidth;
    }

    /**
     * Currently displayed minimum frequency
     */
    public long getMinFrequency()
    {
        return mFrequency - (mBandwidth / 2);
    }

    /**
     * Currently displayed maximum frequency
     */
    public long getMaxFrequency()
    {
        return mFrequency + (mBandwidth / 2);
    }

    public boolean containsFrequency(long frequency)
    {
        return FastMath.abs(mFrequency - frequency) <= (mBandwidth / 2);
    }

    private long getMinDisplayFrequency()
    {
        double bandwidthPerBin = (double)mBandwidth / (double)(mDFTSize.getSize());

        return getMinFrequency() + (int)((mDFTZoomWindowOffset) * bandwidthPerBin);
    }

    private long getMaxDisplayFrequency()
    {
        return getMinDisplayFrequency() + getDisplayBandwidth();
    }

    private int getDisplayBandwidth()
    {
        if(mZoom != 0)
        {
            return mBandwidth / (int)FastMath.pow(2.0, mZoom);
        }

        return mBandwidth;
    }

    /**
     * Returns a list of channel configs that contain the frequency within their
     * min/max frequency settings.
     */
    public ArrayList<Channel> getChannelsAtFrequency(long frequency)
    {
        ArrayList<Channel> configs = new ArrayList<Channel>();

        for(Channel config : mVisibleChannels)
        {
            List<TunerChannel> channels = config.getTunerChannels();

            for(TunerChannel channel: channels)
            {
                if(channel != null && channel.getMinFrequency() <= frequency && channel.getMaxFrequency() >= frequency)
                {
                    configs.add(config);
                }
            }
        }

        return configs;
    }

    @Override
    public void settingDeleted(Setting setting)
    { /* not implemented */ }

    /**
     * Calculates correct spacing and format for frequency labels and major/minor
     * tick lines based on current frequency, bandwidth, zoom and screen size.
     */
    public class LabelSizeManager implements ComponentListener
    {
        private static final double LABEL_FILL_THRESHOLD = 0.5d;

        private DecimalFormat mFrequencyFormat = new DecimalFormat("0.0");

        private boolean mUpdateRequired = true;
        private int mLabelIncrement = 1;
        private int mMajorTickIncrement = 1;
        private int mMinorTickIncrement = 1;

        public String format(long frequency)
        {
            return mFrequencyFormat.format((double)frequency / 1E6D);
        }

        private void setPrecision(int precision)
        {
            if(precision < 1)
            {
                precision = 1;
            }

            if(precision > 5)
            {
                precision = 5;
            }

            mFrequencyFormat.setMinimumFractionDigits(precision);
            mFrequencyFormat.setMaximumFractionDigits(precision);
        }

        private void update(Graphics2D graphics)
        {
            if(mUpdateRequired)
            {
                //Set maximum precision as a starting point
                setPrecision(5);

                FontMetrics fontMetrics =
                    graphics.getFontMetrics(OverlayPanel.this.getFont());

                int maxLabelWidth = fontMetrics.stringWidth(format(getMaxDisplayFrequency()));

                double maxLabels = ((double)OverlayPanel.this.getWidth() * LABEL_FILL_THRESHOLD) / (double)maxLabelWidth;

                //Calculate the next smallest base 10 value for the major increment
                int power = (int)FastMath.log10((double)getDisplayBandwidth() / maxLabels);

                //Set the number of decimal places to display in frequency labels
                int precision = 5 - power;

                int start = (int)FastMath.pow(10.0, power + 1);

                int minimum = (int)FastMath.pow(10.0, power);

                int labelIncrement = start;

                while(((double)getDisplayBandwidth() / (double)labelIncrement) < maxLabels && labelIncrement >= minimum)
                {
                    labelIncrement /= 2;
                    precision++;
                }

                if(labelIncrement == minimum)
                {
                    precision = 5 - power;
                }

                setPrecision(precision);

                mLabelIncrement = labelIncrement;
                mMajorTickIncrement = labelIncrement / 2;
                mMinorTickIncrement = labelIncrement / 10;

                mUpdateRequired = false;
            }
        }

        /**
         * Forces the display to update the label and frequency display
         * calculations
         */
        public void update()
        {
            mUpdateRequired = true;
        }

        public int getMajorTickIncrement(Graphics2D graphics)
        {
            //Check to see if a calculation update is scheduled
            update(graphics);

            return mMajorTickIncrement;
        }

        public int getMinorTickIncrement(Graphics2D graphics)
        {
            return mMinorTickIncrement;
        }

        public int getLabelIncrement(Graphics2D graphics)
        {
            return mLabelIncrement;
        }

        @Override
        public void componentResized(ComponentEvent arg0)
        {
            update();
        }

        public void componentHidden(ComponentEvent arg0)
        {
        }

        public void componentMoved(ComponentEvent arg0)
        {
        }

        public void componentShown(ComponentEvent arg0)
        {
        }

    }

    public enum ChannelDisplay
    {
        ALL, ENABLED, NONE;
    }
}
