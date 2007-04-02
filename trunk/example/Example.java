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
