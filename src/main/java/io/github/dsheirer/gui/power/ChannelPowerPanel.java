package io.github.dsheirer.gui.power;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.squelch.ISquelchConfiguration;
import io.github.dsheirer.gui.control.DbPowerMeter;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.EventQueue;
import java.text.DecimalFormat;

/**
 * Display for channel power and squelch details
 */
public class ChannelPowerPanel extends JPanel implements Listener<ProcessingChain>
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelPowerPanel.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final String NOT_AVAILABLE = "Not Available";
    private PlaylistManager mPlaylistManager;
    private ProcessingChain mProcessingChain;
    private DbPowerMeter mPowerMeter = new DbPowerMeter();
    private PeakMonitor mPeakMonitor = new PeakMonitor(DbPowerMeter.DEFAULT_MINIMUM_POWER);
    private SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private JLabel mPowerLabel;
    private JLabel mPeakLabel;
    private JLabel mSquelchLabel;
    private JLabel mSquelchValueLabel;
    private JButton mSquelchUpButton;
    private JButton mSquelchDownButton;
    private double mSquelchThreshold;

    /**
     * Constructs an instance.
     */
    public ChannelPowerPanel(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        setLayout(new MigLayout("", "[][grow,fill]", "[grow,fill]"));
        mPowerMeter.setPeakVisible(true);
        mPowerMeter.setSquelchThresholdVisible(true);
        add(mPowerMeter);

        JPanel valuePanel = new JPanel();
        valuePanel.setLayout(new MigLayout("", "[right][left][][]", ""));

        mPeakLabel = new JLabel("0");
        valuePanel.add(new JLabel("Peak:"));
        valuePanel.add(mPeakLabel, "wrap");

        mPowerLabel = new JLabel("0");
        valuePanel.add(new JLabel("Power:"));
        valuePanel.add(mPowerLabel, "wrap");

        mSquelchLabel = new JLabel("Squelch:");
        mSquelchLabel.setEnabled(false);
        valuePanel.add(mSquelchLabel);
        mSquelchValueLabel = new JLabel(NOT_AVAILABLE);
        mSquelchValueLabel.setEnabled(false);
        valuePanel.add(mSquelchValueLabel, "wrap");

        IconFontSwing.register(FontAwesome.getIconFont());
        Icon iconUp = IconFontSwing.buildIcon(FontAwesome.ANGLE_UP, 12);
        mSquelchUpButton = new JButton(iconUp);
        mSquelchUpButton.setEnabled(false);
        mSquelchUpButton.addActionListener(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold + 1)));
        valuePanel.add(mSquelchUpButton);

        Icon iconDown = IconFontSwing.buildIcon(FontAwesome.ANGLE_DOWN, 12);
        mSquelchDownButton = new JButton(iconDown);
        mSquelchDownButton.setEnabled(false);
        mSquelchDownButton.addActionListener(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold - 1)));
        valuePanel.add(mSquelchDownButton);

        add(valuePanel);
    }

    /**
     * Updates the channel's decode configuration with a new squelch threshold value
     */
    private void setConfigSquelchThreshold(int threshold)
    {
        if(mProcessingChain != null)
        {
            Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

            if(channel != null && channel.getDecodeConfiguration() instanceof ISquelchConfiguration)
            {
                ISquelchConfiguration configuration = (ISquelchConfiguration)channel.getDecodeConfiguration();
                configuration.setSquelchThreshold(threshold);
                mPlaylistManager.schedulePlaylistSave();
            }
        }
    }

    private void broadcast(SourceEvent sourceEvent)
    {
        if(mProcessingChain != null)
        {
            mProcessingChain.broadcast(sourceEvent);
        }
    }

    /**
     * Resets controls when changing processing chain source.  Note: this must be called on the Swing
     * dispatch thread because it directly invokes swing components.
     */
    private void reset()
    {
        mPeakMonitor.reset();
        mPowerMeter.reset();

        mPeakLabel.setText("0");
        mPowerLabel.setText("0");

        mSquelchLabel.setEnabled(false);
        mSquelchValueLabel.setText("Not Available");
        mSquelchValueLabel.setEnabled(false);
        mSquelchUpButton.setEnabled(false);
        mSquelchDownButton.setEnabled(false);
    }

    /**
     * Receive notifications of request to provide display of processing chain details.
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        if(mProcessingChain != null)
        {
            mProcessingChain.removeSourceEventListener(mSourceEventProcessor);
        }

        //Invoking reset - we're on the Swing dispatch thread here
        reset();

        mProcessingChain = processingChain;

        if(mProcessingChain != null)
        {
            mProcessingChain.addSourceEventListener(mSourceEventProcessor);
        }

        broadcast(SourceEvent.requestCurrentSquelchThreshold(null));
    }

    /**
     * Processor for source event stream to capture power level and squelch related source events.
     */
    private class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                case NOTIFICATION_CHANNEL_POWER ->
                    {
                        final double power = sourceEvent.getValue().doubleValue();
                        final double peak = mPeakMonitor.process(power);

                        EventQueue.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mPowerMeter.setPower(power);
                                mPowerLabel.setText(DECIMAL_FORMAT.format(power));

                                mPowerMeter.setPeak(peak);
                                mPeakLabel.setText(DECIMAL_FORMAT.format(peak));
                            }
                        });

                    }
                case NOTIFICATION_SQUELCH_THRESHOLD ->
                        {
                            final double threshold = sourceEvent.getValue().doubleValue();
                            mSquelchThreshold = threshold;
                            setConfigSquelchThreshold((int)threshold);

                            EventQueue.invokeLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    mPowerMeter.setSquelchThreshold(threshold);
                                    mSquelchLabel.setEnabled(true);
                                    mSquelchValueLabel.setEnabled(true);
                                    mSquelchValueLabel.setText(DECIMAL_FORMAT.format(threshold));
                                    mSquelchDownButton.setEnabled(true);
                                    mSquelchUpButton.setEnabled(true);
                                }
                            });
                        }
            }
        }
    }
}
