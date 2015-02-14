package emit_test_harnesses;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import ralph_emitted.AtomicSetterGetterJava.AtomicSetterGetter;
import ralph_emitted.DurabilityManyServicesJava.SetterGetterWrapper;

import RalphDurability.DurabilityReplayer;

import ralph.RalphGlobals;
import ralph.DurabilityInfo;


public class DurabilityManyServicesReplayer
{
    public final static int NUM_SERVICES_TO_GENERATE = 10;
    public final static int MAX_NUM_OSP_TO_PERFORM = 30;
    
    public static void main(String[] args)
    {
        if (run_test())
        {
            System.out.println(
                "\nSUCCESS in DurabilityManyServicesReplayer\n");
        }
        else
        {
            System.out.println(
                "\nFAILURE in DurabilityManyServicesReplayer\n");
        }
    }

    public static boolean run_test()
    {
        Random rand = new Random();
        List<AtomicSetterGetter> setter_getter_list =
            new ArrayList<AtomicSetterGetter>();
        
        try
        {
            RalphGlobals.Parameters parameters = new RalphGlobals.Parameters();
            RalphGlobals ralph_globals = new RalphGlobals(parameters);
            
            SetterGetterWrapper setter_getter_wrapper =
                SetterGetterWrapper.create_single_sided(ralph_globals);
            

            String text_to_set_to = "";
            int num_to_set_to = 0;
            boolean tf_to_set_to = false;
            for (int i = 0; i < NUM_SERVICES_TO_GENERATE; ++i)
            {
                AtomicSetterGetter internal_setter_getter =
                    AtomicSetterGetter.create_single_sided(ralph_globals);
                setter_getter_wrapper.set_internal(internal_setter_getter);
                setter_getter_list.add(internal_setter_getter);

                for (int j = 0; j < rand.nextInt(MAX_NUM_OSP_TO_PERFORM); ++j)
                {
                    // update vars to set to 
                    text_to_set_to += "a";
                    num_to_set_to += 1;
                    tf_to_set_to = ! tf_to_set_to;

                    internal_setter_getter.set_number(
                        new Double(num_to_set_to));

                    // update atomic text
                    internal_setter_getter.set_text(text_to_set_to);
                
                    // update atomic tf
                    internal_setter_getter.set_tf(tf_to_set_to);
                }
            }
            
            // Now, try to replay from durability file
            DurabilityReplayer replayer =
                (DurabilityReplayer)DurabilityInfo.instance.durability_replayer;

            while(replayer.step(ralph_globals)){}

            // check that all services that we set return correct vals
            // from getters.
            for (AtomicSetterGetter original_setter_getter : setter_getter_list)
            {
                AtomicSetterGetter replayed_setter_getter =
                    (AtomicSetterGetter)replayer.get_endpoint_if_exists(
                        original_setter_getter.uuid());

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
            }

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}