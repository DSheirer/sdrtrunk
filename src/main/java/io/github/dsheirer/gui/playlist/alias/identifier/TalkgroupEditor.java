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

import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.gui.control.LtrFormatter;
import io.github.dsheirer.gui.control.PrefixIdentFormatter;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.protocol.Protocol;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TalkgroupEditor extends IdentifierEditor<Talkgroup>
{
    private static final Logger mLog = LoggerFactory.getLogger(TalkgroupEditor.class);

    private Label mProtocolLabel;
    private Label mFormatLabel;
    private TextField mTalkgroupField;
    private TextFormatter<Integer> mIntegerTextFormatter;
    private ComboBox<IntegerFormat> mFormatComboBox;
    private List<TalkgroupDetail> mTalkgroupDetails = new ArrayList<>();
    private TalkgroupValueChangeListener mTalkgroupValueChangeListener = new TalkgroupValueChangeListener();

    public TalkgroupEditor()
    {
        loadTalkgroupDetails();

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(3);

        GridPane.setConstraints(getProtocolLabel(), 0, 0);
        gridPane.getChildren().add(getProtocolLabel());

        Label valueLabel = new Label("Talkgroup");
        GridPane.setHalignment(valueLabel, HPos.RIGHT);
        GridPane.setConstraints(valueLabel, 1, 0);
        gridPane.getChildren().add(valueLabel);

        GridPane.setConstraints(getTalkgroupField(), 2, 0);
        gridPane.getChildren().add(getTalkgroupField());

        GridPane.setConstraints(getFormatComboBox(), 3, 0);
        gridPane.getChildren().add(getFormatComboBox());

        GridPane.setConstraints(getFormatLabel(), 1, 1, 3, 1);
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

            List<IntegerFormat> formats = getFormats(talkgroup.getProtocol());
            getFormatComboBox().getItems().clear();
            getFormatComboBox().getItems().addAll(formats);
            if(formats.size() == 1)
            {
                getFormatComboBox().setVisible(false);
            }
            else
            {
                getFormatComboBox().setVisible(true);
            }

            //TODO: select the preferred format from the pref service here
            getFormatComboBox().getSelectionModel().select(0);

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

        IntegerFormat format = getFormatComboBox().getSelectionModel().getSelectedItem();

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

            //Force the text field to commit the text value to the formatter with each key press
//            mTalkgroupField.textProperty()
//                .addListener((observable, oldValue, newValue) -> mTalkgroupField.commitValue());
        }

        return mTalkgroupField;
    }

    private ComboBox<IntegerFormat> getFormatComboBox()
    {
        if(mFormatComboBox == null)
        {
            mFormatComboBox = new ComboBox<>();
            mFormatComboBox.setVisible(false);
            mFormatComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateTextFormatter());
        }

        return mFormatComboBox;
    }

    public class TalkgroupValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                mLog.debug("Updating talkgroup value to: " + newValue);
                getItem().setValue(newValue != null ? newValue : 0);
            }
        }
    }

    /**
     * Retrieves a list of integer formats for the specified protocol
     * @param protocol to search for
     * @return list of formats or the DECIMAL format at a minimum.
     */
    public List<IntegerFormat> getFormats(Protocol protocol)
    {
        List<IntegerFormat> formats = new ArrayList<>();

        for(TalkgroupDetail talkgroupDetail: mTalkgroupDetails)
        {
            if(talkgroupDetail.getProtocol() == protocol)
            {
                formats.add(talkgroupDetail.getIntegerFormat());
            }
        }

        if(formats.isEmpty())
        {
            formats.add(IntegerFormat.DECIMAL);
        }

        Collections.sort(formats);
        return formats;
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
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.APCO25, IntegerFormat.DECIMAL, new IntegerFormatter(0,65535),
            "Format: 0 - 65535"));
        mTalkgroupDetails.add(new TalkgroupDetail(Protocol.APCO25, IntegerFormat.HEXADECIMAL, new HexFormatter(0,65535),
            "Format: 0 - FFFF"));
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

    public class TalkgroupDetail
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
