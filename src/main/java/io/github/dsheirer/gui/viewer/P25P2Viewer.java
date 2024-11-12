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
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.module.decode.p25.P25FrequencyBandPreloadDataContent;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.phase1.message.P25FrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.P25P2DecoderState;
import io.github.dsheirer.module.decode.p25.phase2.P25P2MessageFramer;
import io.github.dsheirer.module.decode.p25.phase2.P25P2MessageProcessor;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.record.binary.BinaryReader;
import io.github.dsheirer.util.ThreadPool;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
 * APCO25 Phase 2 viewer panel
 */
public class P25P2Viewer extends VBox
{
    private static final Logger mLog = LoggerFactory.getLogger(P25P2Viewer.class);
    private static final KeyCodeCombination KEY_CODE_COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
    private static final String LAST_SELECTED_DIRECTORY = "last.selected.directory.p25p2";
    private static final String LAST_WACN_VALUE = "last.wacn.value.p25p2";
    private static final String LAST_SYSTEM_VALUE = "last.system.value.p25p2";
    private static final String LAST_NAC_VALUE = "last.nac.value.p25p2";
    private static final String FILE_FREQUENCY_REGEX = ".*\\d{8}_\\d{6}_(\\d{9}).*";
    private Preferences mPreferences = Preferences.userNodeForPackage(P25P2Viewer.class);
    private Button mSelectFileButton;
    private Label mSelectedFileLabel;
    private TableView<MessagePackage> mMessagePackageTableView;
    private ObservableList<MessagePackage> mMessagePackages = FXCollections.observableArrayList();
    private FilteredList<MessagePackage> mFilteredMessagePackages = new FilteredList<>(mMessagePackages);
    private CheckBox mShowTS0;
    private CheckBox mShowTS1;
    private CheckBox mShowTS2;
    private TextField mSearchText;
    private TextField mFindText;
    private Button mFindButton;
    private Button mFindNextButton;
    private ProgressIndicator mLoadingIndicator;
    private IntegerTextField mWACNTextField;
    private IntegerTextField mSystemTextField;
    private IntegerTextField mNACTextField;
    private Button mReloadButton;
    private StringProperty mLoadedFile = new SimpleStringProperty();
    private MessagePackageViewer mMessagePackageViewer;

    public P25P2Viewer()
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
        fileBox.getChildren().addAll(getSelectFileButton(), getSelectedFileLabel());

        HBox scrambleSettingsBox = new HBox();
        scrambleSettingsBox.setAlignment(Pos.BASELINE_LEFT);
        scrambleSettingsBox.setSpacing(5);
        Label wacnLabel = new Label("WACN:");
        Label systemLabel = new Label("SYSTEM:");
        Label nacLabel = new Label("NAC:");
        scrambleSettingsBox.getChildren().addAll(wacnLabel, getWACNTextField(), systemLabel, getSystemTextField(),
                nacLabel, getNACTextField(), getReloadButton());

        HBox filterBox = new HBox();
        filterBox.setMaxWidth(Double.MAX_VALUE);
        filterBox.setAlignment(Pos.BASELINE_CENTER);
        filterBox.setSpacing(5);

        Label searchLabel = new Label("Message Filter:");
        HBox.setMargin(searchLabel, new Insets(0,0,0,15));
        Label findLabel = new Label("Find:");

        HBox.setHgrow(getFindText(), Priority.ALWAYS);
        HBox.setHgrow(getSearchText(), Priority.ALWAYS);

        Label showLabel = new Label("Show:");
        HBox.setMargin(showLabel, new Insets(0,0,0,15));

        filterBox.getChildren().addAll(findLabel, getFindText(), getFindButton(), getFindNextButton(), searchLabel,
                getSearchText(), showLabel, getShowTS0(), getShowTS1(), getShowTS2());

        VBox.setVgrow(fileBox, Priority.NEVER);
        VBox.setVgrow(filterBox, Priority.NEVER);
        VBox.setVgrow(scrambleSettingsBox, Priority.NEVER);
        VBox.setVgrow(getMessagePackageTableView(), Priority.ALWAYS);
        VBox.setVgrow(getMessagePackageViewer(), Priority.NEVER);

