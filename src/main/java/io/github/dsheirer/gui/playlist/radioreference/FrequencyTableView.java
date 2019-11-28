/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.type.Category;
import io.github.dsheirer.rrapi.type.Frequency;
import io.github.dsheirer.rrapi.type.Mode;
import io.github.dsheirer.rrapi.type.SubCategory;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Grid pane with components for visualizing and selecting categories of frequencies
 */
public class FrequencyTableView extends GridPane
{
    private static final Logger mLog = LoggerFactory.getLogger(FrequencyTableView.class);
    private static final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("0.00000");
    private static final Category ALL_CATEGORIES = new AllCategories();
    private static final SubCategory ALL_SUB_CATEGORIES = new AllSubCategories();

    private RadioReference mRadioReference;
    private Label mFrequencyTableLabel;
    private Label mFrequencyTableNameLabel;
    private ComboBox<Category> mCategoryComboBox;
    private ComboBox<SubCategory> mSubCategoryComboBox;
    private TableView<Frequency> mFrequencyTableView;
    private Label mPlaceholderLabel;
    private ProgressIndicator mProgressIndicator;

    public FrequencyTableView(RadioReference radioReference)
    {
        mRadioReference = radioReference;

        setPadding(new Insets(5, 5, 5,5));
        setHgap(5.0);
        setVgap(5.0);

        GridPane.setConstraints(getFrequencyTableLabel(), 0, 0);
        GridPane.setHalignment(getFrequencyTableLabel(), HPos.RIGHT);
        getChildren().add(getFrequencyTableLabel());

        GridPane.setConstraints(getFrequencyTableNameLabel(), 1, 0);
        GridPane.setHgrow(getFrequencyTableNameLabel(), Priority.ALWAYS);
        getChildren().add(getFrequencyTableNameLabel());

        Label categoryLabel = new Label("Category:");
        categoryLabel.setAlignment(Pos.CENTER_RIGHT);
        GridPane.setConstraints(categoryLabel, 0, 1);
        GridPane.setHalignment(categoryLabel, HPos.RIGHT);
        getChildren().add(categoryLabel);

        GridPane.setConstraints(getCategoryComboBox(), 1, 1);
        GridPane.setHgrow(getCategoryComboBox(), Priority.ALWAYS);
        getChildren().add(getCategoryComboBox());

        Label subCategoryLabel = new Label("Sub-Category:");
        subCategoryLabel.setAlignment(Pos.CENTER_RIGHT);
        GridPane.setConstraints(subCategoryLabel, 0, 2);
        GridPane.setHalignment(subCategoryLabel, HPos.RIGHT);
        getChildren().add(subCategoryLabel);

        GridPane.setConstraints(getSubCategoryComboBox(), 1, 2);
        GridPane.setHgrow(getSubCategoryComboBox(), Priority.ALWAYS);
        getChildren().add(getSubCategoryComboBox());

        GridPane.setConstraints(getFrequencyTableView(), 0, 3, 2, 6);
        GridPane.setHgrow(getFrequencyTableView(), Priority.ALWAYS);

        getChildren().add(getFrequencyTableView());
    }

