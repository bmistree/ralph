package emit_test_harnesses;

import java.util.List;
import java.io.IOException;

import ralph.Ralph;
import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import ralph.Endpoint;
import ralph.EndpointConstructorObj;
import ralph.InternalServiceFactory;

import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;

import ralph_emitted.ReplayPartnerNumberChangeJava.NumberSender;
import ralph_emitted.ReplayPartnerNumberChangeJava.NumberReceiver;

import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityReplayContext;

public class ReplayPartnerNumber
{
    private static final String HOST_NAME = "localhost";
    private static final int TCP_LISTENING_PORT = 38689;

    private final static int TCP_CONNECTION_PORT_SENDER = 20494;
    private final static int TCP_CONNECTION_PORT_RECEIVER = 20495;
    private final static int TCP_CONNECTION_PORT_RECONSTRUCTOR = 20496;

    private final static int LAST_NUMBER_UPDATED = 100;


    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in ReplayPartnerNumber\n");
        else
            System.out.println("\nFAILURE in ReplayPartnerNumber\n");
    }

    public static boolean run_test()
    {
        RalphGlobals.Parameters params_sender =
            new RalphGlobals.Parameters();
        params_sender.tcp_port_to_listen_for_connections_on =
            TCP_CONNECTION_PORT_SENDER;

        RalphGlobals.Parameters params_receiver =
            new RalphGlobals.Parameters();
        params_receiver.tcp_port_to_listen_for_connections_on =
            TCP_CONNECTION_PORT_RECEIVER;

        RalphGlobals.Parameters params_reconstructor =
            new RalphGlobals.Parameters();
        params_reconstructor.tcp_port_to_listen_for_connections_on =
            TCP_CONNECTION_PORT_RECONSTRUCTOR;

        try
        {
            RalphGlobals ralph_globals_receiver =
                new RalphGlobals(params_receiver);
            RalphGlobals ralph_globals_sender =
                new RalphGlobals(params_sender);

            Ralph.tcp_connect("127.0.0.1", TCP_CONNECTION_PORT_SENDER,
                              ralph_globals_receiver);

            // wait for the other side to ensure that it's listening
            Thread.sleep(1000);

            NumberSender sender =
                NumberSender.create_single_sided(ralph_globals_sender);

            InternalServiceFactory receiver_factory =
                new InternalServiceFactory(
                    NumberReceiver.factory, ralph_globals_receiver);
            sender.install_partner(receiver_factory);


            for (int i = 0; i <= LAST_NUMBER_UPDATED; ++i)
                sender.send_update_number((double) i);

            // double check that number on receiver is what we'd expect:
            int receive_value = sender.get_number().intValue();
            if (receive_value != LAST_NUMBER_UPDATED)
                return false;

            String receiver_endpt_uuid = sender.get_remote_endpt().service_uuid;

            VersioningInfo.instance.version_saver.flush();


            // Now replay
            RalphGlobals ralph_globals_reconstructor =
                new RalphGlobals(params_reconstructor);
            IReconstructionContext reconstruction_context =
                new ReconstructionContext(
                    VersioningInfo.instance.version_replayer,
                    ralph_globals_reconstructor);

            // now, tries to replay changes to endpoint.
            NumberReceiver replayed_receiver =
                (NumberReceiver) VersionUtil.rebuild_endpoint(
                    receiver_endpt_uuid,ralph_globals_reconstructor,
                    reconstruction_context);

            int replayed_value = replayed_receiver.get_number().intValue();
            if (replayed_value != LAST_NUMBER_UPDATED)
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