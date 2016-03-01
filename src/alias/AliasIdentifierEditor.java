package alias;

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

import sample.Listener;
import alias.AliasEvent.Event;

public class AliasIdentifierEditor extends AliasConfigurationEditor
{
	private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( AliasIdentifierEditor.class );

	private static ListModel<AliasID> EMPTY_MODEL = new DefaultListModel<>();
	private JList<AliasID> mAliasIDList = new JList<>( EMPTY_MODEL );
	private EditorContainer mEditorContainer = new EditorContainer();

	private Alias mAlias;
	private AliasModel mAliasModel;
	
	public AliasIdentifierEditor( AliasModel model )
	{
		mAliasModel = model;
		init();
	}

	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 3", 
			"[grow,fill][grow,fill][grow,fill]", "[][grow,fill][]" ) );

		mAliasIDList.setVisibleRowCount( 4 );
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
					}
				}
			}
		} );
		
		JScrollPane scroller = new JScrollPane( mAliasIDList );
		add( scroller, "span,grow" );
		
		add( mEditorContainer, "span,grow" );

		final JButton newButton = new JButton( "New ..." );
		
		newButton.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JPopupMenu menu = new JPopupMenu();

				menu.add( new AddAliasIdentifierItem( AliasIDType.ESN ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.Fleetsync ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.LoJack ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.LTRNetUID ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.MDC1200 ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.MPT1327 ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.MIN ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.Site ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.Status ) );
				menu.add( new AddAliasIdentifierItem( AliasIDType.Talkgroup ) );
				menu.addSeparator();
				menu.add( new AddAliasIdentifierItem( AliasIDType.Priority ) );
				menu.addSeparator();
				menu.add( new AddAliasIdentifierItem( AliasIDType.NonRecordable ) );
				
				menu.show( e.getComponent(), e.getX(), e.getY() );
			}
		} );
		
		add( newButton );
		add( new JButton( "Copy" ) );
		add( new JButton( "Delete" ), "wrap" );
	}
	
	@Override
	public void setAlias( Alias alias )
	{
		mAlias = alias;
		
		if( mAlias == null || mAlias.getId().isEmpty() )
		{
			mAliasIDList.setModel( EMPTY_MODEL );
		}
		else
		{
			DefaultListModel<AliasID> model = new DefaultListModel<AliasID>();

			List<AliasID> ids = mAlias.getId();
			
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
		mAliasModel.broadcast( new AliasEvent( mAlias, Event.CHANGE ) );
	}
	
	public class EditorContainer extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private ComponentEditor<AliasID> mEditor = new EmptyAliasIDEditor();

		public EditorContainer()
		{
			setLayout( new MigLayout( "","[grow,fill]", "[grow,fill]" ) );

			add( mEditor );
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
			
			mEditor = AliasFactory.getEditor( aliasID );
			
			add( mEditor );

			revalidate();
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
					
					if( id != null && mAlias != null )
					{
						mAlias.addAliasID( id );
						
						EventQueue.invokeLater( new Runnable()
						{
							@Override
							public void run()
							{
								setAlias( mAlias );
								
								mAliasIDList.setSelectedValue( id, true );
							}
						} );
					}
				}
			} );
		}
	}
}
