/*
*   Copyright 2007 Mansystems Nederland B.V.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*/
package com.expertdesk.edplus;

import java.util.HashMap;
import java.util.Map;

import com.remedy.arsys.api.DataType;
import com.remedy.arsys.api.Entry;
import com.remedy.arsys.api.EntryFactory;
import com.remedy.arsys.api.EntryID;
import com.remedy.arsys.api.EntryItem;
import com.remedy.arsys.api.FieldID;
import com.remedy.arsys.api.JoinEntryID;
import com.remedy.arsys.api.NameID;
import com.remedy.arsys.api.Value;

/**
 * Static utility class to convert an {@link Entry} to a {@link Map} and vice versa.
 * @author Hugo Visser
 */
public final class EntryUtil {
    
    private EntryUtil() {
        
    }
    /**
     * Convert an entry to a map of field id, value pairs. So that the {@link Value} can be retrieve by field id like so:
     * <pre>
     *  Entry entry = ...
     *  Map&lt;Long,Value&gt; entryMap = EntryUtil.toEntryMap(entry, new HashMap&lt;Long, Value&gt;());
     *  
     *  // get the value of the request id 
     *  
     *  Value v = entryMap.get(1L);
     * </pre>
     * 
     * @param entry the entry to convert
     * @param entryMap the map to add the result to
     * @return the entryMap that is updated with the fieldId,Value pairs
     */
    public static Map<Long, Value> toEntryMap(Entry entry, Map<Long, Value> entryMap) {
        EntryItem[] items = entry.getEntryItems();
        for (EntryItem item : items) {
            entryMap.put(item.getFieldID().getValue(), item.getValue());
        }
        if (entry.getEntryID() != null) {
            entryMap.put(1L, new Value(entry.getEntryID().toString()));
        }
        return entryMap;
    }

    /**
     * Convert an entry to a map of field id, value pairs. 
     * @param entry the entry to convert
     * @return a new entryMap representing the entry.
     * @see #toEntryMap(Entry, Map)
     */
    public static Map<Long, Value> toEntryMap(Entry entry) {
        Map<Long, Value> entryMap = new HashMap<Long, Value>();
        return toEntryMap(entry, entryMap);
    }

    /**
     * Convert an entryMap to a entry. If the map contains a request id (field id 1) then the value of the field will be set as the entry id.
     * @param form the form to create the entry for
     * @param entryMap the entryMap to convert
     * @return a new entry
     */
    public static Entry toEntry(String form, Map<Long, Value> entryMap) {
        Entry entry = (Entry) EntryFactory.getFactory().newInstance();
        entry.setSchemaID(new NameID(form));
        if (entryMap.get(1L) != null && entryMap.get(1L).getDataType() != DataType.NULL) {
            String id = entryMap.get(1L).toString();
            EntryID entryId = id.indexOf("|") > -1 || id.length() > 15 ? new JoinEntryID(id) : new EntryID(id);
            entry.setEntryID(entryId);
        }

        EntryItem[] items = new EntryItem[entryMap.size()];

        int index = 0;
        for (long fieldId : entryMap.keySet()) {
            items[index++] = new EntryItem(new FieldID(fieldId), entryMap.get(fieldId));
        }

        entry.setEntryItems(items);
        return entry;
    }
}
