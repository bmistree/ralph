package RalphExtended;

import java.util.concurrent.Future;

import ralph.SpeculativeAtomicObject;
import ralph.ICancellableFuture;
import ralph.SpeculativeFuture;
import ralph.RalphGlobals;
import ralph.ActiveEvent;
import ralph.Util;

import RalphServiceActions.LinkFutureBooleans;
import static ralph.FutureAlwaysValue.ALWAYS_TRUE_FUTURE;
import static ralph.FutureAlwaysValue.ALWAYS_FALSE_FUTURE;


/**
   Interfaces to Ralph runtime:

     hardware_first_phase_commit_hook
       Returns a future.  This future can be cancelled by ralph
       runtime, while hardware is still pushing changes.  In this
       case, guaranteed to get a call to hardware_backout_hook.  That
       call should block until all changes have been removed from
       hardware.

       It should be impossible to get another
       hardware_first_phase_commit_hook while we are backing out.
       Adding an assert to ensure.


     hardware_complete_commit_hook
       Changes that have been pushed can be released.  Only called
       after hardware_first_phase_commit_hook.

     hardware_backout_hook
     
       Backout changes that may have been made to a hardware element.
       Only return after barrier response that changes have completely
       been removed from hardware.  Note: this may be called even when
       no changes have been pushed to hardware (eg., if event has been
       preempted on object).  In these cases, nothing to undo.

     hardware_first_phase_commit_speculative_hook

     See SwitchGuardState.java for more about state transitions.
 */
public class ExtendedHardwareOverrides <HardwareChangeApplierType>
{
    // Keeps track of the state this object is in (pushing to
    // hardware, clean, etc.)
    private final ExtendedObjectStateController<HardwareChangeApplierType> state_controller =
        new ExtendedObjectStateController<HardwareChangeApplierType> ();

    // Actually pushes state to hardware
    private final IHardwareChangeApplier<HardwareChangeApplierType>
        hardware_applier;

    // Provides the state to push to hardware.
    private final IHardwareStateSupplier<HardwareChangeApplierType>
        hardware_state_supplier;

    private final ISpeculateListener speculate_listener;

    // The object that is sending this
    // hardware_first_phase_commit_hook, backout, ..., messages.
    private SpeculativeAtomicObject controlling_object;

    // True if want to allow speculation.  False otherwise.
    private final boolean should_speculate;

    private final RalphGlobals ralph_globals;

    // can get duplicate events from speculative object
    private ActiveEvent last_event = null;
    private ICancellableFuture last_future = null;
    
    public ExtendedHardwareOverrides(
        IHardwareChangeApplier<HardwareChangeApplierType> _hardware_applier,
        IHardwareStateSupplier<HardwareChangeApplierType> _hardware_state_supplier,
        ISpeculateListener _speculate_listener,boolean _should_speculate,
        RalphGlobals _ralph_globals)
    {
        hardware_applier = _hardware_applier;
        hardware_state_supplier = _hardware_state_supplier;
        speculate_listener = _speculate_listener;
        should_speculate = _should_speculate;
        ralph_globals = _ralph_globals;
    }

    /**
       Moves state controller back to clean state, regardless of
       whatever else has happened.
     */
    public void force_reset_clean()
    {
        state_controller.get_state_hold_lock();
        state_controller.move_state_clean();
        state_controller.release_lock();
    }
    
    /**
       Must be called before can continue.
     */
    public void set_controlling_object(SpeculativeAtomicObject _controlling_object)
    {
        controlling_object = _controlling_object;
    }
    
    /**
       @see documentation in SpeculativeAtomicObject for purpose/use
       of this method.
     */
    public ICancellableFuture hardware_first_phase_commit_hook(
        ActiveEvent active_event)
    {
        try
        {
            // gets released in finally block at bottom
            ExtendedObjectStateController.State current_state =
                state_controller.get_state_hold_lock();

            // Once an object enters a failed state, cannot perform
            // any operations on it until it gets cleaned up
            if (current_state == ExtendedObjectStateController.State.FAILED)
                return ALWAYS_FALSE_FUTURE;
            
            // handles case of duplicate requests
            if (last_event == active_event)
                return last_future;
        
            
            //// DEBUG
            if (current_state != ExtendedObjectStateController.State.CLEAN)
            {
                Util.logger_assert(
                    "Should only recieve first phase commit " +
                    "request when in clean state " + current_state);
            }
            //// END DEBUG

            last_event = active_event;
            
            if (should_speculate)
                speculate_listener.speculate(active_event);

            // FIXME: What if this is on top of a speculated value.
            // Doesn't that mean that write_lock_holder might be
            // incorrect/pointing to incorrect value?
            if (controlling_object.is_write_lock_holder(active_event))
            {
                state_controller.move_state_pushing_changes(
                    hardware_state_supplier.get_state_to_push(active_event));

                WrapApplyToHardware<HardwareChangeApplierType> to_apply_to_hardware =
                    new WrapApplyToHardware<HardwareChangeApplierType>(
                        state_controller.get_dirty_on_hardware(),false,
                        state_controller,hardware_applier);

                ralph_globals.thread_pool.add_service_action(
                    to_apply_to_hardware);

                // note: do not need to move state transition here
                // ourselves.  to_apply_to_hardware does that for us.
                last_future = to_apply_to_hardware.to_notify_when_complete;
                return last_future;
             }

            // it's a read operation. never made a write to this variable:
            // do not need to ensure that hardware is up (for now).  May
            // want to add read checks as well.
            last_future = ALWAYS_TRUE_FUTURE;
            return last_future;
        }
        finally
        {
            state_controller.release_lock();
        }
    }

