
package java_lib_test;

import java_lib_test.TestClassUtil.ConnectedEndpointPair;
import java_lib_test.TestClassUtil.DefaultEndpoint;
import ralph.Endpoint;
import ralph.LockedActiveEvent;
import ralph.RPCArgObject;
import ralph.ExecutingEventContext;
import java.util.ArrayList;

/**
   Create two, connected endpoints.  Run a transaction across both of
   them.  No conflicting transaction in meantime.
 */
public class PartnersNoConflict
{
    protected static final String test_name = "PartnersNoConflict";
    
    public static void main (String [] args)
    {
        if (PartnersNoConflict.run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static boolean run_test()
    {
        ConnectedEndpointPair endpoint_pair =
            TestClassUtil.create_connected_endpoints();
        DefaultEndpoint endpta = endpoint_pair.endpta;
        DefaultEndpoint endptb = endpoint_pair.endptb;

        try
        {
            LockedActiveEvent root_event =
                endpta._act_event_map.create_root_event();
            ExecutingEventContext ctx = endpta.create_context();

            ctx.hide_partner_call(
                endpta, root_event,"test_partner_method",true,
                new ArrayList<RPCArgObject> (), true);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("\n\nCaught an exception\n\n");
            return false;
        }
        return true;
    }
}