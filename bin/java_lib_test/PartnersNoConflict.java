
package java_lib_test;

import java_lib_test.TestClassUtil.ConnectedEndpointPair;
import java_lib_test.TestClassUtil.DefaultEndpoint;
import ralph.Endpoint;
import ralph.LockedObject;
import ralph.LockedVariables;
import ralph.LockedActiveEvent;
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
       Test that can call an rpc on remote endpoint with
       a reference argument
     */
    public static boolean ref_args_test()
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

            // grab num object from base scope
            LockedObject<Double,Double> num_obj =
                (LockedObject<Double,Double>)endpta.global_var_stack.get_var_if_exists(
                    endpta.NUM_TVAR_NAME);

            // push a new scope onto scope stack and then add boolean
            // and string variables to it.
            endpta.global_var_stack.push();
            String bool_var_name = "bool_var_name";
            String string_var_name = "str_var_name";

            Boolean init_boolean_value = new Boolean(false);
            String init_string_value = "a";
            
            LockedVariables.SingleThreadedLockedTrueFalseVariable bool_var =
                new LockedVariables.SingleThreadedLockedTrueFalseVariable(
                    endpta._host_uuid, false, init_boolean_value);
            
            LockedVariables.SingleThreadedLockedTextVariable string_var =
                new LockedVariables.SingleThreadedLockedTextVariable(
                    endpta._host_uuid, false, init_string_value);
            
            endpta.global_var_stack.add_var(bool_var_name,bool_var);
            endpta.global_var_stack.add_var(string_var_name,string_var);

            // Issue rpc call to partner
            // set up rpc arguments: pass all as references
            ArrayList<RPCArgObject> arg_list = new ArrayList<RPCArgObject>();
            arg_list.add(new RPCArgObject(num_obj,true));
            arg_list.add(new RPCArgObject(bool_var,true));
            arg_list.add(new RPCArgObject(string_var,true));

            
            // actually make call with arguments on partner endpoint
            ctx.hide_partner_call(
                endpta, root_event,"test_partner_args_method",true,
                arg_list, true);

            
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
            LockedActiveEvent root_event =
                endpta._act_event_map.create_root_event();
            ExecutingEventContext ctx = endpta.create_context();

            ctx.hide_partner_call(
                endpta, root_event,"test_partner_method",true,
                new ArrayList<RPCArgObject> (), true);

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