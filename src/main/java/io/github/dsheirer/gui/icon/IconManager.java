/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.icon;

import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.icon.IconModel;
import javafx.beans.binding.Bindings;
import javafx.collections.transformation.SortedList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GUI manager/editor for standard and custom icons used in sdrtrunk
 */
public class IconManager extends Editor<Icon>
{
    private static final Logger mLog = LoggerFactory.getLogger(IconManager.class);
    private IconModel mIconModel;
    private TableView<Icon> mIconTableView;
    private Button mAddButton;
    private Button mEditButton;
    private Button mDeleteButton;
    private Button mSetAsDefaultButton;
    private SortedList<Icon> mIconSortedList;
    private GridPane mEditorPane;
    private Button mSaveButton;
    private Button mCancelButton;
    private Button mFileButton;
    private TextField mFilePathTextField;
    private TextField mNameTextField;
    private TitledPane mContentTitledPane;
    private TitledPane mEditorTitledPane;
    private Accordion mAccordion;

    /**
     * Constructs an instance
     */
    public IconManager(IconModel iconModel)
    {
        mIconModel = iconModel;
        getChildren().addAll(getAccordion());
    }

    private Accordion getAccordion()
    {
        if(mAccordion == null)
        {
            mAccordion = new Accordion();
            mAccordion.getPanes().addAll(getContentTitledPane(), getEditorTitledPane());
            mAccordion.setExpandedPane(getContentTitledPane());
        }

        return mAccordion;
    }

    private TitledPane getContentTitledPane()
    {
        if(mContentTitledPane == null)
        {
            mContentTitledPane = new TitledPane();
            mContentTitledPane.setCollapsible(false);
            VBox buttonsBox = new VBox();
            buttonsBox.setSpacing(10);
            buttonsBox.setPadding(new Insets(10,10,10,10));
            buttonsBox.getChildren().addAll(getAddButton(), getEditButton(), getDeleteButton(), getSetAsDefaultButton());

            HBox.setHgrow(getIconTableView(), Priority.ALWAYS);
            HBox contentBox = new HBox();
            contentBox.setPadding(new Insets(0,0,0,0));
            contentBox.getChildren().addAll(getIconTableView(), buttonsBox);
            mContentTitledPane.setContent(contentBox);
        }

        return mContentTitledPane;
    }

    private TitledPane getEditorTitledPane()
    {
        if(mEditorTitledPane == null)
        {
            mEditorTitledPane = new TitledPane();
            mEditorTitledPane.setDisable(true);
            mEditorTitledPane.setExpanded(false);
            mEditorTitledPane.setContent(getEditorPane());
        }

        return mEditorTitledPane;
    }

    private SortedList<Icon> getIconSortedList()
    {
        if(mIconSortedList == null)
        {
            mIconSortedList = new SortedList<>(mIconModel.iconsProperty(), (o1, o2) -> {
                if(o1.getName() == null && o2.getName() == null)
                {
                    return 0;
                }
                if(o1.getName() == null)
                {
                    return -1;
                }
                if(o2.getName() == null)
                {
                    return 1;
                }

                return o1.getName().compareTo(o2.getName());
            });
        }

        return mIconSortedList;
    }

