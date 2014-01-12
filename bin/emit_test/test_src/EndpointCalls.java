package emit_test_harnesses;

import emit_test_package.BasicRalph.SetterGetter;
import emit_test_package.EndpointLibUser.EndpointUser;
import RalphConnObj.SingleSideConnection;
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
            String dummy_host_uuid = "dummy_host_uuid";

            SetterGetter internal_endpt = new SetterGetter(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            EndpointUser endpt = new EndpointUser(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

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