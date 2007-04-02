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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.remedy.arsys.api.ARException;
import com.remedy.arsys.api.ARServerUser;
import com.remedy.arsys.api.ActiveLink;
import com.remedy.arsys.api.Container;
import com.remedy.arsys.api.Entry;
import com.remedy.arsys.api.Escalation;
import com.remedy.arsys.api.Field;
import com.remedy.arsys.api.FieldListCriteria;
import com.remedy.arsys.api.FieldType;
import com.remedy.arsys.api.Filter;
import com.remedy.arsys.api.Menu;
import com.remedy.arsys.api.NameID;
import com.remedy.arsys.api.Schema;
import com.remedy.arsys.api.Util;
import com.remedy.arsys.api.View;
import com.remedy.arsys.api.ViewListCriteria;

/**
 * The server object wraps a connection to the AR System, and is a starting point for common tasks.
 * 
 * @author Hugo Visser
 * 
 */
public class Server {
    private ARServerUser context;
    private final static String LOGIN_REGEX = "(.*?)(?::(.*?))?@(.*?)(?::([0-9]+){1}(?::([0-9]+))?)?";
    
    /**
     * Create a new Server object, wrapping an existing {@link ARServerUser} object
     * @param context the context to wrap
     */
    public Server(ARServerUser context) {
        this.context = context;
    }
    
    /**
     * Create a new Server from a login string. The login string must have the following format: user[:password]@server[:tcpPort[:rpcNum]].
     * To login as user Demo on server "test" with no password use "Demo@test"
     * @param login the login string
     * @throws IllegalArgumentException if the login string does not match the format.
     */
    public Server(String login) throws IllegalArgumentException {
        Pattern p = Pattern.compile(LOGIN_REGEX);
        Matcher m = p.matcher(login);
        if (!m.matches()) {
            throw new IllegalArgumentException();
        }
        String user = m.group(1);
        String pass = m.group(2);
        String server = m.group(3);
        int tcpPort = m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
        int rpcNum = m.group(5) != null ? Integer.parseInt(m.group(4)) : 0;
        this.context = new ARServerUser(user, pass, Locale.getDefault().toString(), server);
        setPortAndQueue(tcpPort, rpcNum);
    }
    
    /**
     * Create a new Server
     * @param user username
     * @param password password
     * @param authentication authentication 
     * @param locale locale
     * @param server server to login to
     * @param tcpPort the tcp port or 0 to use the portmapper
     * @param rpcNum the rpc number or 0 to use the default rpc number
     */
    public Server(String user, String password, String authentication, Locale locale, String server, int tcpPort, int rpcNum)
    {
        this.context = new ARServerUser(user, password, locale.toString(), server);
        context.setAuthentication(authentication);
        if (tcpPort > 0 || rpcNum > 0) {
           setPortAndQueue(tcpPort, rpcNum);
        }
    }
    /**
     * Create a new Server for the given user, password and server combination
     * @param user username
     * @param password password
     * @param server server to login to
     */
    public Server(String user, String password, String server) {
        this(user, password, null, Locale.getDefault(), server, 0, 0);
    }
    
    /**
     * A convienence method to set the locale.
     * @param locale the locale to set
     * @see ARServerUser#setLocale(String)
     */
    public void setLocale(Locale locale) {
        context.setLocale(locale.toString());
    }
    
    /**
     * A convenience method to set the authentication string.
     * @param authentication the authentication string to set
     * @see ARServerUser#setAuthentication(String)
     */
    public void setAuthentication(String authentication) {
        context.setAuthentication(authentication);
    }
    
    /**
     * Set the TCP port number and the RPC program number for this server
     * @param tcpPort the TCP port number
     * @param rpcNum the RPC program or queue number
     */
    public void setPortAndQueue(int tcpPort, int rpcNum) {
        try {
            Util.ARSetServerPort(context, new NameID(context.getServer()), tcpPort, rpcNum);
        }
        catch (ARException e) {
            // I've never seen an exception being thrown here
            // So for now we wrap it in a RuntimeException
            throw new RuntimeException(e);
        }
    }
    /**
     * Set the TCP port number for this server
     * @param tcpPort the TCP port number
     */
    public void setPort(int tcpPort) {
        setPortAndQueue(tcpPort, 0);
    }
        
    /**
     * Perform a query
     * @param form the form to query
     * @param qualification a qualification to use or null for no qualification
     * @return an {@link Iterable} to iterate the entries
     * @throws ARException
     * @see EntryIterator
     */
    public Iterable<Entry> query(String form, String qualification) throws ARException {
        EntryIterator itr = new EntryIterator(context, form, null);
        if (qualification != null) {
            itr.setQualification(qualification);            
        }
        return itr;
    }

    /**
     * Perform a query
     * @param form the form to query
     * @return an {@link Iterable} to iterate the entries
     * @throws ARException
     */
    public Iterable<Entry> query(String form) throws ARException {
        return query(form, null);
    }

    /**
     * Get a list of all active links on this server
     * @return an {@link Iterable} that can be used to iterate the active links
     */
    public Iterable<ActiveLink> getActiveLinks() {
        return ARObjectIterator.newActiveLinkIterable(context, null);
    }

    
    /**
     * Get a list of all filters on this server
     * @return an {@link Iterable} that can be used to iterate the filters
     */
    public Iterable<Filter> getFilters() {
        return ARObjectIterator.newFilterIterable(context, null);
    }

    /**
     * Get a list of all escalations on this server
     * @return an {@link Iterable} that can be used to iterate the escalations
     */
    public Iterable<Escalation> getEscalations() {
        return ARObjectIterator.newEscalationIterable(context, null);        
    }
    
    /**
     * Get a list of all menus on this server
     * @return an {@link Iterable} that can be used to iterate the menus
     */
    public Iterable<Menu> getMenus() {
        return ARObjectIterator.newMenuIterable(context, null);        
    }
    
    /**
     * Get a list of all schemas (forms) on this server
     * @return an {@link Iterable} that can be used to iterate the schemas
     */
    public Iterable<Schema> getSchemas() {
        return ARObjectIterator.newSchemaIterable(context, null);        
    }

    /**
     * Get a list of all containers (guides, packing lists, webservices etc) on this server
     * @return an {@link Iterable} that can be used to iterate the containers
     */
    public Iterable<Container> getContainers() {
        return ARObjectIterator.newContainerIterable(context, null);        
    }
    
    /**
     * Get a list of all fields on the specified form.
     * @param form the form to return the fields for.
     * @return an {@link Iterable} that can be used to iterate the fields
     */
    public Iterable<Field> getFields(String form) {
        FieldListCriteria flc = new FieldListCriteria(new NameID(form), null, FieldType.AR_ALL_FIELD);
        return ARObjectIterator.newFieldIterable(context, flc);
    }
    /**
     * Get a list of all views on the specified form
     * @param form the form to return the views for.
     * @return an {@link Iterable} that can be used to iterate the views
     */
    public Iterable<View> getViews(String form) {
        ViewListCriteria vlc = new ViewListCriteria(new NameID(form), null);
        return ARObjectIterator.newViewIterable(context, vlc);
    }
    
    /**
     * Get the {@link ARServerUser} context that this server wraps.
     * @return the {@link ARServerUser} for this server
     */
    public ARServerUser getContext() {
        return context;
    }
}
