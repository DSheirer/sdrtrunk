/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.web.api;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a collection: {@code {"items": [...], "nextCursor": ...}}.  The cursor is opaque to clients; pass it
 * back as the cursor query parameter for the next page.  It is null on the last page.
 * @param items on this page
 * @param nextCursor for the next page, or null
 */
public record Page<T>(List<T> items, String nextCursor)
{
    /**
     * Creates a page from a query that read up to limit + 1 rows, so that it knows whether there is another page.
     * @param rows read from the query, at most limit + 1
     * @param limit page size
     * @param cursor creates the cursor from the last item on the page
     * @param view converts a row to the API representation
     */
    public static <R,T> Page<T> of(List<R> rows, int limit, Function<R,String> cursor, Function<R,T> view)
    {
        boolean more = rows.size() > limit;
        List<R> page = more ? rows.subList(0, limit) : rows;
        return new Page<>(page.stream().map(view).toList(), more ? cursor.apply(page.getLast()) : null);
    }
}
