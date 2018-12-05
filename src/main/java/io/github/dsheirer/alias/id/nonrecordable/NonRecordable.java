package io.github.dsheirer.alias.id.nonrecordable;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

@Deprecated //Replaced by the Record class
public class NonRecordable extends AliasID
{
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.NON_RECORDABLE;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public boolean matches(AliasID id)
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "Audio Non-Recordable - **INVALID - USE RECORD INSTEAD**";
    }
}
