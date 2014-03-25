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
import static emit_test_harnesses.BackedSpeculationTestLib.create_switch;
import static emit_test_harnesses.BackedSpeculationTestLib.EventThread;

public class AlwaysWorksBackedSpeculationTest
{
    private final static int NUM_OPS_PER_THREAD = 5000;

    public static void main(String[] args)
    {
        if (AlwaysWorksBackedSpeculationTest.run_test())
            System.out.println("\nSUCCESS in AlwaysWorksBackedSpeculationTest\n");
        else
            System.out.println("\nFAILURE in AlwaysWorksBackedSpeculationTest\n");
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

            AtomicBoolean had_exception = new AtomicBoolean(false);
            EventThread event_1 =
                new EventThread(endpt,false,NUM_OPS_PER_THREAD,had_exception);
            EventThread event_2 =
                new EventThread(endpt,true,NUM_OPS_PER_THREAD,had_exception);

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

}