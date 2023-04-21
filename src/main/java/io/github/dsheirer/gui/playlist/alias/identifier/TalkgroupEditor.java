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

import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
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
 * Editor for talkgroup alias identifiers
 */
public class TalkgroupEditor extends IdentifierEditor<Talkgroup>
{
    private static final Logger mLog = LoggerFactory.getLogger(TalkgroupEditor.class);

    private UserPreferences mUserPreferences;
    private Label mProtocolLabel;
    private Label mFormatLabel;
    private TextField mTalkgroupField;
    private TextFormatter<Integer> mIntegerTextFormatter;
    private List<TalkgroupDetail> mTalkgroupDetails = new ArrayList<>();
    private TalkgroupValueChangeListener mTalkgroupValueChangeListener = new TalkgroupValueChangeListener();

    /**
     * Constructs an instance
     * @param userPreferences for determining user preferred talkgroup formats
     */
    public TalkgroupEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        loadTalkgroupDetails();

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(3);

        GridPane.setConstraints(getProtocolLabel(), 0, 0);
        gridPane.getChildren().add(getProtocolLabel());

        Label valueLabel = new Label("Talkgroup");
        GridPane.setHalignment(valueLabel, HPos.RIGHT);
        GridPane.setConstraints(valueLabel, 1, 0);
        gridPane.getChildren().add(valueLabel);

        GridPane.setConstraints(getTalkgroupField(), 2, 0);
        gridPane.getChildren().add(getTalkgroupField());

        GridPane.setConstraints(getFormatLabel(), 3, 0);
        gridPane.getChildren().add(getFormatLabel());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(Talkgroup item)
    {
        super.setItem(item);

        Talkgroup talkgroup = getItem();

        getProtocolLabel().setDisable(talkgroup == null);
        getTalkgroupField().setDisable(talkgroup == null);

        if(talkgroup != null)
        {
            getProtocolLabel().setText(talkgroup.getProtocol().toString());
            updateTextFormatter();
        }
        else
        {
            getTalkgroupField().setText(null);
        }

        modifiedProperty().set(false);
    }

    private void updateTextFormatter()
    {
        if(mIntegerTextFormatter != null)
        {
            mIntegerTextFormatter.valueProperty().removeListener(mTalkgroupValueChangeListener);
        }

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(getItem().getProtocol());

        if(format != null)
        {
            TalkgroupDetail talkgroupDetail = getTalkgroupDetail(getItem().getProtocol(), format);

            if(talkgroupDetail != null)
            {
                mIntegerTextFormatter = talkgroupDetail.getTextFormatter();
                Integer value = getItem() != null ? getItem().getValue() : null;
                mTalkgroupField.setTextFormatter(mIntegerTextFormatter);
                mTalkgroupField.setTooltip(new Tooltip(talkgroupDetail.getTooltip()));
                mIntegerTextFormatter.setValue(value);
                mIntegerTextFormatter.valueProperty().addListener(mTalkgroupValueChangeListener);
            }
            else
            {
                mLog.warn("Couldn't find talkgroup detail for protocol [" + getItem().getProtocol() +
                        "] and format [" + format + "]");
            }

            getFormatLabel().setText(talkgroupDetail.getTooltip());
        }
        else
        {
            getFormatLabel().setText(" ");
            mLog.warn("Integer format combobox does not have a selected value.");
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

    private TextField getTalkgroupField()
    {
        if(mTalkgroupField == null)
        {
            mTalkgroupField = new TextField();
            mTalkgroupField.setTextFormatter(mIntegerTextFormatter);
        }

        return mTalkgroupField;
    }

    public class TalkgroupValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setValue(newValue != null ? newValue : 0);
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

        mLog.warn("Unable to find talkgroup editor for protocol [" + protocol + "] and format [" + integerFormat +
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

    private void loadTalkgroupDetails()
    {
        mTalkgroupDetails.clear();
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.AM, IntegerFormat.DECIMAL, new IntegerFormatter(1,0xFFFF),
                "Format: 1 - 65535"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.AM, IntegerFormat.HEXADECIMAL, new HexFormatter(1,0xFFFF),
                "Format: 1 - FFFF"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.APCO25, IntegerFormat.DECIMAL, new IntegerFormatter(0,65535),
                "Format: 0 - 65535"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.APCO25, IntegerFormat.HEXADECIMAL, new HexFormatter(0,65535),
                "Format: 0 - FFFF"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.DMR, IntegerFormat.DECIMAL, new IntegerFormatter(1,0xFFFFFF),
                "Format: 1 - 16,777,215"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.DMR, IntegerFormat.HEXADECIMAL, new HexFormatter(1,0xFFFFFF),
                "Format: 1 - FFFFFF"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.FLEETSYNC, IntegerFormat.FORMATTED,
                new PrefixIdentFormatter(0,0xFFFFF), "Format: PPP-IIII = Prefix (0-127), Ident (0-8191)"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.LTR, IntegerFormat.FORMATTED, new LtrFormatter(0,0x3FFF),
                "Format: A-HH-TTT = Area (0-1), Home (1-31), Talkgroup (1-255)"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.MDC1200, IntegerFormat.DECIMAL, new IntegerFormatter(0,0xFFFF),
                "Format: 0 - 65535"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.MDC1200, IntegerFormat.HEXADECIMAL, new HexFormatter(0,0xFFFF),
                "Format: 0 - FFFF"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.MPT1327, IntegerFormat.FORMATTED,
                new PrefixIdentFormatter(0,0xFFFFF), "Format: PPP-IIII = Prefix (0-127), Ident (0-8191)"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.NBFM, IntegerFormat.DECIMAL, new IntegerFormatter(1,0xFFFF),
                "Format: 1 - 65535"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.NBFM, IntegerFormat.HEXADECIMAL, new HexFormatter(1,0xFFFF),
                "Format: 1 - FFFF"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.PASSPORT, IntegerFormat.DECIMAL, new IntegerFormatter(0,0xFFFF),
                "Format: 0 - 65535"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.PASSPORT, IntegerFormat.HEXADECIMAL, new HexFormatter(0,0xFFFF),
                "Format: 0 - FFFF"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.UNKNOWN, IntegerFormat.DECIMAL, new IntegerFormatter(0,16777215),
                "Format: 0 - FFFFFF"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.UNKNOWN, IntegerFormat.FORMATTED, new IntegerFormatter(0,16777215),
                "Format: 0 - FFFFFF"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.UNKNOWN, IntegerFormat.HEXADECIMAL, new HexFormatter(0,16777215),
                "Format: 0 - FFFFFF"));
    }

    public static class TalkgroupDetail
    {
        private Protocol mProtocol;
        private IntegerFormat mIntegerFormat;
        private TextFormatter<Integer> mTextFormatter;
        private String mTooltip;

        public TalkgroupDetail(Protocol protocol, IntegerFormat integerFormat, TextFormatter<Integer> textFormatter,
                               String tooltip)
        {
            mProtocol = protocol;
            mIntegerFormat = integerFormat;
            mTextFormatter = textFormatter;
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

        public TextFormatter<Integer> getTextFormatter()
        {
            return mTextFormatter;
        }

        public String getTooltip()
        {
            return mTooltip;
        }
    }
}
