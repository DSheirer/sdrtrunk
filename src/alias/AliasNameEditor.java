package alias;

import gui.editor.Editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasEvent.Event;
import map.IconManager;
import map.MapIcon;
import map.MapIconListCellRenderer;
import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;

public class AliasNameEditor extends Editor<Alias>
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( AliasNameEditor.class );

	private static ComboBoxModel<String> EMPTY_MODEL = new DefaultComboBoxModel<>();

	private JComboBox<String> mListCombo = new JComboBox<>( EMPTY_MODEL );
	private JComboBox<String> mGroupCombo = new JComboBox<>( EMPTY_MODEL );
    private JComboBox<MapIcon> mMapIconCombo;
    private JTextField mName;
    private JButton mButtonColor;
    private JButton mBtnIconManager;
    
    private AliasModel mAliasModel;
    private SettingsManager mSettingsManager;
    
    public AliasNameEditor( AliasModel aliasModel, SettingsManager settingsManager )
	{
    	mAliasModel = aliasModel;
    	mSettingsManager = settingsManager;
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][grow,fill]", 
				"[][][][][][][grow]" ) );

    	add( new JLabel( "Name:" ) );
    	mName = new JTextField();
    	mName.getDocument().addDocumentListener( new DocumentListener()
		{
			@Override
			public void removeUpdate( DocumentEvent e )
			{
				setModified( true );
			}
			
			@Override
			public void insertUpdate( DocumentEvent e )
			{
				setModified( true );
			}
			
			@Override
			public void changedUpdate( DocumentEvent e ){}
		} );
    	add( mName, "wrap" );

		add( new JLabel( "List:" ) );
		mListCombo.setEditable( true );
		mListCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if( mListCombo.getSelectedItem() != null )
				{
					List<String> groups = mAliasModel
						.getGroupNames( (String)mListCombo.getSelectedItem() );
					
					if( groups.isEmpty() )
					{
						mGroupCombo.setModel( EMPTY_MODEL );
					}
					else
					{
						mGroupCombo.setModel( new DefaultComboBoxModel<String>( 
							groups.toArray( new String[ groups.size() ] ) ) );;
					}
				}
				
				setModified( true );
			}
		} );
		add( mListCombo, "wrap" );

		add( new JLabel( "Group:" ) );
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

    	add( new JLabel( "Color:" ) );

		mButtonColor = new JButton( "Select ..." );
		mButtonColor.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Color newColor = JColorChooser.showDialog(
	                     AliasNameEditor.this,
	                     "Choose color for this alias",
	                     ( hasItem() ? getItem().getMapColor() : null ) );

				if( newColor != null )
				{
					mButtonColor.setForeground( newColor );
					mButtonColor.setBackground( newColor );
					
					setModified( true );
				}
            }
		} );
		add( mButtonColor, "wrap" );
		
		add( new JLabel( "Icon:" ) );
		
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
						new IconManager( mSettingsManager, AliasNameEditor.this );
				
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
		
		add( mBtnIconManager, "span 2,wrap" );

		setModified( false );
	}

	@Override
	public void setItem( Alias alias )
	{
		super.setItem( alias );
		
    	if( hasItem() )
    	{
    		mName.setText( alias.getName() );
    		
			List<String> listNames = mAliasModel.getListNames();
			
			if( listNames.isEmpty() )
			{
				mListCombo.setModel( EMPTY_MODEL );
			}
			else
			{
				mListCombo.setModel( new DefaultComboBoxModel<String>( 
						listNames.toArray( new String[ listNames.size() ] ) ) );;
			}
			
			mListCombo.setSelectedItem( alias.getList() );
			
			List<String> groupNames = mAliasModel.getGroupNames( alias.getList() );
			
			if( groupNames.isEmpty() )
			{
				mGroupCombo.setModel( EMPTY_MODEL );
			}
			else
			{
				mGroupCombo.setModel( new DefaultComboBoxModel<String>( 
						groupNames.toArray( new String[ groupNames.size() ] ) ) );;
			}
			
			mGroupCombo.setSelectedItem( alias.getGroup() );
			
			Color color = alias.getMapColor();
			
			mButtonColor.setBackground( color );
			mButtonColor.setForeground( color );
			
			String iconName = alias.getIconName();
			
			if( iconName == null )
			{
				iconName = SettingsManager.DEFAULT_ICON;
			}
			
			MapIcon savedIcon = mSettingsManager.getMapIcon( iconName );
	
			if( savedIcon != null )
			{
				mMapIconCombo.setSelectedItem( savedIcon );
			}
    	}
    	else
    	{
    		mListCombo.setModel( EMPTY_MODEL );
    		mGroupCombo.setModel( EMPTY_MODEL );
    		mName.setText( null );
    		
			mButtonColor.setBackground( getBackground() );
			mButtonColor.setForeground( getForeground() );
    	}
    	
    	setModified( false );
	}

	@Override
	public void save()
	{
		if( hasItem() && isModified() )
		{
			Alias alias = getItem();
			
			if( mListCombo.getSelectedItem() != null )
			{
				alias.setList( (String)mListCombo.getSelectedItem() );
			}
			
			if( mGroupCombo.getSelectedItem() != null )
			{
				alias.setGroup( (String)mGroupCombo.getSelectedItem() );
			}

			alias.setName( mName.getText() );

			alias.setColor( mButtonColor.getBackground().getRGB() );

			if( mMapIconCombo.getSelectedItem() != null )
			{
				alias.setIconName( ((MapIcon)mMapIconCombo.getSelectedItem()).getName() );
			}
			
			//Broadcast an alias change event to save the updates
			mAliasModel.broadcast( new AliasEvent( getItem(), Event.CHANGE ) ); 
		}
		
		setModified( false );
	}
}
