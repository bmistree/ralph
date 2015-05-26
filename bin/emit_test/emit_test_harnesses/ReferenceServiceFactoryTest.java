package emit_test_harnesses;

import ralph_emitted.ReferenceServiceFactoryJava.Sender;
import ralph_emitted.ReferenceServiceFactoryJava.Receiver;
import ralph_emitted.ReferenceServiceFactoryJava.ReceiverCreated;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.EndpointConstructorObj;
import ralph.Ralph;

public class ReferenceServiceFactoryTest
{
    private final static int TCP_CONNECTION_PORT_RECEIVER = 20494;
    private final static int TCP_CONNECTION_PORT_SENDER = 20495;

    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in ReferenceServiceFactoryTest\n");
        else
            System.out.println("\nFAILURE in ReferenceServiceFactoryTest\n");
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
            RalphGlobals globals_receiver = new RalphGlobals(params_receiver);
            RalphGlobals globals_sender = new RalphGlobals(params_sender);

            // connect hosts a and b, via a tcp connection
            Thread.sleep(500);
            Ralph.tcp_connect("127.0.0.1", TCP_CONNECTION_PORT_RECEIVER, globals_sender);
            Thread.sleep(500);

            // Instantiate SerializeNull and have it build a remote copy
            Sender sender = Sender.external_create(globals_sender);
            InternalServiceFactory service_receiver_factory_to_send =
                new InternalServiceFactory(
                    Receiver.factory, globals_sender);
            sender.install_partner(service_receiver_factory_to_send);

            EndpointConstructorObj receiver_creater =
                ReceiverCreated.factory;

            // service factories are instantiated on sender, rather
            // than receiver (ie, use sender_globals here)
            InternalServiceFactory receiver_creater_service_factory =
                new InternalServiceFactory(receiver_creater, globals_sender);

            Double result =
                sender.instantiate_remote(receiver_creater_service_factory);
            
            if (!result.equals(1.0))
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