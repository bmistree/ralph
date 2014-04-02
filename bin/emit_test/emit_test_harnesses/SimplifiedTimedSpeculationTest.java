package emit_test_harnesses;

import java.util.concurrent.atomic.AtomicBoolean;

import ralph_emitted.SimplifiedBackedSpeculationJava.SimplifiedBackedSpeculation;
import ralph_emitted.SimplifiedBackedSpeculationJava._InternalWrappedLock;
import ralph.RalphGlobals;
import ralph.Variables.AtomicNumberVariable;
import RalphConnObj.SingleSideConnection;
import ralph.ActiveEvent;
import ralph.SpeculativeFuture;
import ralph.ICancellableFuture;

import RalphExtended.IHardwareChangeApplier;
import RalphExtended.IHardwareStateSupplier;
import RalphExtended.ISpeculateListener;
import RalphExtended.ExtendedHardwareOverrides;

public class SimplifiedTimedSpeculationTest
{
    private final static AtomicBoolean had_exception =
        new AtomicBoolean(false);
    private final static int NUM_OPS_TO_RUN_PER_THREAD = 5;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SimplifiedTimedSpeculationTest\n");
        else
            System.out.println("\nFAILURE in SimplifiedTimedSpeculationTest\n");
    }

    public static boolean run_test()
    {
        long time_no_speculation = run(false);
        if (had_exception.get())
            return false;

        long time_speculation = run(true);
        if (had_exception.get())
            return false;

        // speculative version should take no more than 65% of time of
        // non-speculative version.  (In fact, should take only ~50% of time,
        // but adding the additional fudge factor to acommodate start/stop,
        // re-orderings, etc.)
        if ((time_no_speculation*.65) < time_speculation)
            return false;
        
        return true;
    }

    public static long run(boolean should_speculate)
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            SimplifiedBackedSpeculation endpt = new SimplifiedBackedSpeculation(
                ralph_globals,new SingleSideConnection());

            _InternalWrappedLock wrapped_lock =
                create_internal_wrapped_lock(
                    ralph_globals,
                    should_speculate,endpt.get_internal_delay().intValue());
            endpt.set_wrapped_lock(wrapped_lock);
            
            
            EventThread event_1 = new EventThread(endpt);
            EventThread event_2 = new EventThread(endpt);
            
            long start = System.nanoTime();
            event_1.start();
            event_2.start();
            event_1.join();
            event_2.join();
            long end = System.nanoTime();

            return end - start;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            had_exception.set(true);
        }
        return 0;
    }

    public static class EventThread extends Thread
    {
        private final SimplifiedBackedSpeculation endpt;
        public EventThread(SimplifiedBackedSpeculation _endpt)
        {
            endpt = _endpt;
        }
        public void run()
        {
            for (int i = 0; i < NUM_OPS_TO_RUN_PER_THREAD; ++i)
            {
                try
                {
                    endpt.evt();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    had_exception.set(true);
                }
            }
        }
    }

    private static _InternalWrappedLock create_internal_wrapped_lock(
        RalphGlobals ralph_globals,boolean should_speculate,
        int time_to_delay_on_apply)
    {
        _InternalWrappedLock to_return = new _InternalWrappedLock(ralph_globals);
        to_return.lock =
            new InternalLockNumber(
                ralph_globals,should_speculate,time_to_delay_on_apply);
        return to_return;
    }
    
    /**
       The TVar Number lock in each Struct WrappedLock.
     */
    private static class InternalLockNumber
        extends AtomicNumberVariable
        implements IHardwareStateSupplier<Double>, IHardwareChangeApplier<Double>,
                   ISpeculateListener
    {
        private final
            ExtendedHardwareOverrides<Double> extended_hardware_overrides;
        private int time_to_delay_on_apply;
        private boolean should_speculate;
        
        public InternalLockNumber(
            RalphGlobals ralph_globals, boolean _should_speculate,
            int _time_to_delay_on_apply)
        {
            super(false,new Double(0),ralph_globals);
            extended_hardware_overrides =
                new ExtendedHardwareOverrides<Double>(
                    this,this,this,
                    _should_speculate,ralph_globals);
            extended_hardware_overrides.set_controlling_object(this);

            should_speculate = _should_speculate;
            time_to_delay_on_apply = _time_to_delay_on_apply;
        }

        /** Overriding HardwareStateSupplier */
        @Override
        public Double get_state_to_push(ActiveEvent active_event)
        {
            return 0.0;
        }

        /** Overriding HardwareChangeApplier */
        @Override
        public boolean apply(Double to_apply)
        {
            try
            {
                Thread.sleep(time_to_delay_on_apply);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
            return true;
        }
        @Override
        public boolean undo(Double to_undo)
        {
            return true;
        }

        /** Override ISpeculateListener */
        public void speculate(ActiveEvent active_event)
        {
            speculate(active_event,null);
        }
        

        /** Overriding AtomicNumberVariable internal methods */
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