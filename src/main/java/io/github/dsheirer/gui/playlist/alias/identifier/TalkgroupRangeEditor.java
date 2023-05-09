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

package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.gui.control.LtrFormatter;
import io.github.dsheirer.gui.control.PrefixIdentFormatter;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for talkgroup range alias identifiers
 */
public class TalkgroupRangeEditor extends IdentifierEditor<TalkgroupRange>
{
    private static final Logger mLog = LoggerFactory.getLogger(TalkgroupRangeEditor.class);

    private UserPreferences mUserPreferences;
    private Label mProtocolLabel;
    private Label mFormatLabel;
    private TextField mMinTalkgroupField;
    private TextField mMaxTalkgroupField;
    private TextFormatter<Integer> mMinIntegerTextFormatter;
    private TextFormatter<Integer> mMaxIntegerTextFormatter;
    private List<TalkgroupDetail> mTalkgroupDetails;
    private MinTalkgroupValueChangeListener mMinTalkgroupValueChangeListener = new MinTalkgroupValueChangeListener();
    private MaxTalkgroupValueChangeListener mMaxTalkgroupValueChangeListener = new MaxTalkgroupValueChangeListener();

    /**
     * Constructs an instance
     * @param userPreferences for determining user preferred talkgroup formats
     */
    public TalkgroupRangeEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mTalkgroupDetails = createTalkgroupDetails();

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(3);

        GridPane.setConstraints(getProtocolLabel(), 0, 0);
        gridPane.getChildren().add(getProtocolLabel());

        Label valueLabel = new Label("Talkgroup Range");
        GridPane.setHalignment(valueLabel, HPos.RIGHT);
        GridPane.setConstraints(valueLabel, 1, 0);
        gridPane.getChildren().add(valueLabel);

        GridPane.setConstraints(getMinTalkgroupField(), 2, 0);
        gridPane.getChildren().add(getMinTalkgroupField());

        Label dashLabel = new Label("-");
        GridPane.setConstraints(dashLabel, 3, 0);
        gridPane.getChildren().add(dashLabel);

        GridPane.setConstraints(getMaxTalkgroupField(), 4, 0);
        gridPane.getChildren().add(getMaxTalkgroupField());

        GridPane.setConstraints(getFormatLabel(), 5, 0);
        gridPane.getChildren().add(getFormatLabel());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(TalkgroupRange item)
    {
        super.setItem(item);

        TalkgroupRange talkgroupRange = getItem();

        getProtocolLabel().setDisable(talkgroupRange == null);
        getMinTalkgroupField().setDisable(talkgroupRange == null);
        getMaxTalkgroupField().setDisable(talkgroupRange == null);

        if(talkgroupRange != null)
        {
            getProtocolLabel().setText(talkgroupRange.getProtocol().toString());
            updateTextFormatter();
        }
        else
        {
            getProtocolLabel().setText("Unrecognized Protocol");
            getMinTalkgroupField().setText(null);
            getMaxTalkgroupField().setText(null);
        }

        modifiedProperty().set(false);
    }

    private void updateTextFormatter()
    {
        if(mMinIntegerTextFormatter != null)
        {
            mMinIntegerTextFormatter.valueProperty().removeListener(mMinTalkgroupValueChangeListener);
        }

        if(mMaxIntegerTextFormatter != null)
        {
            mMaxIntegerTextFormatter.valueProperty().removeListener(mMaxTalkgroupValueChangeListener);
        }

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(getItem().getProtocol());

        if(format != null)
        {
            TalkgroupDetail talkgroupDetail = getTalkgroupDetail(getItem().getProtocol(), format);

            if(talkgroupDetail != null)
            {
                mMinIntegerTextFormatter = talkgroupDetail.getMinTextFormatter();
                Integer minValue = getItem() != null ? getItem().getMinTalkgroup() : null;
                mMinTalkgroupField.setTextFormatter(mMinIntegerTextFormatter);
                mMinTalkgroupField.setTooltip(new Tooltip(talkgroupDetail.getTooltip()));
                mMinIntegerTextFormatter.setValue(minValue);
                mMinIntegerTextFormatter.valueProperty().addListener(mMinTalkgroupValueChangeListener);

                mMaxIntegerTextFormatter = talkgroupDetail.getMaxTextFormatter();
                Integer maxValue = getItem() != null ? getItem().getMaxTalkgroup() : null;
                mMaxTalkgroupField.setTextFormatter(mMaxIntegerTextFormatter);
                mMaxTalkgroupField.setTooltip(new Tooltip(talkgroupDetail.getTooltip()));
                mMaxIntegerTextFormatter.setValue(maxValue);
                mMaxIntegerTextFormatter.valueProperty().addListener(mMaxTalkgroupValueChangeListener);

                getFormatLabel().setText(talkgroupDetail.getTooltip());
            }
            else
            {
                mLog.warn("Couldn't find talkgroup detail for protocol [" + getItem().getProtocol() +
                        "] and format [" + format + "]");
                getFormatLabel().setText(" ");
            }
        }
        else
        {
            getFormatLabel().setText(" ");
            mMinIntegerTextFormatter.setValue(null);
            mMaxIntegerTextFormatter.setValue(null);
        }
    }

