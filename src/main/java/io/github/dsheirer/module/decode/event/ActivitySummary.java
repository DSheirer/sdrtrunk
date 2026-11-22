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
package io.github.dsheirer.module.decode.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Builds a decoder activity summary as plain text with aligned columns, intended for display with a monospaced font.
 *
 * A summary has a title and a series of sections.  Each section contains label/value fields, tables, and free text
 * lines, in the order they were added.  All fields in a section share a common label width so that the values line
 * up, and each table sizes its columns to the widest header or cell value.  Empty sections and empty tables render
 * as 'None'.
 *
 * <pre>
 * P25 Phase 1 C4FM
 * ================
 *
 * Network
 * -------
 *   WACN    BEE00 [781824]
 *   System  3A1 [929]
 *
 * Talker Aliases
 * --------------
 *   Radio    Talker Alias
 *   -------  ------------
 *   1234     ENGINE 1
 *   1234567  BC
 * </pre>
 */
public class ActivitySummary
{
    public static final String NONE = "None";
    public static final String UNKNOWN = "Unknown";
    private static final String INDENT = "  ";
    private static final String COLUMN_GAP = "  ";

    private final String mTitle;
    private final List<Section> mSections = new ArrayList<>();
    private Section mCurrentSection;

    /**
     * Constructs an instance
     * @param title for the summary, normally the decoder name
     */
    public ActivitySummary(String title)
    {
        mTitle = title;
    }

    /**
     * Starts a new section.  Subsequent fields, tables and text lines are added to this section.
     * @param heading for the section
     * @return this summary for method chaining
     */
    public ActivitySummary section(String heading)
    {
        mCurrentSection = new Section(heading);
        mSections.add(mCurrentSection);
        return this;
    }

    /**
     * Adds a label/value field to the current section.
     * @param label for the field
     * @param value for the field, or null to show 'Unknown'
     * @return this summary for method chaining
     */
    public ActivitySummary field(String label, Object value)
    {
        getCurrentSection().mItems.add(new Field(label, value != null ? String.valueOf(value) : UNKNOWN));
        return this;
    }

    /**
     * Adds a line of text to the current section.
     * @param text to add
     * @return this summary for method chaining
     */
    public ActivitySummary text(Object text)
    {
        getCurrentSection().mItems.add(new Text(String.valueOf(text)));
        return this;
    }

    /**
     * Adds a table to the current section.  Add rows to the returned table.
     * @param headers for each column
     * @return table
     */
    public Table table(String... headers)
    {
        Table table = new Table(headers);
        getCurrentSection().mItems.add(table);
        return table;
    }

    /**
     * Current section, creating an untitled section when none has been started.
     */
    private Section getCurrentSection()
    {
        if(mCurrentSection == null)
        {
            section(null);
        }

        return mCurrentSection;
    }

    /**
     * Formats a frequency in Hertz as Megahertz with 5 decimal places.
     * @param frequency in Hertz
     * @return formatted frequency, or 'Unknown' when the frequency is zero or negative
     */
    public static String mhz(long frequency)
    {
        return frequency > 0 ? String.format(Locale.US, "%.5f", frequency / 1E6d) : UNKNOWN;
    }

    /**
     * Formats a frequency in Hertz as Kilohertz.
     * @param frequency in Hertz
     * @return formatted frequency
     */
    public static String khz(long frequency)
    {
        return String.format(Locale.US, "%.2f", frequency / 1E3d).replace(".00", "");
    }

    /**
     * Formats a signed frequency offset in Hertz as Megahertz with 4 decimal places and a leading sign.
     * @param offset in Hertz
     * @return formatted offset
     */
    public static String offsetMhz(long offset)
    {
        return String.format(Locale.US, "%+.4f", offset / 1E6d);
    }

