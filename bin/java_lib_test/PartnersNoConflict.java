
package java_lib_test;

import java_lib_test.TestClassUtil.ConnectedEndpointPair;
import java_lib_test.TestClassUtil.DefaultEndpoint;
import ralph.Endpoint;
import ralph.RalphObject;
import ralph.Variables;
import ralph.ActiveEvent;
import ralph.RPCArgObject;
import ralph.ExecutingEventContext;
import java.util.ArrayList;
import ralph.RootEventParent;
import RalphCallResults.RootCallResult.ResultType;


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
        return
            PartnersNoConflict.no_args_test() &&
            PartnersNoConflict.ref_args_test();
    }


    /**
       Test that can call an rpc on remote endpoint with reference
       arguments and args passed as references will be updated, while
       args not passed as references will not.
     */
    public static boolean ref_args_test()
    {
        // iterates through all permutations of which rpc arguments
        // are references and which are not.
        for (int i = 0; i <= 1; ++i)
        {
            for (int j =0; j <= 1; ++j)
            {
                for (int k=0; k <= 1; ++k)
                {
                    if (! ref_args_combination(i != 0, j != 0, k != 0))
                        return false;
                }
            }
        }
        return true;
    }
    
    
    /**
       Test that can call an rpc on remote endpoint with
       a reference argument

       @param {boolean} num_arg_ref --- True if should pass a number
       to other side as a reference.  False otherwise.

       @param {boolean} bool_arg_ref --- "" boolean ""

       @param {boolean} string_arg_ref --- "" string ""
       
     */
    public static boolean ref_args_combination(
        boolean num_arg_ref, boolean bool_arg_ref,
        boolean string_arg_ref)
    {
        ConnectedEndpointPair endpoint_pair =
            TestClassUtil.create_connected_endpoints();
        DefaultEndpoint endpta = endpoint_pair.endpta;
        DefaultEndpoint endptb = endpoint_pair.endptb;
        
        try
        {
            ActiveEvent root_event =
                endpta._act_event_map.create_root_atomic_event(null);
            ExecutingEventContext ctx = endpta.create_context();

            // grab num object from base scope
            RalphObject<Double,Double> num_obj =
                (RalphObject<Double,Double>)endpta.global_var_stack.get_var_if_exists(
                    endpta.NUM_TVAR_NAME);

            // push a new scope onto scope stack and then add boolean
            // and string variables to it.
            endpta.global_var_stack.push(false);
            String bool_var_name = "bool_var_name";
            String string_var_name = "str_var_name";

            Boolean init_boolean_value = new Boolean(false);
            String init_string_value = "a";
            
            Variables.SingleThreadedLockedTrueFalseVariable bool_var =
                new Variables.SingleThreadedLockedTrueFalseVariable(
                    endpta._host_uuid, false, init_boolean_value);
            
            Variables.SingleThreadedLockedTextVariable string_var =
                new Variables.SingleThreadedLockedTextVariable(
                    endpta._host_uuid, false, init_string_value);
            
            endpta.global_var_stack.add_var(bool_var_name,bool_var);
            endpta.global_var_stack.add_var(string_var_name,string_var);

            // Issue rpc call to partner
            // set up rpc arguments: pass all as references
            ArrayList<RPCArgObject> arg_list = new ArrayList<RPCArgObject>();
            arg_list.add(new RPCArgObject(num_obj,num_arg_ref));
            arg_list.add(new RPCArgObject(bool_var,bool_arg_ref));
            arg_list.add(new RPCArgObject(string_var,string_arg_ref));

            
            // actually make call with arguments on partner endpoint
            ctx.hide_partner_call(
                endpta, root_event,"test_partner_args_method",true,
                arg_list);

            
            // check that commit worked
            root_event.begin_first_phase_commit();
            RootEventParent root_event_parent =
                (RootEventParent)root_event.event_parent;
            ResultType commit_resp =
                root_event_parent.event_complete_queue.take();
            
            if (commit_resp != ResultType.COMPLETE)
                return false;

            // check that the methods on partner endpoint updated the
            // values of local variables correctly
            ActiveEvent check_event =
                endpta._act_event_map.create_root_atomic_event(null);
            
            // determine what the lcal values should be
            double expected_final_double_value =
                TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL.doubleValue();
            boolean expected_final_boolean_value =
                init_boolean_value.booleanValue();
            String expected_final_string_value =init_string_value;

            if (num_arg_ref)
                expected_final_double_value += 1;
            if (bool_arg_ref)
                expected_final_boolean_value = !expected_final_boolean_value;
            if (string_arg_ref)
                expected_final_string_value += expected_final_string_value;
                

            double recovered_num =
                ((Double)num_obj.get_val(check_event)).doubleValue();
            boolean recovered_bool =
                ((Boolean) bool_var.get_val(check_event)).booleanValue();
            String recovered_string =
                (String) string_var.get_val(check_event);

            if (recovered_num != expected_final_double_value)
                return false;
            
            if (recovered_bool != expected_final_boolean_value)
                return false;
            
            if (!recovered_string.equals(expected_final_string_value))
                return false;

            // reset number variable to init val (may have been
            // modified if passed as a reference).
            num_obj.set_val(
                check_event,
                TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL);
            check_event.begin_first_phase_commit();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("\n\nCaught an exception\n\n");
            return false;
        }
        return true;
    }
    

    /**
       Test that can call an rpc on remote endpoint without passing
       any arguments.
     */
    public static boolean no_args_test()
    {
        ConnectedEndpointPair endpoint_pair =
            TestClassUtil.create_connected_endpoints();
        DefaultEndpoint endpta = endpoint_pair.endpta;
        DefaultEndpoint endptb = endpoint_pair.endptb;

        try
        {
            ActiveEvent root_event =
                endpta._act_event_map.create_root_atomic_event(null);

            ExecutingEventContext ctx = endpta.create_context();

            ctx.hide_partner_call(
                endpta, root_event,"test_partner_method",true,
                new ArrayList<RPCArgObject> ());

            root_event.begin_first_phase_commit();
            RootEventParent root_event_parent =
                (RootEventParent)root_event.event_parent;
            ResultType commit_resp =
                root_event_parent.event_complete_queue.take();
            
            if (commit_resp != ResultType.COMPLETE)
                return false;
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