package java_lib_test;

import ralph.LockedObject;
import ralph.VariableStore;
import ralph.LockedVariables.LockedNumberVariable;
import ralph.RalphGlobals;
import ralph.Endpoint;
import RalphConnObj.SingleSideConnection;

public class TestClassUtil
{
    public static final String NUM_TVAR_NAME = "num_tvar";
    public static final Double NUM_TVAR_INIT_VAL = new Double (5);
    
    public static void print_success(String test_name)
    {
        System.out.println("Test " + test_name + " .....");
        System.out.println("    SUCCESS\n");
    }
    public static void print_failure(String test_name)
    {
        System.out.println("Test " + test_name + " .....");
        System.out.println("    FAILURE\n");
    }

    
    /**
       @returns {Endpoint} --- Contains variables in base stack frame
       with number tvar named NUM_TVAR_NAME and init val
       NUM_TVAR_INIT_VAL.
     */
    public static Endpoint create_default_single_endpoint()
    {
        String dummy_host_uuid = "dummy_host_uuid";

        VariableStore vstore = new VariableStore();
        
        // adding a number tvar
        vstore.add_var(
            NUM_TVAR_NAME,
            new LockedNumberVariable(
                dummy_host_uuid,false, NUM_TVAR_INIT_VAL));

        Endpoint to_return = new Endpoint(
            new RalphGlobals(),
            dummy_host_uuid,
            new SingleSideConnection(),
            vstore);


        return to_return;
    }
    
}