package io.github.dsheirer.module.decode.event.filter.lastheard;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.decode.event.IDecodeEvent;

public class LastHeardFilterSet extends FilterSet<IDecodeEvent>
{

    public LastHeardFilterSet(AliasModel aliasModel, IconModel iconModel)
    {
        super("Last Heard To Channel");

        addFilter(new LastHeardIconFilter("Icon", aliasModel, iconModel));
    }
}