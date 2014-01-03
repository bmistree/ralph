package ralph;
import java.util.ArrayList;

import ralph.Variables;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;

public class ExtendedVariables
{
    public static class ExtendedInternalAtomicList<T,D>
        extends AtomicInternalList<T,D>
    {
        // This contains changes that we pushed to hardware, but haven't
        // committed.
        // guaranteed no concurrent access issues.  Written from three
        // methods: backout, first_phase_commit, and complete_commit.
        // Locks in AtomicActiveEvent ensure that these won't run
        // concurrently
        private ListTypeDataWrapper<T,D> dirty_op_tuples_on_hardware = null;
        
        public ExtendedInternalAtomicList(
            EnsureAtomicWrapper<T,D>_locked_wrapper)
        {
            super();
            init_multithreaded_list_container(
                "",true,
                new ListTypeDataWrapperFactory<T,D>(),
                new ArrayList<RalphObject<T,D>>(),
                _locked_wrapper);
        }

        protected boolean apply_changes_to_hardware(ListTypeDataWrapper<T,D> dirty)
        {
            // run through all the changes made by latest active event
            // and push them to the hardware.
            ArrayList<ListTypeDataWrapper<T,D>.OpTuple> event_changes =
                dirty.partner_change_log;
            for (ListTypeDataWrapper<T,D>.OpTuple change : event_changes)
                System.out.println("\nApplying change to hardware\n");
            return true;
        }

        protected void undo_dirty_changes_to_hardware(
            ListTypeDataWrapper<T,D> to_undo)
        {
            for (ListTypeDataWrapper.OpTuple change : to_undo.partner_change_log)
                System.out.println("\nUndoing change to hardware\n");
        }

        
        @Override
        public void complete_commit(ActiveEvent active_event)
        {
            // do not need a lock 
            dirty_op_tuples_on_hardware = null;
            super.complete_commit(active_event);
        }
        
        @Override
        public void backout (ActiveEvent active_event)
        {
            // if there were dirty values that we had pushed to the
            // hardware, we need to undo them.
            if (dirty_op_tuples_on_hardware != null)
                undo_dirty_changes_to_hardware(dirty_op_tuples_on_hardware);
            dirty_op_tuples_on_hardware = null;
            super.backout(active_event);
        }
        
        @Override
        public boolean first_phase_commit(ActiveEvent active_event)
        {
            // do not need to take locks here because know that this
            // method will only be called from AtomicActiveEvent
            // during first_phase_commit. Because AtomicActiveEvent is
            // in midst of commit, know that these values cannot
            // change.
            if ((write_lock_holder == null) ||
                (active_event.uuid != write_lock_holder.event.uuid))
            {
                // never made a write to this variable: do not need to
                // ensure that hardware is up (for now).  May want to
                // add read checks as well.
                return true;
            }
            
            // log that any changes that we are making will need to be
            // undone if the event backs out.
            dirty_op_tuples_on_hardware = (ListTypeDataWrapper<T,D>)dirty_val;
            return apply_changes_to_hardware(dirty_op_tuples_on_hardware);
        }
    }
}
