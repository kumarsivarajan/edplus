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

import java.util.Iterator;

import com.remedy.arsys.api.ARException;
import com.remedy.arsys.api.ARServerUser;
import com.remedy.arsys.api.ActiveLink;
import com.remedy.arsys.api.ActiveLinkCriteria;
import com.remedy.arsys.api.ActiveLinkFactory;
import com.remedy.arsys.api.Container;
import com.remedy.arsys.api.ContainerCriteria;
import com.remedy.arsys.api.ContainerFactory;
import com.remedy.arsys.api.ContainerListCriteria;
import com.remedy.arsys.api.ContainerType;
import com.remedy.arsys.api.Escalation;
import com.remedy.arsys.api.EscalationCriteria;
import com.remedy.arsys.api.EscalationFactory;
import com.remedy.arsys.api.Field;
import com.remedy.arsys.api.FieldCriteria;
import com.remedy.arsys.api.FieldFactory;
import com.remedy.arsys.api.FieldListCriteria;
import com.remedy.arsys.api.Filter;
import com.remedy.arsys.api.FilterCriteria;
import com.remedy.arsys.api.FilterFactory;
import com.remedy.arsys.api.Menu;
import com.remedy.arsys.api.MenuCriteria;
import com.remedy.arsys.api.MenuFactory;
import com.remedy.arsys.api.MenuListCriteria;
import com.remedy.arsys.api.NameID;
import com.remedy.arsys.api.Schema;
import com.remedy.arsys.api.SchemaCriteria;
import com.remedy.arsys.api.SchemaFactory;
import com.remedy.arsys.api.SchemaListCriteria;
import com.remedy.arsys.api.SchemaType;
import com.remedy.arsys.api.View;
import com.remedy.arsys.api.ViewCriteria;
import com.remedy.arsys.api.ViewFactory;
import com.remedy.arsys.api.ViewListCriteria;
import com.remedy.arsys.api.WorkflowObjectListCriteria;


/**
 * An Iterator to iterate server objects. Use the static newXxxIterable to create an instance of this class. <br />
 * Example:
 * <pre>
 * ARServerUser context = ...
 * for (ActiveLink link : ARObjectIterator.newActiveLinkIterable(context, null)) {
 *      // do something with link
 * }
 * </pre>
 * @author Hugo Visser
 */
public class ARObjectIterator<T> implements Iterator<T>, Iterable<T> {
    private Class type;
    private Object criteriaObject;
    private ARServerUser context;
    private T[] elements;
    private int pos;
    private NameID[] keys;
    private int block = 0;
    private boolean lastBlock = false;
    
    // constructor for ActiveLink, Filter, Escalation
    private ARObjectIterator(ARServerUser context, WorkflowObjectListCriteria wlc,  Class objectType) {
        this.type = objectType;
        this.criteriaObject = wlc;
        this.context = context;        
    }
    
    private ARObjectIterator(ARServerUser context, SchemaListCriteria slc) {
        this.type = Schema.class;
        this.criteriaObject = slc;
        this.context = context;        
    }
    
    private ARObjectIterator(ARServerUser context, ContainerListCriteria clc) {
        this.type = Container.class;
        this.criteriaObject = clc;
        this.context = context;
    }

    private ARObjectIterator(ARServerUser context, MenuListCriteria mlc) {
        this.type = Menu.class;
        this.criteriaObject = mlc;
        this.context = context;
    }

    private ARObjectIterator(ARServerUser context, FieldListCriteria flc) throws IllegalArgumentException {
        if (flc == null) {
            throw new IllegalArgumentException("flc is null");
        }
        this.type = Field.class;
        this.criteriaObject = flc;
        this.context = context;
    }

    private ARObjectIterator(ARServerUser context, ViewListCriteria vlc) throws IllegalArgumentException {
        if (vlc == null) {
            throw new IllegalArgumentException("vlc is null");
        }
        this.type = View.class;
        this.criteriaObject = vlc;
        this.context = context;
    }

    public boolean hasNext() {
        if ((elements == null || pos >= elements.length)) {
            try {
                getNextBlock();
            }
            catch (ARException e) {
                throw new RuntimeException(e);
            }
        }        
        return elements != null;
    }

