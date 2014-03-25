package emit_test_harnesses;

import ralph_emitted.BackedSpeculationJava.BackedSpeculation;
import ralph_emitted.BackedSpeculationJava._InternalSwitch;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import ralph.ActiveEvent;

import RalphExtended.WrapApplyToHardware;
import RalphExtended.IHardwareChangeApplier;
import RalphExtended.IHardwareStateSupplier;
import static emit_test_harnesses.BackedSpeculationTestLib.create_switch;
import static emit_test_harnesses.BackedSpeculationTestLib.EventThread;

public class AlwaysWorksBackedSpeculationTest
{
    private final static int NUM_OPS_PER_THREAD = 5000;
    // gets incremented each time a switch applies to hardware and
    // decremented each time a switch undoes.  In aggregate then, this
    // should be the number of completed events that each switch
    // processes (or NUM_OPS_PER_THREAD*2... x2 because each event
    // hits 2 switches).
    private final static AtomicInteger num_ops_set_on_hardware =
        new AtomicInteger(0);
    
    public static void main(String[] args)
    {
        if (AlwaysWorksBackedSpeculationTest.run_test())
            System.out.println("\nSUCCESS in AlwaysWorksBackedSpeculationTest\n");
        else
            System.out.println("\nFAILURE in AlwaysWorksBackedSpeculationTest\n");
    }


    public static boolean run_test()
    {
        return run_test(
            new AlwaysSucceedsOnHardware(),new AlwaysSucceedsOnHardware(),
            new AlwaysZeroStateSupplier(), new AlwaysZeroStateSupplier());
    }
    
    
    public static boolean run_test(
        IHardwareChangeApplier<Double> hardware_change_applier_switch1,
        IHardwareChangeApplier<Double> hardware_change_applier_switch2,
        IHardwareStateSupplier<Double> hardware_state_supplier_switch1,
        IHardwareStateSupplier<Double> hardware_state_supplier_switch2)
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            BackedSpeculation endpt = new BackedSpeculation(
                ralph_globals,new SingleSideConnection());

            _InternalSwitch switch1 =
                create_switch(
                    ralph_globals,true,hardware_change_applier_switch1,
                    hardware_state_supplier_switch1);

            _InternalSwitch switch2 =
                create_switch(
                    ralph_globals,true,hardware_change_applier_switch2,
                    hardware_state_supplier_switch2);

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

            // see note on this condition above in documenation for
            // num_ops_set_on_hardware.
            if (num_ops_set_on_hardware.get() != (2*NUM_OPS_PER_THREAD))
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
            num_ops_set_on_hardware.getAndIncrement();
            return true;
        }
        @Override
        public boolean undo(Double to_undo)
        {
            // although will not get undo because hardware could not
            // comply, may get an undo message if preempted by another
            // event.
            num_ops_set_on_hardware.getAndDecrement();
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