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

public class ExtendedVariables
{
    public static abstract class ExtendedInternalAtomicList<T,D>
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
            EnsureAtomicWrapper<T,D>_locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(ralph_globals);
            init_multithreaded_list_container(
                true,new ListTypeDataWrapperFactory<T,D>(),
                new ArrayList<RalphObject<T,D>>(),
                _locked_wrapper);
        }

        protected abstract ICancellableFuture apply_changes_to_hardware(
            ListTypeDataWrapper<T,D> dirty);
        protected abstract void undo_dirty_changes_to_hardware(
            ListTypeDataWrapper<T,D> to_undo);
        
        @Override
        protected boolean internal_complete_commit(ActiveEvent active_event)
        {
            _lock();
            boolean write_lock_holder_completed =
                super.internal_complete_commit(active_event);

            if (write_lock_holder_completed)
                dirty_op_tuples_on_hardware = null;
            _unlock();
            return write_lock_holder_completed;
        }


        /**
           Already within lock
         */
        @Override
        protected void obj_request_backout_and_release_lock(
            ActiveEvent active_event)
        {
            if ((write_lock_holder != null) &&
                (active_event.uuid.equals(write_lock_holder.event.uuid)))
            {
                // must undo changes pushing to hardware
                undo_dirty_changes_to_hardware(dirty_op_tuples_on_hardware);
                dirty_op_tuples_on_hardware = null;
            }
            super.obj_request_backout_and_release_lock(active_event);
        }
        
        
        @Override
        protected boolean internal_backout (ActiveEvent active_event)
        {
            _lock();
            boolean write_lock_holder_preempted = super.internal_backout(active_event);
            if (write_lock_holder_preempted)
            {
                // if there were dirty values that we had pushed to the
                // hardware, we need to undo them.
                if (dirty_op_tuples_on_hardware != null)
                    undo_dirty_changes_to_hardware(dirty_op_tuples_on_hardware);
                dirty_op_tuples_on_hardware = null;
            }
            _unlock();
            return write_lock_holder_preempted;
        }


        @Override
        protected ICancellableFuture internal_first_phase_commit(ActiveEvent active_event)
        {
            // do not need to take locks here because know that this
            // method will only be called from AtomicActiveEvent
            // during first_phase_commit. Because AtomicActiveEvent is
            // in midst of commit, know that these values cannot
            // change.
            if ((write_lock_holder == null) ||
                (! active_event.uuid.equals(write_lock_holder.event.uuid)))
            {
                // never made a write to this variable: do not need to
                // ensure that hardware is up (for now).  May want to
                // add read checks as well.
                return ALWAYS_TRUE_FUTURE;
            }
            
            // log that any changes that we are making will need to be
            // undone if the event backs out.
            dirty_op_tuples_on_hardware = (ListTypeDataWrapper<T,D>)dirty_val;
            return apply_changes_to_hardware(dirty_op_tuples_on_hardware);
        }

        @Override
        protected void internal_first_phase_commit_speculative(
            SpeculativeFuture sf)
        {
            ActiveEvent active_event = sf.event;
            Future<Boolean> bool = internal_first_phase_commit(active_event);
            ralph_globals.thread_pool.add_service_action(
                new LinkFutureBooleans(bool,sf));
        }
    }
}
