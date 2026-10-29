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

package io.github.dsheirer.audio.broadcast.thinline;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Turns an SDRTrunk settings object into form fields for the Thinline Radio admin page and writes edited
 * values back through the object's own setters.
 *
 * Field kinds: boolean, integer, number, text, enum, list, object (nested fields), json (anything else).
 * Properties without a setter are sent read-only.
 */
final class ThinlineRadioSettings
{
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final int MAX_DEPTH = 4;

    private ThinlineRadioSettings()
    {
    }

    /**
     * Describes the bean's properties.
     * @param bean to describe
     * @param readOnly property names to send read-only
     * @param hidden property names (lower case fragments) to leave out entirely
     * @param writableOnly when true, properties without a setter are left out and their getters are not called
     */
    static ArrayNode describe(Object bean, Set<String> readOnly, Set<String> hidden, boolean writableOnly)
    {
        return describe(bean, readOnly, hidden, writableOnly, 0);
    }

    private static ArrayNode describe(Object bean, Set<String> readOnly, Set<String> hidden, boolean writableOnly, int depth)
    {
        ArrayNode fields = MAPPER.createArrayNode();
        if(bean == null)
        {
            return fields;
        }

        Map<String, AnnotatedMethod> setters = setters(bean.getClass());
        for(BeanPropertyDefinition property: getters(bean.getClass()))
        {
            String name = property.getName();
            if(isHidden(name, hidden))
            {
                continue;
            }
            AnnotatedMethod setter = setters.get(name);
            if(writableOnly && setter == null)
            {
                continue;
            }

            Object value;
            try
            {
                value = property.getGetter().callOn(bean);
            }
            catch(Exception e)
            {
                continue;
            }

            Class<?> raw = property.getGetter().getRawType();
            boolean writable = setter != null && !readOnly.contains(name);
            ObjectNode field = MAPPER.createObjectNode();
            field.put("name", name);
            field.put("label", label(name));
            field.put("readOnly", !writable);

            String kind = kind(raw, value);
            if(kind.equals("object") && (depth >= MAX_DEPTH || writableOnly))
            {
                kind = "json";
            }
            JavaType content = property.getGetter().getType().getContentType();
            if((kind.equals("list") || kind.equals("json")) && Collection.class.isAssignableFrom(raw) &&
                content != null && content.getRawClass().isEnum())
            {
                kind = "enumList";
                ArrayNode options = field.putArray("options");
                for(Object constant: content.getRawClass().getEnumConstants())
                {
                    ObjectNode option = options.addObject();
                    option.put("value", ((Enum<?>)constant).name());
                    option.put("label", constant.toString());
                }
            }
            field.put("kind", kind);

            switch(kind)
            {
                case "enum" ->
                {
                    field.put("value", value == null ? "" : ((Enum<?>)value).name());
                    ArrayNode options = field.putArray("options");
                    for(Object constant: raw.getEnumConstants())
                    {
                        ObjectNode option = options.addObject();
                        option.put("value", ((Enum<?>)constant).name());
                        option.put("label", constant.toString());
                    }
                }
                case "text" -> field.put("value", value == null ? "" : value.toString());
                case "object" ->
                {
                    field.put("readOnly", false);
                    field.put("type", value.getClass().getSimpleName());
                    field.set("fields", describe(value, readOnly, hidden, false, depth + 1));
                }
                default -> field.set("value", tree(value));
            }
            fields.add(field);
        }
        return fields;
    }

    /**
     * Writes the edited values onto the bean.  Unknown, hidden, and read-only names are ignored.
     * @return number of properties changed
     */
    static int apply(Object bean, JsonNode values, Set<String> readOnly, Set<String> hidden) throws Exception
    {
        return apply(bean, values, readOnly, hidden, 0);
    }

