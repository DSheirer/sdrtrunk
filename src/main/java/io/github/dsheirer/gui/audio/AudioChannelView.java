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

package io.github.dsheirer.gui.audio;

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.audio.playback.AudioChannel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.audio.playback.ReplaySource;
import io.github.dsheirer.audio.playback.TalkgroupHold;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Display for one audio playback channel (mono, or the left/right channel of a stereo output).  Shows the call that
 * is playing: the TO alias or identifier as the headline with the alias icon and color, the FROM radio, the system,
 * site and channel, the elapsed call time and an audio level meter, plus a per-channel mute.  When the call ends,
 * the last call stays on the display, dimmed, until the next call starts.
 */
public class AudioChannelView extends HBox
{
    private static final String NO_CALL = "No audio";
    private static final Color DEFAULT_ACCENT = Color.web("#3ddc84");
    private static final Color DISPLAY_BACKGROUND = Color.web("#161b22");

    private final AudioChannel mAudioChannel;
    private final AliasModel mAliasModel;
    private final IconModel mIconModel;
    private final TalkgroupFormatPreference mTalkgroupFormatPreference;
    private final Runnable mMuteChangeListener;
    private final Listener<AudioEvent> mAudioEventListener = this::receive;
    private final Listener<IdentifierCollection> mMetadataListener = this::receive;

    private final Region mAccentStrip = new Region();
    private final StackPane mIconPane = new StackPane();
    private final ImageView mAliasImage = new ImageView();
    private final IconNode mPlaceholderIcon = new IconNode(FontAwesome.HEADPHONES);
    private final Label mChannelTag = new Label();
    private final Label mStatusLabel = new Label();
    private final Label mElapsedLabel = new Label();
    private final ToggleButton mMuteButton = new ToggleButton();
    private final ToggleButton mHoldButton = new ToggleButton();
    private final Label mHoldLabel = new Label();
    private final AudioPlaybackManager mAudioPlaybackManager;
    private final int mChannelIndex;
    private final IconNode mMuteIcon = new IconNode(FontAwesome.VOLUME_UP);
    private final Label mToLabel = new Label(NO_CALL);
    private final Label mToDetailLabel = new Label();
    private final Label mFromLabel = new Label();
    private final Label mContextLabel = new Label();
    private final HBox mFromBox;
    private final LevelMeter mLevelMeter;

    private IdentifierCollection mIdentifierCollection;
    private Identifier mCallToIdentifier;
    private boolean mActive;
    private boolean mHasPlayed;
    private long mCallStartNanos;
    private long mCallEndNanos;

    /**
     * Constructs an instance
     * @param audioChannel to display
     * @param aliasModel for alias lookup
     * @param iconModel for alias icons
     * @param talkgroupFormatPreference for identifier formatting
     * @param updatesPerSecond rate that {@link #tick()} is invoked
     * @param muteChangeListener notified on the FX thread when this channel's mute state changes
     * @param audioPlaybackManager for setting the hold talkgroup, or null to hide the hold button
     * @param channelIndex of this channel in the audio output
     */
    public AudioChannelView(AudioChannel audioChannel, AliasModel aliasModel, IconModel iconModel,
                            TalkgroupFormatPreference talkgroupFormatPreference, double updatesPerSecond,
                            Runnable muteChangeListener, AudioPlaybackManager audioPlaybackManager, int channelIndex)
    {
        mAudioChannel = audioChannel;
        mAudioPlaybackManager = audioPlaybackManager;
        mChannelIndex = channelIndex;
        mAliasModel = aliasModel;
        mIconModel = iconModel;
        mTalkgroupFormatPreference = talkgroupFormatPreference;
        mMuteChangeListener = muteChangeListener;
        mLevelMeter = new LevelMeter(updatesPerSecond);

        getStyleClass().add("audio-channel");
        setFillHeight(true);
        setMinWidth(0);
        setPrefWidth(420);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);

        mAccentStrip.getStyleClass().add("audio-accent");
        mAccentStrip.setMinWidth(5);
        mAccentStrip.setPrefWidth(5);
        mAccentStrip.setMaxHeight(Double.MAX_VALUE);

