package emit_test_harnesses;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Random;

import ralph.EventPriority.IsSuperFlag;
import ralph_emitted.BackedSpeculationJava.BackedSpeculation;
import ralph_emitted.BackedSpeculationJava._InternalSwitch;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import RalphServiceActions.ServiceAction;
import ralph.ActiveEvent;
import RalphExtended.IHardwareChangeApplier;
import RalphExtended.IHardwareStateSupplier;
import static emit_test_harnesses.AlwaysWorksBackedSpeculationTest.AlwaysZeroStateSupplier;
import static emit_test_harnesses.BackedSpeculationTestLib.create_switch;
import static emit_test_harnesses.BackedSpeculationTestLib.EventThread;



public class RandomFailuresBackedSpeculationTest
{
    private final static int NUM_OPS_PER_THREAD = 1000;
    private final static Random rand = new Random();
    private final static float IND_FAILURE_PROBABILITY = .1f;
    private final static AtomicBoolean had_exception = new AtomicBoolean(false);
    
    public enum WhichSwitch
    {
        SWITCH_ONE,
        SWITCH_TWO
    }
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in RandomFailuresBackedSpeculationTest\n");
        else
            System.out.println("\nFAILURE in RandomFailuresBackedSpeculationTest\n");
    }


    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            BackedSpeculation endpt = new BackedSpeculation(
                ralph_globals,new SingleSideConnection());

            IHardwareStateSupplier<Double> hardware_state_supplier_switch1
                = new AlwaysZeroStateSupplier();
            IHardwareStateSupplier<Double> hardware_state_supplier_switch2
                = new AlwaysZeroStateSupplier();

            RandomFailuresOnHardware hardware_change_applier_switch1 =
                new RandomFailuresOnHardware(
                    endpt, ralph_globals,WhichSwitch.SWITCH_ONE,
                    hardware_state_supplier_switch1);
            RandomFailuresOnHardware hardware_change_applier_switch2 =
                new RandomFailuresOnHardware(
                    endpt, ralph_globals,WhichSwitch.SWITCH_TWO,
                    hardware_state_supplier_switch2);

            
            _InternalSwitch switch1 =
                create_switch(
                    ralph_globals,true,hardware_change_applier_switch1,
                    hardware_state_supplier_switch1);

            _InternalSwitch switch2 =
                create_switch(
                    ralph_globals,true,hardware_change_applier_switch2,
                    hardware_state_supplier_switch2);

            endpt.set_switches(switch1,switch2);


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
    public static class RandomFailuresOnHardware extends ServiceAction
        implements IHardwareChangeApplier<Double> 
    {
        private final BackedSpeculation endpt;
        private final RalphGlobals ralph_globals;
        private final WhichSwitch which_switch;
        private final IHardwareStateSupplier hardware_state_supplier;
        private boolean failed_while_applying_remove = false;
        
        public RandomFailuresOnHardware(
            BackedSpeculation _endpt, RalphGlobals _ralph_globals,
            WhichSwitch _which_switch, IHardwareStateSupplier _hardware_state_supplier)
        {
            endpt = _endpt;
            ralph_globals = _ralph_globals;
            which_switch = _which_switch;
            hardware_state_supplier = _hardware_state_supplier;
        }

        /** IHardwareChangeApplier interface */
        @Override
        public boolean apply(Double to_apply)
        {
            return check_application(to_apply);
        }
        @Override
        public boolean undo(Double to_undo)
        {
            return check_application(to_undo);
        }

        /** ServiceAction interface */
        // Gets called when we enter failed state.  We have to remove
        // ourselves and replace with a new switch.  Using super priority.
        public void run()
        {
            RandomFailuresOnHardware new_change_applier =
                new RandomFailuresOnHardware(
                    endpt,ralph_globals,which_switch,hardware_state_supplier);
            
            _InternalSwitch new_internal_switch =
                create_switch(
                    ralph_globals,true,new_change_applier,
                    hardware_state_supplier);

            try
            {
                if (which_switch == WhichSwitch.SWITCH_ONE)
                {
                    if (failed_while_applying_remove)
                        endpt.set_switch1_and_add_entry(new_internal_switch,IsSuperFlag.SUPER);
                    else
                        endpt.set_switch1(new_internal_switch,IsSuperFlag.SUPER);
                }
                else
                {
                    if (failed_while_applying_remove)
                        endpt.set_switch2_and_add_entry(new_internal_switch,IsSuperFlag.SUPER);
                    else
                        endpt.set_switch2(new_internal_switch,IsSuperFlag.SUPER);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
        }
        
        /**
           @param to_apply --- ==1.0 if failed in the middle of
           removing from dummy flow table.  ==0.0 if failed in the
           middle appending to dummy flow table.
         */
        private boolean check_application(Double to_apply)
        {
            if (rand.nextFloat() < IND_FAILURE_PROBABILITY)
            {
                // 1 is same as value assigned to switch_guard in
                // remove_write_lock of backend_speculation.rph.
                if (to_apply.intValue() == 1)
                    failed_while_applying_remove = true;
                ralph_globals.thread_pool.add_service_action(this);
                return false;
            }
            return true;
        }
    }
}