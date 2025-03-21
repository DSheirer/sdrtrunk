/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.gui.viewer;

import com.google.common.eventbus.EventBus;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.dmr.DMRCrcMaskManager;
import io.github.dsheirer.module.decode.dmr.DMRDecoderState;
import io.github.dsheirer.module.decode.dmr.DMRHardSymbolProcessor;
import io.github.dsheirer.module.decode.dmr.DMRMessageFramer;
import io.github.dsheirer.module.decode.dmr.DMRMessageProcessor;
import io.github.dsheirer.module.decode.dmr.DMRTrafficChannelManager;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.util.ThreadPool;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DMR Viewer panel
 */
public class DmrViewer extends VBox
{
    private static final Logger mLog = LoggerFactory.getLogger(DmrViewer.class);
    private static final KeyCodeCombination KEY_CODE_COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
    private static final String LAST_SELECTED_DIRECTORY = "last.selected.directory.dmr";
    private final Preferences mPreferences = Preferences.userNodeForPackage(DmrViewer.class);
    private Button mSelectFileButton;
    private Label mSelectedFileLabel;
    private TableView<MessagePackage> mMessagePackageTableView;
    private final ObservableList<MessagePackage> mMessagePackages = FXCollections.observableArrayList();
    private final FilteredList<MessagePackage> mFilteredMessagePackages = new FilteredList<>(mMessagePackages);
    private CheckBox mShowTS0;
    private CheckBox mShowTS1;
    private CheckBox mShowTS2;
    private CheckBox mUseCompressedTalkgroups;
    private TextField mSearchText;
    private TextField mFindText;
    private Button mFindButton;
    private Button mFindNextButton;
    private ProgressIndicator mLoadingIndicator;
    private MessagePackageViewer mMessagePackageViewer;

    public DmrViewer()
    {
        setPadding(new Insets(5));
        setSpacing(5);

        HBox fileBox = new HBox();
        fileBox.setMaxWidth(Double.MAX_VALUE);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setSpacing(5);
        getSelectedFileLabel().setAlignment(Pos.BASELINE_CENTER);

        HBox.setHgrow(getSelectFileButton(), Priority.NEVER);
        HBox.setHgrow(getSelectedFileLabel(), Priority.ALWAYS);

        HBox compressedBox = new HBox();
        compressedBox.setAlignment(Pos.CENTER_RIGHT);
        compressedBox.setMaxWidth(Double.MAX_VALUE);
        compressedBox.getChildren().addAll(getUseCompressedTalkgroups());
        HBox.setHgrow(compressedBox, Priority.ALWAYS);

        fileBox.getChildren().addAll(getSelectFileButton(), getSelectedFileLabel(), compressedBox);

        HBox filterBox = new HBox();
        filterBox.setMaxWidth(Double.MAX_VALUE);
        filterBox.setAlignment(Pos.BASELINE_CENTER);
        filterBox.setSpacing(5);

        Label showLabel = new Label("Show:");
        HBox.setMargin(showLabel, new Insets(0,0,0,15));
        Label searchLabel = new Label("Message Filter:");
        HBox.setMargin(searchLabel, new Insets(0,0,0,15));
        Label findLabel = new Label("Find:");

        HBox.setHgrow(getFindText(), Priority.ALWAYS);
        HBox.setHgrow(getSearchText(), Priority.ALWAYS);

        filterBox.getChildren().addAll(findLabel, getFindText(), getFindButton(), getFindNextButton(), searchLabel,
                getSearchText(), showLabel, getShowTS0(), getShowTS1(), getShowTS2());

        VBox.setVgrow(fileBox, Priority.NEVER);
        VBox.setVgrow(filterBox, Priority.NEVER);
        VBox.setVgrow(getMessagePackageTableView(), Priority.ALWAYS);
        VBox.setVgrow(getMessagePackageViewer(), Priority.NEVER);

        getChildren().addAll(fileBox, filterBox, getMessagePackageTableView(), getMessagePackageViewer());
    }

    /**
     * Spinny loading icon to show over the message table view
     */
    private ProgressIndicator getLoadingIndicator()
    {
        if(mLoadingIndicator == null)
        {
            mLoadingIndicator = new ProgressIndicator();
            mLoadingIndicator.setProgress(-1);
            mLoadingIndicator.setVisible(false);
        }

        return mLoadingIndicator;
    }

