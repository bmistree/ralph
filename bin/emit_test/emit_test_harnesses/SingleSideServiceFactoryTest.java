package emit_test_harnesses;

import ralph_emitted.SingleSideServiceFactory.ServiceReceiver;
import ralph_emitted.BasicRalph.SetterGetter;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.EndpointConstructorObj;


public class SingleSideServiceFactoryTest
{
    private final static int NUM_APPENDS_TO_RUN = 20;
    
    public static void main(String[] args)
    {
        if (SingleSideServiceFactoryTest.run_test())
            System.out.println("\nSUCCESS in SingleSideServiceFactoryTest\n");
        else
            System.out.println("\nFAILURE in SingleSideServiceFactoryTest\n");
    }
    
    public static boolean run_test()
    {
        try {

            RalphGlobals ralph_globals = new RalphGlobals();
            ServiceReceiver endpt =
                (ServiceReceiver)ServiceReceiver.factory.construct(
                    ralph_globals,
                    new SingleSideConnection());

            EndpointConstructorObj setter_getter_factory =
                SetterGetter.factory;
            
            InternalServiceFactory service_factory =
                new InternalServiceFactory(setter_getter_factory,ralph_globals);
            
            endpt.set_service_factory(service_factory);
            endpt.construct_endpt();

            Double start_val = endpt.get_endpt_number();
            endpt.increment_endpt_number(1.0);
            if (start_val.equals(endpt.get_endpt_number()))
                return false;

            endpt.construct_endpt();
            if (! endpt.get_endpt_number().equals(start_val))
                return false;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
