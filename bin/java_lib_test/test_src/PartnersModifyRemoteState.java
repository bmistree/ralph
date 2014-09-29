
package java_lib_test;

import java_lib_test.TestClassUtil.ConnectedEndpointPair;
import java_lib_test.TestClassUtil.DefaultEndpoint;
import ralph.Endpoint;
import ralph.RalphObject;
import ralph.Variables;
import ralph.ActiveEvent;
import ralph.ExecutingEventContext;
import java.util.ArrayList;
import ralph.RootEventParent;
import RalphCallResults.RootCallResult.ResultType;


/**
   Create two, connected endpoints.  Run a transaction across both of
   them.  Transaction should update the value of the state on the
   remote endpoint.  Check that it does.
 */
public class PartnersModifyRemoteState
{
    protected static final String test_name = "PartnersModifyRemoteState";

    ///Number of times to increment value on partner.
    private static final int NUM_TIMES_TO_RUN = 20;
    
    public static void main (String [] args)
    {
        if (run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    /**
       Each iteration with partner increments partners remote variable
       by 1.  Check after each call that get correct value.  This also
       tests to ensure that commits from previous updates take effect.
     */
    public static boolean run_test()
    {
        ConnectedEndpointPair endpoint_pair =
            TestClassUtil.create_connected_endpoints();
        DefaultEndpoint endpta = endpoint_pair.endpta;
        DefaultEndpoint endptb = endpoint_pair.endptb;

        // value that we expect to get when we poll other side.
        double expected_value =
            TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL.doubleValue();
        
        try
        {
            for (int i = 0; i < NUM_TIMES_TO_RUN; ++i)
            {
                expected_value += 1;

                Variables.NonAtomicNumberVariable num_var =
                    new Variables.NonAtomicNumberVariable(
                        false,endpta.ralph_globals);
                
                ActiveEvent root_event =
                    endpta._act_event_map.create_root_atomic_event(
                        null,endpta,"dummy");
                ExecutingEventContext ctx = endpta.create_context();

                ArrayList<RalphObject> arg_list = new ArrayList<RalphObject>();
                arg_list.add(num_var);
                
                ctx.hide_partner_call(
                    endpta, root_event,"test_increment_local_num",true,
                    arg_list,null);
                
                // check that can commit changes.
                root_event.local_root_begin_first_phase_commit();
                RootEventParent root_event_parent =
                    (RootEventParent)root_event.event_parent;
                ResultType commit_resp =
                    root_event_parent.event_complete_queue.take();
                
                if (commit_resp != ResultType.COMPLETE)
                    return false;

                
                // check that value on other side contains correct
                // result.
                ActiveEvent check_event =
                    endptb._act_event_map.create_root_atomic_event(
                        null,endptb,"dummy");
                double other_side_value =
                    endptb.num_tvar.get_val(check_event).doubleValue();
                check_event.local_root_begin_first_phase_commit();
                RootEventParent check_event_parent =
                    (RootEventParent)check_event.event_parent;
                ResultType commit_resp_check =
                    check_event_parent.event_complete_queue.take();
                
                if (commit_resp_check != ResultType.COMPLETE)
                    return false;
                
                if (other_side_value != expected_value)
                    return false;
            }
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