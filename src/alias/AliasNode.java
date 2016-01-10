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
package alias;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import map.MapIcon;
import playlist.PlaylistManager;
import settings.SettingsManager;
import alias.action.AliasAction;
import alias.action.beep.BeepAction;
import alias.action.beep.BeepActionNode;
import alias.action.clip.ClipAction;
import alias.action.clip.ClipActionNode;
import alias.action.script.ScriptAction;
import alias.action.script.ScriptActionNode;
import alias.id.esn.ESNNode;
import alias.id.esn.Esn;
import alias.id.fleetsync.FleetsyncID;
import alias.id.fleetsync.FleetsyncIDNode;
import alias.id.fleetsync.StatusID;
import alias.id.fleetsync.StatusIDNode;
import alias.id.lojack.LoJackFunctionAndID;
import alias.id.lojack.LoJackIDNode;
import alias.id.mdc.MDC1200ID;
import alias.id.mdc.MDC1200IDNode;
import alias.id.mobileID.MINNode;
import alias.id.mobileID.Min;
import alias.id.mpt1327.MPT1327ID;
import alias.id.mpt1327.MPT1327IDNode;
import alias.id.siteID.SiteID;
import alias.id.siteID.SiteIDNode;
import alias.id.talkgroup.TalkgroupID;
import alias.id.talkgroup.TalkgroupIDNode;
import alias.id.uniqueID.UniqueID;
import alias.id.uniqueID.UniqueIDNode;
import alias.priority.Priority;
import alias.priority.PriorityNode;
import alias.record.NonRecordable;
import alias.record.NonRecordableNode;
import controller.ConfigurableNode;

public class AliasNode extends ConfigurableNode 
{
    private static final long serialVersionUID = 1L;
    
    private SettingsManager mSettingsManager;
    private PlaylistManager mPlaylistManager;
    
    public AliasNode( PlaylistManager playlistManager, 
    				  SettingsManager settingsManager, 
    				  Alias alias )
	{
    	super( playlistManager, alias );
    	
    	mSettingsManager = settingsManager;
	}
    
    @Override
    public JPanel getEditor()
    {
        return new AliasEditor( this, mSettingsManager );
    }
    
    public Alias getAlias()
    {
    	return (Alias)getUserObject();
    }
    
    public String getIconPath()
    {
    	String icon = getAlias().getIconName();

    	MapIcon mapIcon = mSettingsManager.getMapIcon( icon );

    	if( mapIcon != null )
    	{
    		return mapIcon.getPath();
    	}
    	else
    	{
    		return null;
    	}
    }

