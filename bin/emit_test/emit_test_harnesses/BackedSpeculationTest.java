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

import RalphExtended.WrapApplyToHardware;
import RalphExtended.IHardwareChangeApplier;
import RalphExtended.IHardwareStateSupplier;
import RalphExtended.ISpeculateListener;
import RalphExtended.ExtendedHardwareOverrides;


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

            _InternalSwitch switch1 =
                create_switch(
                    ralph_globals,true,new AlwaysSucceedsOnHardware(),
                    new AlwaysZeroStateSupplier());

            _InternalSwitch switch2 =
                create_switch(
                    ralph_globals,true,new AlwaysSucceedsOnHardware(),
                    new AlwaysZeroStateSupplier());

            endpt.set_switches(switch1,switch2);
            
            
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
        RalphGlobals ralph_globals,boolean should_speculate,
        IHardwareChangeApplier<Double> hardware_change_applier,
        IHardwareStateSupplier<Double> hardware_state_supplier)
    {
        _InternalSwitch to_return = new _InternalSwitch(ralph_globals);
        SpeculateListener speculate_listener = new SpeculateListener();
        InternalSwitchGuard internal_switch_guard = 
            new InternalSwitchGuard(
                ralph_globals,to_return,should_speculate,hardware_change_applier,
                hardware_state_supplier,speculate_listener);
        
        to_return.switch_guard = internal_switch_guard;
        speculate_listener.init(to_return,internal_switch_guard);
        return to_return;
    }
    

    /**
       Just ensures that the change always gets applied to hardware.
     */
    public static class AlwaysSucceedsOnHardware implements IHardwareChangeApplier<Double>
    {
        @Override
        public boolean apply(Double to_apply)
        {
            return true;
        }
        @Override
        public boolean undo(Double to_undo)
        {
            // although will not get undo because hardware could not
            // comply, may get an undo message if preempted by another
            // event.
            return true;
        }
    }

    /**
       Always returns zero for state that want to apply.
     */
    public static class AlwaysZeroStateSupplier implements IHardwareStateSupplier<Double>
    {
        @Override
        public Double get_state_to_push(ActiveEvent active_event)
        {
            return 0.0;
        }
    }

    public static class SpeculateListener implements ISpeculateListener
    {
        private _InternalSwitch internal_switch = null;
        private InternalSwitchGuard internal_switch_guard = null;

        public void init(
            _InternalSwitch _internal_switch,
            InternalSwitchGuard _internal_switch_guard)
        {
            internal_switch = _internal_switch;
            internal_switch_guard = _internal_switch_guard;
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
        public void speculate(ActiveEvent active_event)
        {
            AtomicInternalList<Double,Double>
                internal_ft_list = get_internal_ft_list();
            internal_ft_list.speculate(active_event,null);
            internal_switch_guard.speculate(active_event,null);
        }
    }

    public static class InternalSwitchGuard extends AtomicNumberVariable
    {
        private final ExtendedHardwareOverrides<Double> extended_hardware_overrides;

        public InternalSwitchGuard(
            RalphGlobals ralph_globals,_InternalSwitch internal_switch,
            boolean should_speculate,
            IHardwareChangeApplier<Double> hardware_applier,
            IHardwareStateSupplier<Double> hardware_state_supplier,
            ISpeculateListener speculate_listener)
        {
            super(false,new Double(0),ralph_globals);
            extended_hardware_overrides =
                new ExtendedHardwareOverrides<Double>(
                    hardware_applier,hardware_state_supplier,speculate_listener,
                    should_speculate,ralph_globals);
        }

        @Override
        protected ICancellableFuture hardware_first_phase_commit_hook(
            ActiveEvent active_event)
        {
            return extended_hardware_overrides.hardware_first_phase_commit_hook(
                active_event);
        }

        @Override
        protected void hardware_complete_commit_hook(ActiveEvent active_event)
        {
            extended_hardware_overrides.hardware_complete_commit_hook(active_event);
        }            

        @Override
        protected void hardware_backout_hook(ActiveEvent active_event)
        {
            extended_hardware_overrides.hardware_backout_hook(active_event);
        }

        @Override
        protected boolean hardware_first_phase_commit_speculative_hook(
            SpeculativeFuture sf)
        {
            return extended_hardware_overrides.hardware_first_phase_commit_speculative_hook(sf);
        }
    }
}