package emit_test_harnesses;

import ralph_emitted.AtomicSetterGetterJava.AtomicSetterGetter;
import ralph_emitted.RangeTestJava.RangeTest;

import RalphDurability.DurabilityReplayer;

import ralph.RalphGlobals;
import ralph.DurabilityInfo;
import ralph.RalphObject;
import ralph.EndpointConstructorObj;



public class DurabilityConstructorCheck
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in DurabilityConstructorCheck\n");
        else
            System.out.println("\nFAILURE in DurabilityConstructorCheck\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals.Parameters parameters = new RalphGlobals.Parameters();
            RalphGlobals ralph_globals = new RalphGlobals(parameters);

            AtomicSetterGetter endpt =
                AtomicSetterGetter.create_single_sided(ralph_globals);

            RangeTest rt_service =
                RangeTest.create_single_sided(ralph_globals);


            // Now, try to replay from durability file
            DurabilityReplayer replayer =
                (DurabilityReplayer)DurabilityInfo.instance.durability_replayer;

            while(replayer.step()){}

            // check that the constructor objects are there for
            // AtomicSetterGetter and RangeTest
            EndpointConstructorObj range_constructor =
                replayer.get_constructor_obj(
                    RangeTest.factory.get_canonical_name());

            if (range_constructor == null)
                return false;
            
            EndpointConstructorObj atomic_setter_getter_constructor =
                replayer.get_constructor_obj(
                    AtomicSetterGetter.factory.get_canonical_name());
            
            if (atomic_setter_getter_constructor == null)
                return false;

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}