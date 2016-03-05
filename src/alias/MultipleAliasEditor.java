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
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasEvent.Event;
import alias.id.priority.Priority;
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
	private JCheckBox mRecordCheckBox = new JCheckBox( "Record" );
    private JComboBox<Record> mRecordActionCombo = new JComboBox<>( Record.values() );
	private JCheckBox mPriorityCheckBox = new JCheckBox( "Priority" );
    private JSlider mPrioritySlider;
    private JLabel mPrioritySliderLabel;
	
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
				"[][][][][][][][][][][][][grow,fill]" ) );
		
		mAliasCountLabel = new JLabel( "Alias:" );
		add( mAliasCountLabel );
		
		mAliasCount = new JLabel( "Multiple" );
		add( mAliasCount, "span" );
		
		add( new JSeparator(), "span" );
		
		add( new JLabel( HELP_TEXT ), "span" );
		
		add( mListCheckBox );

		mListCombo.setEditable( true );
		add( mListCombo, "wrap" );

		add( mGroupCheckBox );

		mGroupCombo.setEditable( true );
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
				}
            }
		} );
		add( mButtonColor, "wrap" );
		
		add( mIconCheckBox );
		
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
		
		add( mRecordCheckBox );
		add( mRecordActionCombo );

		//Placeholder
		add( new JLabel( " " ) );
		mPrioritySliderLabel = new JLabel( "Priority: " + Priority.MIN_PRIORITY );
		add( mPrioritySliderLabel );
		
		add( mPriorityCheckBox );
		
		mPrioritySlider = new JSlider( JSlider.HORIZONTAL,
									   Priority.MIN_PRIORITY,
									   Priority.MAX_PRIORITY + 1,
									   Priority.MIN_PRIORITY );

		mPrioritySlider.setMajorTickSpacing( 20 );
		mPrioritySlider.setMinorTickSpacing( 5 );
		mPrioritySlider.setPaintTicks( true );
		mPrioritySlider.setLabelTable( mPrioritySlider.createStandardLabels( 20, 20 ) );
		mPrioritySlider.setPaintLabels( true );
		mPrioritySlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				int priority = mPrioritySlider.getValue();

				if( priority > Priority.MAX_PRIORITY )
				{
					mPrioritySliderLabel.setText( "Priority: Do Not Monitor" );
				}
				else
				{
					mPrioritySliderLabel.setText( "Priority: " + mPrioritySlider.getValue() );
				}
			}
		} );
		mPrioritySlider.setToolTipText( HELP_TEXT );
		
		add( mPrioritySlider );
		
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
				reset();
			}
		} );
		add( resetButton );

		setModified( false );
	}
	
	@Override
	public void setItem( List<Alias> item )
	{
		super.setItem( item );

		StringBuilder sb = new StringBuilder();
		sb.append( "Multiple Selected [ " );
		
		if( hasItem() )
		{
			sb.append( String.valueOf( getAliases().size() ) );
		}
		else
		{
			sb.append( "0" );
		}

		sb.append( " ]" );
		
		mAliasCount.setText( sb.toString() );

		List<String> listNames = mAliasModel.getListNames();
		listNames.add( 0, "" );
		mListCombo.setModel( new DefaultComboBoxModel<String>( 
				listNames.toArray( new String[ listNames.size() ] ) ) );
				
		List<String> groupNames = mAliasModel.getGroupNames();
		groupNames.add( 0, "" );
		mGroupCombo.setModel( new DefaultComboBoxModel<String>( 
				groupNames.toArray( new String[ groupNames.size() ] ) ) );
		
		setModified( false );
	}

	@Override
	public void reset()
	{
		//Reset multi-editor controls
		mListCheckBox.setSelected( false );
		mGroupCheckBox.setSelected( false );
		mColorCheckBox.setSelected( false );
		mIconCheckBox.setSelected( false );
		mRecordCheckBox.setSelected( false );
		mPriorityCheckBox.setSelected( false );
		
		List<String> listNames = mAliasModel.getListNames();
		listNames.add( 0, "" );
		mListCombo.setModel( new DefaultComboBoxModel<String>( 
				listNames.toArray( new String[ listNames.size() ] ) ) );
				
		List<String> groupNames = mAliasModel.getGroupNames();
		groupNames.add( 0, "" );
		mGroupCombo.setModel( new DefaultComboBoxModel<String>( 
				groupNames.toArray( new String[ groupNames.size() ] ) ) );
		
		mButtonColor.setForeground( getForeground() );
		mButtonColor.setBackground( getBackground() );
		
		super.reset();
	}
	
	private boolean hasRequestedChanges()
	{
		return  mListCheckBox.isSelected() ||
				mGroupCheckBox.isSelected() ||
				mColorCheckBox.isSelected() ||
				mIconCheckBox.isSelected() ||
				mRecordCheckBox.isSelected() ||
				mPriorityCheckBox.isSelected();
	}

	@Override
	public void save()
	{
		if( hasRequestedChanges() && hasItem() )
		{
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
				
				if( mRecordCheckBox.isSelected() )
				{
					Record action = 
						(Record)mRecordActionCombo.getSelectedItem();

					alias.setRecordable( action == Record.RECORDABLE );
				}
				
				if( mPriorityCheckBox.isSelected() )
				{
					int priority = mPrioritySlider.getValue();

					//This is a work-around -- we use max priority + 1 in the 
					//gui to indicate do not monitor, and change the value here
					if( priority == Priority.MAX_PRIORITY + 1 )
					{
						priority = Priority.DO_NOT_MONITOR;
					}
					
					alias.setCallPriority( priority );
				}
			}

			for( Alias alias: aliases )
			{
				//Broadcast an alias change event to save the updates
				mAliasModel.broadcast( new AliasEvent( alias, Event.CHANGE ) ); 
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
	
	public enum Record
	{
		NON_RECORDABLE( "Non-Recordable" ),
		RECORDABLE( "Recordable" );
		
		private String mLabel;
		
		private Record( String label )
		{
			mLabel = label;
		}
		
		public String toString()
		{
			return mLabel;
		}
	}
}
