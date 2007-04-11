import java.util.Map;

import com.expertdesk.edplus.EntryUtil;
import com.expertdesk.edplus.Server;
import com.remedy.arsys.api.ARException;
import com.remedy.arsys.api.DataType;
import com.remedy.arsys.api.Entry;
import com.remedy.arsys.api.Value;


/*
 * Created on 11-apr-2007
 *
 */

/**
 * Example for modifying an entry based on a query.
 * This example will modify all matching entries setting a selection field to the given value
 * @author visserh
 */
public class ModifyEntry {

    /**
     * @param args the login string, the form, the query, the field id to modify, the enum value to set
     * @throws ARException 
     */
    public static void main(String[] args) throws ARException {
        if (args.length < 5) {
            System.out.println("Need arguments: user:password@server[:tcpport] formname query fieldId selection-value");
            System.exit(1);
        }
        
        Server server = new Server(args[0]);
        long fieldId = 0;
        Value value = null;
        try {
            fieldId = Long.parseLong(args[3]);
            value = new Value(DataType.ENUM, Long.parseLong(args[4]));            
        }
        catch (NumberFormatException e) {
            System.err.println("The field id and selection value should be a number");
            System.exit(1);
        }
        
        for (Entry e : server.query(args[1], args[2])) {
            System.out.println("Updating entry: "+e.getEntryID());            
            Map<Long, Value> entryMap = EntryUtil.toEntryMap(e);
            // update the field
            entryMap.put(fieldId, value);
            // convert the map back to an entry and store (modify) it            
            e = EntryUtil.toEntry(args[1], entryMap);
            e.setContext(server.getContext());
            e.store();
        }

    }

}
