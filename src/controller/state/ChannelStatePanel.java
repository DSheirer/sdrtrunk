/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package controller.state;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.HashMap;

import javax.swing.JMenu;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import sample.Listener;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;
import controller.channel.Channel;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelState.State;

public abstract class ChannelStatePanel extends JPanel 
						implements Listener<ChangedAttribute>, 
								   SettingChangeListener
{
    private static final long serialVersionUID = 1L;
    
    protected Font mFontDetails = new Font( Font.MONOSPACED, Font.PLAIN, 10 );
    protected Font mFontDecoder = new Font( Font.MONOSPACED, Font.PLAIN, 10 );
    protected Font mFontAuxDecoder = new Font( Font.MONOSPACED, Font.PLAIN, 10 );

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
    
	private HashMap<Channel,ChannelStatePanel> mTrafficPanels = 
			new HashMap<Channel,ChannelStatePanel>();
    
    protected Channel mChannel;

    public ChannelStatePanel( Channel channel )
    {
    	mChannel = channel;

    	/* Register to receive settings updates */
    	getSettingsManager().addListener( this );
    	
    	/* Register to receive channel updates */
    	mChannel.getProcessingChain().getChannelState().addListener( this );

    	getColors();

    	setLayout( new MigLayout( "insets 3 2 2 2", "[grow,fill]", "[]0[]0[]") );
    }
    
    public SettingsManager getSettingsManager()
    {
    	return mChannel.getResourceManager().getSettingsManager();
    }
    
    public void dispose()
    {
    	mTrafficPanels.clear();
    	mChannel = null;
    }
    
    private void getColors()
    {
        mColorChannelBackground = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_BACKGROUND ).getColor();
        mColorChannelSelected = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_SELECTED_CHANNEL ).getColor();
        mColorTopCall = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CALL ).getColor();
        mColorMiddleCall = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CALL ).getColor();
        mColorTopControl = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CONTROL ).getColor();
        mColorMiddleControl = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CONTROL ).getColor();
        mColorTopData = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_DATA ).getColor();
        mColorMiddleData = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_DATA ).getColor();
        mColorTopFade = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_FADE ).getColor();
        mColorMiddleFade = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_FADE ).getColor();
        mColorTopIdle = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_IDLE ).getColor();
        mColorMiddleIdle = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_IDLE ).getColor();
        mColorTopNoTuner = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_NO_TUNER ).getColor();
        mColorMiddleNoTuner = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_NO_TUNER ).getColor();

        mColorLabelDetails = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DETAILS ).getColor();
    	mColorLabelDecoder = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DECODER ).getColor();
    	mColorLabelAuxDecoder = getSettingsManager().getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER ).getColor();
    }
    
    public HashMap<Channel,ChannelStatePanel> getTrafficPanels()
    {
    	return mTrafficPanels;
    }
    
    public JMenu getContextMenu()
    {
    	return mChannel.getContextMenu();
    }
    
    public Channel getChannel()
    {
    	return mChannel;
    }
    
    public ChannelState getChannelState()
    {
    	if( mChannel.getProcessingChain() != null )
    	{
    		return mChannel.getProcessingChain().getChannelState();
    	}
    	
		return null;
    }
    
    public boolean getSelected()
    {
    	return mChannel.getSelected();
    }
    
	public void addPanel( final JPanel panel )
	{
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				add( panel, "span,wrap" );
				validate();
            }
		} );
	}
	
	public void removePanel( final JPanel panel )
	{
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				remove( panel );

				validate();
				
				if( getParent() != null )
				{
					getParent().validate();
				}

				if( panel instanceof ChannelStatePanel )
				{
					((ChannelStatePanel)panel).dispose();
				}
				else if( panel instanceof AuxStatePanel )
				{
					((AuxStatePanel)panel).dispose();
				}
            }
		} );
	}
	
	public void trafficChannelAdded( Channel traffic )
	{
		//Disregard -- sub class can override and implement
	}
	
	public void trafficChannelDeleted( Channel traffic )
	{
		ChannelStatePanel panelToRemove = 
				getTrafficPanels().remove( traffic );

		removePanel( panelToRemove );
	}

    public void setSelected( boolean selected )
    {
    	mChannel.setSelected( selected );

		for( ChannelStatePanel traffic: getTrafficPanels().values() )
		{
			traffic.setSelected( false );
		}
    	
    	repaint();
    }
    
    private GradientPaint getGradient( Color top, Color middle )
    {
    	return new GradientPaint( 0f, -50.0f, top, 
    							  0f, (float)getHeight() / 2.2f, middle, true );
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
    	super.paintComponent( g );
    	
    	setBackground( mColorChannelBackground );
    	
        Graphics2D g2 = (Graphics2D)g.create();

        Paint p = null;
        
        ChannelState channelState = getChannelState();
        
        if( channelState != null )
        {
        	State state = channelState.getState();
        	
        	if( state != null )
        	{
                switch( state )
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
        			case NO_TUNER:
        				p = getGradient( mColorTopNoTuner, mColorMiddleNoTuner );
        				break;
        			case END:
        			case IDLE:
        			default:
        				p = getGradient( mColorTopIdle, mColorMiddleIdle );
        				break;
                }
        	}
        }
        
        g2.setPaint( p );
        g2.fillRect( 0, 0, getWidth(), getHeight() );

        /* Draw bottom separator line */
        g2.setColor( Color.LIGHT_GRAY );
        g2.drawLine( 0, getHeight() - 1, getWidth(), getHeight() - 1 );

        /* Draw channel selected box */
        if( mChannel.getSelected() )
        {
            g2.setColor( mColorChannelSelected );
            g2.drawRect( 1, 1, getWidth() - 2, getHeight() - 2 );
        }
        
        g2.dispose();
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
			}
		}
    }

	@Override
    public void settingDeleted( Setting setting ) {}
}
