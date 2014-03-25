package RalphExtended;

import java.util.concurrent.Future;

import ralph.SpeculativeAtomicObject;
import ralph.ICancellableFuture;
import ralph.SpeculativeFuture;
import ralph.RalphGlobals;
import ralph.ActiveEvent;

import RalphServiceActions.LinkFutureBooleans;


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
            //// DEBUG
            if (current_state != ExtendedObjectStateController.State.CLEAN)
            {
                // FIXME: Maybe should also be able to get here from
                // FAILED state, but should return false.
                System.out.println(
                    "Should only recieve first phase commit request when in clean state");
                assert(false);
            }
            //// END DEBUG

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
                return to_apply_to_hardware.to_notify_when_complete;
             }

            // it's a read operation. never made a write to this variable:
            // do not need to ensure that hardware is up (for now).  May
            // want to add read checks as well.
            return SpeculativeAtomicObject.ALWAYS_TRUE_FUTURE;
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
                if ((current_state != ExtendedObjectStateController.State.STAGED_CHANGES) &&
                    (current_state != ExtendedObjectStateController.State.PUSHING_CHANGES))
                {
                    // FIXME: handle failed state.                    
                    System.out.println("Cannot complete from failed state or clean state.");
                    assert(false);
                }
                //// END DEBUG

                if (current_state == ExtendedObjectStateController.State.PUSHING_CHANGES)
                {
                    current_state =
                        state_controller.wait_staged_or_failed_state_while_holding_lock_returns_holding_lock();
                }

                if (current_state == ExtendedObjectStateController.State.FAILED)
                {
                    // FIXME: Handle being in a failed state.
                    System.out.println("Handle failed state");
                    assert(false);
                }

                state_controller.move_state_clean();
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
        
        if (write_lock_holder_being_preempted)
        {
            ExtendedObjectStateController.State current_state =
                state_controller.get_state_hold_lock();

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
            if ((current_state != ExtendedObjectStateController.State.PUSHING_CHANGES) &&
                (current_state != ExtendedObjectStateController.State.STAGED_CHANGES))
            {
                // FIXME: Handle failed state.
                System.out.println(
                    "Unexpected state when requesting backout " +
                    current_state);
                assert(false);
            }
            //// END DEBUG

            if (current_state == ExtendedObjectStateController.State.PUSHING_CHANGES)
            {
                current_state =
                    state_controller.wait_staged_or_failed_state_while_holding_lock_returns_holding_lock();
            }

            if (current_state == ExtendedObjectStateController.State.FAILED)
            {
                // FIXME: Must handle being in a failed state.
                System.out.println("Handle failed state");
                assert(false);
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
            // apply to hardware should for us.;


            // FIXME: should check what to do if failed though.
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