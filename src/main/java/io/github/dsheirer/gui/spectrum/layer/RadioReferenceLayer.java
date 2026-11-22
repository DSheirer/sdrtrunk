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

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.radioreference.ViewRadioReferenceRequest;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferencePlaylistActions;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.service.radioreference.SiteRecord;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import io.github.dsheirer.gui.spectrum.ChannelCardEntry;
import io.github.dsheirer.gui.spectrum.ChannelMenus;
import io.github.dsheirer.gui.spectrum.ChannelMenus.State;
import io.github.dsheirer.gui.spectrum.ConfigurationRow;
import io.github.dsheirer.gui.spectrum.DisplayCursor;
import io.github.dsheirer.gui.spectrum.DisplaySection;
import io.github.dsheirer.gui.spectrum.RenderArea;
import io.github.dsheirer.gui.spectrum.SpectralDisplayLayer;
import io.github.dsheirer.gui.spectrum.SpectralDisplayModel;
import io.github.dsheirer.service.radioreference.DistanceFormat;
import io.github.dsheirer.service.radioreference.FrequencyCandidate;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex.RankedCandidate;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/**
 * Radio reference candidates layer.  Lists the radio reference frequencies near the frequency under the mouse,
 * nearest to the user's location first, in the Channels hover card (drawn by {@link ChannelCardLayer}) and the
 * Channels context menu.  A candidate that a playlist channel already decodes is listed as that channel.
 *
 * The candidates come from the radio reference frequency index, which needs a premium account and the user's location.
 * Looking up a frequency doesn't call the web service.
 */
public class RadioReferenceLayer extends SpectralDisplayLayer
{
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private static final long MIN_TOLERANCE_HZ = 3_000;
    private static final double TOLERANCE_PIXELS = 3.0;
    private static final int MAX_CANDIDATES = 6;

    private final RadioReferenceFrequencyIndex mIndex;
    private final RadioReferencePlaylistActions mActions;
    private final BooleanProperty mHoverCandidates = new SimpleBooleanProperty(true);
    private long mResultsFrequency = Long.MIN_VALUE;
    private long mResultsTolerance;
    private List<RankedCandidate> mResults = Collections.emptyList();
    private List<RankedCandidate> mCardCandidates;
    private List<ChannelCardEntry> mCardEntries = Collections.emptyList();
    private long mCardEntriesTime;

    /**
     * Constructs an instance.
     * @param index of the radio reference frequencies near the user, or null for a layer that shows nothing
     * @param actions to add candidates to the playlist or enable their channels, or null for no context menu items
     */
    public RadioReferenceLayer(RadioReferenceFrequencyIndex index, RadioReferencePlaylistActions actions)
    {
        mIndex = index;
        mActions = actions;
        mHoverCandidates.addListener((ob, ov, nv) -> requestRender());
    }

    /**
     * Shows the card of radio reference candidates when the mouse rests over the display
     */
    public BooleanProperty hoverCandidatesProperty()
    {
        return mHoverCandidates;
    }

    @Override
    protected void attached()
    {
        if(mIndex != null)
        {
            mIndex.start();
        }
    }

    @Override
    public String getConfigurationTitle()
    {
        return "Radio Reference";
    }

    @Override
    public List<ConfigurationRow> getConfigurationRows()
    {
        if(mIndex == null)
        {
            return Collections.emptyList();
        }

        ConfigurationRow hover = ConfigurationRow.toggle("Hover Candidates", mHoverCandidates);
        Tooltip.install(hover.control(), new Tooltip("When the mouse rests over a signal, show the Radio Reference " +
            "frequencies near it, nearest to your location first"));
        Label status = new Label();
        status.textProperty().bind(mIndex.statusProperty());
        status.setWrapText(true);
        status.setMaxWidth(220);
        return List.of(hover, new ConfigurationRow("Nearby", status));
    }

    @Override
    public String getContextMenuSection()
    {
        return ChannelMenus.SECTION;
    }

