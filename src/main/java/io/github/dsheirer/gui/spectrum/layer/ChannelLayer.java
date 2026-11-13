/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.gui.spectrum.layer;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.channel.ViewChannelRequest;
import io.github.dsheirer.gui.spectrum.ConfigurationRow;
import io.github.dsheirer.gui.spectrum.DisplayCursor;
import io.github.dsheirer.gui.spectrum.DisplaySection;
import io.github.dsheirer.gui.spectrum.RenderArea;
import io.github.dsheirer.gui.spectrum.SpectralDisplayTheme;
import io.github.dsheirer.gui.spectrum.SpectralDisplayLayer;
import io.github.dsheirer.gui.spectrum.SpectralDisplayModel;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Playlist channel layer.  Draws each playlist channel, enabled (processing) or disabled, and each active traffic
 * channel (tracked from the channel processing start and stop events) as a translucent box at its frequency and bandwidth, labeled with the system, site, channel name and
 * decoder.  Processing and selected channels use their own colors.  When the decoder applies a frequency correction,
 * a bracket at the bottom of the box shows the corrected channel position and the PPM error.
 *
 * When the zoom level is too low for a channel's labels to fit in its box, moving the mouse over the channel shows a
 * popout card near the bottom of the spectrum (or the waterfall) with the channel name, system, site, decoder,
 * frequency and state.  The popout can be turned off with the hover details setting.
 *
 * Right-clicking a channel adds a menu item to view or edit the channel in the playlist editor.
 */
