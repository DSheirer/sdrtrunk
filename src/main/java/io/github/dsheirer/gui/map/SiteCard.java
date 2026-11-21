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

package io.github.dsheirer.gui.map;

import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import io.github.dsheirer.rrapi.type.Coverage;
import io.github.dsheirer.service.radioreference.DistanceFormat;
import io.github.dsheirer.service.radioreference.SiteRecord;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Card with the details of radio reference trunked system sites, shown when the mouse rests over a site's tower on the
 * map: the system and protocol, the site, and the site's channels.
 */
public final class SiteCard
{
    private static final int MAX_SITES = 3;
    private static final int CHANNEL_COLUMNS = 3;
    private static final int MAX_CHANNELS = 24;
    private static final double MAX_TEXT_WIDTH = 380;
    private static final String SEPARATOR = " · ";
    private static final String STYLE = "-fx-background-color: -fx-control-inner-background; " +
        "-fx-border-color: -fx-box-border; -fx-background-radius: 3; -fx-border-radius: 3; -fx-padding: 6; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0, 1, 2);";

    /**
     * Channel frequency with a control channel marker: C for control, A for alternate control, or null
     */
    public record Channel(String frequency, String marker) {}

    /**
     * Details of one site
     * @param title system name
     * @param lines protocol, site, identifiers
     * @param channels of the site, up to the card's limit
     * @param notes about the channels
     */
    public record Entry(String title, List<String> lines, List<Channel> channels, List<String> notes) {}

    /**
     * Card content
     * @param entries for the sites shown, nearest first
     * @param moreSites count of the sites under the mouse that aren't shown
     */
    public record Content(List<Entry> entries, int moreSites) {}

    private SiteCard()
    {
    }

    /**
     * Content of the card for the sites
     * @param sites under the mouse, nearest first
     * @param location of the user, for the distances, or null
     */
    public static Content content(List<SiteRecord> sites, RadioReferencePreference.Location location)
    {
        DecimalFormat frequencyFormat = new DecimalFormat("0.00000");
        List<Entry> entries = new ArrayList<>();
        int shown = Math.min(sites.size(), MAX_SITES);

        for(int index = 0; index < shown; index++)
        {
            SiteRecord site = sites.get(index);
            List<String> lines = new ArrayList<>();
            addLine(lines, site.protocol(), site.voice());
            String distance = location != null ? DistanceFormat.format(Coverage.distanceKilometers(
                location.latitude(), location.longitude(), site.latitude(), site.longitude())) : null;
            addLine(lines, "Site " + site.siteNumber(), site.description(), site.county(), distance);
            addLine(lines, site.nac() != null ? "NAC " + site.nac() : null,
                site.rfss() > 0 ? "RFSS " + site.rfss() : null, site.zone() > 0 ? "Zone " + site.zone() : null);

            List<Channel> channels = new ArrayList<>();
            List<String> notes = new ArrayList<>();
            boolean control = false;

            if(site.channels() != null)
            {
                for(SiteRecord.SiteChannel channel: site.channels().subList(0, Math.min(site.channels().size(),
                    MAX_CHANNELS)))
                {
                    String marker = "Control".equals(channel.use()) ? "C" :
                        "Alternate control".equals(channel.use()) ? "A" : null;
                    control |= marker != null;
                    channels.add(new Channel(frequencyFormat.format(channel.frequencyHz() / 1E6), marker));
                }

                if(site.channels().size() > MAX_CHANNELS)
                {
                    notes.add("+ " + (site.channels().size() - MAX_CHANNELS) + " more channels");
                }
            }

            if(control)
            {
                notes.add("C control, A alternate control");
            }

            entries.add(new Entry(site.systemName() != null ? site.systemName() : "(unnamed system)", lines,
                channels, notes.isEmpty() ? List.of() : List.of(String.join(SEPARATOR, notes))));
        }

        return new Content(entries, sites.size() - shown);
    }

    /**
     * Card node for the content
     */
    public static Region create(Content content)
    {
        VBox card = new VBox(2);
        card.setStyle(STYLE);
        card.setMaxWidth(Region.USE_PREF_SIZE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        Font font = Font.font(11);
        Font bold = Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 11);

        for(int index = 0; index < content.entries().size(); index++)
        {
            Entry entry = content.entries().get(index);

            if(index > 0)
            {
                card.getChildren().add(new Separator());
            }

            card.getChildren().add(label(entry.title(), bold));
            entry.lines().forEach(line -> card.getChildren().add(label(line, font)));

            if(!entry.channels().isEmpty())
            {
                GridPane grid = new GridPane();
                grid.setHgap(10);

                for(int channelIndex = 0; channelIndex < entry.channels().size(); channelIndex++)
                {
                    Channel channel = entry.channels().get(channelIndex);
                    HBox cell = new HBox(3, label(channel.frequency(), font));

                    if(channel.marker() != null)
                    {
                        cell.getChildren().add(label(channel.marker(), bold));
                    }

                    grid.add(cell, channelIndex % CHANNEL_COLUMNS, channelIndex / CHANNEL_COLUMNS);
                }

                card.getChildren().add(grid);
            }

            entry.notes().forEach(note -> card.getChildren().add(label(note, font)));
        }

        if(content.moreSites() > 0)
        {
            card.getChildren().add(new Separator());
            card.getChildren().add(label("+ " + content.moreSites() + " more site" +
                (content.moreSites() > 1 ? "s" : "") + " here", font));
        }

        return card;
    }

    private static Label label(String text, Font font)
    {
        Label label = new Label(text);
        label.setFont(font);
        label.setWrapText(true);
        label.setMaxWidth(MAX_TEXT_WIDTH);
        return label;
    }

    /**
     * Adds a line with the non-empty values separated by dots, or nothing when all are empty
     */
    private static void addLine(List<String> lines, String... values)
    {
        List<String> parts = new ArrayList<>();

        for(String value: values)
        {
            if(value != null && !value.isBlank())
            {
                parts.add(value);
            }
        }

        if(!parts.isEmpty())
        {
            lines.add(String.join(SEPARATOR, parts));
        }
    }
}
