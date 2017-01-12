/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package module.decode.state;

import controller.channel.Channel;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEvent.Event;
import controller.channel.ChannelEventListener;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;
import controller.channel.ChannelUtils;
import icon.IconManager;
import module.ProcessingChain;
import module.decode.DecoderFactory;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.RecorderType;
import record.config.RecordConfiguration;
import sample.Listener;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChannelPanel extends JPanel
    implements Listener<ChangedAttribute>,
    SettingChangeListener,
    ChannelEventListener
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(ChannelPanel.class);

    private Font mFontDetails = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private Font mFontDecoder = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    protected Color mColorChannelBackground;
    protected Color mColorChannelSelected;
    protected Color mColorTopCall;
    protected Color mColorMiddleCall;
    protected Color mColorTopControl;
    protected Color mColorMiddleControl;
    protected Color mColorTopData;
    protected Color mColorMiddleData;
    protected Color mColorTopFade;
    protected Color mColorMiddleFade;
    protected Color mColorTopIdle;
    protected Color mColorMiddleIdle;
    protected Color mColorTopNoTuner;
    protected Color mColorMiddleNoTuner;
    protected Color mColorLabelDetails;
    protected Color mColorLabelDecoder;
    protected Color mColorLabelAuxDecoder;

    private JLabel mStateLabel;
    private JLabel mSourceLabel;
    private JLabel mOptionsLabel;

    private JLabel mSystemLabel;
    private JLabel mSiteLabel;
    private JLabel mChannelLabel;

    protected SettingsManager mSettingsManager;
    protected IconManager mIconManager;

    private Channel mChannel;
    private ChannelState mChannelState;
    private List<DecoderPanel> mDecoderPanels = new ArrayList<>();
    private ChannelModel mChannelModel;
    private ChannelProcessingManager mChannelProcessingManager;

    /**
     * Gui component to a channel state.  Provides System, Site and channel
     * name, source and channel state visualization.
     *
     * Updates color background according to current channel state.
     */
    public ChannelPanel(ChannelModel channelModel,
                        ChannelProcessingManager channelProcessingManager,
                        IconManager iconManager,
                        SettingsManager settingsManager,
                        Channel channel)
    {
        mChannelModel = channelModel;
        mChannelProcessingManager = channelProcessingManager;
        mIconManager = iconManager;
        mSettingsManager = settingsManager;
        mChannel = channel;

        ProcessingChain processingChain =
            channelProcessingManager.getProcessingChain(mChannel);

        if(processingChain != null)
        {
            mChannelState = processingChain.getChannelState();

            init();

            for(DecoderState decoderState : processingChain.getDecoderStates())
            {
                DecoderPanel panel = DecoderFactory.getDecoderPanel(mIconManager, mSettingsManager, decoderState);
                mDecoderPanels.add(panel);
                add(panel, "grow,span");
            }

            mChannelState.setChangedAttributeListener(this);
            mChannelModel.addListener(this);
            mSettingsManager.addListener(this);
        }
        else
        {
            mLog.error("Processing Chain was null!");
        }
    }

    private void init()
    {
        getColors();

        setLayout(new MigLayout("insets 1 1 1 1", "[grow,fill]", "[]0[]0[]"));

        mSystemLabel = new JLabel(mChannel.getSystem());
        mSystemLabel.setFont(mFontDetails);
        mSystemLabel.setForeground(mColorLabelDetails);
        add(mSystemLabel);

        mSiteLabel = new JLabel(mChannel.getSite());
        mSiteLabel.setFont(mFontDetails);
        mSiteLabel.setForeground(mColorLabelDetails);
        add(mSiteLabel);

        mChannelLabel = new JLabel(mChannel.getName());
        mChannelLabel.setFont(mFontDetails);
        mChannelLabel.setForeground(mColorLabelDetails);
        add(mChannelLabel, "wrap");

        mStateLabel = new JLabel(mChannelState.getState().getDisplayValue());
        mStateLabel.setFont(mFontDecoder);
        mStateLabel.setForeground(mColorLabelDecoder);
        add(mStateLabel);

        mSourceLabel = new JLabel(mChannel.getSourceConfiguration().getDescription());
        mSourceLabel.setFont(mFontDetails);
        mSourceLabel.setForeground(mColorLabelDetails);
        add(mSourceLabel);

        StringBuilder sb = new StringBuilder();

        if(!mChannel.getEventLogConfiguration().getLoggers().isEmpty())
        {
            sb.append("LOG ");
        }

        RecordConfiguration recordConfig = mChannel.getRecordConfiguration();

        if(recordConfig.getRecorders().contains(RecorderType.AUDIO))
        {
            sb.append("AUDIO ");
        }

        if(recordConfig.getRecorders().contains(RecorderType.BASEBAND) ||
            recordConfig.getRecorders().contains(RecorderType.TRAFFIC_BASEBAND))
        {
            sb.append("BASEBAND");
        }

        mOptionsLabel = new JLabel(sb.toString());
        mOptionsLabel.setFont(mFontDetails);
        mOptionsLabel.setForeground(mColorLabelAuxDecoder);
        add(mOptionsLabel, "wrap");
    }

    public Channel getChannel()
    {
        return mChannel;
    }

    public void dispose()
    {
        mChannelModel.removeListener(this);
        mChannelModel = null;

        mChannelProcessingManager = null;

        mSettingsManager.removeListener(this);
        mSettingsManager = null;

        mChannelState.removeChangedAttributeListener();
        mChannelState = null;
        mChannel = null;

        for(DecoderPanel panel : mDecoderPanels)
        {
            panel.dispose();
            remove(panel);
        }

        mDecoderPanels.clear();
    }

    @Override
    public void receive(final ChangedAttribute changedAttribute)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                switch(changedAttribute)
                {
                    case CHANNEL_STATE:
                        if(mChannelState != null && mChannelState.getState() != null)
                        {
                            mStateLabel.setText(mChannelState.getState().getDisplayValue());
                        }
                        break;
                    case SOURCE:
                        mSourceLabel.setText(mChannel.getSourceConfiguration()
                            .getDescription());
                        break;
                    case CHANNEL_NAME:
                    case SITE_NAME:
                    case SYSTEM_NAME:
                        mChannelLabel.setText(mChannel.toString());
                        break;
                    default:
                        break;
                }

                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        setBackground(mColorChannelBackground);

        Graphics2D g2 = (Graphics2D) g.create();

        Paint p = null;

        final Channel channel = mChannel;

        if(mChannelState != null)
        {
            switch(mChannelState.getState())
            {
                case CALL:
                    p = getGradient(mColorTopCall, mColorMiddleCall);
                    break;
                case CONTROL:
                    p = getGradient(mColorTopControl, mColorMiddleControl);
                    break;
                case DATA:
                    p = getGradient(mColorTopData, mColorMiddleData);
                    break;
                case FADE:
                    p = getGradient(mColorTopFade, mColorMiddleFade);
                    break;
                case IDLE:
                default:
                    p = getGradient(mColorTopIdle, mColorMiddleIdle);
                    break;
            }
        }

        if(p == null)
        {
            p = getGradient(mColorTopIdle, mColorMiddleIdle);
        }

        g2.setPaint(p);
        g2.fillRect(0, 0, getWidth(), getHeight());

        /* Draw bottom separator line */
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

        /* Draw channel selected box */
        if(getChannel() != null && getChannel().isSelected())
        {
            g2.setColor(mColorChannelSelected);
            g2.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
        }

        g2.dispose();
    }


    private void getColors()
    {
        mColorChannelBackground = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_BACKGROUND).getColor();
        mColorChannelSelected = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_SELECTED_CHANNEL).getColor();
        mColorTopCall = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CALL).getColor();
        mColorMiddleCall = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CALL).getColor();
        mColorTopControl = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CONTROL).getColor();
        mColorMiddleControl = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CONTROL).getColor();
        mColorTopData = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_DATA).getColor();
        mColorMiddleData = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_DATA).getColor();
        mColorTopFade = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_FADE).getColor();
        mColorMiddleFade = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_FADE).getColor();
        mColorTopIdle = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_IDLE).getColor();
        mColorMiddleIdle = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_IDLE).getColor();
        mColorTopNoTuner = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_NO_TUNER).getColor();
        mColorMiddleNoTuner = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_NO_TUNER).getColor();

        mColorLabelDetails = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_LABEL_DETAILS).getColor();
        mColorLabelDecoder = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_LABEL_DECODER).getColor();
        mColorLabelAuxDecoder = mSettingsManager.getColorSetting(
            ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER).getColor();
    }

    private GradientPaint getGradient(Color top, Color middle)
    {
        return new GradientPaint(0f, -50.0f, top,
            0f, (float) getHeight() / 2.2f, middle, true);
    }

    @Override
    public void settingChanged(Setting setting)
    {
        if(setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting) setting;

            switch(colorSetting.getColorSettingName())
            {
                case CHANNEL_STATE_BACKGROUND:
                    mColorChannelBackground = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_MIDDLE_CALL:
                    mColorMiddleCall = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_TOP_CALL:
                    mColorTopCall = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_MIDDLE_CONTROL:
                    mColorMiddleControl = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_TOP_CONTROL:
                    mColorTopControl = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_MIDDLE_DATA:
                    mColorMiddleData = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_TOP_DATA:
                    mColorTopData = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_MIDDLE_FADE:
                    mColorMiddleFade = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_TOP_FADE:
                    mColorTopFade = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_MIDDLE_IDLE:
                    mColorMiddleIdle = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_TOP_IDLE:
                    mColorTopIdle = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_MIDDLE_NO_TUNER:
                    mColorMiddleNoTuner = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_GRADIENT_TOP_NO_TUNER:
                    mColorTopNoTuner = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_LABEL_AUX_DECODER:
                    mColorLabelAuxDecoder = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_LABEL_DECODER:
                    mColorLabelDecoder = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_LABEL_DETAILS:
                    mColorLabelDetails = colorSetting.getColor();
                    repaint();
                    break;
                case CHANNEL_STATE_SELECTED_CHANNEL:
                    mColorChannelSelected = colorSetting.getColor();
                    repaint();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void settingDeleted(Setting setting)
    {
    }

    public JMenu getContextMenu()
    {
        return ChannelUtils.getContextMenu(mChannelModel, mChannelProcessingManager,
            mChannel, ChannelPanel.this);
    }

    @Override
    public void channelChanged(ChannelEvent event)
    {
        if(event.getEvent() == Event.NOTIFICATION_SELECTION_CHANGE &&
            event.getChannel() == mChannel)
        {
            repaint();
        }
    }
}
