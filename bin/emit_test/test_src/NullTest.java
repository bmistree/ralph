package emit_test_harnesses;

import emit_test_package.Null.NullService;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class NullTest
{
    public static void main(String[] args)
    {
        if (NullTest.run_test())
            System.out.println("\nSUCCESS in NullTest\n");
        else
            System.out.println("\nFAILURE in NullTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            NullService service = new NullService(
                new RalphGlobals(),
                new SingleSideConnection());

            if (service.get_number() != null)
                return false;

            if (service.get_text() != null)
                return false;

            if (service.get_tf() != null)
                return false;


            Double [] doubles_to_test = {3.3,45.,null,null,30.1,null};
            for (Double d : doubles_to_test)
            {
                if (! check_set_number(service,d))
                    return false;
            }

            String [] strings_to_test = {"hi","hi",null,"hie",null};
            for (String s : strings_to_test)
            {
                if (! check_set_text(service,s))
                    return false;
            }

            Boolean [] bools_to_test = {true,false,null,null,false,false,true};
            for (Boolean b : bools_to_test)
            {
                if (! check_set_tf(service,b))
                    return false;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
       Returns true if test passed, false if it failed.
     */
    private static boolean check_set_number(
        NullService service,Double to_set_to) throws Exception
    {
        service.set_number(to_set_to);

        if (to_set_to == null)
            return to_set_to == service.get_number();

        return service.get_number().equals(to_set_to);
    }

    private static boolean check_set_text(
        NullService service,String to_set_to) throws Exception
    {
        service.set_text(to_set_to);

        if (to_set_to == null)
            return to_set_to == service.get_text();

        return service.get_text().equals(to_set_to);
    }
    
    private static boolean check_set_tf(
        NullService service,Boolean to_set_to) throws Exception
    {
        service.set_tf(to_set_to);

        if (to_set_to == null)
            return to_set_to == service.get_tf();

        return service.get_tf().equals(to_set_to);
    }    
}
