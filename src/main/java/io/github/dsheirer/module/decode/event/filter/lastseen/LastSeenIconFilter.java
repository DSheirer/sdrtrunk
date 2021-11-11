package io.github.dsheirer.module.decode.event.filter.lastseen;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LastSeenIconFilter extends Filter<IDecodeEvent>
{
    private AliasModel mAliasModel;
    private Map<String, FilterElement<String>> mElements = new HashMap<>();

    public LastSeenIconFilter(String name, AliasModel aliasModel, IconModel iconModel)
    {
        super(name);
        this.mAliasModel = aliasModel;

        ObservableList<Icon> icons = iconModel.iconsProperty();
        for (Icon icon : icons) {
            mElements.put(icon.getName(), new FilterElement<>(icon.getName()));
        }
    }

    @Override
    public boolean passes(IDecodeEvent iDecodeEvent)
    {
        if (mEnabled && canProcess(iDecodeEvent))
        {
            return matchAliasToFilter(iDecodeEvent)
                    .map(FilterElement::isEnabled)
                    .orElse(false);
        }
        return false;
    }

    public Optional<FilterElement<String>> matchAliasToFilter(IDecodeEvent iDecodeEvent)
    {
        IdentifierCollection identifierCollection = iDecodeEvent.getIdentifierCollection();
        List<Identifier> identifiers = identifierCollection.getIdentifiers();
        if (identifiers != null && !identifiers.isEmpty())
        {
            AliasList aliasList = mAliasModel.getAliasList(identifierCollection);
            if (aliasList != null)
            {
                for(Identifier identifier: identifiers)
                {
                    if (identifier.getForm() == Form.RADIO)
                    {
                        List<Alias> aliases = aliasList.getAliases(identifier);
                        if (!aliases.isEmpty())
                        {
                            for (Alias alias : aliases) {
                                if (alias.getIconName() == null)
                                {
                                    // Return any to match aliases with no icon defined.
                                    return Optional.of(mElements.get(IconModel.DEFAULT_ICON));
                                }
                                else
                                {
                                    return Optional.of(mElements.get(alias.getIconName()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean canProcess(IDecodeEvent iDecodeEvent)
    {
        return true;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<>(mElements.values());
    }
}