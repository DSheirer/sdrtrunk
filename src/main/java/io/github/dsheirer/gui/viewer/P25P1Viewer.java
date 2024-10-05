/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.SingleChannelState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.audio.P25P1AudioModule;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DecoderState;
import io.github.dsheirer.module.decode.p25.phase1.P25P1MessageFramer;
import io.github.dsheirer.module.decode.p25.phase1.P25P1MessageProcessor;
import io.github.dsheirer.preference.UserPreferences;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
 * APCO25 Phase 1 viewer panel
 */
public class P25P1Viewer extends VBox
{
    private static final Logger mLog = LoggerFactory.getLogger(P25P1Viewer.class);
    private static final KeyCodeCombination KEY_CODE_COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
    private static final String LAST_SELECTED_DIRECTORY = "last.selected.directory.p25p1";
    private static final String FILE_FREQUENCY_REGEX = ".*\\d{8}_\\d{6}_(\\d{9}).*";
    private Preferences mPreferences = Preferences.userNodeForPackage(P25P1Viewer.class);
    private Button mSelectFileButton;
    private Label mSelectedFileLabel;
    private TableView<MessagePackage> mMessagePackageTableView;
    private ObservableList<MessagePackage> mMessagePackages = FXCollections.observableArrayList();
    private FilteredList<MessagePackage> mFilteredMessagePackages = new FilteredList<>(mMessagePackages);
    private TextField mSearchText;
    private TextField mFindText;
    private Button mFindButton;
    private Button mFindNextButton;
    private ProgressIndicator mLoadingIndicator;
    private MessagePackageViewer mMessagePackageViewer;
    private StringProperty mLoadedFile = new SimpleStringProperty();
    private UserPreferences mUserPreferences;

    public P25P1Viewer(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        setPadding(new Insets(5));
        setSpacing(5);

        HBox fileBox = new HBox();
        fileBox.setMaxWidth(Double.MAX_VALUE);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setSpacing(5);
        HBox.setHgrow(getSelectFileButton(), Priority.NEVER);
        HBox.setHgrow(getSelectedFileLabel(), Priority.ALWAYS);
        getSelectedFileLabel().setAlignment(Pos.BASELINE_CENTER);

        fileBox.getChildren().addAll(getSelectFileButton(), getSelectedFileLabel());

        HBox filterBox = new HBox();
        filterBox.setMaxWidth(Double.MAX_VALUE);
        filterBox.setAlignment(Pos.BASELINE_CENTER);
        filterBox.setSpacing(5);

        Label searchLabel = new Label("Message Filter:");
        HBox.setMargin(searchLabel, new Insets(0,0,0,15));
        Label findLabel = new Label("Find:");

        HBox.setHgrow(getFindText(), Priority.ALWAYS);
        HBox.setHgrow(getSearchText(), Priority.ALWAYS);

        filterBox.getChildren().addAll(findLabel, getFindText(), getFindButton(), getFindNextButton(), searchLabel,
                getSearchText());

        VBox.setVgrow(fileBox, Priority.NEVER);
        VBox.setVgrow(filterBox, Priority.NEVER);
        VBox.setVgrow(getMessagePackageTableView(), Priority.ALWAYS);
        VBox.setVgrow(getMessagePackageViewer(), Priority.NEVER);

        getChildren().addAll(fileBox, filterBox, getMessagePackageTableView(), getMessagePackageViewer());

    }

