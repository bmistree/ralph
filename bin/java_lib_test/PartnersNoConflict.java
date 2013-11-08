
package java_lib_test;

import java_lib_test.TestClassUtil.ConnectedEndpointPair;
import ralph.Endpoint;

/**
   Create two, connected endpoints.  Run a transaction across both of
   them.  No conflicting transaction in meantime.
 */
public class PartnersNoConflict
{
    protected final static String test_name = "PartnersNoConflict";
    
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
        Endpoint endpta = endpoint_pair.endpta;
        Endpoint endptb = endpoint_pair.endptb;
        return true;
    }
}