        mAliasImage.setPreserveRatio(true);
        mAliasImage.setFitHeight(36);
        mAliasImage.setFitWidth(36);
        mAliasImage.setSmooth(true);
        mPlaceholderIcon.setIconSize(26);
        mPlaceholderIcon.getStyleClass().add("audio-placeholder-icon");
        mIconPane.getStyleClass().add("audio-icon-pane");
        mIconPane.setMinSize(52, 52);
        mIconPane.setPrefSize(52, 52);
        mIconPane.setMaxSize(52, 52);
        mIconPane.getChildren().addAll(mPlaceholderIcon, mAliasImage);
        VBox iconColumn = new VBox(mIconPane);
        iconColumn.setAlignment(Pos.CENTER);

        mChannelTag.setText(audioChannel.getChannelName());
        mChannelTag.getStyleClass().add("audio-channel-tag");
        mChannelTag.getStyleClass().add("RIGHT".equals(audioChannel.getChannelName()) ? "audio-channel-tag-right" :
            "audio-channel-tag-left");
        mStatusLabel.getStyleClass().add("audio-status");
        mElapsedLabel.getStyleClass().add("audio-elapsed");
        mMuteIcon.setIconSize(15);
        mMuteIcon.getStyleClass().add("audio-button-icon");
        mMuteButton.setGraphic(mMuteIcon);
        mMuteButton.getStyleClass().add("audio-icon-button");
        mMuteButton.setFocusTraversable(true);
        mMuteButton.setOnAction(event -> {
            mAudioChannel.setMuted(mMuteButton.isSelected());
            updateMuted();
        });
        IconNode holdIcon = new IconNode(FontAwesome.THUMB_TACK);
        holdIcon.setIconSize(15);
        holdIcon.getStyleClass().add("audio-button-icon");
        mHoldButton.setGraphic(holdIcon);
        mHoldButton.getStyleClass().addAll("audio-icon-button", "audio-hold-button");
        mHoldButton.setFocusTraversable(true);
        mHoldButton.setOnAction(event -> toggleHold());
        mHoldButton.setVisible(audioPlaybackManager != null);
        mHoldButton.setManaged(audioPlaybackManager != null);
        mHoldLabel.getStyleClass().add("audio-hold-label");
        mHoldLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        mHoldLabel.setMinWidth(0);
        mHoldLabel.setVisible(false);
        mHoldLabel.setManaged(false);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(8, mChannelTag, mStatusLabel, mHoldLabel, headerSpacer, mElapsedLabel, mHoldButton,
            mMuteButton);
        header.setAlignment(Pos.CENTER_LEFT);

        mToLabel.getStyleClass().add("audio-to");
        mToLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        mToLabel.setMinWidth(0);
        mToDetailLabel.getStyleClass().add("audio-to-detail");
        mToDetailLabel.setMinWidth(Region.USE_PREF_SIZE);
        HBox toBox = new HBox(10, mToLabel, mToDetailLabel);
        toBox.setAlignment(Pos.BASELINE_LEFT);

        IconNode fromIcon = new IconNode(FontAwesome.MICROPHONE);
        fromIcon.setIconSize(12);
        fromIcon.getStyleClass().add("audio-detail-icon");
        mFromLabel.getStyleClass().add("audio-detail");
        mFromLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        mFromLabel.setMinWidth(0);
        mFromLabel.setMaxWidth(280);
        mFromBox = new HBox(5, fromIcon, mFromLabel);
        mFromBox.setAlignment(Pos.CENTER_LEFT);
        //The FROM radio keeps its width and the system/site/channel text shrinks first
        mFromBox.setMinWidth(Region.USE_PREF_SIZE);
        mContextLabel.getStyleClass().add("audio-context");
        mContextLabel.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        mContextLabel.setMinWidth(0);
        HBox.setHgrow(mContextLabel, Priority.ALWAYS);
        mContextLabel.setMaxWidth(Double.MAX_VALUE);
        mContextLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox detailBox = new HBox(14, mFromBox, mContextLabel);
        detailBox.setAlignment(Pos.CENTER_LEFT);

        mLevelMeter.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(2, header, toBox, detailBox, mLevelMeter);
        content.getStyleClass().add("audio-channel-content");
        content.setMinWidth(0);
        VBox.setMargin(mLevelMeter, new javafx.geometry.Insets(6, 0, 0, 0));
        HBox.setHgrow(content, Priority.ALWAYS);

        getChildren().addAll(mAccentStrip, iconColumn, content);

        updateDisplay();
        updateMuted();

