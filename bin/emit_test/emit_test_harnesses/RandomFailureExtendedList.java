package emit_test_harnesses;

import ralph_emitted.ManyOpsAtomicListFailJava.TVarListEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.ExtendedVariables.ExtendedInternalAtomicList;
import java.lang.Math;
import RalphAtomicWrappers.BaseAtomicWrappers;
import java.util.ArrayList;
import java.util.List;
import ralph.RalphObject;
import RalphAtomicWrappers.BaseAtomicWrappers;
import RalphDataWrappers.ListTypeDataWrapper;
import java.util.concurrent.atomic.AtomicBoolean;
import ralph.ICancellableFuture;
import static ralph.FutureAlwaysValue.ALWAYS_TRUE_FUTURE;
import static ralph.FutureAlwaysValue.ALWAYS_FALSE_FUTURE;


public class RandomFailureExtendedList
{
    private static final int NUM_TIMES_TO_RUN = 200;
    private static final AtomicBoolean had_other_issue =
        new AtomicBoolean(false);
    
    public static void main(String[] args)
    {
        if (RandomFailureExtendedList.run_test())
            System.out.println("\nSUCCESS in RandomFailureExtendedList\n");
        else
            System.out.println("\nFAILURE in RandomFailureExtendedList\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            TVarListEndpoint endpt = new TVarListEndpoint(
                new RalphGlobals(),new SingleSideConnection());

            // set endpoint's internal list from SynchronizedNumberList
            SynchronizedNumberInternalList synchronized_internal_list =
                new SynchronizedNumberInternalList(endpt.ralph_globals);
            endpt.set_number_list(synchronized_internal_list);

            // run many times
            for (int i = 0; i < NUM_TIMES_TO_RUN; ++i)
                endpt.atomically_do_many_ops();

            // ensure agreement between synchronized list and internal list
            List<Double> synchronized_list =
                synchronized_internal_list.synchronized_list;
            int internal_list_size = endpt.get_list_size().intValue();

            if (synchronized_list.size() != internal_list_size)
                return false;
            for (int i = 0; i < internal_list_size; ++i)
            {
                double internal_value_on_key =
                    endpt.get_entry_at_index(new Double(i)).doubleValue();

                double sync_value_on_key = synchronized_list.get(i);

                if (sync_value_on_key != internal_value_on_key)
                    return false;
            }
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        return ! had_other_issue.get();
    }


    private static class SynchronizedNumberInternalList
        extends ExtendedInternalAtomicList<Double>
    {
        public List<Double> synchronized_list = new ArrayList<Double>();
        private boolean return_false = true;

        public SynchronizedNumberInternalList(RalphGlobals ralph_globals)
        {
            super(BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,ralph_globals);
        }
        
        /**
           Can apply commit one time.  Second time try to commit, fails.
         */
        @Override
        protected ICancellableFuture apply_changes_to_hardware(
            ListTypeDataWrapper<Double,Double> dirty)
        {
            // first, apply changes to synchronized list.  Then, following the
            // changes, randomly fail or pass.  
            for (ListTypeDataWrapper.OpTuple op_tuple :
                     dirty.get_unmodifiable_change_log())
            {
                if (dirty.is_add_key_tuple(op_tuple))
                {
                    RalphObject<Double,Double> what_added =
                        op_tuple.what_added_or_removed;
                    int index = op_tuple.key;
                    try {
                        Double java_what_added =
                            new Double(what_added.get_val(null));
                        synchronized_list.add(index,java_what_added);
                    } catch (Exception _ex) {
                        _ex.printStackTrace();
                        had_other_issue.set(true);
                    }
                }
                else if (dirty.is_delete_key_tuple(op_tuple))
                {
                    int index = op_tuple.key;
                    Double what_removed = synchronized_list.remove(index);
                }
                else
                {
                    System.out.println(
                        "\nNot currently handling writes, " +
                        "just inserts and deletes.\n");
                    had_other_issue.set(true);
                    assert(false);
                }
            }

            if (Math.random() > .5)
                return ALWAYS_FALSE_FUTURE;
            return ALWAYS_TRUE_FUTURE;
        }
        @Override
        protected void undo_dirty_changes_to_hardware(
            ListTypeDataWrapper<Double,Double> to_undo)
        {
            // go backwards through list and do the opposite of the op
            // tuple
            List<ListTypeDataWrapper<Double,Double>.OpTuple> change_log =
                to_undo.get_unmodifiable_change_log();
            for (int i = change_log.size() -1; i >= 0; --i)
            {
                ListTypeDataWrapper.OpTuple op_tuple = change_log.get(i);
                if (to_undo.is_add_key_tuple(op_tuple))
                {
                    int index = op_tuple.key;
                    Double undo_add = synchronized_list.remove(index);
                }
                else if (to_undo.is_delete_key_tuple(op_tuple))
                {
                    int index = op_tuple.key;
                    RalphObject<Double,Double> what_removed =
                        op_tuple.what_added_or_removed;
                    try {
                        Double what_re_adding =
                            new Double(what_removed.get_val(null));
                        synchronized_list.add(index,what_re_adding);
                            
                    } catch (Exception _ex) {
                        _ex.printStackTrace();
                        had_other_issue.set(true);
                    }
                }
                else
                {
                    System.out.println(
                        "\nNot currently handling writes, " +
                        "just inserts and deletes.\n");
                    had_other_issue.set(true);
                    assert(false);
                }                
            }
        }
    }
}