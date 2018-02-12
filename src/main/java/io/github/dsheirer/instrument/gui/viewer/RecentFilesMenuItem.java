package io.github.dsheirer.instrument.gui.viewer;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.prefs.Preferences;

public class RecentFilesMenuItem extends Menu implements EventHandler<ActionEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(RecentFilesMenuItem.class);

    private Preferences mPreferences = Preferences.userNodeForPackage(RecentFilesMenuItem.class);
    private IFileSelectionListener mFileSelectionListener;
    private String mIdentifier;
    private int mSize;

    public RecentFilesMenuItem(String identifier, int size)
    {
        super("Recent");
        mIdentifier = identifier;
        mSize = size;

        load();
    }

    public void setFileSelectionListener(IFileSelectionListener listener)
    {
        mFileSelectionListener = listener;
    }

    public void add(File file)
    {
        if(file != null)
        {
            int index = mSize - 1;

            while(index > 0)
            {
                String newerItem = mPreferences.get(getKey(index - 1), null);

                if(newerItem != null)
                {
                    mPreferences.put(getKey(index), newerItem);
                }

                index--;
            }

            mPreferences.put(getKey(0), file.getAbsolutePath());
        }

        load();
    }

    private String getKey(int index)
    {
        return mIdentifier + ".recent." + index;
    }

    private void load()
    {
        getItems().clear();

        for(int x = 0; x < mSize; x++)
        {
            String path = mPreferences.get(getKey(x), null);

            if(path != null)
            {
                File file = new File(path);

                if(file.isFile())
                {
                    MenuItem menuItem = new MenuItem(file.getName());
                    menuItem.setUserData(file);
                    menuItem.setOnAction(this);
                    getItems().add(menuItem);
                }
            }
        }
    }

    @Override
    public void handle(ActionEvent event)
    {
        if(mFileSelectionListener != null)
        {
            Object fileObject = ((MenuItem)event.getSource()).getUserData();

            if(fileObject instanceof File)
            {
                mFileSelectionListener.fileSelected((File)fileObject);
            }
        }
    }

    public interface IFileSelectionListener
    {
        void fileSelected(File file);
    }
}
