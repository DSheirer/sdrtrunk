/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package alias.id;

import audio.broadcast.BroadcastModel;
import gui.editor.Editor;
import gui.editor.EmptyEditor;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import alias.Alias;
import alias.AliasEvent;
import alias.AliasEvent.Event;
import alias.AliasFactory;
import alias.AliasModel;

public class AliasIdentifierEditor extends Editor<Alias>
{
	private static final long serialVersionUID = 1L;

	private static ListModel<AliasID> EMPTY_MODEL = new DefaultListModel<>();
	private JList<AliasID> mAliasIDList = new JList<>( EMPTY_MODEL );
	private JButton mNewIDButton;
	private JButton mCloneIDButton;
	private JButton mDeleteIDButton;
	private EditorContainer mEditorContainer = new EditorContainer();

	private AliasModel mAliasModel;
    private BroadcastModel mBroadcastModel;
	
	public AliasIdentifierEditor( AliasModel aliasModel, BroadcastModel broadcastModel )
	{
		mAliasModel = aliasModel;
        mBroadcastModel = broadcastModel;

		init();
	}

	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 3", 
			"[grow,fill][grow,fill][grow,fill]", "[][grow,fill][]" ) );

		mAliasIDList.setVisibleRowCount( 6 );
		mAliasIDList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		mAliasIDList.setLayoutOrientation( JList.VERTICAL );
		mAliasIDList.addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent event )
			{
				if( !event.getValueIsAdjusting() )
				{
					JList<?> list = (JList<?>)event.getSource();
					
					Object selectedItem = list.getSelectedValue();
					
					if( selectedItem != null && selectedItem instanceof AliasID )
					{
						AliasID selected = (AliasID)selectedItem;

						mEditorContainer.setAliasID( selected );
						
						mCloneIDButton.setEnabled( true );
						mDeleteIDButton.setEnabled( true );
					}
					else
					{
						mCloneIDButton.setEnabled( false );
						mDeleteIDButton.setEnabled( false );
					}
				}
			}
		} );
		
		JScrollPane scroller = new JScrollPane( mAliasIDList );
		add( scroller, "span,grow" );
		
		add( mEditorContainer, "span,grow" );

		mNewIDButton = new JButton( "New ..." );
		mNewIDButton.setToolTipText( "Create a new Alias Identifier" );
		mNewIDButton.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JPopupMenu menu = new JPopupMenu();

				menu.add( new AddAliasIdentifierItem( AliasIDType.ESN ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.FLEETSYNC) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.LOJACK) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.LTR_NET_UID) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.MDC1200 ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.MPT1327 ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.MIN ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.SITE) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.STATUS) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.TALKGROUP) );

				menu.addSeparator();

                menu.add( new AddAliasIdentifierItem( AliasIDType.BROADCAST_CHANNEL) );

				if( hasItem() )
				{
					Alias alias = getItem();

					if( alias.isRecordable() )
					{
						menu.add( new AddAliasIdentifierItem( AliasIDType.NON_RECORDABLE) );
					}

					if( !alias.hasCallPriority() )
					{
						menu.add( new AddAliasIdentifierItem( AliasIDType.PRIORITY) );
					}
				}

				menu.show( e.getComponent(), e.getX(), e.getY() );
			}
		} );
		
		add( mNewIDButton );
		
		mCloneIDButton = new JButton( "Clone" );
		mCloneIDButton.setToolTipText( "Create a copy of the currently selected identifier" );
		mCloneIDButton.setEnabled( false );
		mCloneIDButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				AliasID selected = mAliasIDList.getSelectedValue();
				
				if( selected != null )
				{
					AliasID clone = AliasFactory.copyOf( selected );

					if( clone != null )
					{
						addAliasIDToList( clone );
					}
				}
			}
		} );
		add( mCloneIDButton );
		
		mDeleteIDButton = new JButton( "Delete" );
		mDeleteIDButton.setToolTipText( "Delete the currently selected identifier" );
		mDeleteIDButton.setEnabled( false );
		mDeleteIDButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				final AliasID selected = mAliasIDList.getSelectedValue();
				
				if( selected != null )
				{
					int choice = JOptionPane.showConfirmDialog( mDeleteIDButton, 
						"Do you want to delete [" + selected.toString() + "]", 
						"Are you sure?", JOptionPane.YES_NO_OPTION );
					
					if( choice == JOptionPane.YES_OPTION && hasItem() )
					{
						Alias alias = getItem();
						
						alias.removeAliasID( selected );
						
						mAliasModel.broadcast( new AliasEvent( alias, Event.CHANGE ) );

						setItem( alias );
					}
				}
			}
		} );
		add( mDeleteIDButton, "wrap" );
	}
	
	@Override
	public void setItem( Alias alias )
	{
		super.setItem( alias );
		
		if( alias == null || alias.getId().isEmpty() )
		{
			mAliasIDList.setModel( EMPTY_MODEL );
		}
		else
		{
			DefaultListModel<AliasID> model = new DefaultListModel<AliasID>();

			List<AliasID> ids = alias.getId();
			
			Collections.sort( ids, new Comparator<AliasID>()
			{
				@Override
				public int compare( AliasID o1, AliasID o2 )
				{
					return o1.toString().compareTo( o2.toString() );
				}
			} );
			
			for( AliasID id: ids )
			{
				model.addElement( id );
			}
			
			mAliasIDList.setModel( model );
		}
		
		mEditorContainer.setAliasID( null );
	}
	
	@Override
	public void save()
	{
		if( isModified() || mEditorContainer.isModified() )
		{
			if( mEditorContainer.isModified() )
			{
				mEditorContainer.save();
			}
			
			setModified( false );
			
			if( hasItem() )
			{
				mAliasModel.broadcast( new AliasEvent( getItem(), Event.CHANGE ) );
			}
		}
	}
	
	public class EditorContainer extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private Editor<AliasID> mEditor = new EmptyEditor<>();

		public EditorContainer()
		{
			setLayout( new MigLayout( "","[grow,fill]", "[grow,fill]" ) );

			add( mEditor );
		}
		
		public boolean isModified()
		{
			if( mEditor != null )
			{
				return mEditor.isModified();
			}
			
			return false;
		}
		
		public void save()
		{
			if( mEditor != null )
			{
				mEditor.save();
			}
		}
		
		public void setAliasID( AliasID aliasID )
		{
			if( mEditor != null )
			{
				if( mEditor.isModified() )
				{
					int option = JOptionPane.showConfirmDialog( 
							EditorContainer.this, 
							"Identifier settings have changed.  Do you want to save these changes?", 
							"Save Changes?",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE );
					
					if( option == JOptionPane.YES_OPTION )
					{
						mEditor.save();
					}
				}
			}
			removeAll();
			
			//This will always give us an editor
			mEditor = AliasFactory.getEditor( aliasID, mBroadcastModel );
			
			add( mEditor );

			revalidate();
		}
	}
	
	private void addAliasIDToList( final AliasID id )
	{
		if( id != null && hasItem() )
		{
			final Alias alias = getItem();
			
			alias.addAliasID( id );
			
			EventQueue.invokeLater( new Runnable()
			{
				@Override
				public void run()
				{
					setItem( alias );
					
					mAliasIDList.setSelectedValue( id, true );
					
					AliasIdentifierEditor.this.setModified( true );
				}
			} );
		}
	}
	
	public class AddAliasIdentifierItem extends JMenuItem
	{
		private static final long serialVersionUID = 1L;
		
		private AliasIDType mAliasIDType;

		public AddAliasIdentifierItem( AliasIDType type )
		{
			super( type.toString() );
			
			mAliasIDType = type;
			
			addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					final AliasID id = AliasFactory.getAliasID( mAliasIDType );
					
					addAliasIDToList( id );
				}
			} );
		}
	}
}