    private TableView<Icon> getIconTableView()
    {
        if(mIconTableView == null)
        {
            mIconTableView = new TableView<>(getIconSortedList());

            TableColumn<Icon,String> iconColumn = new TableColumn("Icon");
            iconColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
            iconColumn.setCellFactory(new IconTableCellFactory());

            TableColumn<Icon,String> nameColumn = new TableColumn<>("Name");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn<Icon,Boolean> typeColumn = new TableColumn<>("Type");
            typeColumn.setPrefWidth(100);
            typeColumn.setCellValueFactory(new PropertyValueFactory<>("defaultIcon"));
            typeColumn.setCellFactory(param -> {
                TableCell tableCell = new TableCell<Icon,Boolean>()
                {
                    @Override
                    protected void updateItem(Boolean item, boolean empty)
                    {
                        Icon icon = getTableRow() != null ? getTableRow().getItem() : null;

                        if(icon != null)
                        {
                            if(icon.getDefaultIcon())
                            {
                                setText("Default");
                            }
                            else if(icon.getStandardIcon())
                            {
                                setText("Standard");
                            }
                            else
                            {
                                setText("Custom");
                            }
                        }
                        else
                        {
                            setText(null);
                        }
                    }
                };

                return tableCell;
            });

            mIconTableView.getColumns().addAll(typeColumn, iconColumn, nameColumn);
            mIconTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                getDeleteButton().setDisable(newValue == null || newValue.getDefaultIcon() || newValue.getStandardIcon());
                getEditButton().setDisable(newValue == null || newValue.getDefaultIcon() || newValue.getStandardIcon());
                getSetAsDefaultButton().setDisable(newValue == null || newValue.getDefaultIcon());
            });
        }

        return mIconTableView;
    }

    private void showEditor(boolean show)
    {
        if(show)
        {
            getEditorTitledPane().setText("Add or Edit an Icon");
            getEditorTitledPane().setDisable(false);
            getContentTitledPane().setCollapsible(true);
            getAccordion().setExpandedPane(getEditorTitledPane());
            getEditorTitledPane().setCollapsible(false);
            getContentTitledPane().setDisable(true);
        }
        else
        {
            getEditorTitledPane().setText(null);
            getContentTitledPane().setDisable(false);
            getEditorTitledPane().setCollapsible(true);
            getAccordion().setExpandedPane(getContentTitledPane());
            getContentTitledPane().setCollapsible(false);
            getEditorTitledPane().setDisable(true);
        }
    }

    private Button getAddButton()
    {
        if(mAddButton == null)
        {
            mAddButton = new Button("Add");
            mAddButton.setMaxWidth(Double.MAX_VALUE);
            mAddButton.disableProperty().bind(getEditorPane().visibleProperty());
            mAddButton.setOnAction(event -> {
                setItem(null);
                showEditor(true);
            });
        }

        return mAddButton;
    }

    private Button getEditButton()
    {
        if(mEditButton == null)
        {
            mEditButton = new Button("Edit");
            mEditButton.setDisable(true);
            mEditButton.setMaxWidth(Double.MAX_VALUE);
            mEditButton.setOnAction(event -> {
                Icon selected = getIconTableView().getSelectionModel().getSelectedItem();

                if(selected != null && !selected.standardIconProperty().get())
                {
                    setItem(selected);
                    showEditor(true);
                }
            });
        }

        return mEditButton;
    }

    private Button getDeleteButton()
    {
        if(mDeleteButton == null)
        {
            mDeleteButton = new Button("Delete");
            mDeleteButton.setDisable(true);
            mDeleteButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteButton.setOnAction(event -> {
                Icon selected = getIconTableView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete alias: " +
                        selected.getName() + "?", ButtonType.YES, ButtonType.NO);
                    alert.setTitle("Delete Alias");
                    alert.setHeaderText("Deleting Alias");
                    alert.showAndWait().ifPresent(buttonType -> {
                        if(buttonType == ButtonType.YES)
                        {
                            mIconModel.removeIcon(selected);
                        }
                    });
                }
            });
        }

        return mDeleteButton;
    }

    private Button getSetAsDefaultButton()
    {
        if(mSetAsDefaultButton == null)
        {
            mSetAsDefaultButton = new Button("Set As Default");
            mSetAsDefaultButton.setDisable(true);
            mSetAsDefaultButton.setMaxWidth(Double.MAX_VALUE);
            mSetAsDefaultButton.setOnAction(event -> {
                Icon selected = getIconTableView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    mIconModel.setDefaultIcon(selected);
                }
            });
        }

        return mSetAsDefaultButton;
    }

    @Override
    public void save()
    {
        String name = getNameTextField().getText();
        String path = getFilePathTextField().getText();

        if(name == null || name.isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please assign a name for the icon", ButtonType.OK);
            alert.setHeaderText("Name is required");
            alert.setTitle("Save Icon");
            alert.showAndWait();
            return;
        }

        if(path == null || path.isEmpty() || !Files.exists(Path.of(path)))
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please select a picture file for the icon", ButtonType.OK);
            alert.setHeaderText("File is required");
            alert.setTitle("Save Icon");
            alert.showAndWait();
            return;
        }

        if(getItem() != null)
        {
            getItem().setName(name);
            getItem().setPath(path);
        }
        else
        {
            Icon icon = new Icon();
            icon.setName(name);
            icon.setPath(path);

            if(icon.getFxImage() != null && icon.getFxImage().getException() != null)
            {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to load icon image from selected " +
                    "file. Please select a valid image file for the icon", ButtonType.OK);
                alert.setHeaderText("Invalid image file");
                alert.setTitle("Save Icon");
                alert.showAndWait();
                return;
            }
            else
            {
                mIconModel.addIcon(icon);
            }
        }

        showEditor(false);
        modifiedProperty().set(false);
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void setItem(Icon item)
    {
        super.setItem(item);

        if(item != null)
        {
            getNameTextField().setText(item.getName());
            getFilePathTextField().setText(item.getPath());
        }
        else
        {
            getNameTextField().setText(null);
            getFilePathTextField().setText(null);
        }

        modifiedProperty().set(false);
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10,10,10,10));
            mEditorPane.setHgap(10);
            mEditorPane.setVgap(10);

            int row = 0;

            Label nameLabel = new Label("Name");
            GridPane.setConstraints(nameLabel, 0, row);
            GridPane.setHalignment(nameLabel, HPos.RIGHT);
            mEditorPane.getChildren().add(nameLabel);

            GridPane.setConstraints(getNameTextField(), 1, row);
            GridPane.setHgrow(getNameTextField(), Priority.ALWAYS);
            mEditorPane.getChildren().add(getNameTextField());

            GridPane.setConstraints(getSaveButton(), 2, row);
            mEditorPane.getChildren().add(getSaveButton());

            GridPane.setConstraints(getFileButton(), 0, ++row);
            mEditorPane.getChildren().add(getFileButton());

            GridPane.setHgrow(getFilePathTextField(), Priority.ALWAYS);
            GridPane.setConstraints(getFilePathTextField(), 1, row);
            mEditorPane.getChildren().add(getFilePathTextField());

            GridPane.setConstraints(getCancelButton(), 2, row);
            mEditorPane.getChildren().add(getCancelButton());
        }

        return mEditorPane;
    }

    private TextField getFilePathTextField()
    {
        if(mFilePathTextField == null)
        {
            mFilePathTextField = new TextField();
        }

        return mFilePathTextField;
    }

    private Button getFileButton()
    {
        if(mFileButton == null)
        {
            mFileButton = new Button("File");
            mFileButton.setMaxWidth(Double.MAX_VALUE);
            mFileButton.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Picture Files", "*.png", "*.tif", "*.tiff",
                        "*.gif", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));

                if(getFilePathTextField().getText() != null)
                {
                    try
                    {
                        Path path = Path.of(getFilePathTextField().getText());

                        if(path.toFile().exists())
                        {
                            fileChooser.setInitialDirectory(path.getParent().toFile());
                            fileChooser.setInitialFileName(path.getFileName().toString());
                        }
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error assigning file text to file chooser");
                    }
                }

                File selected = fileChooser.showOpenDialog(getFileButton().getScene().getWindow());

                if(selected != null)
                {
                    getFilePathTextField().setText(selected.getAbsolutePath());
                }
            });
        }

        return mFileButton;
    }

    private TextField getNameTextField()
    {
        if(mNameTextField == null)
        {
            mNameTextField = new TextField();
        }

        return mNameTextField;
    }

    private Button getSaveButton()
    {
        if(mSaveButton == null)
        {
            mSaveButton = new Button("Save");
            mSaveButton.setMaxWidth(Double.MAX_VALUE);
            mSaveButton.setOnAction(event -> save());
            mSaveButton.disableProperty().bind(Bindings.or(Bindings.isNull(getNameTextField().textProperty()),
                Bindings.isNull(getFilePathTextField().textProperty())));
        }

        return mSaveButton;
    }

    private Button getCancelButton()
    {
        if(mCancelButton == null)
        {
            mCancelButton = new Button("Cancel");
            mCancelButton.setMaxWidth(Double.MAX_VALUE);
            mCancelButton.setOnAction(event -> showEditor(false));
        }

        return mCancelButton;
    }

    public class IconTableCellFactory implements Callback<TableColumn<Icon, String>, TableCell<Icon, String>>
    {
        @Override
        public TableCell<Icon, String> call(TableColumn<Icon, String> param)
        {
            TableCell<Icon,String> tableCell = new TableCell<>()
            {
                @Override
                protected void updateItem(String item, boolean empty)
                {
                    super.updateItem(item, empty);
                    setAlignment(Pos.CENTER);

                    if(empty)
                    {
                        setGraphic(null);
                    }
                    else
                    {
                        if(getTableRow() != null)
                        {
                            Icon icon = getTableRow().getItem();

                            if(icon != null && icon.getFxImage() != null)
                            {
                                setGraphic(new ImageView(icon.getFxImage()));
                                setText(null);
                            }
                            else
                            {
                                setGraphic(null);
                                setText("Can't load image");
                            }
                        }
                    }
                }
            };

            return tableCell;
        }
    }
}
