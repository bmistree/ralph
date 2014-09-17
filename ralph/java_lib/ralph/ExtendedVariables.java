package ralph;
import java.util.ArrayList;
import java.util.concurrent.Future;

import ralph.Variables;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphServiceActions.ServiceAction;
import java.util.concurrent.ExecutionException;
import RalphServiceActions.LinkFutureBooleans;

import static ralph.FutureAlwaysValue.ALWAYS_TRUE_FUTURE;
import static ralph.FutureAlwaysValue.ALWAYS_FALSE_FUTURE;


public class ExtendedVariables
{
    public static abstract class ExtendedInternalAtomicList<T>
        extends AtomicInternalList<T,T>
    {
        // This contains changes that we pushed to hardware, but haven't
        // committed.
        // guaranteed no concurrent access issues.  Written from three
        // methods: backout, first_phase_commit, and complete_commit.
        // Locks in AtomicActiveEvent ensure that these won't run
        // concurrently
        private ListTypeDataWrapper<T,T> dirty_op_tuples_on_hardware = null;
        
        public ExtendedInternalAtomicList(
            EnsureAtomicWrapper<T,T>_locked_wrapper,
            Class<T> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                ralph_globals,true,
                new ListTypeDataWrapperFactory<T,T>(value_type_class),
                new ArrayList<RalphObject<T,T>>(),
                _locked_wrapper);
        }

        protected abstract ICancellableFuture apply_changes_to_hardware(
            ListTypeDataWrapper<T,T> dirty);
        protected abstract void undo_dirty_changes_to_hardware(
            ListTypeDataWrapper<T,T> to_undo);
        

        /**
           Called from within lock.
         */
        @Override
        protected void hardware_complete_commit_hook(ActiveEvent active_event)
        {
            boolean write_lock_holder_being_completed = is_write_lock_holder(active_event);
            if (write_lock_holder_being_completed)
                dirty_op_tuples_on_hardware = null;
        }

        /**
           Called from within lock.
         */
        @Override
        protected void hardware_backout_hook (ActiveEvent active_event)
        {
            boolean write_lock_holder_being_preempted = is_write_lock_holder(active_event);
            if (write_lock_holder_being_preempted)
            {
                // if there were dirty values that we had pushed to the
                // hardware, we need to undo them.
                if (dirty_op_tuples_on_hardware != null)
                    undo_dirty_changes_to_hardware(dirty_op_tuples_on_hardware);
                dirty_op_tuples_on_hardware = null;
            }
        }

        /**
           Called from within lock.

           @returns --- Can be null, eg., if the object is not backed by
           hardware.  Otherwise, call to get on future returns true if if
           can commit in first phase, false otherwise.
         */
        @Override
        protected ICancellableFuture hardware_first_phase_commit_hook(
            ActiveEvent active_event)
        {
            // do not need to take locks here because know that this
            // method will only be called from AtomicActiveEvent
            // during first_phase_commit. Because AtomicActiveEvent is
            // in midst of commit, know that these values cannot
            // change.
            boolean write_lock_holder_being_preempted = is_write_lock_holder(active_event);
            if (! write_lock_holder_being_preempted)
            {
                // never made a write to this variable: do not need to
                // ensure that hardware is up (for now).  May want to
                // add read checks as well.
                return ALWAYS_TRUE_FUTURE;
            }
            
            // log that any changes that we are making will need to be
            // undone if the event backs out.
            dirty_op_tuples_on_hardware = (ListTypeDataWrapper<T,T>)dirty_val;
            return apply_changes_to_hardware(dirty_op_tuples_on_hardware);
        }


        /**
           Called from within lock.

           When a derived object gets promoted to root object, we need
           to deal with any events that began committing to the object
           when it was a derived object.  In our case, we take the
           changes associated with the speculative future and apply
           them to hardware.  We link this speculative future with a
           new future that pushes to hardware.
         */
        @Override
        protected boolean hardware_first_phase_commit_speculative_hook(
            SpeculativeFuture sf)
        {
            ActiveEvent active_event = sf.event;
            Future<Boolean> bool = hardware_first_phase_commit_hook(active_event);
            ralph_globals.thread_pool.add_service_action(
                new LinkFutureBooleans(bool,sf));
            
            return true;
        }
    }
}
