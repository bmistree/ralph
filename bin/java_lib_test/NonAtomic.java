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

public class NonAtomic
{
    protected static String test_name = "NonAtomic";

    public static void main(String [] args)
    {
        if (NonAtomic.run_test())
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
        if (! NonAtomic.test_non_atomic_read(endpt,num_tvar))
            return false;

        if (! NonAtomic.test_concurrent_non_atomic_reads_and_writes(endpt,num_tvar))
            return false;

        return true;
    }

    /**
       Two non-atomics should be able to read and write to data
       concurrently before either commit.
     */
    public static boolean test_concurrent_non_atomic_reads_and_writes(
        Endpoint endpt, LockedNumberVariable num_tvar)
    {
        try
        {
            for (int i =0; i < 20; ++i)
            {
                ActiveEvent reader =
                    endpt._act_event_map.create_root_non_atomic_event();
                ActiveEvent writer =
                    endpt._act_event_map.create_root_non_atomic_event();

                // atomics reader/writers would not be able to execute
                // in parallel.
                num_tvar.get_val(reader);
                num_tvar.set_val(writer,new Double(359));

                reader.begin_first_phase_commit();
                writer.begin_first_phase_commit();
                
                RootEventParent reader_event_parent =
                    (RootEventParent)reader.event_parent;
                RootEventParent writer_event_parent =
                    (RootEventParent)writer.event_parent;

                ResultType reader_commit_resp =
                    reader_event_parent.event_complete_queue.take();
                ResultType writer_commit_resp =
                    writer_event_parent.event_complete_queue.take();
                
                if (reader_commit_resp != ResultType.COMPLETE)
                    return false;
                if (writer_commit_resp != ResultType.COMPLETE)
                    return false;                
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    
    public static boolean test_non_atomic_read(
        Endpoint endpt, LockedNumberVariable num_tvar)
    {
        try
        {
            for (int i = 0; i < 20; ++i)
            {
                ActiveEvent reader =
                    endpt._act_event_map.create_root_non_atomic_event();

                if (! num_tvar.get_val(reader).equals(
                        TestClassUtil.DefaultEndpoint.NUM_TVAR_INIT_VAL))
                {
                    return false;
                }
                reader.begin_first_phase_commit();
                RootEventParent reader_event_parent =
                    (RootEventParent)reader.event_parent;
                ResultType reader_commit_resp =
                    reader_event_parent.event_complete_queue.take();

                if (reader_commit_resp != ResultType.COMPLETE)
                    return false;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    
}

