package emit_test_harnesses;

import java.util.List;
import java.io.IOException;

import ralph.Ralph;
import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import ralph.Endpoint;
import ralph.EndpointConstructorObj;

import RalphConnObj.TCPConnectionObj;

import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;

import ralph_emitted.ReplayPartnerNumberChangeJava.NumberSender;
import ralph_emitted.ReplayPartnerNumberChangeJava.NumberReceiver;

import RalphDurability.DurabilityContext;
import RalphDurability.DurabilityReplayContext;


public class ReplayPartnerNumber
{
    private static final String HOST_NAME = "localhost";
    private static final int TCP_LISTENING_PORT = 38689;

    private static final ReceiverConstructor RECEIVER_CONSTRUCTOR =
        new ReceiverConstructor();
    
    private static NumberSender sender = null;
    private static NumberReceiver receiver = null;
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
            // populates receiver
            Ralph.tcp_accept(
                RECEIVER_CONSTRUCTOR, HOST_NAME, TCP_LISTENING_PORT,
                new RalphGlobals(params_receiver));


            // wait for the other side to ensure that it's listening
            Thread.sleep(1000);
            try
            {
                sender = (NumberSender)Ralph.tcp_connect(
                    NumberSender.factory, HOST_NAME, TCP_LISTENING_PORT,
                    new RalphGlobals(params_sender));
            }
            catch (IOException e)
            {
                e.printStackTrace();
                assert(false);
            }

            // wait for everything to settle down
            Thread.sleep(1000);

            for (int i = 0; i <= LAST_NUMBER_UPDATED; ++i)
                sender.send_update_number((double) i);

            // double check that number on receiver is what we'd expect:
            int receive_value = receiver.get_number().intValue();
            if (receive_value != LAST_NUMBER_UPDATED)
                return false;
            
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
                    receiver._uuid,ralph_globals_reconstructor,
                    reconstruction_context);

            int replayed_value = receiver.get_number().intValue();
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

    /**
       Must override constructor so that can save global SideB after
       constructing it.
     */
    private static class ReceiverConstructor implements EndpointConstructorObj
    {
        private final static String canonical_name =
            ReceiverConstructor.class.getName();
        
        @Override
        public Endpoint construct(
            RalphGlobals globals, RalphConnObj.ConnectionObj conn_obj,
            DurabilityContext durability_context,
            DurabilityReplayContext durability_replay_context)
        {
            receiver =
                (NumberReceiver)
                NumberReceiver.factory.construct(
                    globals,conn_obj,durability_context,
                    durability_replay_context);
            return receiver;
        }
        @Override
        public Endpoint construct(
            RalphGlobals globals, RalphConnObj.ConnectionObj conn_obj,
            List<RalphObject> internal_val_list,DurabilityContext durability_context)
        {
            return construct(globals,conn_obj,durability_context,null);
        }

        @Override
        public String get_canonical_name()
        {
            return canonical_name;
        }
    }
    
}