        getChildren().addAll(fileBox, scrambleSettingsBox, filterBox, getMessagePackageTableView(), getMessagePackageViewer());
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
     * Processes the recording file and loads the content into the viewer
     *
     * Note: invoke this method off of the UI thread in a thread pool executor and the results will be loaded into the
     * message table back on the JavaFX UI thread.
     *
     * @param file containing a .bits recording of decoded data.
     */
    private void load(File file)
    {
        mLog.info("Loading File: " + file);
        if(file != null && file.exists())
        {
            mLoadedFile.set(file.toString());
            mMessagePackages.clear();
            getLoadingIndicator().setVisible(true);
            getSelectedFileLabel().setText("Loading ...");

            int wacn = getWACNTextField().get();
            int system = getSystemTextField().get();
            int nac = getNACTextField().get();
            ScrambleParameters scrambleParameters = new ScrambleParameters(wacn, system, nac);

            ThreadPool.CACHED.submit(() -> {
                List<MessagePackage> messages = new ArrayList<>();
                P25P2MessageFramer messageFramer = new P25P2MessageFramer(null);
                messageFramer.setScrambleParameters(scrambleParameters);
                P25P2MessageProcessor messageProcessor = new P25P2MessageProcessor();
                messageFramer.setListener(messageProcessor);
                Channel empty = new Channel("Empty");
                empty.setDecodeConfiguration(new DecodeConfigP25Phase2());

                MessagePackager messagePackager = new MessagePackager();

                //Setup a temporary event bus to capture channel start processing requests
                EventBus eventBus = new EventBus("debug");
                eventBus.register(messagePackager);
                P25TrafficChannelManager trafficChannelManager = new P25TrafficChannelManager(empty);
                trafficChannelManager.setInterModuleEventBus(eventBus);

                //Register to receive events
                trafficChannelManager.addDecodeEventListener(decodeEvent -> messagePackager.add(decodeEvent));
                PatchGroupManager patchGroupManager = new PatchGroupManager();
                P25P2DecoderState decoderState1 = new P25P2DecoderState(empty, 1, trafficChannelManager,
                        patchGroupManager);
                decoderState1.setDecoderStateListener(decoderStateEvent -> messagePackager.add(decoderStateEvent));
                decoderState1.addDecodeEventListener(decodeEvent -> messagePackager.add(decodeEvent));
                P25P2DecoderState decoderState2 = new P25P2DecoderState(empty, 2, trafficChannelManager,
                        patchGroupManager);
                decoderState2.setDecoderStateListener(decoderStateEvent -> messagePackager.add(decoderStateEvent));
                decoderState2.addDecodeEventListener(decodeEvent -> messagePackager.add(decodeEvent));
                decoderState1.start();
                decoderState2.start();

                long frequency = getFrequencyFromFile(mLoadedFile.get());

                if(frequency > 0)
                {
                    trafficChannelManager.setCurrentControlFrequency(frequency, empty);
                    FrequencyConfigurationIdentifier id = FrequencyConfigurationIdentifier.create(frequency);
                    decoderState1.getConfigurationIdentifierListener().receive(new IdentifierUpdateNotification(id,
                            IdentifierUpdateNotification.Operation.ADD, 1));
                    decoderState2.getConfigurationIdentifierListener().receive(new IdentifierUpdateNotification(id,
                            IdentifierUpdateNotification.Operation.ADD, 2));
                }

                //TODO: testing use - PCWIN TDMA Frequency Band as preload data
                //ID:2 OFFSET:-45000000 SPACING:12500 BASE:851012500 TDMA BW:12500 TIMESLOTS:2 VOCODER:HALF_RATE
                P25FrequencyBand band = new P25FrequencyBand(2, 851012500l, -45000000l, 12500, 12500, 2);
                P25FrequencyBandPreloadDataContent content = new P25FrequencyBandPreloadDataContent(Collections.singleton(band));
                messageProcessor.preload(content);

                messageProcessor.setMessageListener(message -> {
                    if(!(message instanceof StuffBitsMessage))
                    {
//                        System.out.println(message);
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
                        messages.add(messagePackager.getMessageWithEvents());
                    }
                });

                try(BinaryReader reader = new BinaryReader(file.toPath(), 200))
                {
                    while(reader.hasNext())
                    {
                        ByteBuffer buffer = reader.next();
//                        System.out.println("Processing Bytes " + buffer.capacity() + " / " + reader.getByteCounter());
                        messageFramer.receive(buffer);
                    }
                }
                catch(Exception ioe)
                {
                    ioe.printStackTrace();
                }

                Platform.runLater(() -> {
                    getLoadingIndicator().setVisible(false);
                    getSelectedFileLabel().setText(file.getName());
                    mMessagePackages.addAll(messages);
                    getMessagePackageTableView().scrollTo(0);
                });
            });
        }
        else
        {
            mLog.info("Can't load file: " + file);
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

    private IntegerTextField getWACNTextField()
    {
        if(mWACNTextField == null)
        {
            mWACNTextField = new IntegerTextField();
            mWACNTextField.textProperty().addListener((ob, ol, ne) -> mPreferences.putInt(LAST_WACN_VALUE, getWACNTextField().get()));
            int previous = mPreferences.getInt(LAST_WACN_VALUE, 0);
            if(previous > 0)
            {
                getWACNTextField().set(previous);
            }
        }

        return mWACNTextField;
    }

    private IntegerTextField getSystemTextField()
    {
        if(mSystemTextField == null)
        {
            mSystemTextField = new IntegerTextField();
            mSystemTextField.textProperty().addListener((ob, ol, ne) -> mPreferences.putInt(LAST_SYSTEM_VALUE, getSystemTextField().get()));
            int previous = mPreferences.getInt(LAST_SYSTEM_VALUE, 0);
            if(previous > 0)
            {
                getSystemTextField().set(previous);
            }
        }

        return mSystemTextField;
    }

    private IntegerTextField getNACTextField()
    {
        if(mNACTextField == null)
        {
            mNACTextField = new IntegerTextField();
            mNACTextField.textProperty().addListener((ob, ol, ne) -> mPreferences.putInt(LAST_NAC_VALUE, getNACTextField().get()));
            int previous = mPreferences.getInt(LAST_NAC_VALUE, 0);
            if(previous > 0)
            {
                getNACTextField().set(previous);
            }
        }

        return mNACTextField;
    }

    private Button getReloadButton()
    {
        if(mReloadButton == null)
        {
            mReloadButton = new Button("Reload");
            mReloadButton.disableProperty().bind(Bindings.isNull(mLoadedFile));
            mReloadButton.setOnAction(event -> load(new File(mLoadedFile.get())));
        }

        return mReloadButton;
    }

    /**
     * List view control with message packages
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
                fileChooser.setTitle("Select P25 Phase 2 .bits Recording");
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
}
