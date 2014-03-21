package emit_test_harnesses;

import ralph_emitted.BackedSpeculationJava.BackedSpeculation;
import ralph_emitted.BackedSpeculationJava._InternalSwitch;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.Variables.AtomicNumberVariable;

public class BackedSpeculationTest
{
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

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
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
        public InternalSwitchGuard(RalphGlobals ralph_globals)
        {
            super(false,new Double(0),ralph_globals);
        }

        // FIXME: still must fill in all methods for speculation and hooking to
        // hardware.
    }
}