public class ChannelLayer extends SpectralDisplayLayer
{
    private static final Font FONT = Font.font(11);
    private static final DecimalFormat PPM_FORMAT = new DecimalFormat("0.0");
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private static final Font POPOUT_TITLE_FONT = Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 11);
    private static final double MIN_LABEL_BOX_WIDTH = 12;
    private static final double POPOUT_MIN_HIT_WIDTH = 10;
    private static final double POPOUT_PADDING = 6;
    private static final double POPOUT_GAP = 5;
    private static final double POPOUT_SWATCH = 8;
    private static final int POPOUT_MAX_CHANNELS = 4;

    /**
     * Which channels the layer shows.
     */
    public enum ChannelDisplay
    {
        ALL("All Channels"),
        ENABLED("Enabled Channels"),
        NONE("No Channels");

        private final String mLabel;

        ChannelDisplay(String label)
        {
            mLabel = label;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    private final ChannelModel mChannelModel;
    private final ChannelProcessingManager mChannelProcessingManager;
    private final Listener<ChannelEvent> mChannelEventListener = this::receive;
    //Active traffic channels.  Traffic channel managers keep their temporary channels out of the channel model, so
    //they are tracked from the processing start and stop events.
    private final Set<Channel> mTrafficChannels = ConcurrentHashMap.newKeySet();
    private final ObjectProperty<ChannelDisplay> mChannelDisplay = new SimpleObjectProperty<>(ChannelDisplay.ALL);
    private final BooleanProperty mHoverDetails = new SimpleBooleanProperty(true);
    private final Text mMeasure = new Text();
    private final double mTextHeight;
    private volatile boolean mChannelsChanged = true;
    private List<Channel> mChannels = new ArrayList<>();
    private long mQueryMinimum;
    private long mQueryMaximum;

    /**
     * Constructs an instance.
     * @param channelModel for the playlist channels and the active traffic channels
     * @param channelProcessingManager for channel processing start and stop events
     */
    public ChannelLayer(ChannelModel channelModel, ChannelProcessingManager channelProcessingManager)
    {
        mChannelModel = channelModel;
        mChannelProcessingManager = channelProcessingManager;
        mMeasure.setFont(FONT);
        mMeasure.setText("Ag");
        mTextHeight = Math.ceil(mMeasure.getLayoutBounds().getHeight());
        mChannelDisplay.addListener((ob, ov, nv) -> requestRender());
        mHoverDetails.addListener((ob, ov, nv) -> requestRender());

        if(mChannelModel != null)
        {
            mChannelModel.addListener(mChannelEventListener);
        }

        if(mChannelProcessingManager != null)
        {
            mChannelProcessingManager.addChannelEventListener(mChannelEventListener);

            //Traffic channels that were already running before this layer was created
            for(Channel channel: mChannelProcessingManager.getProcessingChannels())
            {
                if(channel.isTrafficChannel())
                {
                    mTrafficChannels.add(channel);
                }
            }
        }
    }

    /**
     * Tracks active traffic channels and refreshes the channels on any channel change.
     */
    private void receive(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        if(channel != null && channel.isTrafficChannel())
        {
            switch(event.getEvent())
            {
                case NOTIFICATION_PROCESSING_START -> mTrafficChannels.add(channel);
                case NOTIFICATION_PROCESSING_STOP, NOTIFICATION_PROCESSING_START_REJECTED, NOTIFICATION_DELETE ->
                        mTrafficChannels.remove(channel);
                default ->
                {
                }
            }
        }

        channelsChanged();
    }

    /**
     * Which channels to show.
     */
    public ObjectProperty<ChannelDisplay> channelDisplayProperty()
    {
        return mChannelDisplay;
    }

    @Override
    public void dispose()
    {
        if(mChannelModel != null)
        {
            mChannelModel.removeListener(mChannelEventListener);
        }

        if(mChannelProcessingManager != null)
        {
            mChannelProcessingManager.removeChannelEventListener(mChannelEventListener);
        }

        mChannels.clear();
        mTrafficChannels.clear();
    }

    private void channelsChanged()
    {
        mChannelsChanged = true;
        requestRender();
    }

    @Override
    public String getConfigurationTitle()
    {
        return "Channels";
    }

    @Override
    public List<ConfigurationRow> getConfigurationRows()
    {
        ComboBox<ChannelDisplay> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(ChannelDisplay.values());
        comboBox.valueProperty().bindBidirectional(mChannelDisplay);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        ConfigurationRow details = ConfigurationRow.toggle("Hover Details", mHoverDetails);
        Tooltip.install(details.control(), new Tooltip("Show channel details under the mouse when the channel " +
                "labels don't fit at the current zoom"));
        return List.of(new ConfigurationRow("Show", comboBox), details);
    }

    /**
     * Shows a popout card with the details of the channels under the mouse when their labels don't fit.
     */
    public BooleanProperty hoverDetailsProperty()
    {
        return mHoverDetails;
    }

    /**
     * Channels that overlap the source bandwidth, refreshed when the channels or the source frequency change.
     */
    private List<Channel> getChannels(SpectralDisplayModel model)
    {
        long minimum = model.getMinFrequency();
        long maximum = model.getMaxFrequency();

        if(mChannelsChanged || minimum != mQueryMinimum || maximum != mQueryMaximum)
        {
            try
            {
                mChannelsChanged = false;
                List<Channel> channels = mChannelModel != null ?
                        mChannelModel.getChannelsInFrequencyRange(minimum, maximum) : new ArrayList<>();

                for(Channel trafficChannel: mTrafficChannels)
                {
                    if(!channels.contains(trafficChannel) && trafficChannel.isWithin(minimum, maximum))
                    {
                        channels.add(trafficChannel);
                    }
                }

                mChannels = channels;
                mQueryMinimum = minimum;
                mQueryMaximum = maximum;
            }
            catch(ConcurrentModificationException cme)
            {
                //The channel lists changed while reading them - try again on the next frame
                mChannelsChanged = true;
                requestRender();
            }
        }

        return mChannels;
    }

    private boolean isShown(Channel channel)
    {
        return switch(mChannelDisplay.get())
        {
            case ALL -> true;
            case ENABLED -> channel.isProcessing();
            default -> false;
        };
    }

    @Override
    public void renderSpectrumBackground(GraphicsContext gc, RenderArea area)
    {
        SpectralDisplayModel model = getModel();

        if(model == null || !model.hasSource() || mChannelDisplay.get() == ChannelDisplay.NONE)
        {
            return;
        }

        SpectralDisplayTheme theme = getSpectralDisplay().getTheme();

        for(Channel channel: getChannels(model))
        {
            if(!isShown(channel))
            {
                continue;
            }

            Color color = channel.isSelected() ? theme.getChannelSelected() :
                    channel.isProcessing() ? theme.getChannelProcessing() : theme.getChannel();
            gc.setFill(color);

            for(TunerChannel tunerChannel: channel.getTunerChannels())
            {
                Box box = getBox(model, tunerChannel, area);

                if(box != null)
                {
                    gc.fillRect(box.x(), 0, box.width(), area.plotHeight());
                }
            }
        }
    }

    @Override
    public void renderSpectrumForeground(GraphicsContext gc, RenderArea area)
    {
        SpectralDisplayModel model = getModel();

        if(model == null || !model.hasSource() || mChannelDisplay.get() == ChannelDisplay.NONE)
        {
            return;
        }

        SpectralDisplayTheme theme = getSpectralDisplay().getTheme();
        Color line = theme.getLine();
        gc.setFont(FONT);
        gc.setTextBaseline(VPos.TOP);
        gc.setLineWidth(1.0);

        for(Channel channel: getChannels(model))
        {
            if(!isShown(channel))
            {
                continue;
            }

            for(TunerChannel tunerChannel: channel.getTunerChannels())
            {
                Box box = getBox(model, tunerChannel, area);

                if(box == null)
                {
                    continue;
                }

                double center = Math.floor(box.center()) + 0.5;
                gc.setStroke(line);
                gc.strokeLine(center, area.plotHeight() * 0.65, center, area.plotHeight() - 1);

                if(box.width() >= MIN_LABEL_BOX_WIDTH)
                {
                    gc.save();
                    gc.beginPath();
                    gc.rect(box.x(), 0, box.width(), area.plotHeight());
                    gc.clip();
                    gc.setFill(line);
                    double y = 1;
                    y = drawLabel(gc, channel.hasSystem() ? channel.getSystem() : null, box, y);
                    y = drawLabel(gc, channel.hasSite() ? channel.getSite() : null, box, y);
                    y = drawLabel(gc, channel.getName(), box, y);
                    drawLabel(gc, channel.getDecodeConfiguration().getDecoderType().getShortDisplayString(), box, y);
                    gc.restore();
                }

                int correction = channel.getChannelFrequencyCorrection();

                if(correction != 0)
                {
                    drawCorrection(gc, model, area, tunerChannel, box, correction);
                }
            }
        }

        renderPopout(gc, area, DisplaySection.SPECTRUM);
    }

    @Override
    public void renderWaterfall(GraphicsContext gc, RenderArea area)
    {
        renderPopout(gc, area, DisplaySection.WATERFALL);
    }

    /**
     * Draws a card near the bottom of the section under the mouse with the details of each channel under the mouse
     * whose labels don't fit in its box at the current zoom.  The card moves to the top of the section when the mouse
     * is over the bottom of the section.
     */
    private void renderPopout(GraphicsContext gc, RenderArea area, DisplaySection section)
    {
        SpectralDisplayModel model = getModel();
        DisplayCursor cursor = getSpectralDisplay().getDisplayCursor();

        if(!mHoverDetails.get() || model == null || !model.hasSource() || cursor == null ||
                cursor.section() != section || mChannelDisplay.get() == ChannelDisplay.NONE)
        {
            return;
        }

        List<PopoutEntry> entries = getPopoutEntries(model, area, cursor.x());

        if(entries.isEmpty())
        {
            return;
        }

        SpectralDisplayTheme theme = getSpectralDisplay().getTheme();
        int shown = Math.min(entries.size(), POPOUT_MAX_CHANNELS);
        String more = entries.size() > shown ? "+ " + (entries.size() - shown) + " more" : null;

        //Measure the card
        double textWidth = 0;
        int lineCount = 0;

        for(int x = 0; x < shown; x++)
        {
            PopoutEntry entry = entries.get(x);
            textWidth = Math.max(textWidth, measure(entry.title(), POPOUT_TITLE_FONT) + POPOUT_SWATCH + 5);

            for(String line: entry.details())
            {
                textWidth = Math.max(textWidth, measure(line, FONT));
            }

            lineCount += 1 + entry.details().size();
        }

        if(more != null)
        {
            textWidth = Math.max(textWidth, measure(more, FONT));
            lineCount++;
        }

        double width = textWidth + (POPOUT_PADDING * 2);
        double height = (lineCount * mTextHeight) + ((shown - 1) * POPOUT_GAP) + (POPOUT_PADDING * 2);
        double bottom = section == DisplaySection.SPECTRUM ? area.plotHeight() : area.height();
        double x = Math.max(4, Math.min(area.width() - width - 4, cursor.x() - (width / 2.0)));
        double y = bottom - height - 6;

        //Keep the card out from under the mouse
        if(cursor.y() >= y - 4)
        {
            y = 6;
        }

        if(y < 0 || width > area.width())
        {
            return;
        }

        gc.setFill(theme.getBackground(0.88));
        gc.fillRoundRect(x, y, width, height, 8, 8);
        gc.setStroke(opaque(theme.getLine()));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(Math.floor(x) + 0.5, Math.floor(y) + 0.5, width, height, 8, 8);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);

        double lineY = y + POPOUT_PADDING;
        double textX = x + POPOUT_PADDING;

        for(int index = 0; index < shown; index++)
        {
            PopoutEntry entry = entries.get(index);
            double swatchY = lineY + ((mTextHeight - POPOUT_SWATCH) / 2.0);
            gc.setFill(opaque(entry.color()));
            gc.fillRect(textX, swatchY, POPOUT_SWATCH, POPOUT_SWATCH);
            gc.setFill(theme.getCursor());
            gc.setFont(POPOUT_TITLE_FONT);
            gc.fillText(entry.title(), textX + POPOUT_SWATCH + 5, lineY);
            lineY += mTextHeight;
            gc.setFill(opaque(theme.getLine()));
            gc.setFont(FONT);

            for(String line: entry.details())
            {
                gc.fillText(line, textX, lineY);
                lineY += mTextHeight;
            }

            lineY += POPOUT_GAP;
        }

        if(more != null)
        {
            gc.fillText(more, textX, lineY - POPOUT_GAP);
        }
    }

    /**
     * Channels under the screen x position whose labels don't fit in the channel box.  The hit area of a narrow
     * channel is widened so it can be found with the mouse.
     */
    private List<PopoutEntry> getPopoutEntries(SpectralDisplayModel model, RenderArea area, double cursorX)
    {
        List<PopoutEntry> entries = new ArrayList<>();
        SpectralDisplayTheme theme = getSpectralDisplay().getTheme();

        for(Channel channel: getChannels(model))
        {
            if(!isShown(channel))
            {
                continue;
            }

            for(TunerChannel tunerChannel: channel.getTunerChannels())
            {
                Box box = getBox(model, tunerChannel, area);

                if(box == null)
                {
                    continue;
                }

                double hitWidth = Math.max(POPOUT_MIN_HIT_WIDTH, box.width());
                double hitStart = box.center() - (hitWidth / 2.0);

                if(cursorX < hitStart || cursorX > hitStart + hitWidth || labelsFit(channel, box))
                {
                    continue;
                }

                List<String> details = new ArrayList<>();
                String systemSite = join(channel.hasSystem() ? channel.getSystem() : null,
                        channel.hasSite() ? channel.getSite() : null);

                if(systemSite != null)
                {
                    details.add(systemSite);
                }

                String state = channel.isProcessing() ? "Enabled" : "Disabled";

                if(channel.isSelected())
                {
                    state += ", Selected";
                }

                details.add(join(channel.getDecodeConfiguration().getDecoderType().getShortDisplayString(),
                        FREQUENCY_FORMAT.format(tunerChannel.getFrequency() / 1E6) + " MHz", state));

                Color color = channel.isSelected() ? theme.getChannelSelected() :
                        channel.isProcessing() ? theme.getChannelProcessing() : theme.getChannel();
                entries.add(new PopoutEntry(channel.getName() != null ? channel.getName() : "(unnamed)", details,
                        color));
                break;
            }
        }

        return entries;
    }

    /**
     * Indicates if all of the channel labels fit inside the channel box without clipping.
     */
    private boolean labelsFit(Channel channel, Box box)
    {
        if(box.width() < MIN_LABEL_BOX_WIDTH)
        {
            return false;
        }

        String[] labels = {channel.hasSystem() ? channel.getSystem() : null,
                channel.hasSite() ? channel.getSite() : null, channel.getName(),
                channel.getDecodeConfiguration().getDecoderType().getShortDisplayString()};

        for(String label: labels)
        {
            if(label != null && !label.isEmpty() && measure(label, FONT) > box.width())
            {
                return false;
            }
        }

        return true;
    }

    private double measure(String text, Font font)
    {
        mMeasure.setFont(font);
        mMeasure.setText(text);
        double width = mMeasure.getLayoutBounds().getWidth();
        mMeasure.setFont(FONT);
        return width;
    }

    /**
     * Joins the non-empty values with a separator, or returns null when there are none.
     */
    private static String join(String... values)
    {
        StringBuilder sb = new StringBuilder();

        for(String value: values)
        {
            if(value != null && !value.isEmpty())
            {
                if(!sb.isEmpty())
                {
                    sb.append(" \u00B7 ");
                }

                sb.append(value);
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }

    private static Color opaque(Color color)
    {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Draws the label centered in the box, or left aligned and clipped when it is wider than the box.
     * @return y position for the next label
     */
    private double drawLabel(GraphicsContext gc, String text, Box box, double y)
    {
        if(text != null && !text.isEmpty())
        {
            mMeasure.setText(text);
            double width = mMeasure.getLayoutBounds().getWidth();

            if(width > box.width())
            {
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(text, box.x() + 1, y);
            }
            else
            {
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(text, box.center(), y);
            }
        }

        return y + mTextHeight;
    }

    /**
     * Draws a bracket at the bottom of the channel at the corrected frequency, labeled with the PPM error.
     */
    private void drawCorrection(GraphicsContext gc, SpectralDisplayModel model, RenderArea area,
                                TunerChannel tunerChannel, Box box, int correction)
    {
        double top = area.plotHeight() * 0.88;
        double bottom = area.plotHeight() * 0.98;
        double corrected = model.getX(tunerChannel.getFrequency() + correction, area.width());
        double start = corrected - (box.width() / 2.0);
        double stop = corrected + (box.width() / 2.0);

        Color correctionColor = getSpectralDisplay().getTheme().getCorrection();
        gc.setStroke(correctionColor);
        gc.strokeLine(start, bottom, stop, bottom);
        gc.strokeLine(start, top, start, bottom);
        gc.strokeLine(stop, top, stop, bottom);

        double ppm = correction / (tunerChannel.getFrequency() / 1E6);
        String label = "PPM " + PPM_FORMAT.format(ppm);
        mMeasure.setText(label);

        if(mMeasure.getLayoutBounds().getWidth() <= box.width() && mTextHeight * 5 <= area.plotHeight())
        {
            gc.setFill(correctionColor);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.BOTTOM);
            gc.fillText(label, start + 1, bottom - 2);
            gc.setTextBaseline(VPos.TOP);
        }
    }

    /**
     * Screen position of the tuner channel, or null when it is outside the display.
     */
    private static Box getBox(SpectralDisplayModel model, TunerChannel tunerChannel, RenderArea area)
    {
        long minimum = (long)Math.floor(model.getDisplayMinFrequency());
        long maximum = (long)Math.ceil(model.getDisplayMaxFrequency());

        if(tunerChannel == null || !tunerChannel.overlaps(minimum, maximum))
        {
            return null;
        }

        double center = model.getX(tunerChannel.getFrequency(), area.width());
        double width = tunerChannel.getBandwidth() / model.getDisplayBandwidth() * area.width();
        return new Box(center - (width / 2.0), width, center);
    }

    @Override
    public List<MenuItem> getContextMenuItems(DisplaySection section, long frequency)
    {
        List<MenuItem> items = new ArrayList<>();
        SpectralDisplayModel model = getModel();

        if(model == null || mChannelDisplay.get() == ChannelDisplay.NONE)
        {
            return items;
        }

        for(Channel channel: getChannels(model))
        {
            if(!isShown(channel))
            {
                continue;
            }

            for(TunerChannel tunerChannel: channel.getTunerChannels())
            {
                if(tunerChannel != null && tunerChannel.getMinFrequency() <= frequency &&
                        frequency <= tunerChannel.getMaxFrequency())
                {
                    MenuItem item = new MenuItem("View/Edit: " + channel.getShortTitle());
                    item.setOnAction(e -> MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel)));
                    items.add(item);
                    break;
                }
            }
        }

        return items;
    }

    /**
     * Channel box screen position.
     */
    private record Box(double x, double width, double center)
    {
    }

    /**
     * Channel details for the popout card.
     */
    private record PopoutEntry(String title, List<String> details, Color color)
    {
    }
}
