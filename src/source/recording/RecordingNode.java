package source.recording;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import source.SourceManager;
import controller.BaseNode;

public class RecordingNode extends BaseNode
{
    private static final long serialVersionUID = 1L;
    
    private JPanel mEditor;
    
    private SourceManager mSourceManager;

    public RecordingNode( SourceManager sourceManager, Recording recording )
    {
    	super( recording );
    }
    
    public Recording getRecording()
    {
    	return (Recording)getUserObject();
    }
    
    public String toString()
    {
    	return getRecording().toString();
    }

    @Override
	public JPanel getEditor()
	{
    	if( mEditor == null )
    	{
    		mEditor = new RecordingEditorPanel( this ); 
    	}
    	
	    return mEditor;
	}
    
	public JPopupMenu getContextMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		
		JMenuItem deleteItem = new JMenuItem( "Delete" );
		
		deleteItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				if( getRecording().hasChannels() )
				{
					JOptionPane.showMessageDialog( getModel().getTree(), 
							"Can't delete recording - currently in use.  Please"
							+ " disable any channel(s) that are using this "
							+ "recording before deleting it",
							"Can't delete recording", 
							JOptionPane.ERROR_MESSAGE );
				}
				else
				{
					mSourceManager.getRecordingSourceManager()
						.removeRecording( getRecording() );
					
					getModel().removeNodeFromParent( RecordingNode.this );
				}
            }
		} );
		
		menu.add( deleteItem );

		return menu;
	}
}
