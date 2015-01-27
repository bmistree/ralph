package emit_test_harnesses;

import ralph_emitted.AtomicSetterGetterJava.AtomicSetterGetter;

import RalphDurability.DurabilityReplayer;

import ralph.RalphGlobals;
import ralph.DurabilityInfo;


public class DurabilitySingleSideReplayer
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in DurabilitySingleSideReplayer\n");
        else
            System.out.println("\nFAILURE in DurabilitySingleSideReplayer\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals.Parameters parameters = new RalphGlobals.Parameters();
            RalphGlobals ralph_globals = new RalphGlobals(parameters);

            AtomicSetterGetter original_setter_getter =
                AtomicSetterGetter.create_single_sided(ralph_globals);

            String text_to_set_to = "";
            for (int i = 0; i < 100; ++i)
            {
                // update atomic number
                original_setter_getter.set_number(new Double(i));

                // update atomic text
                text_to_set_to += "a";
                original_setter_getter.set_text(text_to_set_to);
                
                // update atomic tf
                boolean tf_to_set_to = (i % 2) == 0;
                original_setter_getter.set_tf(tf_to_set_to);
            }


            // Now, try to replay from durability file
            DurabilityReplayer replayer =
                (DurabilityReplayer)DurabilityInfo.instance.durability_replayer;

            while(replayer.step(ralph_globals)){}


            AtomicSetterGetter replayed_setter_getter =
                (AtomicSetterGetter)replayer.get_endpt(original_setter_getter.uuid());

            if (replayed_setter_getter == null)
                return false;

            Double replayed_num = replayed_setter_getter.get_number();
            Double original_num = original_setter_getter.get_number();
            if (! replayed_num.equals(original_num))
                return false;

            String replayed_str = replayed_setter_getter.get_text();
            String original_str = original_setter_getter.get_text();
            if (! replayed_str.equals(original_str))
                return false;

            Boolean replayed_bool = replayed_setter_getter.get_tf();
            Boolean original_bool = original_setter_getter.get_tf();
            if (! replayed_bool.equals(original_bool))
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