    /**
     * Processes the recording file and loads the content into the viewer
     *
     * Note: invoke this method off of the UI thread in a thread pool executor and the results will be loaded into the
     * message table back on the JavaFX UI thread.
     *
     * @param file containing a .bits recording of decoded DMR data.
     */
    private void load(File file)
    {
        if(file != null && file.exists())
        {
            mLoadedFile.set(file.toString());
            mMessagePackages.clear();
            getLoadingIndicator().setVisible(true);
            getSelectedFileLabel().setText("Loading ...");

            ThreadPool.CACHED.submit(() -> {
                List<MessagePackage> messagePackages = new ArrayList<>();
                P25P1MessageFramer messageFramer = new P25P1MessageFramer(null, 9600);
                P25P1MessageProcessor messageProcessor = new P25P1MessageProcessor();
                Broadcaster<IMessage> messageBroadcaster = new Broadcaster<>();
                messageFramer.setListener(messageBroadcaster);
                messageBroadcaster.addListener(messageProcessor);

                Channel empty = new Channel("Empty");
                empty.setDecodeConfiguration(new DecodeConfigP25Phase1());

                MessagePackager messagePackager = new MessagePackager();

                //Setup a temporary event bus to capture channel start processing requests
                EventBus eventBus = new EventBus("debug");
                eventBus.register(messagePackager);
                P25TrafficChannelManager trafficChannelManager = new P25TrafficChannelManager(empty);
                trafficChannelManager.setInterModuleEventBus(eventBus);

                //Register to receive events
                trafficChannelManager.addDecodeEventListener(messagePackager::add);
                P25P1DecoderState decoderState = new P25P1DecoderState(empty, trafficChannelManager);

                Broadcaster<DecoderStateEvent> decoderStateEventBroadcaster = new Broadcaster<>();
                decoderState.setDecoderStateListener(decoderStateEventBroadcaster);

                decoderStateEventBroadcaster.addListener(messagePackager::add);
                decoderState.addDecodeEventListener(messagePackager::add);
                decoderState.start();

                long frequency = getFrequencyFromFile(mLoadedFile.get());

                if(frequency > 0)
                {
                    trafficChannelManager.setCurrentControlFrequency(frequency, empty);
                    FrequencyConfigurationIdentifier id = FrequencyConfigurationIdentifier.create(frequency);
                    decoderState.getConfigurationIdentifierListener().receive(new IdentifierUpdateNotification(id,
                            IdentifierUpdateNotification.Operation.ADD, 1));
                }

                messageProcessor.setMessageListener(message -> {
                    if(!(message instanceof StuffBitsMessage))
                    {
                        //Add the initial message to the packager so that it can be combined with any decoder state events.
                        messagePackager.add(message);
                        decoderState.receive(message);

                        //Collect the packaged message with events
                        messagePackages.add(messagePackager.getMessageWithEvents());
                    }
                });

                P25P1AudioModule audioModule = new P25P1AudioModule(mUserPreferences, new AliasList("debug"));
                decoderState.setIdentifierUpdateListener(audioModule.getIdentifierUpdateListener());
                audioModule.setAudioSegmentListener(messagePackager::add);
                messageBroadcaster.addListener(audioModule);
                audioModule.start();
                SingleChannelState singleChannelState = new SingleChannelState(empty, new AliasModel());
                singleChannelState.setSquelchStateListener(squelchStateEvent -> audioModule.getSquelchStateListener().receive(squelchStateEvent));
                decoderStateEventBroadcaster.addListener(singleChannelState.getDecoderStateListener());
                singleChannelState.start();

                try(BinaryReader reader = new BinaryReader(file.toPath(), 200))
                {
                    while(reader.hasNext())
                    {
                        ByteBuffer buffer = reader.next();
                        messageFramer.receive(buffer);
                    }
                }
                catch(Exception ioe)
                {
                    ioe.printStackTrace();
                }

                audioModule.stop();
                decoderState.stop();
                singleChannelState.stop();

                Platform.runLater(() -> {
                    getLoadingIndicator().setVisible(false);
                    getSelectedFileLabel().setText(file.getName());
                    mMessagePackages.addAll(messagePackages);
                    getMessagePackageTableView().scrollTo(0);
                });
            });
        }
    }

    /**
     * Extracts the channel frequency value from the bits file name to broadcast as the current frequency for each of
     * the decoder states.
     * @param file name to parse
     * @return parsed frequency or zero.
     */
    private static long getFrequencyFromFile(String file)
    {
        if(file == null || file.isEmpty())
        {
            return 0;
        }

        if(file.matches(FILE_FREQUENCY_REGEX))
        {
            Pattern p = Pattern.compile(FILE_FREQUENCY_REGEX);
            Matcher m = p.matcher(file);
            if(m.find())
            {
                try
                {
                    String raw = m.group(1);
                    return Long.parseLong(raw);
                }
                catch(Exception e)
                {
                    mLog.error("Couldn't parse frequency from bits file [" + file + "]");
                }
            }
        }

        return 0;
    }

    /**
     * Updates the filter(s) applies to the list of messages
     */
    private void updateFilters()
    {
        String filterText = getSearchText().getText();

        if(filterText != null && !filterText.isEmpty())
        {
            Predicate<MessagePackage> textPredicate = message -> message.toString().toLowerCase().contains(filterText.toLowerCase());
            mFilteredMessagePackages.setPredicate(textPredicate);
        }
        else
        {
            mFilteredMessagePackages.setPredicate(null);
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
     * List view control with messages
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

            TableColumn validColumn = new TableColumn();
            validColumn.setPrefWidth(50);
            validColumn.setText("Valid");
            validColumn.setCellValueFactory(new PropertyValueFactory<>("valid"));

            TableColumn timeslotColumn = new TableColumn();
            timeslotColumn.setPrefWidth(35);
            timeslotColumn.setText("TS");
            timeslotColumn.setCellValueFactory(new PropertyValueFactory<>("timeslot"));

            TableColumn messageColumn = new TableColumn();
            messageColumn.setPrefWidth(900);
            messageColumn.setText("Message");
            messageColumn.setCellValueFactory((Callback<TableColumn.CellDataFeatures, ObservableValue>) param -> {
                SimpleStringProperty property = new SimpleStringProperty();
                if(param.getValue() instanceof MessagePackage messagePackage)
                {
                    property.set(messagePackage.toString());
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

            TableColumn audioSegmentCountColumn = new TableColumn();
            audioSegmentCountColumn.setPrefWidth(50);
            audioSegmentCountColumn.setText("Audio");
            audioSegmentCountColumn.setCellValueFactory(new PropertyValueFactory<>("audioSegmentCount"));

            mMessagePackageTableView.getColumns().addAll(timestampColumn, validColumn, timeslotColumn, messageColumn,
                    decodeEventCountColumn, decoderStateEventCountColumn, channelStartCountColumn, audioSegmentCountColumn);
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
                fileChooser.setTitle("Select P25 Phase 1 .bits Recording");
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
                final File selected = fileChooser.showOpenDialog(getScene().getWindow());

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
}
