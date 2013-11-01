package java_lib_test;

import ralph.VariableStack;
import ralph.LockedVariables.LockedNumberVariable;
import ralph.LockedObject;
import ralph.Endpoint;
import ralph.LockedActiveEvent;

/**
   Creates an active event that reads and writes to tvar.  checks that
   can have multiple readers of tvar at same time.
 */

public class WriteReadTVar
{
    protected static String test_name = "WriteReadTVar";

    public static void main(String [] args)
    {
        if (WriteReadTVar.run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }


    public static boolean run_test()
    {
        Endpoint endpt = TestClassUtil.create_default_single_endpoint();
        LockedNumberVariable num_tvar =
            (LockedNumberVariable) endpt.global_var_stack.get_var_if_exists(
                TestClassUtil.NUM_TVAR_NAME);
        
        try
        {
            LockedActiveEvent rdr1 = endpt._act_event_map.create_root_event();
            LockedActiveEvent rdr2 = endpt._act_event_map.create_root_event();
            
            if (num_tvar.get_val(rdr1) != TestClassUtil.NUM_TVAR_INIT_VAL)
                return false;

            if (num_tvar.get_val(rdr2) != TestClassUtil.NUM_TVAR_INIT_VAL)
                return false;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        
        return true;
    }
    
}

