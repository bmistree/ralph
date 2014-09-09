package java_lib_test;

import ralph.VariableStack;
import ralph.Variables.AtomicMapVariable;
import ralph.RalphObject;
import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.RootEventParent;
import RalphCallResults.RootCallResult.ResultType;

/**
   Creates an active event that reads and writes to tvar.  checks that
   can have multiple readers of tvar at same time.
 */

public class MapTVarConflict
{
    protected static String test_name = "MapTVarConflict";
    public static final Double INSERTION_INDEX = new Double(30);
    public static final Double ORIGINAL_VAL_INSERTED = new Double(303);
    
    public static void main(String [] args)
    {
        if (MapTVarConflict.run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static boolean run_test()
    {
        Endpoint endpt = TestClassUtil.create_default_single_endpoint();

        AtomicMapVariable<Double,Double> map_tvar =
            (AtomicMapVariable<Double,Double>)
            endpt.global_var_stack.get_var_if_exists(
                TestClassUtil.DefaultEndpoint.MAP_TVAR_NAME);

        // Populate a few values in map.
        if (! MapTVarConflict.test_add_values(endpt,map_tvar))
            return false;
        // Tests concurrent read of tvar.
        if (! MapTVarConflict.test_concurrent_read(endpt,map_tvar))
            return false;
        // Tests preempted read of tvar.
        if (! MapTVarConflict.test_preempted_read(endpt,map_tvar))
            return false;        

        return true;
    }

    public static boolean test_add_values(
        Endpoint endpt,
        AtomicMapVariable<Double,Double> map_tvar)
    {
        try
        {
            // load values into map
            ActiveEvent event =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");

            if (! map_tvar.get_val(event).get_len_boxed(event).equals(0.0))
                return false;

            map_tvar.get_val(event).set_val_on_key(
                event,INSERTION_INDEX,ORIGINAL_VAL_INSERTED);
            
            Double gotten_val = map_tvar.get_val(event).get_val_on_key(
                event,INSERTION_INDEX);

            if (! gotten_val.equals(ORIGINAL_VAL_INSERTED.doubleValue()))
                return false;

            // commit changes
            event.local_root_begin_first_phase_commit();
            RootEventParent event_parent =
                (RootEventParent)event.event_parent;
            ResultType commit_resp =
                event_parent.event_complete_queue.take();
            
            if (commit_resp != ResultType.COMPLETE)
                return false;
            
            // check that map maintains inserted value
            event =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");

            if (! map_tvar.get_val(event).get_len_boxed(event).equals(1.0))
                return false;
            Double key_val =
                map_tvar.get_val(event).get_val_on_key(event,INSERTION_INDEX);
            
            if (! key_val.equals(ORIGINAL_VAL_INSERTED.doubleValue()))
                return false;

            // commit read changes
            event.local_root_begin_first_phase_commit();
            event_parent =
                (RootEventParent)event.event_parent;
            commit_resp =
                event_parent.event_complete_queue.take();

            if (commit_resp != ResultType.COMPLETE)
                return false;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
        return true;
    }
    

    /**
       @returns {boolean} --- Starts two events.  The second event
       that is started (the one with lower priority) reads map_tvar.
       The first event (the event with higher priority) then writes to
       map_tvar.  Returns true if first event preempts second.
     */
    public static boolean test_preempted_read(
        Endpoint endpt,
        AtomicMapVariable<Double,Double> map_tvar)
    {
        try
        {
            ActiveEvent writer =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");
            ActiveEvent reader =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");

            if (! map_tvar.get_val(reader).get_val_on_key(reader,INSERTION_INDEX).equals(
                    ORIGINAL_VAL_INSERTED.doubleValue()))
            {
                return false;
            }

            Double new_val = new Double(
                ORIGINAL_VAL_INSERTED.doubleValue() + 10);
            map_tvar.get_val(writer).set_val_on_key(
                writer,INSERTION_INDEX,new_val);
            
            reader.local_root_begin_first_phase_commit();
            writer.local_root_begin_first_phase_commit();

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
        Endpoint endpt,
        AtomicMapVariable<Double,Double> map_tvar)
    {
        try
        {
            ActiveEvent rdr1 =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");
            ActiveEvent rdr2 =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");
            
            if (! map_tvar.get_val(rdr1).get_val_on_key(
                    rdr1,INSERTION_INDEX).equals(ORIGINAL_VAL_INSERTED.doubleValue()))
                return false;

            if (! map_tvar.get_val(rdr2).get_val_on_key(
                    rdr2,INSERTION_INDEX).equals(ORIGINAL_VAL_INSERTED.doubleValue()))
                return false;
            
            rdr1.local_root_begin_first_phase_commit();
            rdr2.local_root_begin_first_phase_commit();

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

