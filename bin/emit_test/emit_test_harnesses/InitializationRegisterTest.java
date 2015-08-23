package emit_test_harnesses;

import ralph_emitted.InitializationRegisterJava.Main;
import ralph_emitted.InitializationRegisterJava.App;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.Ralph;

public class InitializationRegisterTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in InitializationRegisterTest\n");
        else
            System.out.println("\nFAILURE in InitializationRegisterTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            InternalServiceFactory factory =
                new InternalServiceFactory(App.factory, ralph_globals);
            Main endpt = Main.external_create(ralph_globals);
            Double num = endpt.run(factory);
            if (!num.equals(55.0)) {
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