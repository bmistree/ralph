package emit_test_harnesses;

import ralph_emitted.ReferenceServiceFactoryJava.Sender;
import ralph_emitted.ReferenceServiceFactoryJava.Receiver;
import ralph_emitted.ReferenceServiceFactoryJava.SenderCreated;
import ralph_emitted.ReferenceServiceFactoryJava.ReceiverCreated;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.EndpointConstructorObj;


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
            SameHostConnection conn_obj = new SameHostConnection();
            RalphGlobals sender_globals =
                new RalphGlobals(params_sender);
            Sender sender = new Sender(sender_globals,conn_obj);
            Receiver receiver =
                new Receiver(
                    new RalphGlobals(params_receiver),conn_obj);

            EndpointConstructorObj receiver_creater =
                ReceiverCreated.factory;
            EndpointConstructorObj sender_creater =
                SenderCreated.factory;

            // service factories are instantiated on sender, rather
            // than receiver (ie, use sender_globals here)
            InternalServiceFactory receiver_creater_service_factory =
                new InternalServiceFactory(receiver_creater,sender_globals);
            InternalServiceFactory sender_creater_service_factory =
                new InternalServiceFactory(sender_creater,sender_globals);
            
            Double result =
                sender.instantiate_remote(
                    receiver_creater_service_factory,
                    sender_creater_service_factory);
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