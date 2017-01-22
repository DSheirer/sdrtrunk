package instrument.gui;

import alias.AliasModel;
import audio.broadcast.BroadcastModel;
import controller.channel.ChannelModel;
import controller.channel.map.ChannelMapModel;
import module.decode.DecodeConfigurationEditor;
import net.miginfocom.swing.MigLayout;
import playlist.PlaylistManager;
import source.IControllableFileSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DecoderSelectionFrame extends JInternalFrame
{
    private static final long serialVersionUID = 1L;

    private DecodeConfigurationEditor mDecodeEditor = new DecodeConfigurationEditor(null);
    private PlaylistManager mPlaylistManager;

    private IControllableFileSource mSource;
    private JDesktopPane mDesktop;

    public DecoderSelectionFrame(JDesktopPane desktop,
                                 IControllableFileSource source)
    {
        AliasModel aliasModel = new AliasModel();
        BroadcastModel broadcastModel = new BroadcastModel(null);
        ChannelModel channelModel = new ChannelModel();
        ChannelMapModel channelMapModel = new ChannelMapModel();

        mPlaylistManager = new PlaylistManager(aliasModel, broadcastModel, channelModel, channelMapModel);

        mDesktop = desktop;
        mSource = source;

        initGUI();
    }

    private void initGUI()
    {
        setLayout(new MigLayout("", "[grow,fill]", "[][][grow,fill]"));

        setTitle("Decoders");
        setPreferredSize(new Dimension(700, 450));
        setSize(700, 450);

        setResizable(true);
        setClosable(true);
        setIconifiable(true);
        setMaximizable(false);

        add(mDecodeEditor, "wrap");

        add(new AddDecoderButton(), "span");

    }

    public class AddDecoderButton extends JButton
    {
        private static final long serialVersionUID = 1L;

        public AddDecoderButton()
        {
            super("Add");

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent arg0)
                {
//					DecodeConfiguration config = mDecodeEditor.getDecodeConfig();
//					
//					if( config != null )
//					{
//						DecoderViewFrame decoderFrame = new DecoderViewFrame( 
//								mPlaylistManager, null, mSource );
//
//						decoderFrame.setVisible( true );
//						
//						mDesktop.add( decoderFrame );
//					}
                }
            });
        }
    }

}