    private static int apply(Object bean, JsonNode values, Set<String> readOnly, Set<String> hidden, int depth) throws Exception
    {
        if(bean == null || values == null || !values.isObject())
        {
            return 0;
        }

        Map<String, BeanPropertyDefinition> getters = new HashMap<>();
        for(BeanPropertyDefinition property: getters(bean.getClass()))
        {
            getters.put(property.getName(), property);
        }
        Map<String, AnnotatedMethod> setters = setters(bean.getClass());

        int changed = 0;
        Iterator<Map.Entry<String, JsonNode>> entries = values.fields();
        while(entries.hasNext())
        {
            Map.Entry<String, JsonNode> entry = entries.next();
            String name = entry.getKey();
            JsonNode node = entry.getValue();
            BeanPropertyDefinition getter = getters.get(name);
            if(getter == null || isHidden(name, hidden))
            {
                continue;
            }

            Object current = getter.getGetter().callOn(bean);
            Class<?> raw = getter.getGetter().getRawType();
            if(node.isObject() && kind(raw, current).equals("object") && depth < MAX_DEPTH)
            {
                changed += apply(current, node, readOnly, hidden, depth + 1);
                continue;
            }

            AnnotatedMethod setter = setters.get(name);
            if(setter == null || readOnly.contains(name))
            {
                continue;
            }

            Object next;
            Class<?> parameter = setter.getRawParameterType(0);
            if(Path.class.isAssignableFrom(parameter))
            {
                String text = node.asText("").trim();
                next = text.isEmpty() ? null : Path.of(text);
            }
            else
            {
                JavaType type = setter.getParameterType(0);
                next = MAPPER.convertValue(node, type);
            }

            if(next == null && parameter.isPrimitive())
            {
                continue;
            }
            if(next == null ? current == null : next.equals(current))
            {
                continue;
            }
            setter.callOnWith(bean, next);
            changed++;
        }
        return changed;
    }

    static String label(String name)
    {
        StringBuilder label = new StringBuilder();
        for(int index = 0; index < name.length(); index++)
        {
            char c = name.charAt(index);
            if(index == 0)
            {
                label.append(Character.toUpperCase(c));
            }
            else if(Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(index - 1)))
            {
                label.append(' ').append(c);
            }
            else
            {
                label.append(c);
            }
        }
        return label.toString();
    }

    private static String kind(Class<?> raw, Object value)
    {
        if(raw == boolean.class || raw == Boolean.class)
        {
            return "boolean";
        }
        if(raw == int.class || raw == long.class || raw == short.class || raw == byte.class ||
            raw == Integer.class || raw == Long.class || raw == Short.class || raw == Byte.class)
        {
            return "integer";
        }
        if(raw == double.class || raw == float.class || raw == Double.class || raw == Float.class)
        {
            return "number";
        }
        if(raw.isEnum())
        {
            return "enum";
        }
        if(raw == String.class || Path.class.isAssignableFrom(raw) || raw == Character.class || raw == char.class)
        {
            return "text";
        }
        if(Collection.class.isAssignableFrom(raw) || raw.isArray())
        {
            return simpleList(value) ? "list" : "json";
        }
        if(value != null && !Map.class.isAssignableFrom(raw) && !raw.getName().startsWith("java.") &&
            !raw.getName().startsWith("javafx."))
        {
            return "object";
        }
        return "json";
    }

    private static boolean simpleList(Object value)
    {
        if(!(value instanceof Collection<?> collection))
        {
            return value != null && value.getClass().isArray() && value.getClass().getComponentType().isPrimitive();
        }
        for(Object item: collection)
        {
            if(item != null && !(item instanceof Number || item instanceof String || item instanceof Boolean))
            {
                return false;
            }
        }
        return true;
    }

    private static JsonNode tree(Object value)
    {
        try
        {
            return MAPPER.valueToTree(value);
        }
        catch(Exception e)
        {
            return MAPPER.getNodeFactory().textNode(String.valueOf(value));
        }
    }

    private static boolean isHidden(String name, Set<String> hidden)
    {
        String lower = name.toLowerCase();
        for(String fragment: hidden)
        {
            if(lower.contains(fragment))
            {
                return true;
            }
        }
        return false;
    }

    private static java.util.List<BeanPropertyDefinition> getters(Class<?> type)
    {
        BeanDescription description = MAPPER.getSerializationConfig().introspect(MAPPER.constructType(type));
        return description.findProperties().stream().filter(BeanPropertyDefinition::hasGetter).toList();
    }

    private static Map<String, AnnotatedMethod> setters(Class<?> type)
    {
        Map<String, AnnotatedMethod> setters = new HashMap<>();
        BeanDescription description = MAPPER.getDeserializationConfig().introspect(MAPPER.constructType(type));
        for(BeanPropertyDefinition property: description.findProperties())
        {
            if(property.hasSetter())
            {
                setters.put(property.getName(), property.getSetter());
            }
        }
        return setters;
    }
}
