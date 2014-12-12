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
package controller.activity;

import filter.FilterEditorPanel;
import filter.FilterSet;
import gui.SDRTrunk;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import message.Message;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitButton;

import controller.channel.Channel;
import controller.channel.ChannelEvent;
import controller.channel.ChannelEvent.Event;
import controller.channel.ChannelEventListener;

public class MessageActivityPanel extends JPanel implements ChannelEventListener
{
    private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( SDRTrunk.class );

	private static MessageActivityModel EMPTY_MODEL = new MessageActivityModel();
    
    private JTable mMessageTable;
    
    private Channel mDisplayedChannel;
    
    private MessageManagementPanel mManagementPanel = new MessageManagementPanel();

	public MessageActivityPanel()
	{
    	setLayout( new MigLayout("insets 0 0 0 0", "[][grow,fill]", "[]0[grow,fill]") );

    	add( mManagementPanel, "span,growx" );

    	mMessageTable = new JTable( EMPTY_MODEL );
    	
    	add( new JScrollPane( mMessageTable ), "span,grow" );
	}

	@Override
    public void channelChanged( ChannelEvent event )
    {
		boolean changed = false;
		
		if( event.getEvent() == Event.CHANGE_SELECTED && 
			event.getChannel().isSelected() )
		{
			if( mDisplayedChannel == null || 
				( mDisplayedChannel != null && 
				  mDisplayedChannel != event.getChannel() ) )
			{
				mDisplayedChannel = event.getChannel();

				mMessageTable.setModel( event.getChannel().getProcessingChain()
						.getChannelState().getMessageActivityModel() );

				if( mDisplayedChannel != null )
				{
					mManagementPanel.enable();
				}
				else
				{
					mManagementPanel.disable();
				}
				
				changed = true;
			}
		}
		else if( event.getEvent() == Event.PROCESSING_STOPPED ||
				 event.getEvent() == Event.CHANNEL_DELETED )
		{
			if( mDisplayedChannel != null && 
				mDisplayedChannel == event.getChannel() )
			{
				mDisplayedChannel = null;

				mMessageTable.setModel( EMPTY_MODEL );

				mManagementPanel.disable();

				changed = true;
			}
		}

		if( changed )
		{
			revalidate();
			repaint();
		}
    }
	
	public class MessageManagementPanel extends JPanel
	{
        private static final long serialVersionUID = 1L;

        private MessageHistoryButton mHistoryButton = new MessageHistoryButton();
        private MessageFilterButton mFilterButton = new MessageFilterButton();
        
        public MessageManagementPanel()
        {
        	setLayout( new MigLayout("insets 2 2 5 5", "[]5[left,grow]", "") );

        	disable();
        	
        	add( mFilterButton );
        	add( mHistoryButton );
        }
        
        public void enable()
        {
        	mHistoryButton.setEnabled( true );
        	mFilterButton.setEnabled( true );
        }
        
        public void disable()
        {
        	mHistoryButton.setEnabled( false );
        	mFilterButton.setEnabled( false );
        }
	}
	
	public class MessageHistoryButton extends JideSplitButton
	{
        private static final long serialVersionUID = 1L;
        
        public MessageHistoryButton()
        {
        	super( "Clear" );
        	
        	JPanel historyPanel = new JPanel();
        	
        	historyPanel.add( new JLabel( "Message History:" ) );
        	
        	final JSlider slider = new JSlider();
        	slider.setMinimum( 0 );
        	slider.setMaximum( 2000 );
        	slider.setMajorTickSpacing( 500 );
        	slider.setPaintTicks( true );
        	slider.setPaintLabels( true );
        	
        	slider.addMouseListener( new MouseListener() 
        	{
				@Override
                public void mouseClicked( MouseEvent arg0 )
                {
					if( SwingUtilities.isLeftMouseButton( arg0 ) &&
						arg0.getClickCount() == 2 )
					{
						slider.setValue( 500 ); //default
					}
                }

                public void mouseEntered( MouseEvent arg0 ) {}
                public void mouseExited( MouseEvent arg0 ) {}
                public void mousePressed( MouseEvent arg0 ) {}
                public void mouseReleased( MouseEvent arg0 ) {}
        	} );
        	
        	if( mDisplayedChannel != null )
        	{
            	slider.setValue( mDisplayedChannel.getProcessingChain()
            			.getChannelState().getMessageActivityModel()
            			.getMaxMessageCount() );
        	}
        	else
        	{
        		slider.setValue( 500 );
        	}
        	
        	slider.addChangeListener( new ChangeListener() 
        	{
				@Override
                public void stateChanged( ChangeEvent arg0 )
                {
					if( mDisplayedChannel != null )
					{
						mDisplayedChannel.getProcessingChain().getChannelState()
						.getMessageActivityModel()
						.setMaxMessageCount( slider.getValue() );
					}
                }
        	});

        	historyPanel.add( slider );
        	
        	add( historyPanel );

        	/* Clear messages */
        	addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					if( mDisplayedChannel != null )
					{
						mDisplayedChannel.getProcessingChain().getChannelState()
							.getMessageActivityModel().clear();
					}
				}
			} );
        }
	}

	public class MessageFilterButton extends JideButton
	{
        private static final long serialVersionUID = 1L;
        
        public MessageFilterButton()
        {
        	super( "Filter" );
        	
        	addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent arg0 )
				{
					if( mDisplayedChannel == null )
					{
						JOptionPane.showMessageDialog( MessageFilterButton.this, 
							"Please enable and select a decoding channel to "
							+ "adjust the channel's message display filter" );
					}
					else
					{
						final JFrame editor = new JFrame();
						
						editor.setTitle( "Message Filter Editor" );
						editor.setSize( 600, 400 );
						editor.setLocationRelativeTo( MessageFilterButton.this );
						editor.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
						editor.setLayout( new MigLayout( "", "[grow,fill]", 
								"[grow,fill][][]" ) );

						@SuppressWarnings( "unchecked" )
                        FilterSet<Message> filter = 
								mDisplayedChannel
								.getProcessingChain().getChannelState()
								.getMessageActivityModel().getMessageFilter();
						
						FilterEditorPanel<Message> panel = 
								new FilterEditorPanel<Message>( filter );
						
						JScrollPane scroller = new JScrollPane( panel );
						scroller.setViewportView( panel );
						
						editor.add( scroller, "wrap" );
						
						editor.add( new JLabel( "Right-click to select/deselect "
								+ "all nodes" ), "wrap" );
						
						JButton close = new JButton( "Close" );
						close.addActionListener( new ActionListener() 
						{
							@Override
				            public void actionPerformed( ActionEvent e )
				            {
								editor.dispose();
				            }
						} );
						
						editor.add( close );

						EventQueue.invokeLater( new Runnable() 
				        {
				            public void run()
				            {
				            	editor.setVisible( true );
				            }
				        } );
					}
				}
			} );
        }
	}
}
