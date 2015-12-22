Here are some code samples to get you started using the library

# Connecting to a server #
Connecting to a server is easy, just create an instance of the ```Server``` class like so:

`Server server = new Server("Demo", "demopass", "myserver");`

You might want to set a TCP port:

`server.setPort(1234);`

Or you might want to do this all at once using the syntax user:password@server:port like so:

`Server server = new Server("Demo:demopass@myserver:1234");`

Now that you've created the server connection, you can get the `ARServerUser` object using `server.getContext()` to pass in to the normal AR System API or one of the other classes.

# Performing a query #
To perform a query and iterate the entries you can either use the `Server` object or an `EntryIterator`. The query will be performed in a server friendly way, using chunks. This way querying a table with thousands of records won't hang the server.

The following code snippet shows how to do it using the `Server` object:

```
Server server = new Server("Demo:demopass@myserver:1234");
// Now we query the User form to return all entries
for (Entry e : server.query("User")) {
  // do something with the entry "e" here
}
```

# Creating a new entry in a form #
The `EntryUtil` class can be used to convert a `Map<Long, Value>` to an `Entry` object like this:
```
// create the map using the field id as a key and the `Value` as the value
Map<Long, Value> entryMap = new HashMap<Long, Value>();

// now we set some fields, to be safe add "L" to the field id values as the field id might overflow an int
entryMap.put(536870921L, new Value("This is a character value"));
// lets set field 536870922 to $NULL$
entryMap.put(535870922L, new Value());
// set the status to "new"
entryMap.put(7L, new Value(DataType.ENUM, 0));

// now convert it to a real Entry object, to be submitted in Test Form
Entry entry = EntryUtil.toEntry("Test Form", entryMap);
// set the ARServerUser context on it...
entry.setContext(server.getContext());
// and create it
entry.create();
```

If the request id field is put in the map, you can also update or merge an entry this way. `EntryUtil` will set the entry id on the new entry
and also figure out if that has to be an `EntryID` or a `JoinEntryID`

# Putting it all together #

The following example shows a typical use of the library.
```
import java.util.Map;

import com.expertdesk.edplus.EntryIterator;
import com.expertdesk.edplus.EntryUtil;
import com.expertdesk.edplus.Server;
import com.remedy.arsys.api.ARException;
import com.remedy.arsys.api.Entry;
import com.remedy.arsys.api.Value;


public class Example {
    public static void main(String[] args) throws ARException {
        Server server = new Server("Demo:demopass@myserver:1234");        
        EntryIterator itr = new EntryIterator(server.getContext(), "User");
        // We can either use the field label or the field id. When using the field label, the Locale of the user
        // is used to determine the view for the field labels.
        // So while we can use this:
        // itr.setQualification("'Login name' = \"Bob\"");
        // it is recommended to use this:
        itr.setQualification("'101' = \"Bob\"");
        // We like to retrieve only the field id's for login name, full name and email address
        itr.setEntryListFieldInfo(101,8,103);
        
        if (itr.getNumMatches() > 0) {
            System.out.println("We have a Bob in the system!");
            // now we iterate all matching records.
            for (Entry e : itr) {
                Map<Long, Value> entryMap = EntryUtil.toEntryMap(e);
                // print out the email adress
                System.out.println(entryMap.get(103).toString());
            }
        }
        else {
            System.out.println("There is no Bob in the system.");
        }
    }
}
```

# Iterating server objects #
The edplus library also contains the `ARObjectIterator` that allows you to iterate server objects, such as Filters and Forms. The `Server` class provides utility methods to this class. For example:
```
Server server = new Server("Demo:demopass@myserver:1234");        
for (Schema schema : server.getSchemas()) {
 // do something with schema
}
```

# More Examples #
...are in svn here: http://edplus.googlecode.com/svn/trunk/example/