    public void hardware_complete_commit_hook(ActiveEvent active_event)
    {
        boolean write_lock_holder_being_completed =
            controlling_object.is_write_lock_holder(active_event);
        if (write_lock_holder_being_completed)
        {
            try
            {
                ExtendedObjectStateController.State current_state =
                    state_controller.get_state_hold_lock();
                //// DEBUG
                if (current_state != ExtendedObjectStateController.State.STAGED_CHANGES)
                {
                    // Note: should never be able to get to
                    // complete_commit_hook while still in pushing
                    // changes stage.
                    Util.logger_assert(
                        "Cannot complete from failed state or clean state.");
                }
                //// END DEBUG
                state_controller.move_state_clean();
                last_event = null;
                last_future = null;
            }
            finally
            {
                state_controller.release_lock();
            }
        }
    }

    public void hardware_backout_hook(ActiveEvent active_event)
    {
        boolean write_lock_holder_being_preempted =
            controlling_object.is_write_lock_holder(active_event);
        // handles case of duplicate calls
        if (active_event == last_event)
            return;
        
        if (write_lock_holder_being_preempted)
        {
            ExtendedObjectStateController.State current_state =
                state_controller.get_state_hold_lock();

            // We're in a failed state: we cannot process any state
            // pushes to hardware (including backout) and will never
            // transition out of failed state without explicit
            // intervention from hardware_change_applier.  Note that
            // this is okay because when we are in failed state we do
            // not allow any commits on hardware.
            if (current_state == ExtendedObjectStateController.State.FAILED)
            {
                state_controller.release_lock();
                return;
            }

            
            // Get backout requests even when runtime has not
            // requested changes to be staged on hardware (eg., if
            // event has been preempted on object).  In these cases,
            // nothing to undo.  Just stay in clean state.
            if (current_state == ExtendedObjectStateController.State.CLEAN)
            {
                state_controller.release_lock();
                return;
            }

            //// DEBUG
            if (current_state == ExtendedObjectStateController.State.REMOVING_CHANGES)
            {
                Util.logger_assert(
                    "Cannot already be in removing_changes state " +
                    "when requesting backout.");
            }
            //// END DEBUG

            if (current_state == ExtendedObjectStateController.State.PUSHING_CHANGES)
            {
                current_state =
                    state_controller.wait_staged_or_failed_state_while_holding_lock_returns_holding_lock();
            }

            // while pushing changes, we transitioned into a failed
            // state.  we cannot apply the undo because the hardware
            // is now unresponsive.  While in failed state, cannot
            // apply any changes to hardware.  Relying on
            // hardware_change_applier to remove and reset hardware element.
            if (current_state == ExtendedObjectStateController.State.FAILED)
            {
                state_controller.release_lock();
                return;
            }

            WrapApplyToHardware<HardwareChangeApplierType> to_undo_wrapper =
                new WrapApplyToHardware<HardwareChangeApplierType>(
                    state_controller.get_dirty_on_hardware(),
                    true,state_controller,hardware_applier);


            ralph_globals.thread_pool.add_service_action(to_undo_wrapper);

            // transitions synchronously from removing changes to clean.
            state_controller.move_state_removing_changes();
            state_controller.release_lock();

            to_undo_wrapper.to_notify_when_complete.get();

            // do not need to explicitly transition to clean here;
            // apply to hardware should for us.
        }
    }

    public boolean hardware_first_phase_commit_speculative_hook(
        SpeculativeFuture sf)
    {
        ActiveEvent active_event = sf.event;
        Future<Boolean> bool = hardware_first_phase_commit_hook(active_event);
        ralph_globals.thread_pool.add_service_action(
            new LinkFutureBooleans(bool,sf));

        return true;
    }
}