    /**
     * Processes the recording file and loads the content into the viewer
     * @param file containing a .bits recording of decoded DMR data.
     */
    private void load(File file)
    {
        if(file != null && file.exists())
        {
            mMessagePackages.clear();
            getLoadingIndicator().setVisible(true);
            getSelectedFileLabel().setText("Loading ...");
            final boolean useCompressed = getUseCompressedTalkgroups().isSelected();

            ThreadPool.CACHED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    List<MessagePackage> messagePackages = new ArrayList<>();
                    DMRCrcMaskManager crcMaskManager = new DMRCrcMaskManager(false);
                    DMRMessageFramer messageFramer = new DMRMessageFramer(crcMaskManager);
                    messageFramer.start();
                    DMRHardSymbolProcessor symbolProcessor = new DMRHardSymbolProcessor(messageFramer);
                    DecodeConfigDMR config = new DecodeConfigDMR();
                    config.setUseCompressedTalkgroups(useCompressed);
                    DMRMessageProcessor messageProcessor = new DMRMessageProcessor(config, crcMaskManager);
                    messageFramer.setListener(messageProcessor);
                    MessagePackager messagePackager = new MessagePackager();

                    //Setup a temporary event bus to capture channel start processing requests
                    EventBus eventBus = new EventBus("debug");
                    eventBus.register(messagePackager);
                    Channel empty = new Channel("Empty");
                    empty.setDecodeConfiguration(new DecodeConfigDMR());
                    DMRTrafficChannelManager trafficChannelManager = new DMRTrafficChannelManager(empty);
                    trafficChannelManager.setInterModuleEventBus(eventBus);

                    //Register to receive events
                    trafficChannelManager.addDecodeEventListener(messagePackager::add);
                    DMRDecoderState decoderState1 = new DMRDecoderState(empty, 1, trafficChannelManager);
                    Broadcaster<DecoderStateEvent> decoderStateEventBroadcaster1 = new Broadcaster<>();
                    decoderState1.setDecoderStateListener(decoderStateEventBroadcaster1);
                    decoderStateEventBroadcaster1.addListener(messagePackager::add);
                    DMRDecoderState decoderState2 = new DMRDecoderState(empty, 2, trafficChannelManager);
                    decoderState1.addDecodeEventListener(messagePackager::add);
                    decoderState1.start();

                    Broadcaster<DecoderStateEvent> decoderStateEventBroadcaster2 = new Broadcaster<>();
                    decoderState2.setDecoderStateListener(decoderStateEventBroadcaster2);
                    decoderStateEventBroadcaster2.addListener(messagePackager::add);
                    decoderState2.addDecodeEventListener(messagePackager::add);
                    decoderState2.start();

                    messageProcessor.setMessageListener(message -> {
                        //Add the initial message to the packager so that it can be combined with any decoder state events.
                        messagePackager.add(message);
                        if(message.getTimeslot() == P25P1Message.TIMESLOT_1)
                        {
                            decoderState1.receive(message);
                        }
                        else if(message.getTimeslot() == P25P1Message.TIMESLOT_2)
                        {
                            decoderState2.receive(message);
                        }

                        //Collect the packaged message with events
                        messagePackages.add(messagePackager.getMessageWithEvents());
                    });


                    try(BinaryReader reader = new BinaryReader(file.toPath(), 200))
                    {
                        while(reader.hasNext())
                        {
                            ByteBuffer buffer = reader.next();
                            symbolProcessor.receive(buffer);
                        }
                    }
                    catch(Exception ioe)
                    {
                        ioe.printStackTrace();
                    }

                    Platform.runLater(() -> {
                        getLoadingIndicator().setVisible(false);
                        getSelectedFileLabel().setText(file.getName());
                        mMessagePackages.addAll(messagePackages);
                        getMessagePackageTableView().scrollTo(0);
                    });
                }
            });
        }
    }

    /**
     * Updates the filter(s) applies to the list of messages
     */
    private void updateFilters()
    {
        Predicate<MessagePackage> timeslotPredicate = message ->
                (getShowTS0().isSelected() && (message.getTimeslot() == 0)) ||
                        (getShowTS1().isSelected() && (message.getTimeslot() == 1)) ||
                        (getShowTS2().isSelected() && (message.getTimeslot() == 2));

        String filterText = getSearchText().getText();

        if(filterText == null || filterText.isEmpty())
        {
            mFilteredMessagePackages.setPredicate(timeslotPredicate);
        }
        else
        {
            Predicate<MessagePackage> textPredicate = message -> message.toString().toLowerCase().contains(filterText.toLowerCase());
            mFilteredMessagePackages.setPredicate(timeslotPredicate.and(textPredicate));
        }
    }

    /**
     * Finds and selects the first row containing the text argument.
     * @param text to search for.
     */
    private void find(String text)
    {
        if(text != null && !text.isEmpty())
        {
            for(MessagePackage messagePackage: mFilteredMessagePackages)
            {
                if(messagePackage.toString().toLowerCase().contains(text.toLowerCase()))
                {
                    getMessagePackageTableView().getSelectionModel().select(messagePackage);
                    getMessagePackageTableView().scrollTo(messagePackage);
                    return;
                }
            }
        }
    }

    /**
     * Finds and selects the first row containing the text argument, after the currently selected row.
     * @param text to search for.
     */
    private void findNext(String text)
    {
        if(text != null && !text.isEmpty())
        {
            MessagePackage selected = getMessagePackageTableView().getSelectionModel().getSelectedItem();

            if(selected == null)
            {
                find(text);
                return;
            }

            int row = mFilteredMessagePackages.indexOf(selected);

            for(int x = row + 1; x < mFilteredMessagePackages.size(); x++)
            {
                if(x < mFilteredMessagePackages.size())
                {
                    MessagePackage messagePackage = mFilteredMessagePackages.get(x);

                    if(messagePackage.toString().toLowerCase().contains(text.toLowerCase()))
                    {
                        getMessagePackageTableView().getSelectionModel().select(messagePackage);
                        getMessagePackageTableView().scrollTo(messagePackage);
                        return;
                    }
                }
            }
        }
    }

    private MessagePackageViewer getMessagePackageViewer()
    {
        if(mMessagePackageViewer == null)
        {
            mMessagePackageViewer = new MessagePackageViewer();
            mMessagePackageViewer.setMaxWidth(Double.MAX_VALUE);

            //Register for table selection events to display the selected value.
            getMessagePackageTableView().getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> getMessagePackageViewer().set(newValue));
        }

        return mMessagePackageViewer;
    }

    /**
     * List view control with DMR message packages
     */
    private TableView<MessagePackage> getMessagePackageTableView()
    {
        if(mMessagePackageTableView == null)
        {
            mMessagePackageTableView = new TableView<>();
            mMessagePackageTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            mMessagePackageTableView.setPlaceholder(getLoadingIndicator());
            SortedList<MessagePackage> sortedList = new SortedList<>(mFilteredMessagePackages);
            sortedList.comparatorProperty().bind(mMessagePackageTableView.comparatorProperty());
            mMessagePackageTableView.setItems(sortedList);

            mMessagePackageTableView.setOnKeyPressed(event ->
            {
                if(KEY_CODE_COPY.match(event))
                {
                    final Set<Integer> rows = new TreeSet<>();
                    for (final TablePosition tablePosition : mMessagePackageTableView.getSelectionModel().getSelectedCells())
                    {
                        rows.add(tablePosition.getRow());
                    }

                    final StringBuilder sb = new StringBuilder();
                    boolean firstRow = true;
                    for (final Integer row : rows)
                    {
                        if(firstRow)
                        {
                            firstRow = false;
                        }
                        else
                        {
                            sb.append('\n');
                        }

                        boolean firstCol = true;

                        for (final TableColumn<?, ?> column : mMessagePackageTableView.getColumns())
                        {
                            if(firstCol)
                            {
                                firstCol = false;
                            }
                            else
                            {
                                sb.append('\t');
                            }

                            final Object cellData = column.getCellData(row);
                            sb.append(cellData == null ? "" : cellData.toString());
                        }
                    }
                    final ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(sb.toString());
                    Clipboard.getSystemClipboard().setContent(clipboardContent);
                }
            });

            TableColumn timestampColumn = new TableColumn();
            timestampColumn.setPrefWidth(110);
            timestampColumn.setText("Time");
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

            TableColumn timeslotColumn = new TableColumn();
            timeslotColumn.setPrefWidth(35);
            timeslotColumn.setText("TS");
            timeslotColumn.setCellValueFactory(new PropertyValueFactory<>("timeslot"));

            TableColumn validColumn = new TableColumn();
            validColumn.setPrefWidth(50);
            validColumn.setText("Valid");
            validColumn.setCellValueFactory(new PropertyValueFactory<>("valid"));

            TableColumn messageColumn = new TableColumn();
            messageColumn.setPrefWidth(1000);
            messageColumn.setText("Message");
            messageColumn.setCellValueFactory((Callback<TableColumn.CellDataFeatures, ObservableValue>) param -> {
                SimpleStringProperty property = new SimpleStringProperty();
                if(param.getValue() instanceof MessagePackage messagePackage)
                {
                    property.set(messagePackage.getMessage().toString());
                }

                return property;
            });

            TableColumn decodeEventCountColumn = new TableColumn();
            decodeEventCountColumn.setPrefWidth(50);
            decodeEventCountColumn.setText("Events");
            decodeEventCountColumn.setCellValueFactory(new PropertyValueFactory<>("decodeEventCount"));

            TableColumn decoderStateEventCountColumn = new TableColumn();
            decoderStateEventCountColumn.setPrefWidth(50);
            decoderStateEventCountColumn.setText("States");
            decoderStateEventCountColumn.setCellValueFactory(new PropertyValueFactory<>("decoderStateEventCount"));

            TableColumn channelStartCountColumn = new TableColumn();
            channelStartCountColumn.setPrefWidth(50);
            channelStartCountColumn.setText("Starts");
            channelStartCountColumn.setCellValueFactory(new PropertyValueFactory<>("channelStartProcessingRequestCount"));

            mMessagePackageTableView.getColumns().addAll(timestampColumn, validColumn, timeslotColumn, messageColumn,
                    decodeEventCountColumn, decoderStateEventCountColumn, channelStartCountColumn);
        }

        return mMessagePackageTableView;
    }

    /**
     * File selection button
     * @return button
     */
    private Button getSelectFileButton()
    {
        if(mSelectFileButton == null)
        {
            mSelectFileButton = new Button("Select ...");
            mSelectFileButton.onActionProperty().set(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select DMR .bits Recording");
                String lastDirectory = mPreferences.get(LAST_SELECTED_DIRECTORY, null);
                if(lastDirectory != null)
                {
                    File file = new File(lastDirectory);
                    if(file.exists() && file.isDirectory())
                    {
                        fileChooser.setInitialDirectory(file);
                    }
                }
                fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("sdrtrunk bits recording", "*.bits"));
                File selected = fileChooser.showOpenDialog(getScene().getWindow());

                if(selected != null)
                {
                    mPreferences.put(LAST_SELECTED_DIRECTORY, selected.getParent());
                    load(selected);
                }
            });
        }

        return mSelectFileButton;
    }

    /**
     * Selected file path label.
     */
    private Label getSelectedFileLabel()
    {
        if(mSelectedFileLabel == null)
        {
            mSelectedFileLabel = new Label(" ");
        }

        return mSelectedFileLabel;
    }

    /**
     * Check box to apply filter to show/hide TS0 messages
     * @return check box control
     */
    private CheckBox getShowTS0()
    {
        if(mShowTS0 == null)
        {
            mShowTS0 = new CheckBox("TS0");
            mShowTS0.setSelected(true);
            mShowTS0.setOnAction(event -> updateFilters());
        }

        return mShowTS0;
    }

    /**
     * Check box to apply filter to show/hide TS0 messages
     * @return check box control
     */
    private CheckBox getShowTS1()
    {
        if(mShowTS1 == null)
        {
            mShowTS1 = new CheckBox("TS1");
            mShowTS1.setSelected(true);
            mShowTS1.setOnAction(event -> updateFilters());
        }

        return mShowTS1;
    }

    /**
     * Check box to apply filter to show/hide TS0 messages
     * @return check box control
     */
    private CheckBox getShowTS2()
    {
        if(mShowTS2 == null)
        {
            mShowTS2 = new CheckBox("TS2");
            mShowTS2.setSelected(true);
            mShowTS2.setOnAction(event -> updateFilters());
        }

        return mShowTS2;
    }

    /**
     * Search text filter box
     * @return text control for entering search text
     */
    private TextField getSearchText()
    {
        if(mSearchText == null)
        {
            mSearchText = new TextField();
            mSearchText.textProperty().addListener((observable, oldValue, newValue) -> updateFilters());
        }

        return mSearchText;
    }

    /**
     * Text box for find text
     */
    private TextField getFindText()
    {
        if(mFindText == null)
        {
            mFindText = new TextField();
            mFindText.setOnKeyPressed(event -> {
                if(event.getCode().equals(KeyCode.ENTER))
                {
                    getFindButton().fire();
                }
            });
            mFindText.textProperty().addListener((observable, oldValue, newValue) -> updateFilters());
        }

        return mFindText;
    }

    /**
     * Find button to search for the text in the find text box.
     * @return button
     */
    private Button getFindButton()
    {
        if(mFindButton == null)
        {
            mFindButton = new Button("Find");
            mFindButton.setOnAction(event -> find(getFindText().getText()));
        }

        return mFindButton;
    }

    /**
     * Find next button to search for the text in the find text box.
     * @return button
     */
    private Button getFindNextButton()
    {
        if(mFindNextButton == null)
        {
            mFindNextButton = new Button("Next");
            mFindNextButton.setOnAction(event -> findNext(getFindText().getText()));
        }

        return mFindNextButton;
    }

    /**
     * Use Hytera Tier 3 Compressed Talkgroups
     * @return
     */
    private CheckBox getUseCompressedTalkgroups()
    {
        if(mUseCompressedTalkgroups == null)
        {
            mUseCompressedTalkgroups = new CheckBox("Use Hytera Tier III Compressed Talkgroups");
        }

        return mUseCompressedTalkgroups;
    }
}
