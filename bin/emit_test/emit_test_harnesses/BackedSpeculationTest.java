package emit_test_harnesses;

import java.util.List;
import java.util.ArrayList;
import ralph.RalphObject;
import ralph_emitted.BackedSpeculationJava.BackedSpeculation;
import ralph_emitted.BackedSpeculationJava._InternalSwitch;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.Variables.AtomicNumberVariable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Future;
import RalphExtended.ExtendedObjectStateController;
import RalphServiceActions.LinkFutureBooleans;
import ralph.ActiveEvent;
import ralph.SpeculativeFuture;
import ralph.ICancellableFuture;
import RalphExtended.ExtendedObjectStateController;
import ralph.AtomicInternalList;
import ralph.SpeculativeFuture;
import RalphServiceActions.ServiceAction;
import ralph.Variables.AtomicListVariable;
import java.util.Random;

public class BackedSpeculationTest
{
    private final static int NUM_OPS_PER_THREAD = 5000;
    private final static AtomicBoolean had_exception =
        new AtomicBoolean(false);
    private final static int TIME_TO_SLEEP_MS = 1;
    
    public static void main(String[] args)
    {
        if (BackedSpeculationTest.run_test())
            System.out.println("\nSUCCESS in BackedSpeculationTest\n");
        else
            System.out.println("\nFAILURE in BackedSpeculationTest\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            BackedSpeculation endpt = new BackedSpeculation(
                ralph_globals,new SingleSideConnection());
            
            endpt.set_switches(
                create_switch(ralph_globals,true),
                create_switch(ralph_globals,true));

            
            EventThread event_1 =
                new EventThread(endpt,false,NUM_OPS_PER_THREAD);
            EventThread event_2 =
                new EventThread(endpt,true,NUM_OPS_PER_THREAD);

            event_1.start();
            event_2.start();
            event_1.join();
            event_2.join();

            if (had_exception.get())
                return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    public static class EventThread extends Thread
    {
        private final BackedSpeculation endpt;
        private final boolean event_one;
        private final int num_ops;
        public EventThread(BackedSpeculation _endpt,boolean _event_one,int _num_ops)
        {
            endpt = _endpt;
            event_one = _event_one;
            num_ops = _num_ops;
        }
        public void run()
        {
            for (int i = 0; i < num_ops; ++i)
            {
                try
                {
                    if (event_one)
                        endpt.event1();
                    else
                        endpt.event2();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    had_exception.set(true);
                }
            }
        }
    }

    public static _InternalSwitch create_switch(
        RalphGlobals ralph_globals,boolean should_speculate)
    {
        _InternalSwitch to_return = new _InternalSwitch(ralph_globals);
        to_return.switch_guard =
            new InternalSwitchGuard(ralph_globals,to_return,should_speculate);
        return to_return;
    }
    
    public static class InternalSwitchGuard extends AtomicNumberVariable
    {
        private final ExtendedObjectStateController<Double> state_controller =
            new ExtendedObjectStateController<Double> ();

        private final _InternalSwitch internal_switch;
        

        private final boolean should_speculate;
        
        public InternalSwitchGuard(
            RalphGlobals ralph_globals,_InternalSwitch _internal_switch,
            boolean _should_speculate)
        {
            super(false,new Double(0),ralph_globals);
            internal_switch = _internal_switch;
            should_speculate = _should_speculate;
        }


        private AtomicInternalList<Double,Double> get_internal_ft_list()
        {
            AtomicListVariable<Double,Double> ft_list =
                internal_switch.dummy_flow_table;
            AtomicInternalList<Double,Double> internal_ft_list = null;

            if (ft_list.dirty_val != null)
                internal_ft_list = ft_list.dirty_val.val;
            else
                internal_ft_list = ft_list.val.val;

            return internal_ft_list;
        }

        @Override
        protected ICancellableFuture hardware_first_phase_commit_hook(
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


                // regardless of whether we are a reader or a writer, we need
                // these values so that we can speculate on them.
                AtomicInternalList<Double,Double>
                    internal_ft_list = get_internal_ft_list();

                ArrayList<RalphObject<Double,Double>>
                    to_push = null;

                if (should_speculate)
                {
                    internal_ft_list.speculate(active_event,null);
                    speculate(active_event,null);
                }
                else
                {
                    // if wanted to grab value to speculate on, should do it
                    // here.
                }


                // FIXME: What if this is on top of a speculated value.
                // Doesn't that mean that write_lock_holder might be
                // incorrect/pointing to incorrect value?
                if (is_write_lock_holder(active_event))
                {
                    
                    // FIXME: pushing hard coded value here instead of dynamic
                    //should be pushing.
                    //state_controller.move_state_pushing_changes(to_push);
                    state_controller.move_state_pushing_changes(0.0);
                    WrapApplyToHardware to_apply_to_hardware =
                        new WrapApplyToHardware(
                            state_controller.get_dirty_on_hardware(),false,
                            state_controller);

                    ralph_globals.thread_pool.add_service_action(
                        to_apply_to_hardware);

                    // note: do not need to move state transition here
                    // ourselves.  to_apply_to_hardware does that for us.
                    return to_apply_to_hardware.to_notify_when_complete;
                 }

                // it's a read operation. never made a write to this variable:
                // do not need to ensure that hardware is up (for now).  May
                // want to add read checks as well.
                return ALWAYS_TRUE_FUTURE;
            }
            finally
            {
                state_controller.release_lock();
            }
        }

        @Override
        protected void hardware_complete_commit_hook(ActiveEvent active_event)
        {
            boolean write_lock_holder_being_completed = is_write_lock_holder(active_event);
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

        @Override
        protected void hardware_backout_hook(ActiveEvent active_event)
        {
            boolean write_lock_holder_being_preempted = is_write_lock_holder(active_event);
            if (write_lock_holder_being_preempted)
            {
                ExtendedObjectStateController.State current_state = state_controller.get_state_hold_lock();

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

                WrapApplyToHardware to_undo_wrapper =
                    new WrapApplyToHardware(
                        state_controller.get_dirty_on_hardware(),
                        true,state_controller);

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

        // FIXME: still must fill in all methods for speculation and hooking to
        // hardware.
    }


    public static class WrapApplyToHardware extends ServiceAction
    {
        // private List<RalphObject<Double,Double>> to_apply = null;
        private Double to_apply = null;
        private final boolean undo_changes;
        private final ExtendedObjectStateController state_controller;
        public final SpeculativeFuture to_notify_when_complete =
            new SpeculativeFuture(null);

        private final static Random rand = new Random();
        
        public WrapApplyToHardware(
            Double _to_apply,
            boolean _undo_changes, ExtendedObjectStateController _state_controller)
        {
            to_apply = _to_apply;
            undo_changes = _undo_changes;
            state_controller = _state_controller;
        }


        @Override
        public void run()
        {
            boolean application_successful = true;

            // FIXME: For now, always just returning that application is
            // successful.
            state_controller.get_state_hold_lock();
            if (! application_successful)
                state_controller.move_state_failed();
            else if (undo_changes) // undo succeeded
                state_controller.move_state_clean();
            else // apply succeeded
                state_controller.move_state_staged_changes();
            state_controller.release_lock();

            if (undo_changes)
            {
                try
                {
                    Thread.sleep(1,rand_ns_to_sleep_for());
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    assert(false);
                }
            }
            
            if (application_successful)
                to_notify_when_complete.succeeded();
            else
                to_notify_when_complete.failed();

        }


        private int rand_ns_to_sleep_for()
        {
            // 0ns to 1 ms.
            int MAX = 1000000;
            // int MAX = 1000;
            return rand.nextInt(MAX);
        }
    }
}