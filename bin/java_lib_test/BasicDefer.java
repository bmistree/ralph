package java_lib_test;

import ralph.VariableStack;
import ralph.Variables.LockedNumberVariable;
import ralph.LockedObject;
import ralph.Endpoint;
import ralph.VariableStack;
import ralph.DeferBlock;
import ralph.ActiveEvent;


public class BasicDefer
{
    protected static String test_name = "BasicDefer";
    
    /**
       Test pushes a function stack frame to the stack.  Then, it adds
       a defer to that frame.  Then it pops the frame.  Checks that
       defer gets executed.
     */
    public static void main (String [] args)
    {
        if (BasicDefer.run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static boolean run_test()
    {
        Endpoint endpt = TestClassUtil.create_default_single_endpoint();
        
        ActiveEvent active_event = null;
        try
        {
            active_event = endpt._act_event_map.create_root_atomic_event(null);
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
        
        // push two function frames
        endpt.global_var_stack.push(true);
        endpt.global_var_stack.push(true);

        // add a defer to one
        DeferBlock defer_statement = new DeferBlock(
            endpt.global_var_stack,active_event)
        {
            @Override
            public void run()
            {
                LockedNumberVariable num_tvar =
                    (LockedNumberVariable) vstack.get_var_if_exists(
                        TestClassUtil.DefaultEndpoint.NUM_TVAR_NAME);
                try
                {
                    Double val = num_tvar.get_val(active_event);
                    Double incremented_val = new Double(val.doubleValue() + 1);
                    num_tvar.set_val(active_event,incremented_val);
                }
                catch (Exception _ex)
                {
                    // NOTE: require try catch here, but 
                    _ex.printStackTrace();
                    assert(false);
                }
            }
        };
        endpt.global_var_stack.add_defer(defer_statement);
        endpt.global_var_stack.pop();
        endpt.global_var_stack.pop();

        // read the num_tvar to ensure that it got incremented
        
        LockedNumberVariable num_tvar =
            (LockedNumberVariable) endpt.global_var_stack.get_var_if_exists(
                TestClassUtil.DefaultEndpoint.NUM_TVAR_NAME);

        try
        {
            double current_val = num_tvar.get_val(active_event).doubleValue();
            double expected_val =
                TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL.doubleValue() + 1;
            if (current_val != expected_val)
                return false;
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }    
        
        // test passed
        return true;
    }
}