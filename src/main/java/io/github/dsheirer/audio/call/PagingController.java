/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.audio.call;

import java.text.NumberFormat;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Implements a set of paging controls.
 */
public class PagingController extends HBox
{
    private static final int ICON_SIZE = 20;
    private NumberFormat mNumberFormat = NumberFormat.getIntegerInstance();
    private Button mFirstButton;
    private Button mLastButton;
    private Button mNextButton;
    private Button mPreviousButton;
    private Label mPageLabel;
    private int mPageCount = 1;
    private int mCurrentPage = 1;
    private IPageRequestListener mPageRequestListener;

    /**
     * Constructs an instance
     * @param listener to receive page requests
     */
    public PagingController(IPageRequestListener listener)
    {
        mPageRequestListener = listener;
        setAlignment(Pos.CENTER);
        setSpacing(0);
        getChildren().addAll(getFirstButton(), getPreviousButton(), getPageLabel(), getNextButton(), getLastButton());
        update(1, 1);
    }

    /**
     * Updates the current state of this paging controller.
     * @param currentPage number to display.
     * @param pageCount the total number of available pages.
     */
    public void update(int currentPage, int pageCount)
    {
        mPageCount = pageCount;
        mCurrentPage = currentPage;
        getFirstButton().setDisable(mCurrentPage <= 1);
        getFirstButton().setTooltip(mCurrentPage <= 1 ? null : new Tooltip("Move to page 1"));
        getPreviousButton().setDisable(mCurrentPage <= 1);
        getPreviousButton().setTooltip(mCurrentPage <= 1 ? null : new Tooltip("Move to page " + (mCurrentPage - 1)));
        getNextButton().setDisable(mCurrentPage >= mPageCount);
        getNextButton().setTooltip(mCurrentPage >= mPageCount ? null : new Tooltip("Move to page " + (mCurrentPage + 1)));
        getLastButton().setDisable(mCurrentPage >= mPageCount);
        getLastButton().setTooltip(mCurrentPage >= mPageCount ? null : new Tooltip("Move to page " + mPageCount));
        getPageLabel().setText("Page " + mNumberFormat.format(mCurrentPage) + " of " + mNumberFormat.format(mPageCount));
    }

    /**
     * Total pages available
     * @return page count
     */
    public int getPageCount()
    {
        return mPageCount;
    }

    /**
     * Current page number
     * @return current page.
     */
    public int getCurrentPage()
    {
        return mCurrentPage;
    }

    /**
     * Notifies the listener that the user requests the page number
     * @param page number requested
     */
    private void request(int page)
    {
        if(mPageRequestListener != null)
        {
            mPageRequestListener.showPage(page);
        }
    }

    /**
     * Next page
     */
    private void next()
    {
        request(mCurrentPage + 1);
    }

    /**
     * Previous page
     */
    private void previous()
    {
        request(mCurrentPage - 1);
    }

    /**
     * First page
     */
    private void first()
    {
        request(1);
    }

    /**
     * Last page
     */
    private void last()
    {
        request(mPageCount);
    }

    /**
     * Current page status label.
     */
    private Label getPageLabel()
    {
        if(mPageLabel == null)
        {
            mPageLabel = new Label("Page 1 of 1");
            mPageLabel.setTooltip(new Tooltip("Current Page and Total Page Count"));
            mPageLabel.setAlignment(Pos.CENTER);
            mPageLabel.setMaxWidth(Double.MAX_VALUE);
            mPageLabel.setPadding(new Insets(0, 20, 0, 20));
        }

        return mPageLabel;
    }

    /**
     * First page button.
     */
    private Button getFirstButton()
    {
        if(mFirstButton == null)
        {
            mFirstButton = new Button();
            mFirstButton.setTooltip(new Tooltip("Go To First Page"));
            IconNode iconNode = new IconNode(FontAwesome.ANGLE_DOUBLE_LEFT);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.BLACK);
            mFirstButton.setGraphic(iconNode);
            mFirstButton.setOnAction(event -> first());
        }

        return mFirstButton;
    }

    /**
     * Last page button
     */
    private Button getLastButton()
    {
        if(mLastButton == null)
        {
            mLastButton = new Button();
            mLastButton.setTooltip(new Tooltip("Go To Last Page"));
            IconNode iconNode = new IconNode(FontAwesome.ANGLE_DOUBLE_RIGHT);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.BLACK);
            mLastButton.setGraphic(iconNode);
            mLastButton.setOnAction(event -> last());
        }

        return mLastButton;
    }

    /**
     * Next page button
     */
    private Button getNextButton()
    {
        if(mNextButton == null)
        {
            mNextButton = new Button();
            mNextButton.setTooltip(new Tooltip("Go To Next Page"));
            IconNode iconNode = new IconNode(FontAwesome.ANGLE_RIGHT);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.BLACK);
            mNextButton.setGraphic(iconNode);
            mNextButton.setOnAction(event -> next());
        }

        return mNextButton;
    }

    /**
     * Previous page button.
     */
    private Button getPreviousButton()
    {
        if(mPreviousButton == null)
        {
            mPreviousButton = new Button();
            mPreviousButton.setTooltip(new Tooltip("Go To Previous Page"));
            IconNode iconNode = new IconNode(FontAwesome.ANGLE_LEFT);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.BLACK);
            mPreviousButton.setGraphic(iconNode);
            mPreviousButton.setOnAction(event -> previous());
        }

        return mPreviousButton;
    }
}
