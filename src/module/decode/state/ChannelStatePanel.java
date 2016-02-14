package module.decode.state;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;

import module.ProcessingChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.channel.Channel;
import controller.channel.ChannelUtils;
import module.decode.DecoderFactory;
import net.miginfocom.swing.MigLayout;
import playlist.PlaylistManager;
import record.RecorderType;
import record.config.RecordConfiguration;
import sample.Listener;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;

import controller.channel.Channel;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;
import controller.channel.ChannelUtils;

public class ChannelStatePanel extends JPanel 
				implements Listener<ChangedAttribute>, SettingChangeListener
{
	private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( ChannelStatePanel.class );

    private Font mFontDetails = new Font( Font.MONOSPACED, Font.PLAIN, 12 );
    private Font mFontDecoder = new Font( Font.MONOSPACED, Font.PLAIN, 12 );

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

    private PlaylistManager mPlaylistManager;
	protected SettingsManager mSettingsManager;

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
	public ChannelStatePanel( ChannelModel channelModel,
							  ChannelProcessingManager channelProcessingManager,
							  PlaylistManager playlistManager, 
							  SettingsManager settingsManager, 
							  Channel channel )
	{
		mChannelModel = channelModel;
		mChannelProcessingManager = channelProcessingManager;
		mPlaylistManager = playlistManager;
		
		mSettingsManager = settingsManager;
		mSettingsManager.addListener( this );
		
		mChannel = channel;
		
		ProcessingChain processingChain = 
				channelProcessingManager.getProcessingChain( mChannel );

		if( processingChain != null )
		{
			mChannelState = processingChain.getChannelState();
			
			mChannelState.setChangedAttributeListener( this );
			
			init();

			for( DecoderState decoderState: processingChain.getDecoderStates() )
	    	{
				DecoderPanel panel = DecoderFactory.getDecoderPanel( mSettingsManager, decoderState );
				mDecoderPanels.add( panel );
	    		add( panel, "grow,span" );
	    	}
		}
		else
		{
			mLog.error( "Processing Chain was null!" );
		}
	}
	
	private void init()
	{
    	getColors();

    	setLayout( new MigLayout( "insets 1 1 1 1", "[grow,fill]", "[]0[]0[]") );
		
		mSystemLabel = new JLabel( mChannel.getSystem() );
		mSystemLabel.setFont( mFontDetails );
		mSystemLabel.setForeground( mColorLabelDetails );
		add( mSystemLabel );
		
		mSiteLabel = new JLabel( mChannel.getSite() );
		mSiteLabel.setFont( mFontDetails );
		mSiteLabel.setForeground( mColorLabelDetails );
		add( mSiteLabel );

		mChannelLabel = new JLabel( mChannel.getName() );
		mChannelLabel.setFont( mFontDetails );
		mChannelLabel.setForeground( mColorLabelDetails );
		add( mChannelLabel, "wrap" );
		
		mStateLabel = new JLabel( mChannelState.getState().getDisplayValue() );
		mStateLabel.setFont( mFontDecoder );
		mStateLabel.setForeground( mColorLabelDecoder );
		add( mStateLabel );
		
		mSourceLabel = new JLabel( mChannel.getSourceConfiguration().getDescription() );
		mSourceLabel.setFont( mFontDetails );
		mSourceLabel.setForeground( mColorLabelDetails );
		add( mSourceLabel );
		
		StringBuilder sb = new StringBuilder();

		if( !mChannel.getEventLogConfiguration().getLoggers().isEmpty() )
		{
			sb.append( "LOG " );
		}
		
		RecordConfiguration recordConfig = mChannel.getRecordConfiguration();
		
		if( recordConfig.getRecorders().contains( RecorderType.AUDIO ) )
		{
			sb.append( "AUDIO " );
		}
		
		if( recordConfig.getRecorders().contains( RecorderType.BASEBAND ) ||
			recordConfig.getRecorders().contains( RecorderType.TRAFFIC_BASEBAND ) )
		{
			sb.append( "BASEBAND" );
		}
		
		mOptionsLabel = new JLabel( sb.toString() );
		mOptionsLabel.setFont( mFontDetails );
		mOptionsLabel.setForeground( mColorLabelAuxDecoder );
		add( mOptionsLabel, "wrap" );
	}
	
	public Channel getChannel()
	{
		return mChannel;
	}
	
	public void dispose()
	{
		mPlaylistManager = null;
		
		mSettingsManager.removeListener( this );
		mSettingsManager = null;
		
		mChannelState.removeChangedAttributeListener();
		mChannelState = null;
		mChannel = null;
		
		for( DecoderPanel panel: mDecoderPanels )
		{
			panel.dispose();
			remove( panel );
		}
		
		mDecoderPanels.clear();
	}
	
	@Override
	public void receive( final ChangedAttribute changedAttribute )
	{
		EventQueue.invokeLater( new Runnable()
		{
			@Override
            public void run()
            {
				switch( changedAttribute )
				{
					case CHANNEL_STATE:
						if( mChannelState.getState() != null )
						{
				    		mStateLabel.setText( mChannelState.getState().getDisplayValue() );
						}
						break;
					case SOURCE:
			    		mSourceLabel.setText( mChannel.getSourceConfiguration()
			    				.getDescription() );
						break;
					case CHANNEL_NAME:
					case SITE_NAME:
					case SYSTEM_NAME:
				    	mChannelLabel.setText( mChannel.toString() );
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
    	super.paintComponent( g );
    	
    	setBackground( mColorChannelBackground );
    	
        Graphics2D g2 = (Graphics2D)g.create();

        Paint p = null;

        switch( mChannelState.getState() )
        {
			case CALL:
				p = getGradient( mColorTopCall, mColorMiddleCall );
				break;
			case CONTROL:
				p = getGradient( mColorTopControl, mColorMiddleControl );
				break;
			case DATA:
				p = getGradient( mColorTopData, mColorMiddleData );
				break;
			case FADE:
				p = getGradient( mColorTopFade, mColorMiddleFade );
				break;
			case IDLE:
			default:
				p = getGradient( mColorTopIdle, mColorMiddleIdle );
				break;
       }
        
        g2.setPaint( p );
        g2.fillRect( 0, 0, getWidth(), getHeight() );

        /* Draw bottom separator line */
        g2.setColor( Color.LIGHT_GRAY );
        g2.drawLine( 0, getHeight() - 1, getWidth(), getHeight() - 1 );

        /* Draw channel selected box */
        if( getChannel() != null && getChannel().isSelected() )
        {
            g2.setColor( mColorChannelSelected );
            g2.drawRect( 1, 1, getWidth() - 2, getHeight() - 2 );
        }
        
        g2.dispose();
    }
	
	
    private void getColors()
    {
        mColorChannelBackground = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_BACKGROUND ).getColor();
        mColorChannelSelected = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_SELECTED_CHANNEL ).getColor();
        mColorTopCall = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CALL ).getColor();
        mColorMiddleCall = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CALL ).getColor();
        mColorTopControl = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CONTROL ).getColor();
        mColorMiddleControl = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CONTROL ).getColor();
        mColorTopData = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_DATA ).getColor();
        mColorMiddleData = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_DATA ).getColor();
        mColorTopFade = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_FADE ).getColor();
        mColorMiddleFade = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_FADE ).getColor();
        mColorTopIdle = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_IDLE ).getColor();
        mColorMiddleIdle = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_IDLE ).getColor();
        mColorTopNoTuner = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_NO_TUNER ).getColor();
        mColorMiddleNoTuner = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_NO_TUNER ).getColor();

        mColorLabelDetails = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DETAILS ).getColor();
    	mColorLabelDecoder = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DECODER ).getColor();
    	mColorLabelAuxDecoder = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER ).getColor();
    }
    
    private GradientPaint getGradient( Color top, Color middle )
    {
        return new GradientPaint( 0f, -50.0f, top, 
                              0f, (float)getHeight() / 2.2f, middle, true );
    }

	@Override
    public void settingChanged( Setting setting )
    {
		if( setting instanceof ColorSetting )
		{
			ColorSetting colorSetting = (ColorSetting)setting;

			switch( colorSetting.getColorSettingName() )
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
    public void settingDeleted( Setting setting ) {}

	public JMenu getContextMenu()
	{
		return ChannelUtils.getContextMenu( mChannelModel, mChannelProcessingManager, 
				mPlaylistManager, mChannel, ChannelStatePanel.this );
	}
}
