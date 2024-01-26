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

package io.github.dsheirer.audio.call;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Calls database repository
 */
public interface CallRepository extends JpaRepository<Call,Long>
{
    @Query("SELECT t FROM Call t WHERE t.toId like ?1 OR t.toAlias like ?1 OR t.fromId like ?1 OR t.fromAlias like ?1")
    Page<Call> findByAnyIdWithPagination(String value, Pageable pageable);

    @Query("SELECT t FROM Call t WHERE t.toId like ?1 OR t.toAlias like ?1")
    Page<Call> findByToWithPagination(String value, Pageable pageable);

    @Query("SELECT t FROM Call t WHERE t.fromId like ?1 OR t.fromAlias like ?1")
    Page<Call> findByFromWithPagination(String value, Pageable pageable);

    @Query("SELECT t FROM Call t WHERE t.callType like ?1")
    Page<Call> findByCallTypeWithPagination(String value, Pageable pageable);

    @Query("SELECT t FROM Call t WHERE t.channel like ?1")
    Page<Call> findByChannelWithPagination(String value, Pageable pageable);

    @Query("SELECT t FROM Call t WHERE t.protocol like ?1")
    Page<Call> findByProtocolWithPagination(String value, Pageable pageable);

    @Query("SELECT t FROM Call t WHERE t.site like ?1")
    Page<Call> findBySiteWithPagination(String value, Pageable pageable);

    @Query("SELECT t FROM Call t WHERE t.system like ?1")
    Page<Call> findBySystemWithPagination(String value, Pageable pageable);

    /**
     * Find all calls between the timestamp range arguments.
     * @param start timestamp
     * @param end timestamp
     * @return pageable set of calls.
     */
    Page<Call> findCallsByEventTimeBetweenOrderByEventTime(long start, long end, Pageable pageable);
}
