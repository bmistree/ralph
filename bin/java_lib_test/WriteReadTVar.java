package java_lib_test;

import ralph.VariableStack;
import ralph.Variables.LockedNumberVariable;
import ralph.RalphObject;
import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.RootEventParent;
import RalphCallResults.RootCallResult.ResultType;

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
                TestClassUtil.DefaultEndpoint.NUM_TVAR_NAME);

        // Tests concurrent read of tvar.
        if (! WriteReadTVar.test_concurrent_read(endpt,num_tvar))
            return false;

        if (! WriteReadTVar.test_preempted_read(endpt,num_tvar))
            return false;        

        return true;
    }

    /**
       @returns {boolean} --- Starts two events.  The second event
       that is started (the one with lower priority) reads num_tvar.
       The first event (the event with higher priority) then writes to
       num_tvar.  Returns true if first event preempts second.
     */
    public static boolean test_preempted_read(
        Endpoint endpt, LockedNumberVariable num_tvar)
    {
        try
        {
            ActiveEvent writer =
                endpt._act_event_map.create_root_atomic_event(null);
            ActiveEvent reader =
                endpt._act_event_map.create_root_atomic_event(null);

            if (! num_tvar.get_val(reader).equals(
                    TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL))
            {
                return false;
            }

            num_tvar.set_val(writer,TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL + 1);

            
            reader.begin_first_phase_commit();
            writer.begin_first_phase_commit();

            RootEventParent reader_event_parent =
                (RootEventParent)reader.event_parent;
            ResultType reader_commit_resp =
                reader_event_parent.event_complete_queue.take();

            RootEventParent writer_event_parent =
                (RootEventParent)writer.event_parent;
            ResultType writer_commit_resp = 
                writer_event_parent.event_complete_queue.take();

            
            if (reader_commit_resp == ResultType.COMPLETE)
                return false;

            if (writer_commit_resp != ResultType.COMPLETE)
                return false;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        
        return true;
    }

    
    /**
       @returns {boolean} --- True if test passed; false if test
       failed.  (Test passes if two read events are able to access
       variable and commit; false otherwise.)
     */
    public static boolean test_concurrent_read(
        Endpoint endpt, LockedNumberVariable num_tvar)
    {
        try
        {
            ActiveEvent rdr1 =
                endpt._act_event_map.create_root_atomic_event(null);
            ActiveEvent rdr2 =
                endpt._act_event_map.create_root_atomic_event(null);
            
            if (! num_tvar.get_val(rdr1).equals(
                    TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL))
                return false;

            if (! num_tvar.get_val(rdr2).equals(
                    TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL))
                return false;

            rdr1.begin_first_phase_commit();
            rdr2.begin_first_phase_commit();

            ((RootEventParent)rdr1.event_parent).event_complete_queue.take();
            ((RootEventParent)rdr2.event_parent).event_complete_queue.take();
            
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    
}