    /**
     * Renders the summary
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(mTitle != null)
        {
            sb.append(mTitle).append("\n").append("=".repeat(mTitle.length())).append("\n");
        }

        for(Section section: mSections)
        {
            sb.append("\n");
            section.render(sb);
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Appends the line to the string builder with trailing whitespace removed.
     */
    private static void line(StringBuilder sb, String line)
    {
        sb.append(line.stripTrailing()).append("\n");
    }

    private static String pad(String value, int width)
    {
        return value.length() >= width ? value : value + " ".repeat(width - value.length());
    }

    /**
     * Summary section
     */
    private static class Section
    {
        private final String mHeading;
        private final List<Object> mItems = new ArrayList<>();

        Section(String heading)
        {
            mHeading = heading;
        }

        void render(StringBuilder sb)
        {
            if(mHeading != null)
            {
                sb.append(mHeading).append("\n").append("-".repeat(mHeading.length())).append("\n");
            }

            if(mItems.isEmpty())
            {
                line(sb, INDENT + NONE);
                return;
            }

            int labelWidth = 0;

            for(Object item: mItems)
            {
                if(item instanceof Field field)
                {
                    labelWidth = Math.max(labelWidth, field.mLabel.length());
                }
            }

            Object previous = null;

            for(Object item: mItems)
            {
                //Separate a table from the items around it with a blank line
                if(previous != null && (item instanceof Table || previous instanceof Table))
                {
                    sb.append("\n");
                }

                if(item instanceof Field field)
                {
                    line(sb, INDENT + pad(field.mLabel, labelWidth) + COLUMN_GAP + field.mValue);
                }
                else if(item instanceof Text text)
                {
                    line(sb, INDENT + text.mText);
                }
                else if(item instanceof Table table)
                {
                    table.render(sb);
                }

                previous = item;
            }
        }
    }

    private record Field(String mLabel, String mValue) {}

    private record Text(String mText) {}

    /**
     * Table with column widths sized to the widest header or cell in each column.
     */
    public static class Table
    {
        private final String[] mHeaders;
        private final List<String[]> mRows = new ArrayList<>();

        private Table(String[] headers)
        {
            mHeaders = headers;
        }

        /**
         * Adds a row to the table.  Null cells render as empty, missing cells render as empty and extra cells are
         * ignored.
         * @param cells for the row
         * @return this table for method chaining
         */
        public Table row(Object... cells)
        {
            String[] row = new String[mHeaders.length];
            Arrays.fill(row, "");

            for(int x = 0; x < Math.min(cells.length, row.length); x++)
            {
                row[x] = cells[x] != null ? String.valueOf(cells[x]) : "";
            }

            mRows.add(row);
            return this;
        }

        /**
         * Indicates if the table has any rows.
         */
        public boolean isEmpty()
        {
            return mRows.isEmpty();
        }

        void render(StringBuilder sb)
        {
            if(mRows.isEmpty())
            {
                line(sb, INDENT + NONE);
                return;
            }

            int[] widths = new int[mHeaders.length];

            for(int x = 0; x < mHeaders.length; x++)
            {
                widths[x] = mHeaders[x].length();

                for(String[] row: mRows)
                {
                    widths[x] = Math.max(widths[x], row[x].length());
                }
            }

            String[] underlines = new String[mHeaders.length];

            for(int x = 0; x < mHeaders.length; x++)
            {
                underlines[x] = "-".repeat(widths[x]);
            }

            renderRow(sb, mHeaders, widths);
            renderRow(sb, underlines, widths);

            for(String[] row: mRows)
            {
                renderRow(sb, row, widths);
            }
        }

        private static void renderRow(StringBuilder sb, String[] cells, int[] widths)
        {
            StringBuilder row = new StringBuilder(INDENT);

            for(int x = 0; x < cells.length; x++)
            {
                if(x > 0)
                {
                    row.append(COLUMN_GAP);
                }

                row.append(pad(cells[x], widths[x]));
            }

            line(sb, row.toString());
        }
    }
}
