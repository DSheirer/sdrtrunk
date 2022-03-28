/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.playback;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.audio.AudioException;
import io.github.dsheirer.audio.IAudioController;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.mixer.MixerChannelConfiguration;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.FloatControl;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class AudioPanel extends JPanel implements Listener<AudioEvent>
{
    private static final long serialVersionUID = 1L;

    private static final Logger mLog = LoggerFactory.getLogger(AudioPanel.class);

    private static ImageIcon MUTED_ICON = IconModel.getScaledIcon("images/audio_muted.png", 20);
    private static ImageIcon UNMUTED_ICON = IconModel.getScaledIcon("images/audio_unmuted.png", 20);

    private IconModel mIconModel;
    private SettingsManager mSettingsManager;
    private IAudioController mController;
    private AliasModel mAliasModel;
    private UserPreferences mUserPreferences;

    private JButton mMuteButton;
    private AudioChannelsPanel mAudioChannelsPanel;

    public AudioPanel(IconModel iconModel, UserPreferences userPreferences, SettingsManager settingsManager,
                      IAudioController controller, AliasModel aliasModel)
    {
        mIconModel = iconModel;
        mSettingsManager = settingsManager;
        mController = controller;
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;

        mController.addControllerListener(this);

        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[]0[grow,fill]", "[fill]0[]"));
        setBackground(Color.BLACK);

        mMuteButton = new MuteButton();
        mMuteButton.setBackground(getBackground());
        add(mMuteButton);

        mAudioChannelsPanel = new AudioChannelsPanel(mIconModel, mUserPreferences, mSettingsManager, mController, mAliasModel);

        add(mAudioChannelsPanel);

        addMouseListener(new MouseSelectionListener());
    }

    @Override
    public void receive(AudioEvent event)
    {
        switch(event.getType())
        {
            case AUDIO_CONFIGURATION_CHANGE_STARTED:
                break;
            case AUDIO_CONFIGURATION_CHANGE_COMPLETE:
                EventQueue.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        remove(mAudioChannelsPanel);
                        mAudioChannelsPanel.dispose();
                        mAudioChannelsPanel = new AudioChannelsPanel(mIconModel, mUserPreferences, mSettingsManager, mController, mAliasModel);
                        add(mAudioChannelsPanel);
                        mAudioChannelsPanel.repaint();
                        revalidate();
                        repaint();
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * Audio output mute control menu item.
     */
    public class AudioOutputMuteItem extends JMenuItem
    {
        private static final long serialVersionUID = 1L;

        private AudioOutput mAudioOutput;

        public AudioOutputMuteItem(AudioOutput audioOutput)
        {
            super(audioOutput.isMuted() ? "Unmute" : "Mute");

            mAudioOutput = audioOutput;

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    mAudioOutput.setMuted(!mAudioOutput.isMuted());
                }
            });
        }
    }

    /**
     * Mouse listener
     */
    public class MouseSelectionListener implements MouseListener
    {
        @Override
        public void mouseClicked(MouseEvent event)
        {
            if(SwingUtilities.isRightMouseButton(event))
            {
                JPopupMenu popup = new JPopupMenu();

				/* Audio mixer/output selection menus */
                JMenuItem outputMenu = new JMenuItem("Configure ...");
                Icon icon = IconFontSwing.buildIcon(FontAwesome.COG, 14);
                outputMenu.setIcon(icon);
                outputMenu.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.AUDIO_OUTPUT));
                    }
                });
                popup.add(outputMenu);
                popup.add(new JPopupMenu.Separator());

				/* Audio output mute and volume control */
                for(AudioOutput output : mController.getAudioOutputs())
                {
                    JMenu menu = new JMenu("Channel: " + output.getChannelName());

                    menu.add(new AudioOutputMuteItem(output));

                    if(output.hasGainControl())
                    {
                        JMenu volume = new JMenu("Volume");
                        volume.add(new VolumeSlider(output.getGainControl()));
                        menu.add(volume);
                    }

                    popup.add(menu);
                }

                popup.show(event.getComponent(), event.getX(), event.getY());
            }
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
        }
    }

    /**
     * Audio volume (gain) adjustment slider control
     */
    public class VolumeSlider extends JSlider
    {
        private static final long serialVersionUID = 1L;

        private FloatControl mFloatControl;

        public VolumeSlider(FloatControl control)
        {
            super(0, 100, 0);

            setMajorTickSpacing(25);
            setMinorTickSpacing(5);
            setPaintTicks(true);
            setPaintLabels(true);

            mFloatControl = control;

            setValue(getIntegerValue(mFloatControl.getValue()));

            addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent event)
                {
                    mFloatControl.shift(mFloatControl.getValue(),
                        getFloatValue(VolumeSlider.this.getValue()),
                        1000);
                }
            });

            addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent event)
                {
                    if(event.getClickCount() == 2)
                    {
                        VolumeSlider.this.setValue(50);
                    }
                }

                public void mouseReleased(MouseEvent arg0)
                {
                }

                public void mousePressed(MouseEvent arg0)
                {
                }

                public void mouseExited(MouseEvent arg0)
                {
                }

                public void mouseEntered(MouseEvent arg0)
                {
                }
            });
        }

        /**
         * Converts the integer value to a floating point value to use in the
         * float control.  Assumes an integer value of 50 is the 0.0 dB mid
         * point (ie no gain ) value.
         */
        private int getIntegerValue(float value)
        {
            if(value == 0.0f)
            {
                return 50;
            }
            else if(value < 0.0f)
            {
                float ratio = value / mFloatControl.getMinimum();

                return 50 - (int) (ratio * 50.0f);
            }
            else
            {
                float ratio = value / mFloatControl.getMaximum();

                return 50 + (int) (ratio * 50.0f);
            }
        }

        private float getFloatValue(int value)
        {
            if(value == 50)
            {
                return 0.0f;
            }
            else if(value < 50)
            {
                return (float) (50 - value) / 50.0f * mFloatControl.getMinimum();
            }
            else
            {
                return (float) (value - 50) / 50.0f * mFloatControl.getMaximum();
            }
        }
    }

    /**
     * Mixer/Channel configuration selection item
     */
    public class MixerSelectionItem extends JMenuItem
    {
        private static final long serialVersionUID = 1L;

        private MixerChannelConfiguration mConfiguration;

        public MixerSelectionItem(MixerChannelConfiguration config)
        {
            super(config.toString());

            mConfiguration = config;

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        mController.setMixerChannelConfiguration(mConfiguration);
                    }
                    catch(AudioException e1)
                    {
                        mLog.error("Couldn't set mixer channel configuration "
                            + "to: " + mConfiguration.toString());

                        JOptionPane.showMessageDialog(MixerSelectionItem.this,
                            "Couldn't set [" + mConfiguration.toString() +
                                "] as the audio output device");
                    }
                }
            });
        }
    }

    /**
     * Mute button to mute all audio output channels exposed by the audio
     * controller
     */
    public class MuteButton extends JButton
    {
        private static final long serialVersionUID = 1L;

        private boolean mMuted = false;

        public MuteButton()
        {
            setIcon(UNMUTED_ICON);
            setBorderPainted(false);
            getAccessibleContext().setAccessibleName("Mute");

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    mMuted = !mMuted;

                    for(AudioOutput output : mController.getAudioOutputs())
                    {
                        output.setMuted(mMuted);
                    }

                    EventQueue.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            setIcon(mMuted ? MUTED_ICON : UNMUTED_ICON);
                            getAccessibleContext().setAccessibleName(mMuted ? "Unmute" : "Mute");
                        }
                    });
                }
            });
        }
    }
}
