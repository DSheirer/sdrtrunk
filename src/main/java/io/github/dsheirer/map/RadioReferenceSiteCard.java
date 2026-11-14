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

package io.github.dsheirer.map;

import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import io.github.dsheirer.rrapi.type.Coverage;
import io.github.dsheirer.service.radioreference.DistanceFormat;
import io.github.dsheirer.service.radioreference.SiteRecord;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * HTML card with the details of radio reference trunked system sites, shown when the mouse is over a site's tower on
 * the map: the system and protocol, the site, and the site's channels.
 */
public final class RadioReferenceSiteCard
{
    private static final int MAX_SITES = 3;
    private static final int CHANNEL_COLUMNS = 3;
    private static final int MAX_CHANNELS = 24;
    private static final String SEPARATOR = " &middot; ";

    private RadioReferenceSiteCard()
    {
    }

    /**
     * Card for the sites
     * @param sites under the mouse, nearest first
     * @param location of the user, for the distances, or null
     * @return HTML for a tooltip
     */
    public static String html(List<SiteRecord> sites, RadioReferencePreference.Location location)
    {
        DecimalFormat frequencyFormat = new DecimalFormat("0.00000");
        StringBuilder sb = new StringBuilder("<html><body style='padding:3px'>");
        int shown = Math.min(sites.size(), MAX_SITES);

        for(int index = 0; index < shown; index++)
        {
            SiteRecord site = sites.get(index);

            if(index > 0)
            {
                sb.append("<hr>");
            }

            sb.append("<div><b>").append(escape(site.systemName() != null ? site.systemName() : "(unnamed system)"))
                .append("</b></div>");
            appendLine(sb, site.protocol(), site.voice());

            String siteName = "Site " + site.siteNumber();
            String distance = location != null ? DistanceFormat.format(Coverage.distanceKilometers(
                location.latitude(), location.longitude(), site.latitude(), site.longitude())) : null;
            appendLine(sb, siteName, site.description(), site.county(), distance);
            appendLine(sb, site.nac() != null ? "NAC " + site.nac() : null,
                site.rfss() > 0 ? "RFSS " + site.rfss() : null, site.zone() > 0 ? "Zone " + site.zone() : null);
            appendChannels(sb, site.channels(), frequencyFormat);
        }

        if(sites.size() > shown)
        {
            sb.append("<hr><div>+ ").append(sites.size() - shown).append(" more site")
                .append(sites.size() - shown > 1 ? "s" : "").append(" here</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Channels in columns, control channels marked C and alternate control channels marked A
     */
    private static void appendChannels(StringBuilder sb, List<SiteRecord.SiteChannel> channels,
                                       DecimalFormat frequencyFormat)
    {
        if(channels == null || channels.isEmpty())
        {
            return;
        }

        int shown = Math.min(channels.size(), MAX_CHANNELS);
        boolean control = false;
        sb.append("<table cellspacing='0' cellpadding='1'>");

        for(int index = 0; index < shown; index++)
        {
            if(index % CHANNEL_COLUMNS == 0)
            {
                sb.append("<tr>");
            }

            SiteRecord.SiteChannel channel = channels.get(index);
            sb.append("<td style='padding-right:8px'>").append(frequencyFormat.format(channel.frequencyHz() / 1E6));

            if("Control".equals(channel.use()))
            {
                sb.append(" <b>C</b>");
                control = true;
            }
            else if("Alternate control".equals(channel.use()))
            {
                sb.append(" <b>A</b>");
                control = true;
            }

            sb.append("</td>");

            if(index % CHANNEL_COLUMNS == CHANNEL_COLUMNS - 1)
            {
                sb.append("</tr>");
            }
        }

        if(shown % CHANNEL_COLUMNS != 0)
        {
            sb.append("</tr>");
        }

        sb.append("</table>");
        List<String> notes = new ArrayList<>();

        if(channels.size() > shown)
        {
            notes.add("+ " + (channels.size() - shown) + " more channels");
        }

        if(control)
        {
            notes.add("C control, A alternate control");
        }

        if(!notes.isEmpty())
        {
            appendLine(sb, notes.toArray(new String[0]));
        }
    }

    /**
     * Appends a line with the non-empty values separated by dots, or nothing when all are empty
     */
    private static void appendLine(StringBuilder sb, String... values)
    {
        StringBuilder line = new StringBuilder();

        for(String value: values)
        {
            if(value != null && !value.isBlank())
            {
                if(!line.isEmpty())
                {
                    line.append(SEPARATOR);
                }

                line.append(escape(value));
            }
        }

        if(!line.isEmpty())
        {
            sb.append("<div>").append(line).append("</div>");
        }
    }

    private static String escape(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
