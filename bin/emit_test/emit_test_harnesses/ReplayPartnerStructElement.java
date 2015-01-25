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

import RalphDurability.DurabilityContext;
import RalphDurability.DurabilityReplayContext;

import ralph_emitted.ReplayPartnerStructElementJava.StructSender;
import ralph_emitted.ReplayPartnerStructElementJava.StructReceiver;


public class ReplayPartnerStructElement
{
    private static final String HOST_NAME = "localhost";
    private static final int TCP_LISTENING_PORT = 38689;

    private static final ReceiverConstructor RECEIVER_CONSTRUCTOR =
        new ReceiverConstructor();
    
    private static StructSender sender = null;
    private static StructReceiver receiver = null;
    private final static int TCP_CONNECTION_PORT_SENDER = 20494;
    private final static int TCP_CONNECTION_PORT_RECEIVER = 20495;
    private final static int TCP_CONNECTION_PORT_RECONSTRUCTOR = 20496;
    
    private final static int LAST_NUMBER_UPDATED = 100;
    
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in ReplayPartnerStructElement\n");
        else
            System.out.println("\nFAILURE in ReplayPartnerStructElement\n");
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
                sender = (StructSender)Ralph.tcp_connect(
                    StructSender.factory, HOST_NAME, TCP_LISTENING_PORT,
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
                sender.send_struct((double) i);

            // double check that number on receiver is what we'd expect:
            if (!perform_correctness_checks(receiver))
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
            StructReceiver replayed_receiver =
                (StructReceiver) VersionUtil.rebuild_endpoint(
                    receiver._uuid,ralph_globals_reconstructor,
                    reconstruction_context);

            // ensure replayed receiver has correct internal values.
            if (!perform_correctness_checks(replayed_receiver))
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
                (StructReceiver) StructReceiver.factory.construct(
                    globals,conn_obj,durability_context,
                    durability_replay_context);
            return receiver;
        }
        @Override
        public Endpoint construct(
            RalphGlobals globals, RalphConnObj.ConnectionObj conn_obj,
            List<RalphObject> internal_val_list,
            DurabilityContext durability_context)
        {
            return construct(globals,conn_obj,durability_context,null);
        }

        @Override
        public String get_canonical_name()
        {
            return canonical_name;
        }
    }

    private static boolean perform_correctness_checks(StructReceiver receiver)
        throws Exception
    {
        int size_of_struct_list = receiver.struct_list_size().intValue();
        // note +1 because we performed this many sets.
        if (size_of_struct_list != (LAST_NUMBER_UPDATED +1))
            return false;

        for (int i = 0; i <= LAST_NUMBER_UPDATED; ++i)
        {
            int internal_struct_value =
                receiver.struct_list_internal_val((double)i).intValue();
            if (i != internal_struct_value)
                return false;
        }
        return true;
    }

}