    /**
     * Channel entries for the candidates at the frequency, in the card's order.  A candidate that a playlist channel
     * already decodes is listed as that playlist channel, otherwise as a candidate to add to the playlist (and
     * start).  Both can be viewed in the playlist editor's Radio Reference tab (View in Radio Reference).  One entry per site, since the site's
     * frequencies share its channel.  Adding never duplicates a playlist channel.
     */
    @Override
    public List<MenuItem> getContextMenuItems(DisplaySection section, long frequency)
    {
        if(mIndex == null || mActions == null)
        {
            return Collections.emptyList();
        }

        //The card's candidates when the mouse hasn't moved since the card was shown
        List<RankedCandidate> candidates = frequency == mResultsFrequency ? mResults :
            mIndex.lookup(frequency, mResultsTolerance > 0 ? mResultsTolerance : MIN_TOLERANCE_HZ);
        List<MenuItem> items = new ArrayList<>();
        Set<String> offered = new HashSet<>();

        for(RankedCandidate ranked: candidates)
        {
            if(offered.size() >= MAX_CANDIDATES)
            {
                break;
            }

            FrequencyCandidate candidate = ranked.candidate();

            if(candidate.isSiteFrequency())
            {
                SiteRecord site = mIndex.getSite(candidate.systemId(), candidate.siteId());

                //A repeater site (LTR) has a channel per repeater, otherwise one channel for the site
                String key = site == null ? null : RadioReferencePlaylistActions.isRepeaterSite(site) ?
                    "repeater:" + candidate.frequencyHz() : "site:" + site.systemId() + ":" + site.siteId();

                if(site != null && offered.add(key))
                {
                    items.add(createSiteEntry(site, ranked));
                }
            }
            else if(offered.add("frequency:" + candidate.frequencyHz() + ":" + candidate.title() + ":" +
                candidate.source()))
            {
                items.add(createConventionalEntry(ranked, RadioReferencePlaylistActions.getDecoderType(candidate)));
            }
        }

        //Playlist channels first, then the candidates, each in the card's order
        items.sort(Comparator.comparing(item -> item.getUserData() == null));
        return items;
    }

    /**
     * Entry for a trunked system site, or for the candidate's repeater of a repeater site (LTR)
     */
    private Menu createSiteEntry(SiteRecord site, RankedCandidate ranked)
    {
        FrequencyCandidate candidate = ranked.candidate();
        boolean repeater = RadioReferencePlaylistActions.isRepeaterSite(site);
        ViewRadioReferenceRequest view = ViewRadioReferenceRequest.site(site.systemId(), site.siteId());
        String unsupported = RadioReferencePlaylistActions.getUnsupportedReason(site);
        Channel existing = unsupported != null ? null : repeater ? mActions.findChannel(List.of(candidate.frequencyHz()),
            RadioReferencePlaylistActions.getSiteDecoderTypes(site)) : mActions.findSiteChannel(site);

        if(existing != null)
        {
            return createPlaylistEntry(existing, view);
        }

        String title = site.systemName() + " \u00B7 Site " + site.siteNumber() +
            (site.description() != null ? " " + site.description() : "");

        if(repeater)
        {
            title += site.channels().stream().filter(channel -> channel.frequencyHz() == candidate.frequencyHz() &&
                channel.logicalChannelNumber() > 0).map(channel -> " LCN " + channel.logicalChannelNumber())
                .findFirst().orElse("");
        }

        Menu menu = ChannelMenus.createEntry(title, describe(ranked), State.CANDIDATE, null);

        if(unsupported != null)
        {
            menu.getItems().add(ChannelMenus.createNote(unsupported));
        }
        else if(repeater)
        {
            menu.getItems().addAll(
                ChannelMenus.createItem("Add to Playlist and Start", State.CANDIDATE,
                    () -> mActions.addRepeater(site, candidate.frequencyHz(), true, this::showError)),
                ChannelMenus.createItem("Add to Playlist", State.CANDIDATE,
                    () -> mActions.addRepeater(site, candidate.frequencyHz(), false, this::showError)));
        }
        else
        {
            //Start on the candidate frequency when it's a control or alternate control channel
            menu.getItems().addAll(
                ChannelMenus.createItem("Add to Playlist and Start", State.CANDIDATE,
                    () -> mActions.addSite(site, candidate.frequencyHz(), true, this::showError)),
                ChannelMenus.createItem("Add to Playlist", State.CANDIDATE,
                    () -> mActions.addSite(site, candidate.frequencyHz(), false, this::showError)));
        }

        addViewItem(menu, State.CANDIDATE, view);
        return menu;
    }

