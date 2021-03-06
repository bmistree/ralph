package emit_test_harnesses;

import ralph_emitted.BasicRalphJava.SetterGetter;
import ralph_emitted.EndpointLibUserJava.EndpointUser;
import ralph.RalphGlobals;

public class EndpointCalls
{
    public static void main(String[] args)
    {
        if (EndpointCalls.run_test())
            System.out.println("\nSUCCESS in EndpointCalls\n");
        else
            System.out.println("\nFAILURE in EndpointCalls\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            
            SetterGetter internal_endpt =
                SetterGetter.create_single_sided(ralph_globals);

            EndpointUser endpt =
                EndpointUser.create_single_sided(ralph_globals);

            endpt.set_endpoint(internal_endpt);
            

            // testing numbers
            double original_internal_number = endpt.get_number().doubleValue();
            for (int i = 0; i < 20; ++i)
            {
                double new_number = original_internal_number + ((double)1);
                endpt.set_number(new_number);
                double gotten_number = endpt.get_number().doubleValue();
                if (gotten_number != new_number)
                    return false;

                double internal_gotten_number = internal_endpt.get_number().doubleValue();
                if (internal_gotten_number != new_number)
                    return false;
            }

            // testing texts
            String original_internal_text = endpt.get_text();
            for (int i = 0; i < 20; ++i)
            {
                String new_text = original_internal_text + "hello";
                endpt.set_text(new_text);
                String gotten_text = endpt.get_text();
                if (! new_text.equals(gotten_text))
                    return false;

                String internal_gotten_text = internal_endpt.get_text();
                if (! new_text.equals(internal_gotten_text))
                    return false;
            }

            // testing tfs
            boolean original_internal_bool = endpt.get_tf().booleanValue();
            boolean new_boolean = original_internal_bool;
            for (int i = 0; i < 20; ++i)
            {
                new_boolean = ! new_boolean;
                endpt.set_tf(new_boolean);
                boolean gotten_boolean = endpt.get_tf().booleanValue();
                if (gotten_boolean != new_boolean)
                    return false;

                boolean internal_gotten_boolean = internal_endpt.get_tf().booleanValue();
                if (internal_gotten_boolean != new_boolean)
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