package emit_test_harnesses;

import ralph_emitted.SingleSideServiceFactoryJava.ServiceReceiver;
import ralph_emitted.PartnerServiceFactoryJava.ServiceFactorySender;
import ralph_emitted.BasicRalphJava.SetterGetter;

import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.EndpointConstructorObj;
import ralph.Ralph;


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
            RalphGlobals sender_globals =
                new RalphGlobals(params_sender);
            ServiceFactorySender service_sender_endpt =
                ServiceFactorySender.external_create(sender_globals);

            RalphGlobals receiver_globals =
                new RalphGlobals(params_receiver);

            // connect receiver to sender as separate endpoints
            Thread.sleep(500);
            Ralph.tcp_connect("127.0.0.1",TCP_CONNECTION_PORT_RECEIVER, sender_globals);
            Thread.sleep(500);

            // Everything should be connected now.

            // 1. Generate a remote ServiceReceiver
            InternalServiceFactory service_receiver_factory_to_send =
                new InternalServiceFactory(
                    ServiceReceiver.factory, sender_globals);
            service_sender_endpt.install_remote_service_receiver(service_receiver_factory_to_send);

            // 2. Send a SetterGetter to the previously-installed ServiceReceiver.
            InternalServiceFactory service_setter_getter_factory_to_send =
                new InternalServiceFactory(
                    SetterGetter.factory, sender_globals);
            service_sender_endpt.send_service_factory_to_partner(
                service_setter_getter_factory_to_send);

            // 3. Construct endpoint on remote ServiceReceiver
            service_sender_endpt.construct_endpt_on_remote();

            // 4. Test to ensure that the constructed endpoint works.
            Double start_val = service_sender_endpt.get_remote_endpt_number();
            service_sender_endpt.increment_remote_endpt_number(1.0);
            if (start_val.equals(service_sender_endpt.get_remote_endpt_number()))
                return false;

            service_sender_endpt.construct_endpt_on_remote();
            if (! service_sender_endpt.get_remote_endpt_number().equals(start_val))
                return false;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