    /**
     * Entry for a conventional frequency
     */
    private Menu createConventionalEntry(RankedCandidate ranked, DecoderType decoderType)
    {
        FrequencyCandidate candidate = ranked.candidate();
        String sourceKey = mIndex.getSourceKey(candidate);
        ViewRadioReferenceRequest view = sourceKey != null ?
            ViewRadioReferenceRequest.frequency(sourceKey, candidate) : null;
        Channel existing = decoderType != null ? mActions.findConventionalChannel(candidate) : null;

        if(existing != null)
        {
            return createPlaylistEntry(existing, view);
        }

        Menu menu = ChannelMenus.createEntry(candidate.title() != null ? candidate.title() : "(untitled)",
            describe(ranked), State.CANDIDATE, null);

        if(decoderType == null)
        {
            menu.getItems().add(ChannelMenus.createNote("Mode isn't supported"));
        }
        else
        {
            menu.getItems().addAll(
                ChannelMenus.createItem("Add to Playlist and Start", State.CANDIDATE,
                    () -> mActions.addConventional(candidate, true, this::showError)),
                ChannelMenus.createItem("Add to Playlist", State.CANDIDATE,
                    () -> mActions.addConventional(candidate, false, this::showError)));
        }

        addViewItem(menu, State.CANDIDATE, view);
        return menu;
    }

    /**
     * Entry for the playlist channel that decodes a candidate: enable or disable it, view or edit it, and view the
     * candidate in the Radio Reference tab
     * @param view request for the candidate, or null when its listing isn't known
     */
    private Menu createPlaylistEntry(Channel channel, ViewRadioReferenceRequest view)
    {
        Runnable toggle = channel.isTrafficChannel() ? null : () -> {
            if(channel.isProcessing())
            {
                mActions.disable(channel, this::showError);
            }
            else
            {
                mActions.enable(channel, this::showError);
            }
        };
        Menu menu = ChannelMenus.createChannelEntry(channel, null, toggle);
        addViewItem(menu, channel.isProcessing() ? State.ENABLED : State.DISABLED, view);
        return menu;
    }

    /**
     * Adds the item that shows the candidate in the playlist editor's Radio Reference tab
     * @param request to show the candidate, or null when the candidate's listing isn't known
     */
    private static void addViewItem(Menu menu, State state, ViewRadioReferenceRequest request)
    {
        MenuItem view = ChannelMenus.createItem("View in Radio Reference", state,
            () -> MyEventBus.getGlobalEventBus().post(request));
        view.setDisable(request == null);
        menu.getItems().addAll(ChannelMenus.createSeparator(), view);
    }

