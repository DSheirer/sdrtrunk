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
package alias.action;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.Alias;
import alias.AliasEvent;
import alias.AliasEvent.Event;
import alias.AliasFactory;
import alias.AliasModel;

public class AliasActionEditor extends Editor<Alias>
{
	private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( AliasActionEditor.class );

	private static ListModel<AliasAction> EMPTY_MODEL = new DefaultListModel<>();
	private JList<AliasAction> mAliasActionList = new JList<>( EMPTY_MODEL );
	private JButton mNewActionButton;
	private JButton mCloneActionButton;
	private JButton mDeleteActionButton;
	private EditorContainer mEditorContainer = new EditorContainer();

	private AliasModel mAliasModel;
	
	public AliasActionEditor( AliasModel model )
	{
		mAliasModel = model;
		init();
	}

	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 3", 
			"[grow,fill][grow,fill][grow,fill]", "[][grow,fill][]" ) );

		mAliasActionList.setVisibleRowCount( 6 );
		mAliasActionList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		mAliasActionList.setLayoutOrientation( JList.VERTICAL );
		mAliasActionList.addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent event )
			{
				if( !event.getValueIsAdjusting() )
				{
					JList<?> list = (JList<?>)event.getSource();
					
					Object selectedItem = list.getSelectedValue();
					
					if( selectedItem != null && selectedItem instanceof AliasAction )
					{
						AliasAction selected = (AliasAction)selectedItem;

						mEditorContainer.setAliasAction( selected );
						
						mCloneActionButton.setEnabled( true );
						mDeleteActionButton.setEnabled( true );
					}
					else
					{
						mCloneActionButton.setEnabled( false );
						mDeleteActionButton.setEnabled( false );
					}
				}
			}
		} );
		
		JScrollPane scroller = new JScrollPane( mAliasActionList );
		add( scroller, "span,grow" );
		
		add( mEditorContainer, "span,grow" );

		mNewActionButton = new JButton( "New ..." );
		mNewActionButton.setToolTipText( "Create a new Alias Action" );
		mNewActionButton.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JPopupMenu menu = new JPopupMenu();

				menu.add( new AddAliasActionItem( AliasActionType.BEEP ) );
				menu.add( new AddAliasActionItem( AliasActionType.CLIP ) );
				menu.add( new AddAliasActionItem( AliasActionType.SCRIPT ) );
				
				menu.show( e.getComponent(), e.getX(), e.getY() );
			}
		} );
		
		add( mNewActionButton );
		
		mCloneActionButton = new JButton( "Clone" );
		mCloneActionButton.setToolTipText( "Create a copy of the currently selected action" );
		mCloneActionButton.setEnabled( false );
		mCloneActionButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				AliasAction selected = mAliasActionList.getSelectedValue();
				
				if( selected != null )
				{
					AliasAction clone = AliasFactory.copyOf( selected );

					if( clone != null )
					{
						addAliasActionToList( clone );
					}
				}
			}
		} );
		add( mCloneActionButton );
		
		mDeleteActionButton = new JButton( "Delete" );
		mDeleteActionButton.setToolTipText( "Delete the currently selected action" );
		mDeleteActionButton.setEnabled( false );
		mDeleteActionButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				final AliasAction selected = mAliasActionList.getSelectedValue();
				
				if( selected != null )
				{
					int choice = JOptionPane.showConfirmDialog( mDeleteActionButton, 
						"Do you want to delete [" + selected.toString() + "]", 
						"Are you sure?", JOptionPane.YES_NO_OPTION );
					
					if( choice == JOptionPane.YES_OPTION && hasItem() )
					{
						Alias alias = getItem();
						
						alias.removeAliasAction( selected );
						
						mAliasModel.broadcast( new AliasEvent( alias, Event.CHANGE ) );

						setItem( alias );
					}
				}
			}
		} );
		add( mDeleteActionButton, "wrap" );
	}
	
	@Override
	public void setItem( Alias alias )
	{
		super.setItem( alias );
		
		if( alias == null || alias.getAction().isEmpty() )
		{
			mAliasActionList.setModel( EMPTY_MODEL );
		}
		else
		{
			DefaultListModel<AliasAction> model = new DefaultListModel<>();

			List<AliasAction> actions = alias.getAction();
			
			Collections.sort( actions, new Comparator<AliasAction>()
			{
				@Override
				public int compare( AliasAction o1, AliasAction o2 )
				{
					return o1.toString().compareTo( o2.toString() );
				}
			} );
			
			for( AliasAction action: actions )
			{
				model.addElement( action );
			}
			
			mAliasActionList.setModel( model );
		}
		
		mEditorContainer.setAliasAction( null );
	}
	
	@Override
	public void save()
	{
		if( mEditorContainer.isModified() )
		{
			mEditorContainer.save();
			
			if( hasItem() )
			{
				mAliasModel.broadcast( new AliasEvent( getItem(), Event.CHANGE ) );
			}
		}
	}
	
	public class EditorContainer extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private Editor<AliasAction> mEditor = new EmptyEditor<>();

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
		
		public void setAliasAction( AliasAction aliasAction )
		{
			if( mEditor != null )
			{
				if( mEditor.isModified() )
				{
					int option = JOptionPane.showConfirmDialog( 
							EditorContainer.this, 
							"Action settings have changed.  Do you want to save these changes?", 
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
			mEditor = AliasFactory.getEditor( aliasAction );
			
			add( mEditor );

			revalidate();
		}
	}
	
	private void addAliasActionToList( final AliasAction action )
	{
		if( action != null && hasItem() )
		{
			final Alias alias = getItem();
			
			alias.addAliasAction( action );
			
			EventQueue.invokeLater( new Runnable()
			{
				@Override
				public void run()
				{
					setItem( alias );
					
					mAliasActionList.setSelectedValue( action, true );
				}
			} );
		}
	}
	
	public class AddAliasActionItem extends JMenuItem
	{
		private static final long serialVersionUID = 1L;
		
		private AliasActionType mAliasActionType;

		public AddAliasActionItem( AliasActionType type )
		{
			super( type.toString() );
			
			mAliasActionType = type;
			
			addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					final AliasAction action = 
							AliasFactory.getAliasAction( mAliasActionType );
					
					addAliasActionToList( action );
				}
			} );
		}
	}
}
