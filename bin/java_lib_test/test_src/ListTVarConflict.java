package java_lib_test;

import java.util.HashMap;
import ralph.Variables.AtomicListVariable;
import ralph.RalphObject;
import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.RootEventParent;
import RalphCallResults.RootCallResult.ResultType;
import java_lib_test.TestClassUtil.DefaultEndpoint;

/**
   Creates an active event that reads and writes to tvar.  checks that
   can have multiple readers of tvar at same time.
 */

public class ListTVarConflict
{
    protected static String test_name = "ListTVarConflict";
    public static final Double INSERTION_INDEX = new Double(30);
    public static final Double TO_INSERT_0 = new Double(303);
    public static final Double TO_INSERT_1 = new Double(304);
    public static final Double TO_INSERT_2 = new Double(305);
    
    public static void main(String [] args)
    {
        if (run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static boolean run_test()
    {
        DefaultEndpoint endpt = TestClassUtil.create_default_single_endpoint();

        // Populate a few values in map.
        if (! ListTVarConflict.test_add_values(endpt,endpt.list_tvar))
            return false;
        // Tests concurrent read of tvar.
        if (! ListTVarConflict.test_concurrent_read(endpt,endpt.list_tvar))
            return false;
        // Tests preempted read of tvar.
        if (! ListTVarConflict.test_preempted_read(endpt,endpt.list_tvar))
            return false;        

        return true;
    }

    public static boolean test_add_values(
        Endpoint endpt,
        AtomicListVariable<Double,Double> list_tvar)
    {
        try
        {
            // load values into list
            ActiveEvent event =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");

            if (! list_tvar.get_val(event).get_len_boxed(event).equals(0.0))
                return false;
            
            list_tvar.get_val(event).append(event,TO_INSERT_1);
            list_tvar.get_val(event).append(event,TO_INSERT_2);
            list_tvar.get_val(event).insert(event, new Integer(0),TO_INSERT_0);
            
            Double gotten_val_index_2 = list_tvar.get_val(event).get_val_on_key(
                event,new Integer(2));

            if (! gotten_val_index_2.equals(TO_INSERT_2.doubleValue()))
                return false;

            // commit changes
            event.local_root_begin_first_phase_commit();
            RootEventParent event_parent =
                (RootEventParent)event.event_parent;
            ResultType commit_resp =
                event_parent.event_complete_queue.take();
            
            if (commit_resp != ResultType.COMPLETE)
                return false;
            
            // check that list maintains inserted value
            event =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");

            if (! list_tvar.get_val(event).get_len_boxed(event).equals(3.0))
                return false;

            Double key_val =
                list_tvar.get_val(event).get_val_on_key(event,new Integer(1));
            
            if (! key_val.equals(TO_INSERT_1.doubleValue()))
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
       that is started (the one with lower priority) reads list_tvar.
       The first event (the event with higher priority) then writes to
       list_tvar.  Returns true if first event preempts second.
     */
    public static boolean test_preempted_read(
        Endpoint endpt,
        AtomicListVariable<Double,Double> list_tvar)
    {
        try
        {
            ActiveEvent writer =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");
            ActiveEvent reader =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");

            if (! list_tvar.get_val(reader).get_val_on_key(reader,new Integer(0)).equals(
                    TO_INSERT_0.doubleValue()))
            {
                return false;
            }

            // insert a new value in list
            Double new_val = new Double(
                TO_INSERT_0.doubleValue() + 10);
            list_tvar.get_val(writer).append(
                writer,new_val);
            
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
        AtomicListVariable<Double,Double> list_tvar)
    {
        try
        {
            ActiveEvent rdr1 =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");
            ActiveEvent rdr2 =
                endpt._act_event_map.create_root_atomic_event(
                    null,endpt,"dummy");

            // read all values with active event rdr1
            if (! list_tvar.get_val(rdr1).get_val_on_key(
                    rdr1,new Integer(0)).equals(TO_INSERT_0.doubleValue()))
                return false;
            if (! list_tvar.get_val(rdr1).get_val_on_key(
                    rdr1,new Integer(1)).equals(TO_INSERT_1.doubleValue()))
                return false;
            if (! list_tvar.get_val(rdr1).get_val_on_key(
                    rdr1,new Integer(2)).equals(TO_INSERT_2.doubleValue()))
                return false;

            // read all values with active event rdr2
            if (! list_tvar.get_val(rdr2).get_val_on_key(
                    rdr2,new Integer(0)).equals(TO_INSERT_0.doubleValue()))
                return false;
            if (! list_tvar.get_val(rdr2).get_val_on_key(
                    rdr2,new Integer(1)).equals(TO_INSERT_1.doubleValue()))
                return false;
            if (! list_tvar.get_val(rdr2).get_val_on_key(
                    rdr2,new Integer(1)).equals(TO_INSERT_1.doubleValue()))
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

