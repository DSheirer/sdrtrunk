package io.github.dsheirer.module.decode.event.filter.lastseen;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.decode.event.IDecodeEvent;

public class LastSeenFilterSet extends FilterSet<IDecodeEvent>
{

    public LastSeenFilterSet(AliasModel aliasModel, IconModel iconModel)
    {
        super("Last Seen From Radio");

        addFilter(new LastSeenIconFilter("Icon", aliasModel, iconModel));
    }
}