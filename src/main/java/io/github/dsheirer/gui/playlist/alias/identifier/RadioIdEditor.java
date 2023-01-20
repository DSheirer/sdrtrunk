/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.protocol.Protocol;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Editor for talkgroup alias identifiers
 */
public class RadioIdEditor extends IdentifierEditor<Radio>
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioIdEditor.class);

    private UserPreferences mUserPreferences;
    private Label mProtocolLabel;
    private Label mFormatLabel;
    private TextField mRadioIdField;
    private TextFormatter<Integer> mIntegerTextFormatter;
    private List<RadioDetail> mRadioDetails = new ArrayList<>();
    private RadioValueChangeListener mRadioValueChangeListener = new RadioValueChangeListener();

    /**
     * Constructs an instance
     * @param userPreferences for determining user preferred talkgroup formats
     */
    public RadioIdEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        loadRadioDetails();

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(3);

        GridPane.setConstraints(getProtocolLabel(), 0, 0);
        gridPane.getChildren().add(getProtocolLabel());

        Label valueLabel = new Label("Radio ID");
        GridPane.setHalignment(valueLabel, HPos.RIGHT);
        GridPane.setConstraints(valueLabel, 1, 0);
        gridPane.getChildren().add(valueLabel);

        GridPane.setConstraints(getRadioIdField(), 2, 0);
        gridPane.getChildren().add(getRadioIdField());

        GridPane.setConstraints(getFormatLabel(), 3, 0);
        gridPane.getChildren().add(getFormatLabel());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(Radio item)
    {
        super.setItem(item);

        Radio radioId = getItem();

        getProtocolLabel().setDisable(radioId == null);
        getRadioIdField().setDisable(radioId == null);

        if(radioId != null)
        {
            getProtocolLabel().setText(radioId.getProtocol().toString());
            updateTextFormatter();
        }
        else
        {
            getRadioIdField().setText(null);
        }

        modifiedProperty().set(false);
    }

    private void updateTextFormatter()
    {
        if(mIntegerTextFormatter != null)
        {
            mIntegerTextFormatter.valueProperty().removeListener(mRadioValueChangeListener);
        }

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(getItem().getProtocol());

        if(format != null)
        {
            RadioDetail radioDetail = getRadioDetail(getItem().getProtocol(), format);

            if(radioDetail != null)
            {
                mIntegerTextFormatter = radioDetail.getTextFormatter();
                Integer value = getItem() != null ? getItem().getValue() : null;
                mRadioIdField.setTextFormatter(mIntegerTextFormatter);
                mRadioIdField.setTooltip(new Tooltip(radioDetail.getTooltip()));
                mIntegerTextFormatter.setValue(value);
                mIntegerTextFormatter.valueProperty().addListener(mRadioValueChangeListener);
            }
            else
            {
                mLog.warn("Couldn't find radio detail for protocol [" + getItem().getProtocol() +
                    "] and format [" + format + "]");
            }

            getFormatLabel().setText(radioDetail.getTooltip());
        }
        else
        {
            getFormatLabel().setText(" ");
            mLog.warn("Integer format combo box does not have a selected value.");
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

    private TextField getRadioIdField()
    {
        if(mRadioIdField == null)
        {
            mRadioIdField = new TextField();
            mRadioIdField.setTextFormatter(mIntegerTextFormatter);
        }

        return mRadioIdField;
    }

    public class RadioValueChangeListener implements ChangeListener<Integer>
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
     * Radio detail formatting details for the specified protocol and format
     * @param protocol for the radio ID
     * @param integerFormat for formatting the value
     * @return detail or null
     */
    private RadioDetail getRadioDetail(Protocol protocol, IntegerFormat integerFormat)
    {
        for(RadioDetail detail: mRadioDetails)
        {
            if(detail.getProtocol() == protocol && detail.getIntegerFormat() == integerFormat)
            {
                return detail;
            }
        }

        mLog.warn("Unable to find radio id editor for protocol [" + protocol + "] and format [" + integerFormat +
                "] - using default editor");

        //Use a default instance
        for(RadioDetail detail: mRadioDetails)
        {
            if(detail.getProtocol() == Protocol.UNKNOWN && detail.getIntegerFormat() == integerFormat)
            {
                return detail;
            }
        }

        mLog.warn("No Radio Detail is configured for protocol [" + protocol + "] and format [" + integerFormat + "]");
        return null;
    }

    private void loadRadioDetails()
    {
        mRadioDetails.clear();
        mRadioDetails.add(new RadioDetail(Protocol.APCO25, IntegerFormat.DECIMAL, new IntegerFormatter(0,0xFFFFFF),
            "Format: 0 - 16777215"));
        mRadioDetails.add(new RadioDetail(Protocol.APCO25, IntegerFormat.HEXADECIMAL, new HexFormatter(0,0xFFFFFF),
            "Format: 0 - FFFFFF"));
        mRadioDetails.add(new RadioDetail(Protocol.DMR, IntegerFormat.DECIMAL, new IntegerFormatter(0,0xFFFFFF),
            "Format: 0 - 16777215"));
        mRadioDetails.add(new RadioDetail(Protocol.DMR, IntegerFormat.HEXADECIMAL, new HexFormatter(0,0xFFFFFF),
            "Format: 0 - FFFFFF"));
        mRadioDetails.add(new RadioDetail(Protocol.PASSPORT, IntegerFormat.DECIMAL, new IntegerFormatter(0,0x7FFFFF),
            "Format: 0 - 8388607"));
        mRadioDetails.add(new RadioDetail(Protocol.PASSPORT, IntegerFormat.HEXADECIMAL, new HexFormatter(0,0x7FFFFF),
            "Format: 0 - 7FFFFF"));
        mRadioDetails.add(new RadioDetail(Protocol.UNKNOWN, IntegerFormat.DECIMAL, new IntegerFormatter(0,16777215),
            "Format: 0 - FFFFFF"));
        mRadioDetails.add(new RadioDetail(Protocol.UNKNOWN, IntegerFormat.FORMATTED, new IntegerFormatter(0,16777215),
            "Format: 0 - FFFFFF"));
        mRadioDetails.add(new RadioDetail(Protocol.UNKNOWN, IntegerFormat.HEXADECIMAL, new HexFormatter(0,16777215),
            "Format: 0 - FFFFFF"));
    }

    public class RadioDetail
    {
        private Protocol mProtocol;
        private IntegerFormat mIntegerFormat;
        private TextFormatter<Integer> mTextFormatter;
        private String mTooltip;

        public RadioDetail(Protocol protocol, IntegerFormat integerFormat, TextFormatter<Integer> textFormatter,
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
