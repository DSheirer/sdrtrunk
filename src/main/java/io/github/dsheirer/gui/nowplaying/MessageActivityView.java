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
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageHistory;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.event.MessageItem;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.sample.Listener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;

/**
 * Table of the decoded messages for the selected channel.
 */
public class MessageActivityView extends HistoryTableView<MessageItem,IMessage> implements Listener<ProcessingChain>
{
    private static final String TABLE_KEY = "now.playing.messages";
    private static final String SWING_TABLE_KEY = "message.activity.panel";
    private MessageHistory mCurrentMessageHistory;
    private Listener<IMessage> mCurrentMessageListener;
    private FilterSet<IMessage> mMessageFilterSet;

    /**
     * Constructs an instance.  Construct on the JavaFX application thread.
     * @param settings for the column settings
     */
    public MessageActivityView(Settings settings)
    {
        super(createColumns(), TABLE_KEY, settings.getSwingSettings(), SWING_TABLE_KEY,
            "Message Filter Editor", "Message Columns");
    }

    @Override
    protected boolean include(MessageItem item)
    {
        FilterSet<IMessage> filterSet = mMessageFilterSet;

        if(filterSet != null && item.getMessage() != null)
        {
            return filterSet.canProcess(item.getMessage()) && filterSet.passes(item.getMessage());
        }

        return false;
    }

    /**
     * Shows the messages for the processing chain.  Call on the JavaFX application thread.
     * @param processingChain to show, or null to clear the view
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        if(mCurrentMessageHistory != null)
        {
            mCurrentMessageHistory.removeListener(mCurrentMessageListener);
        }

        //Unregister from changes made to the filter set
        if(mMessageFilterSet != null)
        {
            mMessageFilterSet.register(null);
        }

        mCurrentMessageHistory = null;
        mCurrentMessageListener = null;

        if(processingChain != null)
        {
            MessageHistory history = processingChain.getMessageHistory();
            FilterSet<IMessage> filterSet = DecoderFactory.getMessageFilters(processingChain.getModules());
            mMessageFilterSet = filterSet;
            //Filter changes come from the filter editor
            filterSet.register(() -> Platform.runLater(() -> {
                if(mMessageFilterSet == filterSet)
                {
                    refilter();
                }
            }));
            getHistoryControls().setFilterSet(filterSet);

            //A listener for each history so that messages from the previous channel are ignored after a change
            Listener<IMessage> listener = new Listener<>()
            {
                @Override
                public void receive(IMessage message)
                {
                    //Don't process tail bits or stuff bits message fragments
                    if(!(message instanceof StuffBitsMessage))
                    {
                        MessageItem item = new MessageItem(message);

                        Platform.runLater(() -> {
                            if(mCurrentMessageListener == this)
                            {
                                add(item);
                            }
                        });
                    }
                }
            };

            mCurrentMessageHistory = history;
            mCurrentMessageListener = listener;

            List<MessageItem> items = new ArrayList<>();

            for(IMessage message: history.getItems())
            {
                items.add(new MessageItem(message));
            }

            setItems(items);
            refilter();
            history.addListener(listener);
            getHistoryControls().setControlsEnabled(true);
        }
        else
        {
            mMessageFilterSet = null;
            clear();
            getHistoryControls().setControlsEnabled(false);
        }
    }

    private static List<TableColumn<Row<MessageItem>,?>> createColumns()
    {
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        List<TableColumn<Row<MessageItem>,?>> columns = new ArrayList<>();
        columns.add(NowPlayingCells.column("time", "Time", null, Pos.CENTER_LEFT,
            row -> row.getItem().getTimestamp(timeFormat), null));
        columns.add(NowPlayingCells.column("protocol", "Protocol", null, Pos.CENTER_LEFT,
            row -> row.getItem().getProtocol(), null));
        columns.add(NowPlayingCells.column("timeslot", "Timeslot", null, Pos.CENTER_LEFT,
            row -> row.getItem().getTimeslot(), null));
        columns.add(NowPlayingCells.column("message", "Message", null, Pos.CENTER_LEFT,
            row -> row.getItem().getText(), null));
        return columns;
    }
}
