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
package map;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;

public class IconManager extends JFrame 
							  implements ActionListener, SettingChangeListener
											
{
	public enum Mode { NORMAL, NEW, EDIT }

	private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( IconManager.class );
    
    private static final String sMSG_NEW = "Type a name for the new icon, "
		+ "select the file, and click Save,\nor Cancel to reset";
    private static final String sMSG_EDIT = "Change the name for the selected \n"
		+ "icon and/or the file, and click Save\nto update, or Cancel to reset";
    private static final String sMSG_NORMAL = "Select an icon to edit, or \n"
		+ "click New to add an icon";

    private JTextField mIconName;
    private JTextField mIconPath;
    private JButton mButtonLeft = new JButton( "New" );
    private JButton mButtonRight = new JButton( "Edit" );
    private JButton mButtonFile = new JButton( "File" );
    private JTextArea mInstructionLabel = new JTextArea();
    private JList<MapIcon> mIconList;
    private JFileChooser mFileChooser;
    private Mode mMode = Mode.NORMAL;
    
    private SettingsManager mSettingsManager;
    
	public IconManager( SettingsManager settingsManager, Component displayOver )
	{
		mSettingsManager = settingsManager;

		init( displayOver );
	}
	
	private void init( Component displayOver )
	{
		setTitle( "Icon Manager" );
		setLocationRelativeTo( displayOver );
		setSize( 400, 400 );
    	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    	setLayout( new MigLayout( "", "[grow,fill]", "[grow,fill]" ) );

    	/**
    	 * Editor Panel
    	 */
		JPanel editorPanel = new JPanel();
		editorPanel.setLayout( new MigLayout( "", "", "[][][][grow,fill]" ) );

		mButtonFile.addActionListener( this );
		editorPanel.add( mButtonFile, "align right" );
		
		mIconPath = new JTextField();
		editorPanel.add( mIconPath, "grow,wrap" );

		editorPanel.add( new JLabel( "Name:" ), "align right" );
		
		mIconName = new JTextField();
		editorPanel.add( mIconName, "grow,wrap" );
		
		mButtonLeft.addActionListener( this );
		editorPanel.add( mButtonLeft );

		mButtonRight.addActionListener( this );
		mButtonRight.setEnabled( false );
		editorPanel.add(  mButtonRight, "wrap" );
		
		mInstructionLabel.setText( sMSG_NORMAL );
		mInstructionLabel.setRows( 3 );
		mInstructionLabel.setBackground( getBackground() );
		editorPanel.add(  mInstructionLabel, "span,grow,wrap" );
		
		/**
		 * MapIcon list
		 */
		MapIcon[] icons = mSettingsManager.getMapIcons();
		Arrays.sort( icons );
		
    	mIconList = new JList<MapIcon>( icons );

    	MapIconListCellRenderer renderer = new MapIconListCellRenderer();
		renderer.setPreferredSize( new Dimension( 200, 30 ) );
		mIconList.setCellRenderer( renderer );
		
		mIconList.addListSelectionListener( new ListSelectionListener() 
		{
			@Override
            public void valueChanged( ListSelectionEvent event )
            {
				/**
				 * Refresh the display by setting the mode to normal.  If we 
				 * were in editing or new mode, and the user clicks in the list
				 * throw away the changes and set the mode to normal
				 */
				setMode( Mode.NORMAL );
            }
		} );
		
		mIconList.addMouseListener( new MouseListener() 
		{
			@Override
            public void mouseClicked( MouseEvent event )
            {
				if( event.getButton() == MouseEvent.BUTTON3 )
				{
					//Select the item
					mIconList.setSelectedIndex( 
							mIconList.locationToIndex( event.getPoint() ) );

					//Determine if it can be deleted
					final MapIcon icon = mIconList.getSelectedValue();
					
					if( icon != null )
					{
						JPopupMenu context = new JPopupMenu();
						
						JMenuItem setDefault = new JMenuItem( "Set As Default" );

						setDefault.addActionListener( new ActionListener() 
						{
							@Override
                            public void actionPerformed( ActionEvent e )
                            {
								mSettingsManager.setDefaultIcon( icon );
								
								refreshList();
                            }
						} );
						
						context.add(  setDefault );
						
						if( icon.isEditable() )
						{
							JMenuItem deleteItem = new JMenuItem( "Delete Icon" );
							deleteItem.addActionListener( new ActionListener() 
							{
								@Override
	                            public void actionPerformed( ActionEvent arg0 )
	                            {
									mSettingsManager.deleteMapIcon( icon );
	                            }
							} );
							
							context.add(  deleteItem );
						}
						
						context.show( mIconList, event.getX(), event.getY() );
					}
				}
            }

			//Unused
            public void mouseEntered( MouseEvent arg0 ) {}
            public void mouseExited( MouseEvent arg0 ) {}
            public void mousePressed( MouseEvent arg0 ) {}
            public void mouseReleased( MouseEvent arg0 ) {}
		} );

		JScrollPane scroller = new JScrollPane( mIconList );

		/**
		 * Splitpane to hold editor and icon list
		 */
		JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, 
											   scroller, 
											   editorPanel );
		splitPane.setResizeWeight( 0.7D );
		
		add( splitPane );
	}
	
	private void setMode( Mode mode )
	{
		MapIcon selected = mIconList.getSelectedValue();

		switch( mode )
		{
			case NORMAL:
				setTitle( "Icon Manager" );
				mButtonLeft.setText( "New" );
				mButtonRight.setText( "Edit" );
				
				if( selected != null )
				{
					if( selected.isEditable() )
					{
						mButtonRight.setEnabled( true );
						mIconName.setText( selected.getName() );
						mIconPath.setText( selected.getPath() );
					}
					else
					{
						mButtonRight.setEnabled( false );
						mIconName.setText( selected.getName() + " [NO EDIT]" );
						mIconPath.setText( null );
					}
				}
				else
				{
					mButtonRight.setEnabled( false );
					mIconName.setText( null );
					mIconPath.setText( null );
				}
				mInstructionLabel.setText( sMSG_NORMAL );
				break;
			case EDIT:
				if( selected != null )
				{
					setTitle( "Icon Manager - editing [" + selected.getName() + "]" );
				}
				else
				{
					setTitle( "Icon Manager - edit [*]" );
				}
				mButtonLeft.setText( "Save" );
				mButtonRight.setText( "Cancel" );
				mButtonRight.setEnabled( true );
				mInstructionLabel.setText( sMSG_EDIT );
				break;
			case NEW:
				setTitle( "Icon Manager - add new icon" );
				mButtonLeft.setText( "Save" );
				mButtonRight.setText( "Cancel" );
				mButtonRight.setEnabled( true );
				mIconName.setText( null );
				mIconPath.setText( null );
				mInstructionLabel.setText( sMSG_NEW );
				break;
		}
		
		mMode = mode;
		
		repaint();
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		if( e.getSource() == mButtonFile )
		{
	        if ( mFileChooser == null ) 
	        {
	            mFileChooser = new JFileChooser();
	 
		        //Add a custom file filter and disable the default
	            mFileChooser.addChoosableFileFilter( new ImageFilter() );
	            mFileChooser.setAcceptAllFileFilterUsed( false );
	        }
	 
	        int returnVal = mFileChooser.showDialog( this, "Select" );
	 
	        if ( returnVal == JFileChooser.APPROVE_OPTION ) 
	        {
	            File file = mFileChooser.getSelectedFile();
	            
	            if( file != null )
	            {
	            	mIconPath.setText( file.getAbsolutePath() );
	            }
	        } 
	        
	        //Reset the file chooser for the next time it's shown.
	        mFileChooser.setSelectedFile( null );
		}
		else if( e.getSource() == mButtonLeft )
		{
			switch( mMode )
			{
				case EDIT: //Save
					save();
					break;
				case NEW: //Save
					save();
					break;
				case NORMAL:
					setMode( Mode.NEW );
					break;
			}
		}
		else if( e.getSource() == mButtonRight )
		{
			switch( mMode )
			{
				case EDIT: //Cancel
				case NEW:  //Cancel
					setMode( Mode.NORMAL );
					break;
				case NORMAL: //Edit
					setMode( Mode.EDIT );
					break;
			}
		}
    }
	
	private void refreshList()
	{
		MapIcon[] icons = mSettingsManager.getMapIcons();
		
		Arrays.sort( icons );

		mIconList.setListData( icons );
		
		repaint();
	}
	
	private void save()
	{
		String name = mIconName.getText();
		String path = mIconPath.getText();

		switch( mMode )
		{
			case NEW:
				if( name != null && !name.isEmpty() )
				{
					if( path != null && !path.isEmpty() )
					{
						mSettingsManager.addMapIcon( new MapIcon( name, path, true ) );
					}
					else
					{
						JOptionPane.showMessageDialog( this, 
								"Please select an image file", 
								"Filename and path missing",
							    JOptionPane.ERROR_MESSAGE );	
					}
				}
				else
				{
					JOptionPane.showMessageDialog( this, 
							"Please type a name for the new icon", 
							"Name missing",
						    JOptionPane.ERROR_MESSAGE );	
				}
				break;
				
			case EDIT:
				MapIcon selected = mIconList.getSelectedValue();
				
				if( name != null && !name.isEmpty() )
				{
					if( path != null && !path.isEmpty() )
					{
						selected.setName( name );
						selected.setPath( path );
						mSettingsManager.updatMapIcon( selected, name, path );
					}
					else
					{
						JOptionPane.showMessageDialog( this, 
								"Please select an image file", 
								"Filename and path missing",
							    JOptionPane.ERROR_MESSAGE );	
					}
				}
				else
				{
					JOptionPane.showMessageDialog( this, 
							"Please type a name for the new icon", 
							"Name missing",
						    JOptionPane.ERROR_MESSAGE );	
				}

				break;
			default:
				break;
		}
		
	}
	
	public class ImageFilter extends FileFilter 
	{
	    //Accept all directories and all gif, jpg, tiff, or png files.
	    public boolean accept(File f) 
	    {
	        if ( f.isDirectory() ) 
	        {
	            return true;
	        }
	 
	        String extension = getExtension( f );
	        
	        if (extension != null) 
	        {
	            if (extension.equals( "tiff" ) ||
	                extension.equals( "tif" ) ||
	                extension.equals( "gif" ) ||
	                extension.equals( "jpeg" ) ||
	                extension.equals( "jpg" ) ||
	                extension.equals( "png" ) ) 
	            {
                    return true;
	            } 
	            else 
	            {
	                return false;
	            }
	        }
	 
	        return false;
	    }
	 
	    //The description of this filter
	    public String getDescription() 
	    {
	        return "Images (*.gif,*.jpg,*.png.";
	    }
	    
	    public String getExtension( File f ) 
	    {
	        String ext = null;
	        String s = f.getName();
	        int i = s.lastIndexOf('.');
	 
	        if (i > 0 &&  i < s.length() - 1) {
	            ext = s.substring(i+1).toLowerCase();
	        }
	        return ext;
	    }	    
	}	
	
	@Override
    public void settingChanged( Setting setting )
    {
		if( setting instanceof MapIcon )
		{
			refreshList();
			setMode( Mode.NORMAL );
		}
    }

	@Override
    public void settingDeleted( Setting setting )
    {
		if( setting instanceof MapIcon )
		{
			refreshList();
			setMode( Mode.NORMAL );
		}
    }
}
