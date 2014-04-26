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
import javax.swing.JSeparator;

import map.MapIcon;
import alias.esn.ESNNode;
import alias.esn.Esn;
import alias.fleetsync.FleetsyncID;
import alias.fleetsync.FleetsyncIDNode;
import alias.fleetsync.StatusID;
import alias.fleetsync.StatusIDNode;
import alias.mdc.MDC1200ID;
import alias.mdc.MDC1200IDNode;
import alias.mobileID.MINNode;
import alias.mobileID.Min;
import alias.mpt1327.MPT1327ID;
import alias.mpt1327.MPT1327IDNode;
import alias.siteID.SiteID;
import alias.siteID.SiteIDNode;
import alias.talkgroup.TalkgroupID;
import alias.talkgroup.TalkgroupIDNode;
import alias.uniqueID.UniqueID;
import alias.uniqueID.UniqueIDNode;
import controller.ConfigurableNode;

public class AliasNode extends ConfigurableNode 
{
    private static final long serialVersionUID = 1L;
    
    public AliasNode( Alias alias )
	{
    	super( alias );
	}
    
    @Override
    public JPanel getEditor()
    {
        return new AliasEditor( this, getModel().getResourceManager() );
    }
    
    public Alias getAlias()
    {
    	return (Alias)getUserObject();
    }
    
    public String getIconPath()
    {
    	String icon = getAlias().getIconName();

    	MapIcon mapIcon = getModel().getResourceManager()
    			.getSettingsManager().getMapIcon( icon );

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
    		if( aliasID instanceof Esn )
    		{
        		getModel().addNode( new ESNNode( (Esn)aliasID ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( aliasID instanceof FleetsyncID )
    		{
        		getModel().addNode( new FleetsyncIDNode( (FleetsyncID)aliasID ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( aliasID instanceof MDC1200ID )
    		{
        		getModel().addNode( new MDC1200IDNode( (MDC1200ID)aliasID ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( aliasID instanceof Min )
    		{
        		getModel().addNode( new MINNode( (Min)aliasID ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( aliasID instanceof MPT1327ID )
    		{
        		getModel().addNode( new MPT1327IDNode( (MPT1327ID)aliasID ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( aliasID instanceof SiteID )
    		{
        		getModel().addNode( new SiteIDNode( (SiteID)aliasID ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( aliasID instanceof StatusID )
    		{
        		getModel().addNode( new StatusIDNode( (StatusID)aliasID ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( aliasID instanceof TalkgroupID )
    		{
        		getModel().addNode( new TalkgroupIDNode( (TalkgroupID)aliasID ), 
        				AliasNode.this, getChildCount() );
    		}
    		else if( aliasID instanceof UniqueID )
    		{
        		getModel().addNode( new UniqueIDNode( (UniqueID)aliasID ), 
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
				
				ESNNode node = new ESNNode( esn );
				
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
				
				FleetsyncIDNode node = new FleetsyncIDNode( fs );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addFleetsyncItem );

		JMenuItem addMDCItem = new JMenuItem( "MDC-1200" );
		addMDCItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				MDC1200ID mdc = new MDC1200ID();
				
				getAlias().addAliasID( mdc );
				
				MDC1200IDNode node = new MDC1200IDNode( mdc );
				
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
				
				MINNode node = new MINNode( min );
				
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
				
				MPT1327IDNode node = new MPT1327IDNode( mpt );
				
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
				
				SiteIDNode node = new SiteIDNode( siteID );
				
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
				
				StatusIDNode node = new StatusIDNode( statusID );
				
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
				
				TalkgroupIDNode node = new TalkgroupIDNode( tg );
				
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
				
				UniqueIDNode node = new UniqueIDNode( uid );
				
				getModel().addNode( node, 
									AliasNode.this, 
									AliasNode.this.getChildCount() );
				
				node.show();
            }
		} );
		addIDMenu.add( addUniqueIDItem );

		retVal.add( addIDMenu );
		
		retVal.add(  new JSeparator() );
		
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
