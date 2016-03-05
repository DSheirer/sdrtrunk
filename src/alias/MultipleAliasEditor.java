package alias;

import gui.editor.Editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasEvent.Event;
import map.IconManager;
import map.MapIcon;
import map.MapIconListCellRenderer;
import net.miginfocom.swing.MigLayout;
import sample.Listener;
import settings.SettingsManager;

public class MultipleAliasEditor extends Editor<List<Alias>> 
					implements Listener<AliasEvent>
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( MultipleAliasEditor.class );
	
	private static String HELP_TEXT = 
		"<html>Select attributes below to change for all selected aliases</html>";

	private JLabel mAliasCountLabel;
	private JLabel mAliasCount;

	private JCheckBox mListCheckBox = new JCheckBox( "List" );
	private JComboBox<String> mListCombo = new JComboBox<>();
	private JCheckBox mGroupCheckBox = new JCheckBox( "Group" );
	private JComboBox<String> mGroupCombo = new JComboBox<>();
	private JCheckBox mIconCheckBox = new JCheckBox( "Icon" );
    private JComboBox<MapIcon> mMapIconCombo;
	private JCheckBox mColorCheckBox = new JCheckBox( "Color" );
    private JButton mButtonColor;
    private JButton mBtnIconManager;
	
	private AliasModel mAliasModel;
	private SettingsManager mSettingsManager;
	
	public MultipleAliasEditor( AliasModel aliasModel, SettingsManager settingsManager )
	{
		mAliasModel = aliasModel;
		mAliasModel.addListener( this );
		
		mSettingsManager = settingsManager;
		
		init();
	}
	
	public List<Alias> getAliases()
	{
		if( hasItem() )
		{
			return (List<Alias>)getItem();
		}
		
		return null;
	}
	
	public void init()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[grow,fill][grow,fill]", 
				"[][][][][][][][][][grow,fill]" ) );
		
		mAliasCountLabel = new JLabel( "Alias:" );
		add( mAliasCountLabel );
		
		mAliasCount = new JLabel( "Multiple" );
		add( mAliasCount, "span" );
		
		add( new JSeparator(), "span" );
		
		add( new JLabel( HELP_TEXT ), "span" );
		
		add( mListCheckBox );

		mListCombo.setEditable( true );
		mListCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		add( mListCombo, "wrap" );

		add( mGroupCheckBox );

		mGroupCombo.setEditable( true );
		mGroupCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
    	add( mGroupCombo, "wrap" );

    	add( mColorCheckBox );

		mButtonColor = new JButton( "Select ..." );
		mButtonColor.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Color newColor = JColorChooser.showDialog(
	                     MultipleAliasEditor.this,
	                     "Choose color for this alias", null );

				if( newColor != null )
				{
					mButtonColor.setForeground( newColor );
					mButtonColor.setBackground( newColor );
					
					setModified( true );
				}
            }
		} );
		add( mButtonColor, "wrap" );
		
		add( mIconCheckBox );
		
		mMapIconCombo = new JComboBox<MapIcon>( mSettingsManager.getMapIcons() );

		MapIconListCellRenderer renderer = new MapIconListCellRenderer();
		renderer.setPreferredSize( new Dimension( 200, 30 ) );
		mMapIconCombo.setRenderer( renderer );
		mMapIconCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );

		add( mMapIconCombo, "wrap" );

		//Dummy place holder
		add( new JLabel() );
		
		mBtnIconManager = new JButton( "Icon Manager" );
		mBtnIconManager.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent arg0 )
            {
				final IconManager iconManager = 
						new IconManager( mSettingsManager, MultipleAliasEditor.this );
				
				EventQueue.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						iconManager.setVisible( true );
					}
				} );
            }
		} );
		
		add( mBtnIconManager, "span,wrap" );
		
		JButton saveButton = new JButton( "Save" );
		saveButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				save();
			}
		} );
		add( saveButton );

		JButton resetButton = new JButton( "Reset" );
		resetButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( false );
				setItem( getAliases() );
			}
		} );
		add( resetButton );

		setModified( false );
	}
	
	@Override
	public void setItem( List<Alias> item )
	{
		mLog.debug( "setItem invoked" );
		super.setItem( item );

		StringBuilder sb = new StringBuilder();
		sb.append( "Multiple [" );
		
		if( hasItem() )
		{
			sb.append( String.valueOf( getAliases().size() ) );

			List<String> listNames = mAliasModel.getListNames();
			listNames.add( 0, "" );
			mListCombo.setModel( new DefaultComboBoxModel<String>( 
					listNames.toArray( new String[ listNames.size() ] ) ) );
					
			List<String> groupNames = mAliasModel.getGroupNames();
			groupNames.add( 0, "" );
			mGroupCombo.setModel( new DefaultComboBoxModel<String>( 
					groupNames.toArray( new String[ groupNames.size() ] ) ) );
		}
		else
		{
			sb.append( "0" );
		}

		sb.append( "]" );
		
		mAliasCount.setText( sb.toString() );

		setModified( false );
	}

	@Override
	public void save()
	{
		if( isModified() && hasItem() )
		{
			setModified( false );
			
			List<Alias> aliases = getAliases();
			
			for( Alias alias: aliases )
			{
				if( mListCheckBox.isSelected() )
				{
					String list = null;
					
					if( mListCombo.getSelectedItem() != null )
					{
						list = (String)mListCombo.getSelectedItem();
					}
					
					alias.setList( list );
				}
				
				if( mGroupCheckBox.isSelected() )
				{
					String group = null;
					
					if( mGroupCombo.getSelectedItem() != null )
					{
						group = (String)mGroupCombo.getSelectedItem();
					}
					
					mLog.debug( "Group is:" + group );
					alias.setGroup( group );
				}
				
				if( mColorCheckBox.isSelected() )
				{
					alias.setColor( mButtonColor.getForeground().getRGB() );
				}
				
				if( mIconCheckBox.isSelected() )
				{
					if( mMapIconCombo.getSelectedItem() != null )
					{
						alias.setIconName( ((MapIcon)mMapIconCombo.getSelectedItem()).getName() );
					}
					else
					{
						alias.setIconName( null );
					}
				}
			}

			if( mListCheckBox.isSelected() ||
					mGroupCheckBox.isSelected() ||
					mColorCheckBox.isSelected() ||
					mIconCheckBox.isSelected() )
			{
				mLog.debug( "Broadcasting change event" );

				for( Alias alias: aliases )
				{
					mLog.debug( "Group:" + alias.getGroup() );
					//Broadcast an alias change event to save the updates
					mAliasModel.broadcast( new AliasEvent( alias, Event.CHANGE ) ); 
				}
			}
		}
	}

	@Override
	public void receive( AliasEvent event )
	{
		if( event.getEvent() == Event.DELETE && 
			hasItem() &&
			getAliases().contains( event.getAlias() ) )
		{
			getAliases().remove( event.getAlias() );
			setItem( getAliases() );
		}
	}
}
