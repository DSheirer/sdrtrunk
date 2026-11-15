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

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.event.DecodeEventHistory;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.filter.DecodeEventFilterSet;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;

/**
 * Table of the decode events for the selected channel.
 */
public class DecodeEventView extends HistoryTableView<IDecodeEvent,IDecodeEvent> implements Listener<ProcessingChain>
{
    private static final String TABLE_KEY = "now.playing.events";
    private static final String SWING_TABLE_KEY = "decode.event.panel";
    private final TimestampFormatter mTimestampFormatter;
    private final FilterSet<IDecodeEvent> mFilterSet = new DecodeEventFilterSet();
    private DecodeEventHistory mCurrentEventHistory;
    private Listener<IDecodeEvent> mCurrentEventListener;

    /**
     * Constructs an instance.  Construct on the JavaFX application thread.
     * @param iconModel for alias icons
     * @param userPreferences for display formats and column settings
     * @param aliasModel for aliasing identifiers
     */
    public DecodeEventView(IconModel iconModel, UserPreferences userPreferences, AliasModel aliasModel)
    {
        this(iconModel, userPreferences, aliasModel, new TimestampFormatter(userPreferences));
    }

    private DecodeEventView(IconModel iconModel, UserPreferences userPreferences, AliasModel aliasModel,
                            TimestampFormatter timestampFormatter)
    {
        super(createColumns(iconModel, userPreferences, aliasModel, timestampFormatter), TABLE_KEY,
            userPreferences.getSwingPreference(), SWING_TABLE_KEY, "Event Filter Editor", "Event Columns");
        mTimestampFormatter = timestampFormatter;
        getHistoryControls().setFilterSet(mFilterSet);
        //Filter changes come from the Swing filter editor
        mFilterSet.register(() -> Platform.runLater(this::refilter));
        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Prepares this view for disposal
     */
    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
        receive(null);
    }

    /**
     * Receives preference update notifications via the event bus
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.DECODE_EVENT || preferenceType == PreferenceType.TALKGROUP_FORMAT ||
            preferenceType == PreferenceType.APPLICATION)
        {
            Platform.runLater(this::refreshAll);
        }
    }

    @Override
    protected boolean include(IDecodeEvent event)
    {
        return mFilterSet.canProcess(event) && mFilterSet.passes(event);
    }

    /**
     * Shows the decode events for the processing chain.  Call on the JavaFX application thread.
     * @param processingChain to show, or null to clear the view
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        if(mCurrentEventHistory != null)
        {
            mCurrentEventHistory.removeListener(mCurrentEventListener);
        }

        mCurrentEventHistory = null;
        mCurrentEventListener = null;

        if(processingChain != null)
        {
            DecodeEventHistory history = processingChain.getDecodeEventHistory();
            //A listener for each history so that events from the previous channel are ignored after a change
            Listener<IDecodeEvent> listener = new Listener<>()
            {
                @Override
                public void receive(IDecodeEvent event)
                {
                    Platform.runLater(() -> {
                        if(mCurrentEventListener == this)
                        {
                            add(event);
                        }
                    });
                }
            };
            mCurrentEventHistory = history;
            mCurrentEventListener = listener;
            setItems(history.getItems());
            history.addListener(listener);
            getHistoryControls().setControlsEnabled(true);
        }
        else
        {
            clear();
            getHistoryControls().setControlsEnabled(false);
        }
    }

    @Override
    protected void refreshAll()
    {
        mTimestampFormatter.update();
        super.refreshAll();
    }

    private static List<TableColumn<Row<IDecodeEvent>,?>> createColumns(IconModel iconModel,
                                                                        UserPreferences userPreferences,
                                                                        AliasModel aliasModel,
                                                                        TimestampFormatter timestampFormatter)
    {
        List<TableColumn<Row<IDecodeEvent>,?>> columns = new ArrayList<>();

        TableColumn<Row<IDecodeEvent>,Long> time = NowPlayingCells.column("time", "Time", null, Pos.CENTER,
            row -> row.getItem().getTimeStart(), Row::revisionProperty);
        time.setCellFactory(NowPlayingCells.formatted(timestampFormatter::format));
        columns.add(time);

        TableColumn<Row<IDecodeEvent>,Long> duration = NowPlayingCells.column("duration", "Duration", null,
            Pos.CENTER, row -> row.getItem().getDuration(), Row::revisionProperty);
        DecimalFormat durationFormat = new DecimalFormat("0.0");
        duration.setCellFactory(NowPlayingCells.formatted(value -> value > 0 ? durationFormat.format(value / 1e3d) : null));
        columns.add(duration);

        columns.add(NowPlayingCells.column("event", "Event", null, Pos.CENTER_LEFT,
            row -> row.getItem().getEventType() != null ? row.getItem().getEventType().getLabel() : null,
            Row::revisionProperty));

        columns.add(NowPlayingCells.column("from", "From", null, Pos.CENTER,
            row -> formatIdentifiers(row.getItem().getIdentifierCollection(), Role.FROM, userPreferences),
            Row::revisionProperty));

        TableColumn<Row<IDecodeEvent>,AliasDisplay> fromAlias = NowPlayingCells.column("from.alias", "Alias",
            "From Alias", Pos.CENTER, row -> getAliases(row.getItem().getIdentifierCollection(), Role.FROM, aliasModel),
            Row::revisionProperty);
        fromAlias.setCellFactory(NowPlayingCells.alias(iconModel));
        columns.add(fromAlias);

        columns.add(NowPlayingCells.column("to", "To", null, Pos.CENTER,
            row -> formatIdentifiers(row.getItem().getIdentifierCollection(), Role.TO, userPreferences),
            Row::revisionProperty));

        TableColumn<Row<IDecodeEvent>,AliasDisplay> toAlias = NowPlayingCells.column("to.alias", "Alias",
            "To Alias", Pos.CENTER, row -> getAliases(row.getItem().getIdentifierCollection(), Role.TO, aliasModel),
            Row::revisionProperty);
        toAlias.setCellFactory(NowPlayingCells.alias(iconModel));
        columns.add(toAlias);

        columns.add(NowPlayingCells.column("channel", "Channel", null, Pos.CENTER,
            row -> getChannel(row.getItem()), Row::revisionProperty));

        TableColumn<Row<IDecodeEvent>,Long> frequency = NowPlayingCells.column("frequency", "Frequency", null,
            Pos.CENTER, row -> {
                IChannelDescriptor descriptor = row.getItem().getChannelDescriptor();
                return descriptor != null && descriptor.getDownlinkFrequency() > 0 ?
                    descriptor.getDownlinkFrequency() : null;
            }, Row::revisionProperty);
        DecimalFormat frequencyFormat = new DecimalFormat("0.00000");
        frequency.setCellFactory(NowPlayingCells.formatted(value -> frequencyFormat.format(value / 1e6d)));
        columns.add(frequency);

        columns.add(NowPlayingCells.column("details", "Details", null, Pos.CENTER_LEFT,
            row -> row.getItem().getDetails(), Row::revisionProperty));

        return columns;
    }

    /**
     * Event timestamp formatter that uses the timestamp format preference.
     */
    private static class TimestampFormatter
    {
        private final UserPreferences mUserPreferences;
        private SimpleDateFormat mFormat;

