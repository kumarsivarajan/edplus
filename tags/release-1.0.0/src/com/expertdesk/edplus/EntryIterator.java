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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import com.remedy.arsys.api.ARException;
import com.remedy.arsys.api.ARQualifierHelper;
import com.remedy.arsys.api.ARServerUser;
import com.remedy.arsys.api.ArithmeticOrRelationalOperand;
import com.remedy.arsys.api.Constants;
import com.remedy.arsys.api.Entry;
import com.remedy.arsys.api.EntryCriteria;
import com.remedy.arsys.api.EntryFactory;
import com.remedy.arsys.api.EntryID;
import com.remedy.arsys.api.EntryKey;
import com.remedy.arsys.api.EntryListCriteria;
import com.remedy.arsys.api.EntryListFieldInfo;
import com.remedy.arsys.api.Field;
import com.remedy.arsys.api.FieldID;
import com.remedy.arsys.api.FieldListCriteria;
import com.remedy.arsys.api.FieldType;
import com.remedy.arsys.api.NameID;
import com.remedy.arsys.api.QualifierInfo;
import com.remedy.arsys.api.RelationalOperationInfo;
import com.remedy.arsys.api.SortInfo;
import com.remedy.arsys.api.StatusInfo;
import com.remedy.arsys.api.Value;
import com.remedy.arsys.api.View;
import com.remedy.arsys.api.ViewListCriteria;

/**
 * This class implements an {@link Iterator} and {@link Iterable} to iterate entries in an AR System form.
 * 
 * @author Hugo Visser
 * 
 */
public class EntryIterator implements Iterator<Entry>, Iterable<Entry> {
    /**
     * The default chunk size value
     */
    public final static int DEFAULT_CHUNK_SIZE = 10000;
    private int absoluteIndex;
    private EntryKey[] entryKeys;
    private Entry[] entries;
    private Entry currentEntry;
    private int entryIndex;
    private int keyIndex;
    private ARServerUser context;
    private QualifierInfo qualifier;
    private int chunkSize = DEFAULT_CHUNK_SIZE;
    private int numMatches = -1;
    private SortInfo[] sorting;
    private String form;
    private boolean fastFail = false;
    private boolean lastChunk = false;
    private EntryCriteria ec = null;

     
    /**
     * @param context the server context
     * @param form form to query
     * @param qualifier qualification to use or null to use the default qualification "1=1"
     */
    public EntryIterator(ARServerUser context, String form, QualifierInfo qualifier) {
        this.context = context;
        this.form = form;
        if (qualifier != null) {
            this.qualifier = qualifier;            
        }
        else {
            this.qualifier = new QualifierInfo(new RelationalOperationInfo(RelationalOperationInfo.AR_REL_OP_EQUAL, new ArithmeticOrRelationalOperand(new Value(1)), new ArithmeticOrRelationalOperand(new Value(1))));
        }
    }
    /**
     * @param context the server context
     * @param form form to query
     */
    public EntryIterator(ARServerUser context, String form) {
        this(context, form, null);
    }
    /**
     * Set a qualification. Note that when using field names in the qualification, the field names will be matched to the field label on the first
     * view that matches the locale of the {@link com.remedy.arsys.api.ARServerUser} context.
     * @param qualification
     * @throws ARException when the qualification cannot be parsed.
     */
    public void setQualification(String qualification) throws ARException {
        ARQualifierHelper helper = new ARQualifierHelper();
        FieldListCriteria flc = new FieldListCriteria(new NameID(form), null, FieldType.AR_DATA_FIELD);

        List<Field> fieldList = new ArrayList<Field>();
        for (Field field : ARObjectIterator.newFieldIterable(context, flc)) {
            if (field.getFieldOption() != Constants.AR_FIELD_OPTION_DISPLAY) {
                fieldList.add(field);
            }
        }
        
        ViewListCriteria vlc = new ViewListCriteria(new NameID(form), null);        
        
        View view = null;
        
        for (View v : ARObjectIterator.newViewIterable(context, vlc)) {
            if (view == null) {
                view = v;
            }
            if (context.getLocale() != null) {
                if (context.getLocale().equals(v.getLocale())) {
                    view = v;
                    break;
                }
                if (context.getLocale().indexOf("_") > -1 && v.getLocale() != null && v.getLocale().startsWith(context.getLocale().substring(0, context.getLocale().indexOf("_")))) {
                    view = v;
                }
            }
        }
        
        helper.generateFieldMaps(fieldList.toArray(new Field[fieldList.size()]), view.getVUIId().getValue() , "tag", fieldList.toArray(new Field[fieldList.size()]));
        qualifier = helper.getQualifier(context, qualification);
    }
    
    /**
     * Reset the iterator, this will perform a new query.
     */
    public void reset() {
        numMatches = -1;
        entryKeys = null;
        entries = null;
        entryIndex = 0;
        keyIndex = 0;
        absoluteIndex = 0;
        lastChunk = false;
    }
    /**
     * Set the chunk size of a single query.
     * @param size the required chunk size default is {@link #DEFAULT_CHUNK_SIZE}
     */
    public void setChunkSize(int size) {
        this.chunkSize = size;
    }
    /**
     * Set the sorting
     * @param sorting the sorting
     */
    public void setSorting(SortInfo[] sorting) {
        this.sorting = sorting;
    }
    
