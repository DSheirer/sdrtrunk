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

import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.module.decode.p25.phase2.P25P2MessageFramer;
import io.github.dsheirer.module.decode.p25.phase2.P25P2MessageProcessor;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.record.binary.BinaryReader;
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
    private Preferences mPreferences = Preferences.userNodeForPackage(P25P2Viewer.class);
    private Button mSelectFileButton;
    private Label mSelectedFileLabel;
    private TableView<IMessage> mMessageTableView;
    private ObservableList<IMessage> mMessages = FXCollections.observableArrayList();
    private FilteredList<IMessage> mFilteredMessages = new FilteredList<>(mMessages);
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
        VBox.setVgrow(getMessageTableView(), Priority.ALWAYS);

        getChildren().addAll(fileBox, scrambleSettingsBox, filterBox, getMessageTableView());
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
            mMessages.clear();
            getLoadingIndicator().setVisible(true);
            getSelectedFileLabel().setText("Loading ...");

            int wacn = getWACNTextField().get();
            int system = getSystemTextField().get();
            int nac = getNACTextField().get();
            ScrambleParameters scrambleParameters = new ScrambleParameters(wacn, system, nac);

            ThreadPool.CACHED.submit(() -> {
                List<IMessage> messages = new ArrayList<>();
                P25P2MessageFramer messageFramer = new P25P2MessageFramer(null, 9600);
                messageFramer.setScrambleParameters(scrambleParameters);
                P25P2MessageProcessor messageProcessor = new P25P2MessageProcessor();
                messageFramer.setListener(messageProcessor);
                messageProcessor.setMessageListener(message -> {
                    if(!(message instanceof StuffBitsMessage))
                    {
                        messages.add(message);
                    }
                });

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

                Platform.runLater(() -> {
                    getLoadingIndicator().setVisible(false);
                    getSelectedFileLabel().setText(file.getName());
                    mMessages.addAll(messages);
                    getMessageTableView().scrollTo(0);
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
        Predicate<IMessage> timeslotPredicate = message ->
                (getShowTS0().isSelected() && (message.getTimeslot() == 0)) ||
                        (getShowTS1().isSelected() && (message.getTimeslot() == 1)) ||
                        (getShowTS2().isSelected() && (message.getTimeslot() == 2));

        String filterText = getSearchText().getText();

        if(filterText == null || filterText.isEmpty())
        {
            mFilteredMessages.setPredicate(timeslotPredicate);
        }
        else
        {
            Predicate<IMessage> textPredicate = message -> message.toString().toLowerCase().contains(filterText.toLowerCase());
            mFilteredMessages.setPredicate(timeslotPredicate.and(textPredicate));
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
            for(IMessage message: mFilteredMessages)
            {
                if(message.toString().toLowerCase().contains(text.toLowerCase()))
                {
                    getMessageTableView().getSelectionModel().select(message);
                    getMessageTableView().scrollTo(message);
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
            IMessage selected = getMessageTableView().getSelectionModel().getSelectedItem();

            if(selected == null)
            {
                find(text);
                return;
            }

            int row = mFilteredMessages.indexOf(selected);

            for(int x = row + 1; x < mFilteredMessages.size(); x++)
            {
                if(x < mFilteredMessages.size())
                {
                    IMessage message = mFilteredMessages.get(x);

                    if(message.toString().toLowerCase().contains(text.toLowerCase()))
                    {
                        getMessageTableView().getSelectionModel().select(message);
                        getMessageTableView().scrollTo(message);
                        return;
                    }
                }
            }
        }
    }

    private IntegerTextField getWACNTextField()
    {
        if(mWACNTextField == null)
        {
            mWACNTextField = new IntegerTextField();
        }

        return mWACNTextField;
    }

    private IntegerTextField getSystemTextField()
    {
        if(mSystemTextField == null)
        {
            mSystemTextField = new IntegerTextField();
        }

        return mSystemTextField;
    }

    private IntegerTextField getNACTextField()
    {
        if(mNACTextField == null)
        {
            mNACTextField = new IntegerTextField();
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
     * List view control with DMR messages
     */
    private TableView<IMessage> getMessageTableView()
    {
        if(mMessageTableView == null)
        {
            mMessageTableView = new TableView<>();
            mMessageTableView.setPlaceholder(getLoadingIndicator());
            SortedList<IMessage> sortedList = new SortedList<>(mFilteredMessages);
            sortedList.comparatorProperty().bind(mMessageTableView.comparatorProperty());
            mMessageTableView.setItems(sortedList);

            mMessageTableView.setOnKeyPressed(event ->
            {
                if(KEY_CODE_COPY.match(event))
                {
                    final Set<Integer> rows = new TreeSet<>();
                    for (final TablePosition tablePosition : mMessageTableView.getSelectionModel().getSelectedCells())
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

                        for (final TableColumn<?, ?> column : mMessageTableView.getColumns())
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
            messageColumn.setPrefWidth(1000);
            messageColumn.setText("Message");
            messageColumn.setCellValueFactory((Callback<TableColumn.CellDataFeatures, ObservableValue>) param -> {
                SimpleStringProperty property = new SimpleStringProperty();
                if(param.getValue() instanceof IMessage message)
                {
                    property.set(message.toString());
                }

                return property;
            });

            mMessageTableView.getColumns().addAll(timestampColumn, validColumn, timeslotColumn, messageColumn);
        }

        return mMessageTableView;
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