    private void setLoading(boolean loading)
    {
        if(Platform.isFxApplicationThread())
        {
            getFrequencyTableView().setPlaceholder(loading ? getProgressIndicator() : getPlaceholderLabel());
        }
        else
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    getFrequencyTableView().setPlaceholder(loading ? getProgressIndicator() : getPlaceholderLabel());
                }
            });
        }
    }

    public void clear()
    {
        if(Platform.isFxApplicationThread())
        {
            getFrequencyTableNameLabel().setText(null);
            getCategoryComboBox().getItems().clear();
            getSubCategoryComboBox().getItems().clear();
            getFrequencyTableView().getItems().clear();
        }
        else
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    getFrequencyTableNameLabel().setText(null);
                    getCategoryComboBox().getItems().clear();
                    getSubCategoryComboBox().getItems().clear();
                    getFrequencyTableView().getItems().clear();
                }
            });
        }
    }

    public void update(String label, String name, List<Category> categories)
    {
        if(categories != null)
        {
            Collections.sort(categories, new Comparator<Category>()
            {
                @Override
                public int compare(Category o1, Category o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });
        }

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                clear();
                getFrequencyTableLabel().setText(label);
                getFrequencyTableNameLabel().setText(name);
                if(categories != null && !categories.isEmpty())
                {
                    getCategoryComboBox().getItems().add(ALL_CATEGORIES);
                    getCategoryComboBox().getItems().addAll(categories);
                    getCategoryComboBox().getSelectionModel().select(ALL_CATEGORIES);
                }
                else
                {
                    setLoading(false);
                }
            }
        });
    }

    private ProgressIndicator getProgressIndicator()
    {
        if(mProgressIndicator == null)
        {
            mProgressIndicator = new ProgressIndicator();
            mProgressIndicator.setProgress(-1);
        }

        return mProgressIndicator;
    }

    private Label getPlaceholderLabel()
    {
        if(mPlaceholderLabel == null)
        {
            mPlaceholderLabel = new Label("No Frequencies Available");
        }

        return mPlaceholderLabel;
    }

    private TableView<Frequency> getFrequencyTableView()
    {
        if(mFrequencyTableView == null)
        {
            mFrequencyTableView = new TableView<>();
            mFrequencyTableView.setPlaceholder(getPlaceholderLabel());

            TableColumn frequencyColumn = new TableColumn();
            frequencyColumn.setText("Frequency");
            frequencyColumn.setCellValueFactory(new FrequencyCellValueFactory());
            frequencyColumn.setPrefWidth(100);

            TableColumn modeColumn = new TableColumn();
            modeColumn.setText("Mode");
            modeColumn.setCellValueFactory(new ModeCellValueFactory());
            modeColumn.setPrefWidth(100);

            TableColumn toneColumn = new TableColumn();
            toneColumn.setText("Tone");
            toneColumn.setCellValueFactory(new PropertyValueFactory<>("tone"));
            toneColumn.setPrefWidth(100);

            TableColumn alphaTagColumn = new TableColumn();
            alphaTagColumn.setText("Alpha Tag");
            alphaTagColumn.setCellValueFactory(new PropertyValueFactory<>("alphaTag"));
            alphaTagColumn.setPrefWidth(125);

            TableColumn descriptionColumn = new TableColumn();
            descriptionColumn.setText("Description");
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            descriptionColumn.setPrefWidth(525);

            mFrequencyTableView.getColumns().addAll(frequencyColumn, modeColumn, toneColumn, alphaTagColumn, descriptionColumn);
        }

        return mFrequencyTableView;
    }

    private Label getFrequencyTableLabel()
    {
        if(mFrequencyTableLabel == null)
        {
            mFrequencyTableLabel = new Label();
            mFrequencyTableLabel.setAlignment(Pos.CENTER_RIGHT);
        }

        return mFrequencyTableLabel;
    }

    private Label getFrequencyTableNameLabel()
    {
        if(mFrequencyTableNameLabel == null)
        {
            mFrequencyTableNameLabel = new Label();
        }

        return mFrequencyTableNameLabel;
    }

    private ComboBox<Category> getCategoryComboBox()
    {
        if(mCategoryComboBox == null)
        {
            mCategoryComboBox = new ComboBox<>();
            mCategoryComboBox.setMaxWidth(Double.MAX_VALUE);
            mCategoryComboBox.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    getFrequencyTableView().getItems().clear();
                    getSubCategoryComboBox().getItems().clear();
                    Category selected = mCategoryComboBox.getSelectionModel().getSelectedItem();

                    if(selected != null)
                    {
                        List<SubCategory> subCategories = new ArrayList<>();

                        if(selected.equals(ALL_CATEGORIES))
                        {
                            for(Category category: getCategoryComboBox().getItems())
                            {
                                if(category.getSubCategories() != null)
                                {
                                    subCategories.addAll(category.getSubCategories());
                                }
                            }
                        }
                        else if(selected.getSubCategories() != null)
                        {
                            subCategories.addAll(selected.getSubCategories());
                        }

                        Collections.sort(subCategories, new Comparator<SubCategory>()
                        {
                            @Override
                            public int compare(SubCategory o1, SubCategory o2)
                            {
                                return o1.getName().compareTo(o2.getName());
                            }
                        });

                        getSubCategoryComboBox().getItems().add(ALL_SUB_CATEGORIES);
                        getSubCategoryComboBox().getItems().addAll(subCategories);
                        getSubCategoryComboBox().getSelectionModel().select(ALL_SUB_CATEGORIES);
                    }
                }
            });
            mCategoryComboBox.setConverter(new StringConverter<Category>()
            {
                @Override
                public String toString(Category category)
                {
                    if(category != null)
                    {
                        return category.getName();
                    }

                    return null;
                }

                @Override
                public Category fromString(String string)
                {
                    if(string != null)
                    {
                        for(Category category: mCategoryComboBox.getItems())
                        {
                            if(category.getName().contentEquals(string))
                            {
                                return category;
                            }
                        }
                    }

                    return null;
                }
            });
        }

        return mCategoryComboBox;
    }

    private ComboBox<SubCategory> getSubCategoryComboBox()
    {
        if(mSubCategoryComboBox == null)
        {
            mSubCategoryComboBox = new ComboBox<>();
            mSubCategoryComboBox.setMaxWidth(Double.MAX_VALUE);
            mSubCategoryComboBox.setConverter(new StringConverter<SubCategory>()
            {
                @Override
                public String toString(SubCategory subCategory)
                {
                    if(subCategory != null)
                    {
                        return subCategory.getName();
                    }

                    return null;
                }

                @Override
                public SubCategory fromString(String string)
                {
                    if(string != null)
                    {
                        for(SubCategory subCategory: mSubCategoryComboBox.getItems())
                        {
                            if(subCategory.getName().contentEquals(string))
                            {
                                return subCategory;
                            }
                        }
                    }

                    return null;
                }
            });

            mSubCategoryComboBox.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    setLoading(true);
                    getFrequencyTableView().getItems().clear();

                    SubCategory subCategory = mSubCategoryComboBox.getValue();

                    if(subCategory != null)
                    {
                        ThreadPool.SCHEDULED.submit(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    List<Frequency> frequencies = new ArrayList<>();

                                    if(subCategory.equals(ALL_SUB_CATEGORIES))
                                    {
                                        for(SubCategory subCategory1: getSubCategoryComboBox().getItems())
                                        {
                                            if(subCategory1.equals(ALL_SUB_CATEGORIES))
                                            {
                                                //skip
                                            }
                                            else
                                            {
                                                final List<Frequency> subCatFrequencies = mRadioReference.getService()
                                                        .getSubCategoryFrequencies(subCategory1.getSubCategoryId());

                                                if(subCatFrequencies != null)
                                                {
                                                    frequencies.addAll(subCatFrequencies);
                                                }
                                            }
                                        }
                                    }
                                    else
                                    {
                                        final List<Frequency> subCatFrequencies = mRadioReference.getService()
                                                .getSubCategoryFrequencies(subCategory.getSubCategoryId());

                                        if(subCatFrequencies != null)
                                        {
                                            frequencies.addAll(subCatFrequencies);
                                        }
                                    }

                                    Collections.sort(frequencies, new Comparator<Frequency>()
                                    {
                                        @Override
                                        public int compare(Frequency o1, Frequency o2)
                                        {
                                            return o1.getAlphaTag().compareTo(o2.getAlphaTag());
                                        }
                                    });

                                    Platform.runLater(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            getFrequencyTableView().getItems().addAll(frequencies);
                                            setLoading(false);
                                        }
                                    });
                                }
                                catch(RadioReferenceException rre)
                                {
                                    mLog.error("Error retrieving frequencies for subcategory [" + subCategory.getName() + "]");
                                }
                            }
                        });
                    }
                }
            });
        }

        return mSubCategoryComboBox;
    }

    public static class AllCategories extends Category
    {
        public AllCategories()
        {
            this.setCategoryId(-1);
            this.setName("(All)");
        }
    }

    public static class AllSubCategories extends SubCategory
    {
        public AllSubCategories()
        {
            this.setSubCategoryId(-1);
            this.setName("(All)");
        }
    }

    public class FrequencyCellValueFactory implements Callback<TableColumn.CellDataFeatures<Frequency, String>,
            ObservableValue<String>>
    {
        private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("0.00000");
        private SimpleStringProperty mFrequencyFormatted = new SimpleStringProperty();

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Frequency, String> param)
        {
            mFrequencyFormatted.set(FREQUENCY_FORMATTER.format(param.getValue().getDownlink()));
            return mFrequencyFormatted;
        }
    }

    public class ModeCellValueFactory implements Callback<TableColumn.CellDataFeatures<Frequency, String>,
            ObservableValue<String>>
    {
        private SimpleStringProperty mMode = new SimpleStringProperty();

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Frequency, String> param)
        {
            String mode = param.getValue().getMode();

            try
            {
                Integer modeInt = Integer.valueOf(mode);
                Mode modeObj = mRadioReference.getService().getModesMap().get(modeInt);

                if(modeObj != null)
                {
                    mMode.set(modeObj != null ? modeObj.getName() : null);
                }
            }
            catch(Exception pe)
            {
                mMode.set("Error");
            }

            return mMode;
        }
    }
}