    public T next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }
        return elements[pos++];
    }

    public void remove() {
        
    }

    public Iterator<T> iterator() {
        return this;
    }

    @SuppressWarnings("unchecked")
    private void getNextBlock() throws ARException {
        if (lastBlock) {
            pos = 0;
            elements = null;
            return;
        }
        
        if (type == ActiveLink.class) {
            elements = (T[]) getNextBlockActiveLink();
        }
        else 
        if (type == Filter.class) {
            elements = (T[]) getNextBlockFilter();
        }
        else
        if (type == Escalation.class) {
            elements = (T[]) getNextBlockEscalation();
        }
        else
        if (type == Menu.class) {
            elements = (T[]) getMenus();
            lastBlock = true;
        }
        else
        if (type == Schema.class) {
            elements = (T[]) getSchemas();
            lastBlock = true;
        }
        else
        if (type == Container.class) {
            elements = (T[])getContainers();
            lastBlock = true;
        }
        else 
        if (type == Field.class) {
            elements = (T[])getFields();
            lastBlock = true;
        }
        else 
        if (type == View.class) {
            elements = (T[])getViews();
            lastBlock = true;
        }
        
        if (elements == null) {
            lastBlock = true;
        }
        
        pos = 0;            
    }
    
    private View[] getViews() throws ARException {
        ViewListCriteria vlc = (ViewListCriteria)criteriaObject;
        ViewCriteria vc = new ViewCriteria();
        vc.setRetrieveAll(true);
        return ViewFactory.findObjects(context, vlc, vc);
    }
    
    private Field[] getFields() throws ARException {
        FieldListCriteria flc = (FieldListCriteria)criteriaObject;            
        FieldCriteria fc = new FieldCriteria();
        fc.setRetrieveAll(true);
        return FieldFactory.findObjects(context, flc, fc);
    }
    
    private Container[] getContainers() throws ARException {
        if (criteriaObject == null) {
            criteriaObject = new ContainerListCriteria();
            ContainerListCriteria clc = (ContainerListCriteria)criteriaObject;
            clc.setAttribute(true);
            clc.setTypes(new ContainerType[] {ContainerType.ALL});
        }
        ContainerCriteria cc = new ContainerCriteria();
        cc.setRetrieveAll(true);
        return ContainerFactory.findObjects(context, (ContainerListCriteria)criteriaObject, cc);
    }
    
    private Schema[] getSchemas() throws ARException {
        if (criteriaObject == null) {
            criteriaObject = new SchemaListCriteria(SchemaType.ALL, true, null, null);
        }
        if (keys == null) {
            keys = SchemaFactory.find(context, (SchemaListCriteria)criteriaObject);
        }
        SchemaCriteria sc = new SchemaCriteria();
        sc.setRetrieveAll(true);
        return SchemaFactory.findObjects(context, (SchemaListCriteria)criteriaObject, sc);
    }
    
    private Menu[] getMenus() throws ARException {
        if (criteriaObject == null) {
            criteriaObject = new MenuListCriteria();
        }
        MenuCriteria mc = new MenuCriteria();
        mc.setRetrieveAll(true);
        return MenuFactory.findObjects(context, (MenuListCriteria)criteriaObject, mc);
    }

    private ActiveLink[] getNextBlockActiveLink() throws ARException {
        if (criteriaObject == null) {
            criteriaObject = new WorkflowObjectListCriteria();
        }
        if (keys == null) {
            keys = ActiveLinkFactory.find(context, (WorkflowObjectListCriteria) criteriaObject);
            block = 0;
        }
        WorkflowObjectListCriteria wlc = (WorkflowObjectListCriteria)criteriaObject;
        int size = Math.min(keys.length - (block * 100), 100);
        if (size <= 0) {            
            return null;
        }
        else {
            NameID[] k = new NameID[size];
            for (int i=0; i < size; i++) {
                k[i] = keys[block*100 + i];
            }
            ActiveLinkCriteria alc = new ActiveLinkCriteria();
            alc.setRetrieveAll(true);
            wlc.setWorkflowNames(k);
            block++;            
            return ActiveLinkFactory.findObjects(context, wlc, alc);
        }
    }
    
    private Filter[] getNextBlockFilter() throws ARException {
        if (criteriaObject == null) {
            criteriaObject = new WorkflowObjectListCriteria();
        }
        if (keys == null) {
            keys = FilterFactory.find(context, (WorkflowObjectListCriteria) criteriaObject);
            block = 0;
        }
        WorkflowObjectListCriteria wlc = (WorkflowObjectListCriteria)criteriaObject;
        int size = Math.min(keys.length - (block * 100), 100);
        if (size <= 0) {
            return null;
        }
        else {
            NameID[] k = new NameID[size];
            for (int i=0; i < size; i++) {
                k[i] = keys[block*100 + i];
            }
            FilterCriteria flc = new FilterCriteria();
            flc.setRetrieveAll(true);
            wlc.setWorkflowNames(k);
            block++;            
            return FilterFactory.findObjects(context, wlc, flc);
        }
    }

    private Escalation[] getNextBlockEscalation() throws ARException {
        if (criteriaObject == null) {
            criteriaObject = new WorkflowObjectListCriteria();
        }
        if (keys == null) {
            keys = EscalationFactory.find(context, (WorkflowObjectListCriteria) criteriaObject);
            block = 0;
        }
        WorkflowObjectListCriteria wlc = (WorkflowObjectListCriteria)criteriaObject;
        int size = Math.min(keys.length - (block * 100), 100);
        if (size <= 0) {
            return null;
        }
        else {
            NameID[] k = new NameID[size];
            for (int i=0; i < size; i++) {
                k[i] = keys[block*100 + i];
            }
            EscalationCriteria elc = new EscalationCriteria();
            elc.setRetrieveAll(true);
            wlc.setWorkflowNames(k);
            block++;            
            return EscalationFactory.findObjects(context, wlc, elc);
        }
    }
    /**
     * Create an iterable that iterates {@link ActiveLink}s
     * @param context the server context
     * @param wlc criteria that the objects should match or null to iterate all objects
     * @return the iterable
     */
    public static Iterable<ActiveLink> newActiveLinkIterable(ARServerUser context, WorkflowObjectListCriteria wlc) {
        return new ARObjectIterator<ActiveLink>(context, wlc, ActiveLink.class);
    }

    /**
     * Create an iterable that iterates {@link Filter}s
     * @param context the server context
     * @param wlc criteria that the objects should match or null to iterate all objects
     * @return the iterable
     */
    public static Iterable<Filter> newFilterIterable(ARServerUser context, WorkflowObjectListCriteria wlc) {
        return new ARObjectIterator<Filter>(context, wlc, Filter.class);
    }
    
    /**
     * Create an iterable that iterates {@link Escalation}s
     * @param context the server context
     * @param wlc criteria that the objects should match or null to iterate all objects
     * @return the iterable
     */
    public static Iterable<Escalation> newEscalationIterable(ARServerUser context, WorkflowObjectListCriteria wlc) {
        return new ARObjectIterator<Escalation>(context, wlc, Escalation.class);
    }

    /**
     * Create an iterable that iterates {@link Schema}s
     * @param context the server context
     * @param slc criteria that the objects should match or null to iterate all objects
     * @return the iterable
     */
    public static Iterable<Schema> newSchemaIterable(ARServerUser context, SchemaListCriteria slc) {
        return new ARObjectIterator<Schema>(context, slc);
    }

    /**
     * Create an iterable that iterates {@link Container}s (guides, packing lists etc)
     * @param context the server context
     * @param clc criteria that the objects should match or null to iterate all objects
     * @return the iterable
     */
    public static Iterable<Container> newContainerIterable(ARServerUser context, ContainerListCriteria clc) {
        return new ARObjectIterator<Container>(context, clc);
    }

    /**
     * Create an iterable that iterates {@link Menu}s
     * @param context the server context
     * @param mlc criteria that the objects should match or null to iterate all objects
     * @return the iterable
     */
    public static Iterable<Menu> newMenuIterable(ARServerUser context, MenuListCriteria mlc) {
        return new ARObjectIterator<Menu>(context, mlc);
    }
    
    /**
     * Create an iterable that iterates {@link Field}s
     * @param context the server context
     * @param flc criteria that the objects should match
     * @return the iterable
     * @throws IllegalArgumentException if flc is null
     */
    public static Iterable<Field> newFieldIterable(ARServerUser context, FieldListCriteria flc) throws IllegalArgumentException {
        return new ARObjectIterator<Field>(context, flc);
    }

    /**
     * Create an iterable that iterates {@link View}s
     * @param context the server context
     * @param vlc criteria that the objects should match
     * @return the iterable
     * @throws IllegalArgumentException if flc is null
     */
    public static Iterable<View> newViewIterable(ARServerUser context, ViewListCriteria vlc) throws IllegalArgumentException {
        return new ARObjectIterator<View>(context, vlc);
    }
}