        TimestampFormatter(UserPreferences userPreferences)
        {
            mUserPreferences = userPreferences;
            update();
        }

        /**
         * Updates the format from the preference
         */
        void update()
        {
            mFormat = mUserPreferences.getDecodeEventPreference().getTimestampFormat().getFormatter();
        }

        String format(long timestamp)
        {
            return mFormat.format(new Date(timestamp));
        }
    }

    /**
     * Channel descriptor and timeslot for the event
     */
    private static String getChannel(IDecodeEvent event)
    {
        IChannelDescriptor channelDescriptor = event.getChannelDescriptor();
        boolean timeslot = event.hasTimeslot() && !event.toString().contains("TS");

        if(channelDescriptor != null)
        {
            return timeslot ? channelDescriptor + " TS" + event.getTimeslot() : channelDescriptor.toString();
        }

        return timeslot ? "TS" + event.getTimeslot() : null;
    }

    /**
     * Formats the identifiers for the role as a comma separated list
     */
    private static String formatIdentifiers(IdentifierCollection identifierCollection, Role role,
                                            UserPreferences userPreferences)
    {
        if(identifierCollection == null)
        {
            return null;
        }

        List<Identifier> identifiers = identifierCollection.getIdentifiers(role);

        if(identifiers == null || identifiers.isEmpty())
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for(Identifier identifier: identifiers)
        {
            if(!sb.isEmpty())
            {
                sb.append(",");
            }

            if(identifier.getForm() == Form.TALKGROUP || identifier.getForm() == Form.RADIO ||
                identifier.getForm() == Form.PATCH_GROUP)
            {
                sb.append(userPreferences.getTalkgroupFormatPreference().format(identifier));
            }
            else
            {
                sb.append(identifier);
            }
        }

        return sb.toString();
    }

    /**
     * Aliases for the identifiers with the role.  The display is styled by the first alias of the last aliased
     * identifier, the same as the Swing version of the table.
     */
    private static AliasDisplay getAliases(IdentifierCollection identifierCollection, Role role, AliasModel aliasModel)
    {
        if(identifierCollection == null)
        {
            return null;
        }

        List<Identifier> identifiers = identifierCollection.getIdentifiers(role);

        if(identifiers == null || identifiers.isEmpty())
        {
            return null;
        }

        AliasList aliasList = aliasModel.getAliasList(identifierCollection);

        if(aliasList == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        Alias styleAlias = null;

        for(Identifier identifier: identifiers)
        {
            List<Alias> aliases = aliasList.getAliases(identifier);

            if(!aliases.isEmpty())
            {
                if(!sb.isEmpty())
                {
                    sb.append(",");
                }

                sb.append(Joiner.on(", ").skipNulls().join(aliases));
                styleAlias = aliases.getFirst();
            }
        }

        return styleAlias != null ? AliasDisplay.of(sb.toString(), styleAlias) : null;
    }
}
