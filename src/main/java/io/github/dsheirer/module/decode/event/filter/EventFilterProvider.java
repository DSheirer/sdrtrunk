package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.filter.FilterSet;

public interface EventFilterProvider<T> {
    FilterSet<T> getFilterSet();
    void setFilterSet(FilterSet<T> filterSet);
}