    @Override
    public void save()
    {
        //no-op
    }

    @Override
    public void dispose()
    {
        //no-op
    }

    private Label getFormatLabel()
    {
        if(mFormatLabel == null)
        {
            mFormatLabel = new Label(" ");
        }

        return mFormatLabel;
    }

    private Label getProtocolLabel()
    {
        if(mProtocolLabel == null)
        {
            mProtocolLabel = new Label();
        }

        return mProtocolLabel;
    }

    private TextField getMinTalkgroupField()
    {
        if(mMinTalkgroupField == null)
        {
            mMinTalkgroupField = new TextField();
            mMinTalkgroupField.setTextFormatter(mMinIntegerTextFormatter);
        }

        return mMinTalkgroupField;
    }

    private TextField getMaxTalkgroupField()
    {
        if(mMaxTalkgroupField == null)
        {
            mMaxTalkgroupField = new TextField();
            mMaxTalkgroupField.setTextFormatter(mMinIntegerTextFormatter);
        }

        return mMaxTalkgroupField;
    }

    public class MinTalkgroupValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setMinTalkgroup(newValue != null ? newValue : 0);
                modifiedProperty().set(true);
            }
        }
    }

    public class MaxTalkgroupValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setMaxTalkgroup(newValue != null ? newValue : 0);
                modifiedProperty().set(true);
            }
        }
    }

    /**
     * Talkgroup detail formatting details for the specified protocol and format
     * @param protocol for the talkgroup
     * @param integerFormat for formatting the value
     * @return detail or null
     */
    private TalkgroupDetail getTalkgroupDetail(Protocol protocol, IntegerFormat integerFormat)
    {
        for(TalkgroupDetail detail: mTalkgroupDetails)
        {
            if(detail.getProtocol() == protocol && detail.getIntegerFormat() == integerFormat)
            {
                return detail;
            }
        }

        mLog.warn("Unable to find talkgroup range editor for protocol [" + protocol + "] and format [" + integerFormat +
                "] - using default editor");

        //Use a default instance
        for(TalkgroupDetail detail: mTalkgroupDetails)
        {
            if(detail.getProtocol() == Protocol.UNKNOWN && detail.getIntegerFormat() == integerFormat)
            {
                return detail;
            }
        }

        mLog.warn("No Talkgroup Detail is configured for protocol [" + protocol + "] and format [" + integerFormat + "]");
        return null;
    }

    /**
     * Creates a set of talkgroup details with integer text formatters
     */
    private List<TalkgroupDetail> createTalkgroupDetails()
    {
        List<TalkgroupDetail> details = new ArrayList<>();
        details.add(new TalkgroupDetail(Protocol.AM, IntegerFormat.DECIMAL, new IntegerFormatter(0,65535),
                new IntegerFormatter(0,65535), "Format 0 - 65535"));
        details.add(new TalkgroupDetail(Protocol.AM, IntegerFormat.HEXADECIMAL, new IntegerFormatter(0,65535),
                new IntegerFormatter(0,65535), "Format 0 - FFFF"));
        details.add(new TalkgroupDetail(Protocol.APCO25, IntegerFormat.DECIMAL, new IntegerFormatter(0,65535),
                new IntegerFormatter(0,65535), "Format: 0 - 65535"));
        details.add(new TalkgroupDetail(Protocol.APCO25, IntegerFormat.HEXADECIMAL, new HexFormatter(0,65535),
                new HexFormatter(0,65535), "Format: 0 - FFFF"));
        details.add(new TalkgroupDetail(Protocol.FLEETSYNC, IntegerFormat.FORMATTED,
                new PrefixIdentFormatter(0,0xFFFFF), new PrefixIdentFormatter(0,0xFFFFF),
                "Format: PPP-IIII = Prefix (0-127), Ident (0-8191)"));
        details.add(new TalkgroupDetail(Protocol.LTR, IntegerFormat.FORMATTED, new LtrFormatter(0,0x3FFF),
                new LtrFormatter(0,0x3FFF), "Format: A-HH-TTT = Area (0-1), Home (1-31), Talkgroup (1-255)"));
        details.add(new TalkgroupDetail(Protocol.MDC1200, IntegerFormat.DECIMAL, new IntegerFormatter(0,0xFFFF),
                new IntegerFormatter(0,0xFFFF), "Format: 0 - 65535"));
        details.add(new TalkgroupDetail(Protocol.MDC1200, IntegerFormat.HEXADECIMAL, new HexFormatter(0,0xFFFF),
                new HexFormatter(0,0xFFFF), "Format: 0 - FFFF"));
        details.add(new TalkgroupDetail(Protocol.MPT1327, IntegerFormat.FORMATTED,
                new PrefixIdentFormatter(0,0xFFFFF), new PrefixIdentFormatter(0,0xFFFFF),
                "Format: PPP-IIII = Prefix (0-127), Ident (1-8191)"));
        details.add(new TalkgroupDetail(Protocol.NBFM, IntegerFormat.DECIMAL, new IntegerFormatter(0,65535),
                new IntegerFormatter(0,65535), "Format 0 - 65535"));
        details.add(new TalkgroupDetail(Protocol.NBFM, IntegerFormat.HEXADECIMAL, new IntegerFormatter(0,65535),
                new IntegerFormatter(0,65535), "Format 0 - FFFF"));
        details.add(new TalkgroupDetail(Protocol.PASSPORT, IntegerFormat.DECIMAL, new IntegerFormatter(0,0xFFFF),
                new IntegerFormatter(0,0xFFFF), "Format: 0 - 65535"));
        details.add(new TalkgroupDetail(Protocol.PASSPORT, IntegerFormat.HEXADECIMAL, new HexFormatter(0,0xFFFF),
                new HexFormatter(0,0xFFFF), "Format: 0 - FFFF"));
        details.add(new TalkgroupDetail(Protocol.UNKNOWN, IntegerFormat.DECIMAL, new IntegerFormatter(0,16777215),
                new IntegerFormatter(0,16777215), "Format: 0 - 16777215"));
        details.add(new TalkgroupDetail(Protocol.UNKNOWN, IntegerFormat.FORMATTED, new IntegerFormatter(0,16777215),
                new IntegerFormatter(0,16777215), "Format: 0 - FFFFFF"));
        details.add(new TalkgroupDetail(Protocol.UNKNOWN, IntegerFormat.HEXADECIMAL, new HexFormatter(0,16777215),
                new HexFormatter(0,16777215), "Format: 0 - FFFFFF"));
        details.add(new TalkgroupDetail(Protocol.DMR, IntegerFormat.DECIMAL, new IntegerFormatter(1,16777215),
                new IntegerFormatter(1,16777215), "Format: 1 - 16777215"));
        details.add(new TalkgroupDetail(Protocol.DMR, IntegerFormat.HEXADECIMAL, new HexFormatter(1,0XFFFFFF),
                new HexFormatter(1, 0xFFFFFF), "Format: 0 - FFFFFF"));

        return details;
    }

    public class TalkgroupDetail
    {
        private Protocol mProtocol;
        private IntegerFormat mIntegerFormat;
        private TextFormatter<Integer> mMinTextFormatter;
        private TextFormatter<Integer> mMaxTextFormatter;
        private String mTooltip;

        public TalkgroupDetail(Protocol protocol, IntegerFormat integerFormat, TextFormatter<Integer> minTextFormatter,
                               TextFormatter<Integer> maxTextFormatter, String tooltip)
        {
            mProtocol = protocol;
            mIntegerFormat = integerFormat;
            mMinTextFormatter = minTextFormatter;
            mMaxTextFormatter = maxTextFormatter;
            mTooltip = tooltip;
        }

        public Protocol getProtocol()
        {
            return mProtocol;
        }

        public IntegerFormat getIntegerFormat()
        {
            return mIntegerFormat;
        }

        public TextFormatter<Integer> getMinTextFormatter()
        {
            return mMinTextFormatter;
        }

        public TextFormatter<Integer> getMaxTextFormatter()
        {
            return mMaxTextFormatter;
        }

        public String getTooltip()
        {
            return mTooltip;
        }
    }
}