    /**
     * Set the fast failing behaviour of the iterator, default is false. When fast failing is enabled, the iterator will throw a {@link ConcurrentModificationException}
     * if entries are added to the form or deleted from the form that is being iterated. Basically the number of matching items must not change while iterating 
     * the form. Using {@link #remove()} or {@link #removeEntry()} is the correct way to remove an entry while iterating the form. Fast failing can impact performance
     * as the total number of entries is retrieved when a new chunk of data is queried.
     * @param fastFail set to true for fast failing behaviour, false otherwise.
     */
    public void setFastFail(boolean fastFail) {
        this.fastFail = fastFail;
    }
    
    /**
     * Get the number of entries that match the query.
     * @return the number of entries that have matched the query
     * @throws ARException
     */
    public int getNumMatches() throws ARException {
        prepareEntries();
        return numMatches;
    }
    
    public boolean hasNext() {
        try {
            prepareEntries();
        }
        catch (ARException e) {
            throw new RuntimeException(e);
        }                
        return entryIndex < entries.length;
    }

    public Entry next() {
        if (!hasNext()) {
            throw new IllegalStateException("No next entry");
        }
        currentEntry = entries[entryIndex];
        entryIndex++;
        absoluteIndex++;
        currentEntry.setContext(context);
        return currentEntry;
    }

    /**
     * Removes the current entry by invoking {@link #removeEntry()} any
     * exception that occurs is wrapped in a {@link java.lang.RuntimeException}
     * 
     * @see #removeEntry()
     */
    public void remove() {
        try {
            removeEntry();
        }
        catch (ARException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Removes the current entry
     * 
     * @throws ARException if an AR System error occurs
     * @throws IllegalStateException if an attempt is made to remove the entry twice.
     */
    public void removeEntry() throws ARException, IllegalStateException {
        if (currentEntry == null) {
            throw new IllegalStateException();
        }
        currentEntry.setContext(context);
        currentEntry.remove();
        currentEntry = null;
        absoluteIndex--;
        numMatches--;
    }
    /**
     * Set the fields that should be returned for each entry or null to return all fields
     * @param fieldList the fields to return
     */
    public void setEntryListFieldInfo(EntryListFieldInfo[] fieldList) {
        this.ec = new EntryCriteria(fieldList);
    }
    
    /**
     * Set the fields that should be returned for each entry. This is a convenience method so that you can use
     * <pre>
     * EntryIterator itr = ...
     * itr.setEntryListFieldInfo(1,8);
     * </pre>
     * in stead of:
     * <pre>
     * EntryIterator itr = ...
     * itr.setEntryListFieldInfo(new EntryListFieldInfo[] {new EntryListFieldInfo(new FieldID(1)), new EntryListFieldInfo(new FieldID(8))});
     * </pre>
     * 
     * 
     * @param fieldIds the fields to return
     * @see #setEntryListFieldInfo(EntryListFieldInfo[])
     */
    public void setEntryListFieldInfo(long... fieldIds) {
        EntryListFieldInfo[] fieldInfo = new EntryListFieldInfo[fieldIds.length];
        for (int i=0; i < fieldIds.length; i++) {
            fieldInfo[i] = new EntryListFieldInfo(new FieldID(fieldIds[i]));
        }
        setEntryListFieldInfo(fieldInfo);
    }

    private void prepareEntries() throws ARException {        
        if (entryKeys == null || keyIndex >= entryKeys.length && !lastChunk && entryIndex >= entries.length) {
            nextKeyChunk();
            if (entryKeys.length == 0) {
                lastChunk = true;                
            }
        }
        
        if (entries == null || entryIndex >= entries.length) {
            nextEntryChunk();
        }
        
    }

    private void nextKeyChunk() throws ARException {
        EntryListCriteria elc = new EntryListCriteria(new NameID(form), qualifier, absoluteIndex, chunkSize, null,
                sorting, null);
        Integer matches = null;
        if (fastFail || numMatches == -1) {
            matches = new Integer(0);
        }
        entryKeys = EntryFactory.find(context, elc, false, matches);
        if (matches != null && matches.intValue() != numMatches && numMatches > -1 && fastFail) {
            throw new ConcurrentModificationException();
        }
        if (matches != null) {
            numMatches = matches.intValue();            
        }
        if (entryKeys == null) {
            entryKeys = new EntryKey[0];
        }
        if (resultIsLimited(context.getLastStatus())) {
            chunkSize = entryKeys.length;
        }
        keyIndex = 0;
    }

    private void nextEntryChunk() throws ARException {
        EntryListCriteria elc = new EntryListCriteria();
        elc.setSchemaID(new NameID(form));
        
        int size = Math.min(100, entryKeys.length - keyIndex);
        
        if (size <= 0) {
            entries = new Entry[0];
            entryIndex = 0;
            return;
        }
        
        EntryID[] ids = new EntryID[size];
        
        for (int i=keyIndex; i < keyIndex + size; i++) {
            ids[i-keyIndex] = entryKeys[i].getEntryID();
        }
        
        elc.setEntriesToRetrieve(ids);
        
        entries = EntryFactory.findObjects(context, elc, ec, false, null);
        if (entries == null) {
            entries = new Entry[0];
        }
        entryIndex = 0;
        keyIndex+=size;
    }

    private boolean resultIsLimited(StatusInfo[] status) {
        for (int i = 0; i < status.length; i++) {
            if (status[i].getMessageNum() == 72)
                return true;
        }
        return false;
    }
    
    public Iterator<Entry> iterator() {
        return this;
    }

}