    /**
     * Shows an error alert over the display.  Can be called from any thread.
     */
    private void showError(String message)
    {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setTitle("Radio Reference");
            alert.setHeaderText("Can't add or start the channel");

            if(getSpectralDisplay() != null && getSpectralDisplay().getScene() != null)
            {
                alert.initOwner(getSpectralDisplay().getScene().getWindow());
            }

            alert.show();
        });
    }

    /**
     * Card entries for the candidates at the mouse position, in the card's order.  A candidate that a playlist
     * channel already decodes is listed as that playlist channel.
     */
    @Override
    public List<ChannelCardEntry> getCardEntries(DisplayCursor cursor, RenderArea area)
    {
        SpectralDisplayModel model = getModel();

        if(mIndex == null || !mHoverCandidates.get() || model == null || area.width() <= 0)
        {
            return Collections.emptyList();
        }

        List<RankedCandidate> candidates = getCandidates(cursor.frequency(), getTolerance(model, area));

        //The playlist channels of the candidates, looked up once per lookup or second, since that copies the playlist
        if(candidates != mCardCandidates || System.currentTimeMillis() - mCardEntriesTime > 1000)
        {
            mCardCandidates = candidates;
            mCardEntriesTime = System.currentTimeMillis();
            mCardEntries = createCardEntries(candidates);
        }

        return mCardEntries;
    }

    private List<ChannelCardEntry> createCardEntries(List<RankedCandidate> candidates)
    {
        List<ChannelCardEntry> entries = new ArrayList<>();
        Set<String> listed = new HashSet<>();

        for(RankedCandidate ranked: candidates)
        {
            FrequencyCandidate candidate = ranked.candidate();
            Channel existing = mActions != null ? findChannel(candidate) : null;

            if(existing != null)
            {
                entries.add(ChannelMenus.createCardEntry(existing, null, true));
            }
            //The same listing in several places (e.g. a county and its agency) is shown once
            else if(listed.add(candidate.frequencyHz() + "|" + candidate.title() + "|" + candidate.source()))
            {
                List<String> details = new ArrayList<>();
                details.add(describe(ranked));
                String source = source(candidate);

                if(source != null)
                {
                    details.add(source);
                }

                String title = candidate.title() != null ? candidate.title() : "(untitled)";
                entries.add(new ChannelCardEntry(title, details, State.CANDIDATE, null, true));
            }
        }

        return entries;
    }

    /**
     * Playlist channel that decodes the candidate, or null
     */
    private Channel findChannel(FrequencyCandidate candidate)
    {
        if(candidate.isSiteFrequency())
        {
            SiteRecord site = mIndex.getSite(candidate.systemId(), candidate.siteId());

            if(site == null || RadioReferencePlaylistActions.getUnsupportedReason(site) != null)
            {
                return null;
            }

            return RadioReferencePlaylistActions.isRepeaterSite(site) ?
                mActions.findChannel(List.of(candidate.frequencyHz()),
                    RadioReferencePlaylistActions.getSiteDecoderTypes(site)) : mActions.findSiteChannel(site);
        }

        return RadioReferencePlaylistActions.getDecoderType(candidate) != null ?
            mActions.findConventionalChannel(candidate) : null;
    }

    /**
     * Frequency tolerance for the candidates: a few pixels or a DFT bin at the current zoom, but at least 3 kHz
     */
    private static long getTolerance(SpectralDisplayModel model, RenderArea area)
    {
        double hertzPerPixel = model.getDisplayBandwidth() / area.width();
        return Math.max(MIN_TOLERANCE_HZ, Math.round(Math.max(TOLERANCE_PIXELS * hertzPerPixel, model.getBinWidth())));
    }

    /**
     * Candidates for the frequency, looked up again only when the frequency or the tolerance changes
     */
    private List<RankedCandidate> getCandidates(long frequency, long tolerance)
    {
        if(frequency != mResultsFrequency || tolerance != mResultsTolerance)
        {
            mResults = mIndex.lookup(frequency, tolerance);
            mResultsFrequency = frequency;
            mResultsTolerance = tolerance;
        }

        return mResults;
    }

    /**
     * Frequency, mode or use, tone, encryption and distance of the candidate
     */
    private String describe(RankedCandidate ranked)
    {
        FrequencyCandidate candidate = ranked.candidate();
        return join(FREQUENCY_FORMAT.format(candidate.frequencyHz() / 1E6) + " MHz", candidate.use(), candidate.mode(),
            candidate.tone(), candidate.encrypted() ? "Encrypted" : null,
            ranked.hasDistance() ? DistanceFormat.format(ranked.distanceKilometers()) : null);
    }

    /**
     * Where the candidate is listed, with its alpha tag when the title isn't the alpha tag
     */
    private static String source(FrequencyCandidate candidate)
    {
        String alphaTag = candidate.alphaTag() != null && !candidate.alphaTag().equals(candidate.title()) ?
            candidate.alphaTag() : null;
        return join(alphaTag, candidate.source());
    }

    private static String join(String... values)
    {
        StringBuilder sb = new StringBuilder();

        for(String value: values)
        {
            if(value != null && !value.isEmpty())
            {
                if(!sb.isEmpty())
                {
                    sb.append(" · ");
                }

                sb.append(value);
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }
}