    public void init()
    {
    	for( AliasID aliasID: getAlias().getId() )
    	{
    		switch( aliasID.getType() )
    		{
    			case ESN:
            		getModel().addNode( new ESNNode( mPlaylistManager, (Esn)aliasID ), 
            				AliasNode.this, getChildCount() );
    				break;
				case Fleetsync:
	        		getModel().addNode( new FleetsyncIDNode( mPlaylistManager, (FleetsyncID)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case LoJack:
	        		getModel().addNode( new LoJackIDNode( mPlaylistManager, (LoJackFunctionAndID)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case LTRNetUID:
	        		getModel().addNode( new UniqueIDNode( mPlaylistManager, (UniqueID)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case MDC1200:
	        		getModel().addNode( new MDC1200IDNode( mPlaylistManager, (MDC1200ID)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case MIN:
	        		getModel().addNode( new MINNode( mPlaylistManager, (Min)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case MPT1327:
	        		getModel().addNode( new MPT1327IDNode( mPlaylistManager, (MPT1327ID)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case NonRecordable:
	        		getModel().addNode( new NonRecordableNode( mPlaylistManager, (NonRecordable)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case Priority:
	        		getModel().addNode( new PriorityNode( mPlaylistManager, (Priority)aliasID ), 
	        				AliasNode.this, getChildCount() );
	        		break;
				case Site:
	        		getModel().addNode( new SiteIDNode( mPlaylistManager, (SiteID)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case Status:
	        		getModel().addNode( new StatusIDNode( mPlaylistManager, (StatusID)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				case Talkgroup:
	        		getModel().addNode( new TalkgroupIDNode( mPlaylistManager, (TalkgroupID)aliasID ), 
	        				AliasNode.this, getChildCount() );
					break;
				default:
					throw new IllegalArgumentException( "Unrecognized alias ID [" + 
							aliasID.getType() + "]");
    		}
    	}
    	
    	for( AliasAction action: getAlias().getAction() )
    	{
    		if( action instanceof BeepAction )
    		{
        		getModel().addNode( new BeepActionNode( mPlaylistManager, (BeepAction)action ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( action instanceof ClipAction )
    		{
        		getModel().addNode( new ClipActionNode( mPlaylistManager, (ClipAction)action ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( action instanceof ScriptAction )
    		{
        		getModel().addNode( new ScriptActionNode( mPlaylistManager, (ScriptAction)action ), 
        				AliasNode.this, getChildCount() );
    		}
    	}
    	
    	sort();
    }

    public String toString()
    {
    	return getAlias().getName() + " [" + this.getChildCount() + "]";
    }

	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();

		JMenu addIDMenu = new JMenu( "Add ID" );
		
		JMenuItem addESNItem = new JMenuItem( "ESN" );
		addESNItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Esn esn = new Esn();
				
				getAlias().addAliasID( esn );
				
				ESNNode node = new ESNNode( mPlaylistManager, esn );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addESNItem );
		
		JMenuItem addFleetsyncItem = new JMenuItem( "Fleetsync" );
		addFleetsyncItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				FleetsyncID fs = new FleetsyncID();
				
				getAlias().addAliasID( fs );
				
				FleetsyncIDNode node = new FleetsyncIDNode( mPlaylistManager, fs );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addFleetsyncItem );

		JMenuItem addLoJackItem = new JMenuItem( "LoJack" );
		addLoJackItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				LoJackFunctionAndID lj = new LoJackFunctionAndID();
				
				getAlias().addAliasID( lj );
				
				LoJackIDNode node = new LoJackIDNode( mPlaylistManager, lj );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addLoJackItem );

		JMenuItem addMDCItem = new JMenuItem( "MDC-1200" );
		addMDCItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				MDC1200ID mdc = new MDC1200ID();
				
				getAlias().addAliasID( mdc );
				
				MDC1200IDNode node = new MDC1200IDNode( mPlaylistManager, mdc );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addMDCItem );

		JMenuItem addMINItem = new JMenuItem( "MIN" );
		addMINItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Min min = new Min();
				
				getAlias().addAliasID( min );
				
				MINNode node = new MINNode( mPlaylistManager, min );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addMINItem );
		
		JMenuItem addMPTItem = new JMenuItem( "MPT-1327" );
		addMPTItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				MPT1327ID mpt = new MPT1327ID();
				
				getAlias().addAliasID( mpt );
				
				MPT1327IDNode node = new MPT1327IDNode( mPlaylistManager, mpt );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addMPTItem );

		JMenuItem addSiteItem = new JMenuItem( "Site" );
		addSiteItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				SiteID siteID = new SiteID();
				
				getAlias().addAliasID( siteID );
				
				SiteIDNode node = new SiteIDNode( mPlaylistManager, siteID );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addSiteItem );
		
		JMenuItem addStatusItem = new JMenuItem( "Status" );
		addStatusItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				StatusID statusID = new StatusID();
				
				getAlias().addAliasID( statusID );
				
				StatusIDNode node = new StatusIDNode( mPlaylistManager, statusID );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		
		addIDMenu.add( addStatusItem );
		
		JMenuItem addTalkgroupItem = new JMenuItem( "Talkgroup" );
		addTalkgroupItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				TalkgroupID tg = new TalkgroupID();
				
				getAlias().addAliasID( tg );
				
				TalkgroupIDNode node = new TalkgroupIDNode( mPlaylistManager, tg );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addTalkgroupItem );
		
		JMenuItem addUniqueIDItem = new JMenuItem( "Unique ID" );
		addUniqueIDItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				UniqueID uid = new UniqueID();
				
				getAlias().addAliasID( uid );
				
				UniqueIDNode node = new UniqueIDNode( mPlaylistManager, uid );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addUniqueIDItem );

		retVal.add( addIDMenu );
		
		JMenu addActionMenu = new JMenu( "Add Action" );
		
		JMenuItem addClipItem = new JMenuItem( "Audio Clip" );
		
		addClipItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				ClipAction clipAction = new ClipAction();

				getAlias().addAliasAction( clipAction );
				
				ClipActionNode node = new ClipActionNode( mPlaylistManager, clipAction );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		
		addActionMenu.add( addClipItem );

		JMenuItem addBeepItem = new JMenuItem( "Beep" );
		
		addBeepItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				BeepAction beepAction = new BeepAction();

				getAlias().addAliasAction( beepAction );
				
				BeepActionNode node = new BeepActionNode( mPlaylistManager, beepAction );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		
		addActionMenu.add( addBeepItem );
		
		JMenuItem addScriptItem = new JMenuItem( "Script" );
		
		addScriptItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				ScriptAction scriptAction = new ScriptAction();

				getAlias().addAliasAction( scriptAction );
				
				ScriptActionNode node = new ScriptActionNode( mPlaylistManager, scriptAction );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		
		addActionMenu.add( addScriptItem );

		retVal.add( addActionMenu );
		
		retVal.addSeparator();

		if( getAlias().isRecordable() )
		{
			JMenuItem nonRecordableItem = new JMenuItem( "Set Non-Recordable" );
			
			nonRecordableItem.addActionListener( new ActionListener() 
			{
				@Override
			    public void actionPerformed( ActionEvent e )
			    {
					NonRecordable non = new NonRecordable();
					
					getAlias().addAliasID( non );

					NonRecordableNode node = new NonRecordableNode( mPlaylistManager, non );
					
					getModel().addNode( node, 
										AliasNode.this, 
										AliasNode.this.getChildCount() );
					
					node.show();
			    }
			} );
			
			retVal.add( nonRecordableItem );
			retVal.addSeparator();
		}

		if( !getAlias().hasPriority() )
		{
			JMenuItem priorityItem = new JMenuItem( "Set Call Priority" );
			
			priorityItem.addActionListener( new ActionListener() 
			{
				@Override
			    public void actionPerformed( ActionEvent e )
			    {
					Priority priority = new Priority();
					
					getAlias().addAliasID( priority );

					PriorityNode node = new PriorityNode( mPlaylistManager, priority );
					
					getModel().addNode( node, 
										AliasNode.this, 
										AliasNode.this.getChildCount() );
					
					node.show();
			    }
			} );
			
			retVal.add( priorityItem );
			retVal.addSeparator();
		}


		JMenuItem deleteItem = new JMenuItem( "Delete" );
		deleteItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				int n = JOptionPane.showConfirmDialog( getModel().getTree(),
				    "Are you sure you want to permanently delete this node?" );				
				
				if( n == JOptionPane.YES_OPTION )
				{
					GroupNode parent = (GroupNode)getParent();
					
					parent.getGroup().removeAlias( getAlias() );

					save();

					getModel().removeNodeFromParent( AliasNode.this );
				}
            }
		} );
		
		retVal.add( deleteItem );
		
		return retVal;
	}
}
