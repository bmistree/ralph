package emit_test_harnesses;

import ralph_emitted.SingleSideServiceFactoryJava.ServiceReceiver;
import ralph_emitted.PartnerServiceFactoryJava.ServiceFactorySender;
import ralph_emitted.BasicRalphJava.SetterGetter;
import RalphConnObj.SameHostConnection;

import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.EndpointConstructorObj;


public class PartnerSendServiceFactoryTest
{
    private final static int NUM_APPENDS_TO_RUN = 20;
    private final static int TCP_CONNECTION_PORT_RECEIVER = 20494;
    private final static int TCP_CONNECTION_PORT_SENDER = 20495;

    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in PartnerSendServiceFactoryTest\n");
        else
            System.out.println("\nFAILURE in PartnerSendServiceFactoryTest\n");
    }
    
    public static boolean run_test()
    {
        RalphGlobals.Parameters params_receiver =
            new RalphGlobals.Parameters();
        params_receiver.tcp_port_to_listen_for_connections_on =
            TCP_CONNECTION_PORT_RECEIVER;
        
        RalphGlobals.Parameters params_sender =
            new RalphGlobals.Parameters();
        params_sender.tcp_port_to_listen_for_connections_on =
            TCP_CONNECTION_PORT_SENDER;

        
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();            
            
            RalphGlobals receiver_globals =
                new RalphGlobals(params_receiver);
            ServiceReceiver service_receiver_endpt =
                ServiceReceiver.external_create(receiver_globals,conn_obj);
            
            RalphGlobals sender_globals =
                new RalphGlobals(params_sender);
            ServiceFactorySender service_sender_endpt =
                ServiceFactorySender.external_create(sender_globals,conn_obj);
            

            // build service factory to send to other side
            EndpointConstructorObj setter_getter_factory =
                SetterGetter.factory;
            
            InternalServiceFactory service_factory_to_send =
                new InternalServiceFactory(
                    setter_getter_factory,sender_globals);

            // actually send service factory to partner
            service_sender_endpt.send_service_factory_to_partner(
                service_factory_to_send);

            
            // construct endpt based on service factory sent and test
            // to ensure that the constructed endpoint works.
            service_receiver_endpt.construct_endpt();

            Double start_val = service_receiver_endpt.get_endpt_number();
            service_receiver_endpt.increment_endpt_number(1.0);
            if (start_val.equals(service_receiver_endpt.get_endpt_number()))
                return false;

            service_receiver_endpt.construct_endpt();
            if (! service_receiver_endpt.get_endpt_number().equals(start_val))
                return false;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
