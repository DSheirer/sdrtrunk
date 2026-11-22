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
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.gui.spectrum.ChannelCardEntry;
import io.github.dsheirer.gui.spectrum.ChannelMenus;
import io.github.dsheirer.gui.spectrum.ConfigurationRow;
import io.github.dsheirer.gui.spectrum.DisplayCursor;
import io.github.dsheirer.gui.spectrum.DisplaySection;
import io.github.dsheirer.gui.spectrum.RenderArea;
import io.github.dsheirer.gui.spectrum.SpectralDisplayTheme;
import io.github.dsheirer.gui.spectrum.SpectralDisplayLayer;
import io.github.dsheirer.gui.spectrum.SpectralDisplayModel;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.util.ThreadPool;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Playlist channel layer.  Draws each playlist channel, enabled (processing) or disabled, and each active traffic
 * channel (tracked from the channel processing start and stop events) as a translucent box at its frequency and bandwidth, labeled with the system, site, channel name and
 * decoder.  Processing and selected channels use their own colors.  When the decoder applies a frequency correction,
 * a bracket at the bottom of the box shows the corrected channel position and the PPM error.
 *
 * When the zoom level is too low for a channel's labels to fit in its box, resting the mouse over the channel lists
 * it in the Channels hover card (drawn by {@link ChannelCardLayer}) with its system, site, decoder, frequency and
 * state.  This can be turned off with the hover details setting.
 *
 * Right-clicking a channel lists it in the Channels context menu, to enable or disable it or view or edit it in the
 * playlist editor.
 */
public class ChannelLayer extends SpectralDisplayLayer
{
    private static final Font FONT = Font.font(11);
    private static final DecimalFormat PPM_FORMAT = new DecimalFormat("0.0");
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private static final double MIN_LABEL_BOX_WIDTH = 12;
    private static final double MIN_HIT_WIDTH = 10;

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
    }

    /**
     * Card entries for the channels under the mouse.  A channel whose labels don't fit in its box at the current
     * zoom prompts the card.  The hit area of a narrow channel is widened so it can be found with the mouse.
     */
    @Override
    public List<ChannelCardEntry> getCardEntries(DisplayCursor cursor, RenderArea area)
    {
        SpectralDisplayModel model = getModel();
        List<ChannelCardEntry> entries = new ArrayList<>();

        if(!mHoverDetails.get() || model == null || mChannelDisplay.get() == ChannelDisplay.NONE)
        {
            return entries;
        }

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

                double hitWidth = Math.max(MIN_HIT_WIDTH, box.width());
                double hitStart = box.center() - (hitWidth / 2.0);

                if(cursor.x() >= hitStart && cursor.x() <= hitStart + hitWidth)
                {
                    entries.add(ChannelMenus.createCardEntry(channel, tunerChannel, !labelsFit(channel, box)));
                    break;
                }
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
                    //Traffic channels are started and stopped by their decoder, not from the playlist
                    Runnable toggle = !channel.isTrafficChannel() && mChannelProcessingManager != null ?
                            () -> toggle(channel) : null;
                    items.add(ChannelMenus.createChannelEntry(channel, tunerChannel, toggle));
                    break;
                }
            }
        }

        return items;
    }

    @Override
    public String getContextMenuSection()
    {
        return ChannelMenus.SECTION;
    }

    /**
     * Stops the channel when it's processing, otherwise starts it
     */
    private void toggle(Channel channel)
    {
        boolean processing = channel.isProcessing();

        ThreadPool.CACHED.execute(() -> {
            try
            {
                if(processing)
                {
                    mChannelProcessingManager.stop(channel);
                }
                else
                {
                    mChannelProcessingManager.start(channel);
                }
            }
            catch(ChannelException ce)
            {
                showError((processing ? "Can't stop " : "Can't start ") + channel.getShortTitle(), ce.getMessage());
            }
        });
    }

    /**
     * Shows an error alert over the display.  Can be called from any thread.
     */
    private void showError(String header, String message)
    {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setTitle("Channel");
            alert.setHeaderText(header);

            if(getSpectralDisplay() != null && getSpectralDisplay().getScene() != null)
            {
                alert.initOwner(getSpectralDisplay().getScene().getWindow());
            }

            alert.show();
        });
    }

    /**
     * Channel box screen position.
     */
    private record Box(double x, double width, double center)
    {
    }

}
