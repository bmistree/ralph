package emit_test_harnesses;

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

public class BackedSpeculationTest
{
    private final static int NUM_OPS_PER_THREAD = 100;
    private final static AtomicBoolean had_exception =
        new AtomicBoolean(false);
    
    
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
                create_switch(ralph_globals),
                create_switch(ralph_globals));

            
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

    public static _InternalSwitch create_switch(RalphGlobals ralph_globals)
    {
        _InternalSwitch to_return = new _InternalSwitch(ralph_globals);
        to_return.switch_guard = new InternalSwitchGuard(ralph_globals);
        return to_return;
    }
    
    public static class InternalSwitchGuard extends AtomicNumberVariable
    {
        private ExtendedObjectStateController<Double> state_controller =
            new ExtendedObjectStateController<Double> ();
        
        public InternalSwitchGuard(RalphGlobals ralph_globals)
        {
            super(false,new Double(0),ralph_globals);
        }


        @Override
        protected ICancellableFuture hardware_first_phase_commit_hook(
            ActiveEvent active_event)
        {
            // FIXME: Ignoring backout.
            if (is_write_lock_holder(active_event))
            {
                state_controller.get_state_hold_lock();
                state_controller.move_state_pushing_changes(dirty_val.val);
                state_controller.release_lock();
            }
            return ALWAYS_TRUE_FUTURE;
        }

        @Override
        protected void hardware_complete_commit_hook(ActiveEvent active_event)
        {
            // FIXME: Ignoring backout
            if (is_write_lock_holder(active_event))
            {
                state_controller.get_state_hold_lock();
                state_controller.move_state_clean();
                state_controller.release_lock();
            }
        }

        @Override
        protected void hardware_backout_hook(ActiveEvent active_event)
        {
            if (is_write_lock_holder(active_event))
            {
                state_controller.get_state_hold_lock();
                state_controller.move_state_removing_changes();
                state_controller.release_lock();
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
}