        mAudioChannel.addAudioEventListener(mAudioEventListener);
        mAudioChannel.setIdentifierCollectionListener(mMetadataListener);
        mAudioChannel.setHoldListener(hold -> Platform.runLater(this::updateHold));
        updateHold();
    }

    /**
     * Holds the talkgroup of the current (or last) call on this channel, or releases the hold.
     */
    private void toggleHold()
    {
        if(mAudioPlaybackManager == null)
        {
            return;
        }

        if(mAudioChannel.getHold() != null)
        {
            mAudioPlaybackManager.setHold(mChannelIndex, null);
        }
        else
        {
            TalkgroupHold hold = getCallHold();

            if(hold != null)
            {
                mAudioPlaybackManager.setHold(mChannelIndex, hold);
            }
        }

        updateHold();
    }

    /**
     * Hold for the TO talkgroup of the current or last call, labeled with the talkgroup and its alias, or null.
     */
    private TalkgroupHold getCallHold()
    {
        IdentifierCollection collection = mIdentifierCollection;
        TalkgroupHold hold = TalkgroupHold.from(collection, null);

        if(hold == null)
        {
            return null;
        }

        Identifier to = collection.getToIdentifier();
        AliasList aliasList = mAliasModel != null ? mAliasModel.getAliasList(collection) : null;
        List<Alias> aliases = getAliases(aliasList, to);
        String label = format(to);

        if(!aliases.isEmpty())
        {
            label = label + " " + Joiner.on(", ").skipNulls().join(aliases);
        }

        return new TalkgroupHold(hold.protocol(), hold.talkgroup(), label);
    }

    /**
     * Updates the hold button and label from the channel's hold talkgroup.  FX thread.
     */
    private void updateHold()
    {
        TalkgroupHold hold = mAudioChannel.getHold();
        boolean held = hold != null;
        mHoldButton.setSelected(held);
        mHoldButton.setDisable(!held && getCallHold() == null);
        mHoldButton.setTooltip(new Tooltip(held ? "Release hold on talkgroup " + hold :
            "Hold this talkgroup: only play calls to this talkgroup on the " + getChannelDescription()));
        mHoldButton.setAccessibleText(held ? "Release hold" : "Hold talkgroup");
        mHoldLabel.setText(held ? "HOLD " + hold : "");
        mHoldLabel.setVisible(held);
        mHoldLabel.setManaged(held);
        pseudoClassStateChanged(AudioView.HELD, held);
    }

    /**
     * Deregisters from the audio channel.
     */
    public void dispose()
    {
        mAudioChannel.removeAudioEventListener(mAudioEventListener);
        mAudioChannel.removeAudioMetadataListener();
        mAudioChannel.setHoldListener(null);
    }

    /**
     * Channel displayed by this view
     */
    public AudioChannel getAudioChannel()
    {
        return mAudioChannel;
    }

    /**
     * Periodic update of the level meter and elapsed time.  Invoke on the FX application thread.
     */
    public void tick()
    {
        mLevelMeter.update(mAudioChannel.getAndResetPeakLevel());

        if(mActive)
        {
            updateElapsed();
        }
    }

    /**
     * Refreshes the identifier text, for example when the identifier format preference changes.
     */
    public void refresh()
    {
        updateDisplay();
    }

    /**
     * Disables the level meter while the whole audio output (every channel) is muted.
     * @param outputMuted true when every channel is muted
     */
    public void setOutputMuted(boolean outputMuted)
    {
        mLevelMeter.setDisable(outputMuted);
    }

    /**
     * Updates the muted display after the channel or the whole output was (un)muted.
     */
    public void updateMuted()
    {
        boolean muted = mAudioChannel.isMuted();
        mMuteButton.setSelected(muted);
        mMuteIcon.setIconCode(muted ? FontAwesome.VOLUME_OFF : FontAwesome.VOLUME_UP);
        mMuteButton.setTooltip(new Tooltip(muted ? "Unmute " + getChannelDescription() : "Mute " +
            getChannelDescription()));
        mMuteButton.setAccessibleText(muted ? "Unmute" : "Mute");
        mLevelMeter.setDimmed(muted);
        pseudoClassStateChanged(AudioView.MUTED, muted);
        updateStatus();
    }

    private String getChannelDescription()
    {
        return switch(mAudioChannel.getChannelName())
        {
            case "LEFT" -> "left channel";
            case "RIGHT" -> "right channel";
            default -> "audio";
        };
    }

    /**
     * Audio channel events, received on the audio thread.
     */
    private void receive(AudioEvent audioEvent)
    {
        switch(audioEvent.getType())
        {
            case AUDIO_STOPPED -> Platform.runLater(() -> setIdentifierCollection(null));
            case REPLAY_STARTED, REPLAY_STOPPED -> Platform.runLater(this::updateReplay);
            case AUDIO_MUTED, AUDIO_UNMUTED -> Platform.runLater(() -> {
                updateMuted();
                mMuteChangeListener.run();
            });
            default -> {}
        }
    }

    /**
     * Audio metadata for the playing call, received on the audio thread.  Null when playback ends.
     */
    private void receive(IdentifierCollection identifierCollection)
    {
        Platform.runLater(() -> setIdentifierCollection(identifierCollection));
    }

    /**
     * Updates the call metadata.  FX thread only.
     */
    private void setIdentifierCollection(IdentifierCollection identifierCollection)
    {
        Identifier to = identifierCollection != null ? identifierCollection.getToIdentifier() : null;

        if(identifierCollection == null || to == null)
        {
            if(mActive)
            {
                mActive = false;
                mCallEndNanos = System.nanoTime();
                updateElapsed();
                mLevelMeter.reset();
            }

            //Keep the last call's identifiers so that the display shows the last call, dimmed.
            updateStatus();
            pseudoClassStateChanged(AudioView.ACTIVE, false);
            return;
        }

        if(!mActive || !to.equals(mCallToIdentifier))
        {
            mCallStartNanos = System.nanoTime();
        }

        mActive = true;
        mHasPlayed = true;
        mCallToIdentifier = to;
        mIdentifierCollection = identifierCollection;
        pseudoClassStateChanged(AudioView.ACTIVE, true);
        updateDisplay();
        updateElapsed();
        updateHold();
    }

    /**
     * Shows the recorded call that is replaying on this channel, or restores the call display when the replay ends.
     * FX thread.
     */
    private void updateReplay()
    {
        ReplaySource replay = mAudioChannel.getReplay();

        if(replay != null && !replay.isFinished())
        {
            mActive = false;
            mHasPlayed = true;
            mCallStartNanos = System.nanoTime();
            mToLabel.setText("Replay");
            mToDetailLabel.setText("");
            mFromBox.setVisible(false);
            mContextLabel.setText(replay.getDescription());
            setAlias(null);
            pseudoClassStateChanged(AudioView.ACTIVE, true);
            mStatusLabel.setText(mAudioChannel.isMuted() ? "MUTED" : "REPLAY");
        }
        else
        {
            pseudoClassStateChanged(AudioView.ACTIVE, false);
            updateDisplay();
        }
    }

    /**
     * Updates the status chip text.
     */
    private void updateStatus()
    {
        ReplaySource replay = mAudioChannel.getReplay();

        if(replay != null && !replay.isFinished())
        {
            mStatusLabel.setText(mAudioChannel.isMuted() ? "MUTED" : "REPLAY");
            mElapsedLabel.setVisible(false);
            return;
        }

        String status;

        if(mActive)
        {
            status = mAudioChannel.isMuted() ? "MUTED" : "PLAYING";
        }
        else if(mAudioChannel.isMuted())
        {
            status = "MUTED";
        }
        else
        {
            status = mHasPlayed ? "LAST CALL" : "IDLE";
        }

        mStatusLabel.setText(status);
        mElapsedLabel.setVisible(mHasPlayed);
    }

    private void updateElapsed()
    {
        long end = mActive ? System.nanoTime() : mCallEndNanos;
        long seconds = Math.max(0, (end - mCallStartNanos) / 1_000_000_000L);
        String text = seconds >= 3600 ? String.format("%d:%02d:%02d", seconds / 3600, (seconds / 60) % 60,
            seconds % 60) : String.format("%d:%02d", seconds / 60, seconds % 60);

        if(!text.equals(mElapsedLabel.getText()))
        {
            mElapsedLabel.setText(text);
        }
    }

    /**
     * Updates the identifiers, alias icon and accent color from the current identifier collection.
     */
    private void updateDisplay()
    {
        updateStatus();

        IdentifierCollection collection = mIdentifierCollection;

        if(collection == null)
        {
            mToLabel.setText(NO_CALL);
            mToDetailLabel.setText("");
            mFromBox.setVisible(false);
            mContextLabel.setText("");
            setAlias(null);
            return;
        }

        AliasList aliasList = mAliasModel != null ? mAliasModel.getAliasList(collection) : null;

        Identifier to = collection.getToIdentifier();
        List<Alias> toAliases = getAliases(aliasList, to);
        String toId = format(to);

        if(toAliases.isEmpty())
        {
            mToLabel.setText(toId != null ? toId : "Unknown");
            mToDetailLabel.setText("");
            setAlias(null);
        }
        else
        {
            mToLabel.setText(Joiner.on(", ").skipNulls().join(toAliases));
            mToDetailLabel.setText(toId != null ? toId : "");
            setAlias(toAliases.getFirst());
        }

        String from = getFromText(collection, aliasList);
        mFromLabel.setText(from != null ? from : "");
        mFromBox.setVisible(from != null);
        mContextLabel.setText(getContextText(collection));
    }

    /**
     * FROM radio alias and/or identifier text, or null when the call has no FROM identifier.
     */
    private String getFromText(IdentifierCollection collection, AliasList aliasList)
    {
        Identifier from = collection.getFromIdentifier();
        String fromId = format(from);
        String fromName = null;

        List<Alias> fromAliases = getAliases(aliasList, from);

        if(!fromAliases.isEmpty())
        {
            fromName = Joiner.on(", ").skipNulls().join(fromAliases);
        }
        else
        {
            List<Identifier> talkerAliases = collection.getIdentifiers(Form.TALKER_ALIAS);

            if(!talkerAliases.isEmpty())
            {
                Object value = talkerAliases.getFirst().getValue();
                fromName = value != null ? value.toString() : null;
            }
        }

        if(fromName != null && fromId != null)
        {
            return fromName + "  " + fromId;
        }

        return fromName != null ? fromName : fromId;
    }

    /**
     * System, site, channel name and protocol text.
     */
    private static String getContextText(IdentifierCollection collection)
    {
        List<String> parts = new ArrayList<>();
        add(parts, collection, Form.SYSTEM);
        add(parts, collection, Form.SITE);
        add(parts, collection, Form.CHANNEL_NAME);
        add(parts, collection, Form.DECODER_TYPE);
        return String.join("  ·  ", parts);
    }

    private static void add(List<String> parts, IdentifierCollection collection, Form form)
    {
        List<Identifier> identifiers = collection.getIdentifiers(IdentifierClass.CONFIGURATION, form);

        if(!identifiers.isEmpty())
        {
            String text = identifiers.getFirst().toString();

            if(text != null && !text.isBlank() && !parts.contains(text))
            {
                parts.add(text);
            }
        }
    }

    private static List<Alias> getAliases(AliasList aliasList, Identifier identifier)
    {
        if(aliasList == null || identifier == null)
        {
            return Collections.emptyList();
        }

        List<Alias> aliases = aliasList.getAliases(identifier);
        return aliases != null ? aliases : Collections.emptyList();
    }

    private String format(Identifier identifier)
    {
        if(identifier == null)
        {
            return null;
        }

        return mTalkgroupFormatPreference != null ? mTalkgroupFormatPreference.format(identifier) :
            identifier.toString();
    }

    /**
     * Applies the alias icon and color, or the defaults when the alias is null.
     */
    private void setAlias(Alias alias)
    {
        Icon icon = alias != null && alias.getIconName() != null && mIconModel != null ?
            mIconModel.getIcon(alias.getIconName()) : null;
        mAliasImage.setImage(icon != null ? icon.getFxImage() : null);
        mPlaceholderIcon.setVisible(mAliasImage.getImage() == null);

        Color accent = alias != null ? toReadable(alias.getColor()) : DEFAULT_ACCENT;
        String rgb = toWeb(accent);
        setStyle("-audio-accent: " + rgb + ";");
    }

    /**
     * Converts the alias RGB color to a color that stands out against the dark display background.
     */
    static Color toReadable(int rgb)
    {
        Color color = Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        double luminance = luminance(color);
        double background = luminance(DISPLAY_BACKGROUND);

        //WCAG contrast ratio of at least 3:1 for the accent against the display background
        int steps = 0;

        while((luminance + 0.05) / (background + 0.05) < 3.0 && steps++ < 10)
        {
            color = color.interpolate(Color.WHITE, 0.2);
            luminance = luminance(color);
        }

        return color;
    }

    private static double luminance(Color color)
    {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
    }

    private static double linear(double channel)
    {
        return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static String toWeb(Color color)
    {
        return String.format("#%02x%02x%02x", (int)Math.round(color.getRed() * 255),
            (int)Math.round(color.getGreen() * 255), (int)Math.round(color.getBlue() * 255));
    }
}
