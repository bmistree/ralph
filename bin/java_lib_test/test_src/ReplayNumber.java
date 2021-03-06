package java_lib_test;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import ralph.RalphGlobals;
import ralph.ActiveEvent;
import ralph.ExecutingEventContext;
import ralph.VersioningInfo;
import ralph.Endpoint;
import ralph.RalphObject;
import ralph.RootEventParent;
import ralph.VersioningInfo;
import RalphVersions.IVersionReplayer;
import RalphVersions.ReconstructionContext;
import ralph.Variables.AtomicNumberVariable;
import RalphVersions.InMemoryVersionManager;
import RalphVersions.ObjectHistory;
import RalphConnObj.SingleSideConnection;
import RalphCallResults.RootCallResult.ResultType;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import RalphVersions.ObjectContentsDeserializers;
import RalphVersions.ObjectHistory.SingleObjectChange;

public class ReplayNumber
{
    protected static final String test_name = "ReplayNumber";
    private static final int NUM_SETS = 5;
    
    public static void main(String [] args)
    {
        String prefix = "Test " + test_name + "\n";
        if (run_test())
            System.out.println(prefix + " SUCCEDED\n");
        else
            System.out.println(prefix + " FAILED\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            Endpoint endpt = new DefaultEndpoint(ralph_globals);

            Double initial_value = 3.;
            AtomicNumberVariable atom_num =
                new AtomicNumberVariable(false,initial_value,ralph_globals);

            List<Double> updates_set = new ArrayList<Double>();
            for (int i = 0; i < NUM_SETS; ++i)
            {
                Double to_set_to = new Double((double)i);
                updates_set.add(to_set_to);
                boolean set_complete = perform_set(atom_num,to_set_to,endpt);
                if (! set_complete)
                    return false;
            }

            // first check that the number of changes to the object is
            // the same as the size of the object's history.
            IVersionReplayer replayer =
                VersioningInfo.instance.version_replayer;
            ObjectHistory obj_history =
                replayer.get_full_object_history(atom_num.uuid());
            
            if (obj_history == null)
                return false;

            if (obj_history.history.size() != updates_set.size())
                return false;

            ReconstructionContext reconstruction_context =
                new ReconstructionContext (replayer,ralph_globals);
            
            // now replay object and see if it matches
            ObjectContents construction_contents =
                obj_history.get_construction_contents();
            
            AtomicNumberVariable replayed_atom_num =
                (AtomicNumberVariable) ObjectContentsDeserializers.deserialize(
                    construction_contents,ralph_globals,
                    reconstruction_context);
            
            Double initial_replayed_internal_val =
                replayed_atom_num.get_val(null);
            // warning: I know it's not great that I'm testing
            // equality of floating point numbers, but it'll likely be
            // fine because I started with integers.
            if (! initial_replayed_internal_val.equals(initial_value))
                return false;

            // compare updates against logged updates
            int updates_set_index = 0;
            Set <SingleObjectChange> single_object_change_set =
                obj_history.history;
            for (SingleObjectChange change : single_object_change_set)
            {
                SingleObjectChange.number_incorporate_single_object_change(
                    change,replayed_atom_num,null);
                Double replayed_val = replayed_atom_num.get_val(null);
                Double actual_val = updates_set.get(updates_set_index);
                
                // warning: I know it's not great that I'm testing
                // equality of floating point numbers, but it'll
                // likely be fine because I started with integers.
                if (!actual_val.equals(replayed_val))
                    return false;
                
                ++updates_set_index;
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
       @returns {boolean} true if event completes.  false if it
       doesn't.  Note: events should alwasy complete, so return value
       is almost like an error code.
     */
    public static boolean perform_set(
        AtomicNumberVariable atom_num, Double to_set_to,
        Endpoint endpt) throws Exception
    {
        // generate event
        ActiveEvent writer =
            endpt._act_event_map.create_root_non_atomic_event(
                endpt,"dummy");
        RootEventParent writer_event_parent =
            (RootEventParent)writer.event_parent;

        // set value
        atom_num.set_val(writer,to_set_to);
        writer.local_root_begin_first_phase_commit();

        // try commiting change to value
        ResultType writer_commit_resp =
            writer_event_parent.event_complete_queue.take();
        if (writer_commit_resp != ResultType.COMPLETE)
            return false;
        return true;
    }

    public static class DefaultEndpoint extends Endpoint
    {
        public DefaultEndpoint(RalphGlobals ralph_globals)
        {
            super(
                ralph_globals,new SingleSideConnection(),
                // EndpointConstructorObj isn't needed for this test.
                null);
        }
        @Override
        protected RalphObject _handle_rpc_call(
            String to_exec_internal_name,ActiveEvent active_event,
            ExecutingEventContext ctx,
            Object...args)
            throws ApplicationException, BackoutException, NetworkException,
            StoppedException
        {
            // not worrying about receiving rpc calls.
            return null;
        }
    }
    
}