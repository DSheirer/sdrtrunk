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

import io.github.dsheirer.gui.spectrum.ChannelCardEntry;
import io.github.dsheirer.gui.spectrum.ChannelMenus;
import io.github.dsheirer.gui.spectrum.CursorReadout;
import io.github.dsheirer.gui.spectrum.DisplayCursor;
import io.github.dsheirer.gui.spectrum.DisplaySection;
import io.github.dsheirer.gui.spectrum.RenderArea;
import io.github.dsheirer.gui.spectrum.SpectralDisplayLayer;
import io.github.dsheirer.gui.spectrum.SpectralDisplayModel;
import io.github.dsheirer.gui.spectrum.SpectralDisplayTheme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javafx.animation.PauseTransition;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Channels hover card.  When the mouse rests over the spectrum or the waterfall, shows a card next to the mouse with
 * the channels at the mouse position that the other layers provide (see
 * {@link SpectralDisplayLayer#getCardEntries}): playlist channels first, then radio reference candidates.  The titles
 * have the colors of the Channels context menu entries: one for an enabled playlist channel, one for a disabled one
 * and one for a candidate.  The card replaces the cursor readout and ends with the readout lines.
 *
 * The card is shown when an entry prompts it, e.g. a playlist channel whose labels don't fit in its box or a
 * candidate; the other channels at the mouse position are then listed too.
 */
public class ChannelCardLayer extends SpectralDisplayLayer
{
    private static final Duration HOVER_DELAY = Duration.millis(400);
    private static final Font FONT = Font.font(11);
    private static final Font TITLE_FONT = Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 11);
    private static final int MAX_ENTRIES = 6;
    private static final double MAX_TEXT_WIDTH = 380;
    private static final double PADDING = 6;
    private static final double GAP = 5;
    //Space beside the text and below the readout lines, as in the cursor readout, so the readout lines stay in place
    private static final double PADDING_X = 4;
    //Space kept between the card and the edges of the section
    private static final double EDGE_MARGIN = 2;
    private static final double READOUT_PADDING = 2;
    private static final String HEADER = ChannelMenus.SECTION;

    private final Text mMeasure = new Text();
    private final double mTextHeight;
    private PauseTransition mHoverTimer;
    private DisplaySection mHoverSection;
    private double mHoverX = Double.NaN;
    private double mHoverY = Double.NaN;
    private boolean mHoverReady;
    private List<ChannelCardEntry> mEntries = Collections.emptyList();
    private Card mCard;

    /**
     * Card text, measured when the entries or the room change
     * @param entries lines of each shown entry: the title, then the details
     * @param states of the shown entries
     * @param more line, or null
     * @param textWidth of the widest line
     * @param lineCount of the header, entry and more lines
     * @param source entries the card was made from
     * @param maxTextWidth that the lines were shortened to
     * @param maxHeight that the card was limited to
     * @param readoutLines that the card was sized for
     */
    private record Card(List<List<String>> entries, List<ChannelMenus.State> states, String more, double textWidth,
                        int lineCount, List<ChannelCardEntry> source, double maxTextWidth, double maxHeight,
                        int readoutLines)
    {
    }

    /**
     * Constructs an instance
     */
    public ChannelCardLayer()
    {
        mMeasure.setFont(FONT);
        mMeasure.setText("Ag");
        mTextHeight = Math.ceil(mMeasure.getLayoutBounds().getHeight());
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
            drawCard(gc, area, cursor, mEntries);
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
        if(!mHoverReady || model == null || !model.hasSource() || cursor == null || cursor.section() != mHoverSection ||
            cursor.x() != mHoverX || cursor.y() != mHoverY || area.width() <= 0)
        {
            return false;
        }

        mEntries = getEntries(cursor, area);
        return mEntries.stream().anyMatch(ChannelCardEntry::prompts);
    }

    /**
     * Entries of the other layers, each channel once, playlist channels first
     */
    private List<ChannelCardEntry> getEntries(DisplayCursor cursor, RenderArea area)
    {
        List<ChannelCardEntry> entries = new ArrayList<>();
        Set<Object> keys = Collections.newSetFromMap(new IdentityHashMap<>());

        for(SpectralDisplayLayer layer: getSpectralDisplay().getLayers())
        {
            if(layer == this)
            {
                continue;
            }

            for(ChannelCardEntry entry: layer.getCardEntries(cursor, area))
            {
                if(entry.key() == null || keys.add(entry.key()))
                {
                    entries.add(entry);
                }
            }
        }

        entries.sort(Comparator.comparing(entry -> entry.state() == ChannelMenus.State.CANDIDATE));
        return entries;
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
     * Draws the card in place of the cursor readout: beside the mouse and above it, placed like the readout, with the
     * channels above the readout lines so the frequency and level stay where the readout showed them.  The card fits
     * the larger side of the mouse and the height of the section, showing as many channels as fit.
     */
    private void drawCard(GraphicsContext gc, RenderArea area, DisplayCursor cursor, List<ChannelCardEntry> entries)
    {
        //The level line changes with the mouse height while the channels don't
        List<String> readout = CursorReadout.lines(cursor, area, getModel());
        double readoutWidth = 0;

        for(String line: readout)
        {
            readoutWidth = Math.max(readoutWidth, measure(line, FONT));
        }

        double room = Math.max(area.width() - cursor.x(), cursor.x()) - CursorReadout.GAP_X - EDGE_MARGIN;
        double maxTextWidth = Math.max(readoutWidth, Math.min(MAX_TEXT_WIDTH, room - (PADDING_X * 2)));
        //Above the mouse, so the readout lines stay where the readout showed them, when an entry fits there
        double above = cursor.y() - CursorReadout.GAP_Y;
        double maxHeight = above >= cardHeight(5, 1, true, readout.size()) ? above : area.plotHeight() - EDGE_MARGIN;

        if(mCard == null || !mCard.source().equals(entries) || mCard.maxTextWidth() != maxTextWidth ||
            mCard.maxHeight() != maxHeight || mCard.readoutLines() != readout.size())
        {
            mCard = createCard(entries, maxTextWidth, maxHeight, readout.size());
        }

        Card card = mCard;
        SpectralDisplayTheme theme = getSpectralDisplay().getTheme();
        Color background = opaque(theme.getBackground());
        Color detail = opaque(theme.getLine());
        double width = Math.max(card.textWidth(), readoutWidth) + (PADDING_X * 2);
        double height = cardHeight(card.lineCount(), card.entries().size(), card.more() != null, readout.size());
        double x = Math.max(0, CursorReadout.x(cursor, width, area));
        double y = CursorReadout.y(cursor, height, area);

        gc.setFill(theme.getBackground(0.92));
        gc.fillRoundRect(x, y, width, height, 8, 8);
        gc.setStroke(detail);
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(Math.floor(x) + 0.5, Math.floor(y) + 0.5, width, height, 8, 8);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);

        double textX = x + PADDING_X;
        double lineY = y + PADDING;
        gc.setFill(detail);
        gc.setFont(TITLE_FONT);
        gc.fillText(HEADER, textX, lineY);
        lineY += mTextHeight + GAP;

        for(int index = 0; index < card.entries().size(); index++)
        {
            List<String> lines = card.entries().get(index);
            gc.setFill(ChannelMenus.readable(ChannelMenus.getColor(theme, card.states().get(index)), background));
            gc.setFont(TITLE_FONT);
            gc.fillText(lines.get(0), textX, lineY);
            lineY += mTextHeight;
            gc.setFill(detail);
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
            gc.setFill(detail);
            gc.setFont(FONT);
            gc.fillText(card.more(), textX, lineY);
            lineY += mTextHeight + GAP;
        }

        //The readout lines, separated from the channels, in the readout's style
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
     * @param lineCount of the header, entry and more lines
     * @param entries number of entries
     * @param more true when the card has a more line
     * @param readoutLines number of readout lines
     */
    private double cardHeight(int lineCount, int entries, boolean more, int readoutLines)
    {
        int gaps = 1 + entries + (more ? 1 : 0);
        return ((lineCount + readoutLines) * mTextHeight) + (gaps * GAP) + PADDING + READOUT_PADDING;
    }

    /**
     * Card text for as many entries as fit the height, shortened to the width
     */
    private Card createCard(List<ChannelCardEntry> source, double maxTextWidth, double maxHeight, int readoutLines)
    {
        int limit = Math.min(source.size(), MAX_ENTRIES);
        List<List<String>> entries = new ArrayList<>();
        List<ChannelMenus.State> states = new ArrayList<>();
        double textWidth = measure(HEADER, TITLE_FONT);
        int lineCount = 1;

        for(int index = 0; index < limit; index++)
        {
            ChannelCardEntry entry = source.get(index);
            List<String> lines = new ArrayList<>();
            lines.add(fit(entry.title(), TITLE_FONT, maxTextWidth));

            for(String detail: entry.details())
            {
                if(detail != null)
                {
                    lines.add(fit(detail, FONT, maxTextWidth));
                }
            }

            //Keep room for the more line when entries are left out
            boolean more = index + 1 < source.size();

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
            states.add(entry.state());
        }

        String more = source.size() > entries.size() ? "+ " + (source.size() - entries.size()) + " more" : null;

        if(more != null)
        {
            textWidth = Math.max(textWidth, measure(more, FONT));
            lineCount++;
        }

        return new Card(entries, states, more, textWidth, lineCount, source, maxTextWidth, maxHeight, readoutLines);
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

    private static Color opaque(Color color)
    {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue());
    }
}
