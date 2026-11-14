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
import io.github.dsheirer.gui.playlist.radioreference.RadioReferencePlaylistActions;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.service.radioreference.SiteRecord;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import io.github.dsheirer.gui.spectrum.ConfigurationRow;
import io.github.dsheirer.gui.spectrum.CursorReadout;
import io.github.dsheirer.gui.spectrum.DisplayCursor;
import io.github.dsheirer.gui.spectrum.DisplaySection;
import io.github.dsheirer.gui.spectrum.RenderArea;
import io.github.dsheirer.gui.spectrum.SpectralDisplayLayer;
import io.github.dsheirer.gui.spectrum.SpectralDisplayModel;
import io.github.dsheirer.gui.spectrum.SpectralDisplayTheme;
import io.github.dsheirer.service.radioreference.DistanceFormat;
import io.github.dsheirer.service.radioreference.FrequencyCandidate;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex.RankedCandidate;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Radio reference candidates layer.  When the mouse rests over the spectrum or the waterfall, shows a card next to the
 * mouse with the radio reference frequencies near the frequency under the mouse, nearest to the user's location first.
 *
 * The candidates come from the radio reference frequency index, which needs a premium account and the user's location.
 * Looking up a frequency doesn't call the web service.
 */
public class RadioReferenceLayer extends SpectralDisplayLayer
{
    private static final Duration HOVER_DELAY = Duration.millis(400);
    private static final Font FONT = Font.font(11);
    private static final Font TITLE_FONT = Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 11);
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private static final long MIN_TOLERANCE_HZ = 3_000;
    private static final double TOLERANCE_PIXELS = 3.0;
    private static final int MAX_CANDIDATES = 6;
    private static final double MAX_TEXT_WIDTH = 380;
    private static final double PADDING = 6;
    private static final double GAP = 5;
    //Space beside the text and below the readout lines, as in the cursor readout, so the readout lines stay in place
    private static final double PADDING_X = 4;
    //Space kept between the card and the edges of the section
    private static final double EDGE_MARGIN = 2;
    private static final double READOUT_PADDING = 2;
    private static final String HEADER = "Radio Reference";

    private final RadioReferenceFrequencyIndex mIndex;
    private final RadioReferencePlaylistActions mActions;
    private final BooleanProperty mHoverCandidates = new SimpleBooleanProperty(true);
    private final Text mMeasure = new Text();
    private final double mTextHeight;
    private PauseTransition mHoverTimer;
    private DisplaySection mHoverSection;
    private double mHoverX = Double.NaN;
    private double mHoverY = Double.NaN;
    private boolean mHoverReady;
    private long mResultsFrequency = Long.MIN_VALUE;
    private long mResultsTolerance;
    private List<RankedCandidate> mResults = Collections.emptyList();
    private Card mCard;

    /**
     * Candidate text of the card, measured once per lookup
     * @param entries lines of each candidate: the title, then the details
     * @param more line, or null
     * @param textWidth of the widest line
     * @param lineCount of the header, candidate and more lines
     * @param maxTextWidth that the lines were shortened to
     * @param maxHeight that the card was limited to
     * @param readoutLines that the card was sized for
     */
    private record Card(List<List<String>> entries, String more, double textWidth, int lineCount,
                        double maxTextWidth, double maxHeight, int readoutLines)
    {
    }

    /**
     * Constructs an instance.
     * @param index of the radio reference frequencies near the user, or null for a layer that shows nothing
     * @param actions to add candidates to the playlist or enable their channels, or null for no context menu items
     */
    public RadioReferenceLayer(RadioReferenceFrequencyIndex index, RadioReferencePlaylistActions actions)
    {
        mIndex = index;
        mActions = actions;
        mMeasure.setFont(FONT);
        mMeasure.setText("Ag");
        mTextHeight = Math.ceil(mMeasure.getLayoutBounds().getHeight());
        mHoverCandidates.addListener((ob, ov, nv) -> requestRender());
    }

    /**
     * Shows the card of radio reference candidates when the mouse rests over the display
     */
    public BooleanProperty hoverCandidatesProperty()
    {
        return mHoverCandidates;
    }

    @Override
    protected void attached()
    {
        if(mIndex != null)
        {
            mIndex.start();
        }
    }

    @Override
    public void dispose()
    {
        if(mHoverTimer != null)
        {
            mHoverTimer.stop();
        }
    }

    @Override
    public String getConfigurationTitle()
    {
        return "Radio Reference";
    }

    @Override
    public List<ConfigurationRow> getConfigurationRows()
    {
        if(mIndex == null)
        {
            return Collections.emptyList();
        }

        ConfigurationRow hover = ConfigurationRow.toggle("Hover Candidates", mHoverCandidates);
        Tooltip.install(hover.control(), new Tooltip("When the mouse rests over a signal, show the Radio Reference " +
            "frequencies near it, nearest to your location first"));
        Label status = new Label();
        status.textProperty().bind(mIndex.statusProperty());
        status.setWrapText(true);
        status.setMaxWidth(220);
        return List.of(hover, new ConfigurationRow("Nearby", status));
    }

    /**
     * Context menu items for the candidates at the frequency, in the card's order: add a candidate to the playlist and
     * start it, or enable or disable the playlist channel that already decodes it.  One item per site, and per
     * frequency and protocol, so an item never adds a duplicate channel.
     */
    @Override
    public List<MenuItem> getContextMenuItems(DisplaySection section, long frequency)
    {
        if(mIndex == null || mActions == null)
        {
            return Collections.emptyList();
        }

        //The card's candidates when the mouse hasn't moved since the card was shown
        List<RankedCandidate> candidates = frequency == mResultsFrequency ? mResults :
            mIndex.lookup(frequency, mResultsTolerance > 0 ? mResultsTolerance : MIN_TOLERANCE_HZ);
        List<MenuItem> items = new ArrayList<>();
        Set<String> offered = new HashSet<>();

        for(RankedCandidate ranked: candidates)
        {
            if(offered.size() >= MAX_CANDIDATES)
            {
                break;
            }

            FrequencyCandidate candidate = ranked.candidate();

            if(candidate.isSiteFrequency())
            {
                SiteRecord site = mIndex.getSite(candidate.systemId(), candidate.siteId());

                //A repeater site (LTR) has a channel per repeater, otherwise one channel for the site
                String key = site == null ? null : RadioReferencePlaylistActions.isRepeaterSite(site) ?
                    "repeater:" + candidate.frequencyHz() : "site:" + site.systemId() + ":" + site.siteId();

                if(site != null && offered.add(key))
                {
                    addSiteItem(items, site, candidate, frequency);
                }
            }
            else
            {
                DecoderType decoderType = RadioReferencePlaylistActions.getDecoderType(candidate);
                Set<DecoderType> family = RadioReferencePlaylistActions.getProtocolFamily(decoderType);
                String protocol = family.isEmpty() ? String.valueOf(candidate.mode()) : family.iterator().next().name();

                if(offered.add("frequency:" + candidate.frequencyHz() + ":" + protocol))
                {
                    addConventionalItem(items, candidate, decoderType, frequency);
                }
            }
        }

        if(!items.isEmpty())
        {
            MenuItem header = new MenuItem("Radio Reference");
            header.setDisable(true);
            items.add(0, header);
        }

        return items;
    }

    private void addSiteItem(List<MenuItem> items, SiteRecord site, FrequencyCandidate candidate, long frequency)
    {
        String label = site.systemName() + " \u00B7 Site " + site.siteNumber() +
            (site.description() != null ? " " + site.description() : "");
        String unsupported = RadioReferencePlaylistActions.getUnsupportedReason(site);

        if(unsupported != null)
        {
            MenuItem item = new MenuItem(label + " \u2013 " + unsupported);
            item.setDisable(true);
            items.add(item);
            return;
        }

        if(RadioReferencePlaylistActions.isRepeaterSite(site))
        {
            addRepeaterItem(items, site, candidate.frequencyHz(), label, frequency);
            return;
        }

        Channel existing = mActions.findSiteChannel(site);

        if(existing != null)
        {
            //The channel layer offers the channels at the clicked frequency
            if(!covers(existing, frequency))
            {
                items.add(createEnableItem(existing));
            }
        }
        else
        {
            MenuItem item = new MenuItem("Add and Start: " + label);
            //Start on the right-clicked frequency when it's a control or alternate control channel
            item.setOnAction(event -> mActions.addSite(site, candidate.frequencyHz(), this::showError));
            items.add(item);
        }
    }

    /**
     * Menu item for one repeater of a repeater site (LTR): add and start it, or enable or disable its channel
     */
    private void addRepeaterItem(List<MenuItem> items, SiteRecord site, long repeaterFrequency, String label,
                                 long frequency)
    {
        Channel existing = mActions.findChannel(List.of(repeaterFrequency),
            RadioReferencePlaylistActions.getSiteDecoderTypes(site));

        if(existing != null)
        {
            if(!covers(existing, frequency))
            {
                items.add(createEnableItem(existing));
            }

            return;
        }

        String lcn = site.channels().stream().filter(channel -> channel.frequencyHz() == repeaterFrequency &&
            channel.logicalChannelNumber() > 0).map(channel -> " LCN " + channel.logicalChannelNumber())
            .findFirst().orElse("");
        MenuItem item = new MenuItem("Add and Start: " + label + lcn + " (" +
            FREQUENCY_FORMAT.format(repeaterFrequency / 1E6) + " MHz)");
        item.setOnAction(event -> mActions.addRepeater(site, repeaterFrequency, this::showError));
        items.add(item);
    }

    private void addConventionalItem(List<MenuItem> items, FrequencyCandidate candidate, DecoderType decoderType,
                                     long frequency)
    {
        String label = candidate.title() + " (" + FREQUENCY_FORMAT.format(candidate.frequencyHz() / 1E6) + " MHz" +
            (candidate.mode() != null ? " " + candidate.mode() : "") + ")";

        if(decoderType == null)
        {
            MenuItem item = new MenuItem(label + " \u2013 mode isn't supported");
            item.setDisable(true);
            items.add(item);
            return;
        }

        Channel existing = mActions.findConventionalChannel(candidate);

        if(existing != null)
        {
            if(!covers(existing, frequency))
            {
                items.add(createEnableItem(existing));
            }
        }
        else
        {
            MenuItem item = new MenuItem("Add and Start: " + label);
            item.setOnAction(event -> mActions.addConventional(candidate, this::showError));
            items.add(item);
        }
    }

    /**
     * Menu item that enables the playlist channel when it's disabled, or disables it when it's enabled
     */
    private MenuItem createEnableItem(Channel channel)
    {
        boolean processing = channel.isProcessing();
        MenuItem item = new MenuItem((processing ? "Disable: " : "Enable: ") + channel.getShortTitle());
        item.setOnAction(event -> {
            if(processing)
            {
                mActions.disable(channel, this::showError);
            }
            else
            {
                mActions.enable(channel, this::showError);
            }
        });
        return item;
    }

    /**
     * Indicates if one of the channel's tuner channels includes the frequency
     */
    private static boolean covers(Channel channel, long frequency)
    {
        for(TunerChannel tunerChannel: channel.getTunerChannels())
        {
            if(tunerChannel != null && tunerChannel.getMinFrequency() <= frequency &&
                frequency <= tunerChannel.getMaxFrequency())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Shows an error alert over the display.  Can be called from any thread.
     */
    private void showError(String message)
    {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setTitle("Radio Reference");
            alert.setHeaderText("Can't add or start the channel");

            if(getSpectralDisplay() != null && getSpectralDisplay().getScene() != null)
            {
                alert.initOwner(getSpectralDisplay().getScene().getWindow());
            }

            alert.show();
        });
    }

    @Override
    public void renderSpectrumForeground(GraphicsContext gc, RenderArea area)
    {
        render(gc, area, DisplaySection.SPECTRUM);
    }

    @Override
    public void renderWaterfall(GraphicsContext gc, RenderArea area)
    {
        render(gc, area, DisplaySection.WATERFALL);
    }

    /**
     * Draws the card in the section under the mouse once the mouse has rested for the hover delay
     */
    private void render(GraphicsContext gc, RenderArea area, DisplaySection section)
    {
        DisplayCursor cursor = getSpectralDisplay() != null ? getSpectralDisplay().getDisplayCursor() : null;
        trackHover(cursor);

        if(cursor != null && cursor.section() == section && replacesCursorReadout(cursor, area))
        {
            drawCard(gc, area, cursor, getCandidates(cursor.frequency(), getTolerance(getModel(), area)));
        }
    }

    /**
     * The card replaces the cursor readout while it's shown: it ends with the readout lines, in the readout's place.
     */
    @Override
    public boolean replacesCursorReadout(DisplayCursor cursor, RenderArea area)
    {
        SpectralDisplayModel model = getModel();

        //Only for the position the mouse rested at - the readout comes back as soon as the mouse moves
        if(mIndex == null || !mHoverCandidates.get() || !mHoverReady || model == null || !model.hasSource() ||
            cursor == null || cursor.section() != mHoverSection || cursor.x() != mHoverX || cursor.y() != mHoverY ||
            area.width() <= 0)
        {
            return false;
        }

        return !getCandidates(cursor.frequency(), getTolerance(model, area)).isEmpty();
    }

    /**
     * Frequency tolerance for the candidates: a few pixels or a DFT bin at the current zoom, but at least 3 kHz
     */
    private static long getTolerance(SpectralDisplayModel model, RenderArea area)
    {
        double hertzPerPixel = model.getDisplayBandwidth() / area.width();
        return Math.max(MIN_TOLERANCE_HZ, Math.round(Math.max(TOLERANCE_PIXELS * hertzPerPixel, model.getBinWidth())));
    }

    /**
     * Restarts the hover delay each time the mouse moves, and hides the card until it passes
     */
    private void trackHover(DisplayCursor cursor)
    {
        if(cursor == null)
        {
            if(mHoverSection != null)
            {
                mHoverSection = null;
                mHoverReady = false;
                getHoverTimer().stop();
            }

            return;
        }

        if(cursor.section() != mHoverSection || cursor.x() != mHoverX || cursor.y() != mHoverY)
        {
            mHoverSection = cursor.section();
            mHoverX = cursor.x();
            mHoverY = cursor.y();
            mHoverReady = false;
            getHoverTimer().playFromStart();
        }
    }

    private PauseTransition getHoverTimer()
    {
        if(mHoverTimer == null)
        {
            mHoverTimer = new PauseTransition(HOVER_DELAY);
            mHoverTimer.setOnFinished(event -> {
                mHoverReady = true;
                requestRender();
            });
        }

        return mHoverTimer;
    }

    /**
     * Candidates for the frequency, looked up again only when the frequency or the tolerance changes
     */
    private List<RankedCandidate> getCandidates(long frequency, long tolerance)
    {
        if(frequency != mResultsFrequency || tolerance != mResultsTolerance)
        {
            mResults = mIndex.lookup(frequency, tolerance);
            mResultsFrequency = frequency;
            mResultsTolerance = tolerance;
            mCard = null;
        }

        return mResults;
    }

    /**
     * Draws the card in place of the cursor readout: beside the mouse and above it, placed like the readout, with the
     * candidates above the readout lines so the frequency and level stay where the readout showed them.  The card
     * fits the larger side of the mouse and the height of the section, showing as many candidates as fit.
     */
    private void drawCard(GraphicsContext gc, RenderArea area, DisplayCursor cursor, List<RankedCandidate> candidates)
    {
        //The level line changes with the mouse height while the candidates don't
        List<String> readout = CursorReadout.lines(cursor, area, getModel());
        double readoutWidth = 0;

        for(String line: readout)
        {
            readoutWidth = Math.max(readoutWidth, measure(line, FONT));
        }

        double room = Math.max(area.width() - cursor.x(), cursor.x()) - CursorReadout.GAP_X - EDGE_MARGIN;
        double maxTextWidth = Math.max(readoutWidth, Math.min(MAX_TEXT_WIDTH, room - (PADDING_X * 2)));
        //Above the mouse, so the readout lines stay where the readout showed them, when a candidate fits there
        double above = cursor.y() - CursorReadout.GAP_Y;
        double maxHeight = above >= cardHeight(5, 1, true, readout.size()) ? above : area.plotHeight() - EDGE_MARGIN;

        if(mCard == null || mCard.maxTextWidth() != maxTextWidth || mCard.maxHeight() != maxHeight ||
            mCard.readoutLines() != readout.size())
        {
            mCard = createCard(candidates, maxTextWidth, maxHeight, readout.size());
        }

        Card card = mCard;
        SpectralDisplayTheme theme = getSpectralDisplay().getTheme();
        double width = Math.max(card.textWidth(), readoutWidth) + (PADDING_X * 2);
        double height = cardHeight(card.lineCount(), card.entries().size(), card.more() != null, readout.size());
        double x = Math.max(0, CursorReadout.x(cursor, width, area));
        double y = CursorReadout.y(cursor, height, area);

        gc.setFill(theme.getBackground(0.92));
        gc.fillRoundRect(x, y, width, height, 8, 8);
        gc.setStroke(opaque(theme.getLine()));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(Math.floor(x) + 0.5, Math.floor(y) + 0.5, width, height, 8, 8);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);

        double textX = x + PADDING_X;
        double lineY = y + PADDING;
        gc.setFill(theme.getCursor());
        gc.setFont(TITLE_FONT);
        gc.fillText(HEADER, textX, lineY);
        lineY += mTextHeight + GAP;

        for(List<String> lines: card.entries())
        {
            gc.setFill(theme.getCursor());
            gc.setFont(TITLE_FONT);
            gc.fillText(lines.get(0), textX, lineY);
            lineY += mTextHeight;
            gc.setFill(opaque(theme.getLine()));
            gc.setFont(FONT);

            for(int line = 1; line < lines.size(); line++)
            {
                gc.fillText(lines.get(line), textX, lineY);
                lineY += mTextHeight;
            }

            lineY += GAP;
        }

        if(card.more() != null)
        {
            gc.setFill(opaque(theme.getLine()));
            gc.setFont(FONT);
            gc.fillText(card.more(), textX, lineY);
            lineY += mTextHeight + GAP;
        }

        //The readout lines, separated from the candidates, in the readout's style
        double separator = Math.floor(lineY - (GAP / 2.0)) + 0.5;
        gc.setStroke(theme.getLine(0.5));
        gc.strokeLine(textX, separator, x + width - PADDING_X, separator);
        gc.setFill(theme.getCursor());
        gc.setFont(FONT);

        for(String line: readout)
        {
            gc.fillText(line, textX, lineY);
            lineY += mTextHeight;
        }
    }

    /**
     * Height of a card with the lines
     * @param lineCount of the header, candidate and more lines
     * @param entries number of candidates
     * @param more true when the card has a more line
     * @param readoutLines number of readout lines
     */
    private double cardHeight(int lineCount, int entries, boolean more, int readoutLines)
    {
        int gaps = 1 + entries + (more ? 1 : 0);
        return ((lineCount + readoutLines) * mTextHeight) + (gaps * GAP) + PADDING + READOUT_PADDING;
    }

    /**
     * Card text for as many candidates as fit the height: each candidate's title, details and source, shortened to
     * the width
     */
    private Card createCard(List<RankedCandidate> candidates, double maxTextWidth, double maxHeight,
                            int readoutLines)
    {
        int limit = Math.min(candidates.size(), MAX_CANDIDATES);
        List<List<String>> entries = new ArrayList<>();
        double textWidth = measure(HEADER, TITLE_FONT);
        int lineCount = 1;

        for(int index = 0; index < limit; index++)
        {
            RankedCandidate candidate = candidates.get(index);
            List<String> lines = new ArrayList<>();
            String title = candidate.candidate().title() != null ? candidate.candidate().title() : "(untitled)";
            lines.add(fit(title, TITLE_FONT, maxTextWidth));
            lines.add(fit(describe(candidate), FONT, maxTextWidth));
            String source = source(candidate.candidate());

            if(source != null)
            {
                lines.add(fit(source, FONT, maxTextWidth));
            }

            //Keep room for the more line when candidates are left out
            boolean more = index + 1 < candidates.size();

            if(!entries.isEmpty() && cardHeight(lineCount + lines.size() + (more ? 1 : 0), entries.size() + 1, more,
                readoutLines) > maxHeight)
            {
                break;
            }

            textWidth = Math.max(textWidth, measure(lines.get(0), TITLE_FONT));

            for(int line = 1; line < lines.size(); line++)
            {
                textWidth = Math.max(textWidth, measure(lines.get(line), FONT));
            }

            lineCount += lines.size();
            entries.add(lines);
        }

        String more = candidates.size() > entries.size() ? "+ " + (candidates.size() - entries.size()) + " more" : null;

        if(more != null)
        {
            textWidth = Math.max(textWidth, measure(more, FONT));
            lineCount++;
        }

        return new Card(entries, more, textWidth, lineCount, maxTextWidth, maxHeight, readoutLines);
    }

    /**
     * Frequency, mode or use, tone, encryption and distance of the candidate
     */
    private String describe(RankedCandidate ranked)
    {
        FrequencyCandidate candidate = ranked.candidate();
        return join(FREQUENCY_FORMAT.format(candidate.frequencyHz() / 1E6) + " MHz", candidate.use(), candidate.mode(),
            candidate.tone(), candidate.encrypted() ? "Encrypted" : null,
            ranked.hasDistance() ? DistanceFormat.format(ranked.distanceKilometers()) : null);
    }

    /**
     * Where the candidate is listed, with its alpha tag when the title isn't the alpha tag
     */
    private static String source(FrequencyCandidate candidate)
    {
        String alphaTag = candidate.alphaTag() != null && !candidate.alphaTag().equals(candidate.title()) ?
            candidate.alphaTag() : null;
        return join(alphaTag, candidate.source());
    }

    /**
     * Text shortened with an ellipsis to fit the width
     */
    private String fit(String text, Font font, double maxWidth)
    {
        if(measure(text, font) <= maxWidth)
        {
            return text;
        }

        int length = text.length();

        while(length > 1 && measure(text.substring(0, length) + "…", font) > maxWidth)
        {
            length--;
        }

        return text.substring(0, length) + "…";
    }

    private double measure(String text, Font font)
    {
        mMeasure.setFont(font);
        mMeasure.setText(text);
        double width = mMeasure.getLayoutBounds().getWidth();
        mMeasure.setFont(FONT);
        return width;
    }

    private static String join(String... values)
    {
        StringBuilder sb = new StringBuilder();

        for(String value: values)
        {
            if(value != null && !value.isEmpty())
            {
                if(!sb.isEmpty())
                {
                    sb.append(" · ");
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
}
