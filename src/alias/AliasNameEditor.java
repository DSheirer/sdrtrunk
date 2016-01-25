package alias;

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

import map.IconManager;
import map.MapIcon;
import map.MapIconListCellRenderer;
import net.miginfocom.swing.MigLayout;
import settings.SettingsManager;

public class AliasNameEditor extends AliasConfigurationEditor
{
	private static final long serialVersionUID = 1L;
	
	private static ComboBoxModel<String> EMPTY_MODEL = new DefaultComboBoxModel<>();

	private JComboBox<String> mListCombo = new JComboBox<>( EMPTY_MODEL );
	private JComboBox<String> mGroupCombo = new JComboBox<>( EMPTY_MODEL );
    private JComboBox<MapIcon> mMapIconCombo;
    private JTextField mName;
    private JButton mButtonColor;
    private JButton mBtnIconManager;
    
    private Alias mAlias;
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
			}
		} );
		add( mListCombo, "wrap" );

		add( new JLabel( "Group:" ) );
		mGroupCombo.setEditable( true );
    	add( mGroupCombo, "wrap" );

    	add( new JLabel( "Name:" ) );
    	mName = new JTextField();
    	add( mName, "wrap" );

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
	                     ( mAlias != null ? mAlias.getMapColor() : null ) );

				if( newColor != null )
				{
					mButtonColor.setForeground( newColor );
					mButtonColor.setBackground( newColor );
				}
            }
		} );
		add( mButtonColor, "wrap" );
		
		add( new JLabel( "Icon:" ) );
		
		mMapIconCombo = new JComboBox<MapIcon>( mSettingsManager.getMapIcons() );

		MapIconListCellRenderer renderer = new MapIconListCellRenderer();
		renderer.setPreferredSize( new Dimension( 200, 30 ) );
		mMapIconCombo.setRenderer( renderer );

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
		
	}

	@Override
	public void setAlias( Alias alias )
	{
    	mAlias = alias;

    	if( mAlias != null )
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
			
			mListCombo.setSelectedItem( mAlias.getList() );
			
			List<String> groupNames = mAliasModel.getGroupNames( mAlias.getList() );
			
			if( groupNames.isEmpty() )
			{
				mGroupCombo.setModel( EMPTY_MODEL );
			}
			else
			{
				mGroupCombo.setModel( new DefaultComboBoxModel<String>( 
						groupNames.toArray( new String[ groupNames.size() ] ) ) );;
			}
			
			mGroupCombo.setSelectedItem( mAlias.getGroup() );
			
			Color color = mAlias.getMapColor();
			
			mButtonColor.setBackground( color );
			mButtonColor.setForeground( color );
			
			String iconName = mAlias.getIconName();
			
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
	}

	@Override
	public void save()
	{
		if( mAlias != null )
		{
			if( mListCombo.getSelectedItem() != null )
			{
				mAlias.setList( (String)mListCombo.getSelectedItem() );
			}
			
			if( mGroupCombo.getSelectedItem() != null )
			{
				mAlias.setGroup( (String)mGroupCombo.getSelectedItem() );
			}

			mAlias.setName( mName.getText() );

			mAlias.setColor( mButtonColor.getBackground().getRGB() );

			if( mMapIconCombo.getSelectedItem() != null )
			{
				mAlias.setIconName( ((MapIcon)mMapIconCombo.getSelectedItem()).getName() );
			}
		}
	}
}
