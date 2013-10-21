package ralph;

import java.util.HashMap;
import java.util.Map.Entry;

import WaldoExceptions.BackoutException;

import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleMapDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleNumberDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleTextDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleTrueFalseDelta;

/**
 * '''
_VariableStore keeps track of peered data, sequence local data (which
also has its peered bit marked in its WaldoReferenceBase), and a
single endpoint's global data.  Events can query the variable store
for these variables and also to determine what peered data have been
modified by an event (so that we know what to serialize to other sides
when completing a sequence block call).

An endpoint holds a single _VariableStore, global_store, which holds
endpoint global and peered data.  This gets created once on each
endpoint when the connection is first established.  It should neither
shrink nor grow.  Each executing event also has a copy of another
_VariableStore that keeps track of sequence local stae.

Three challenges:

  1) Problem: When requesting sequence blocks calls on our partner, we need to
     identify which peered and sequence local data have changed
     locally.  That way, we can send only modified data to the other
     side.

     Solution: @see _VariableStore.generate_deltas below.  Just run
     through all stored data and return all data that this
     invalidation listener has modified since beginning.

  2) We need to use consistent names for these modified data between
     both endpoints so that we can associate the variables on each
     side with each other.

     Solution: Each peered wVariable has a unique name????  The
     _name_to_var_map in each _VariableStore is indexed by these
     names.
     
     
  3) Through the course of a message sequence, we need to keep a list
     of sequence-local data available.

     Solution: We have a single execution event per message that we
     receive.  Each has its own separate context,
     _ExecutionEventContext, that holds on to a copy of a separate
     variable store for sequence local data.
     
     
     
         '''
    Each executing event has two, separate stores:

       * global_store --- Keeps track of endpoint globals as well as
         peered data.  Note: cannot change global_store.  Values there
         will only be written once.

       * sequence_local_store --- Keeps track of sequence local data.

    (Local data and function arguments are not put in any store.)

    Can query the variable store with the unique name of a variable to
    get the variable back for use.
    '''

'''

 * @author bmistree
 *
 */

public class VariableStore 
{
    private String host_uuid;
    private HashMap<String,LockedObject> name_to_var_map = 
        new HashMap<String, LockedObject>() ;
	
    public VariableStore(String _host_uuid)
    {
        host_uuid = _host_uuid;		
    }

    /**
     * @param {String} unique_name ---

     @param {_WaldoVariable} waldo_variable 
    */
    public void add_var(String unique_name,LockedObject waldo_variable)
    {
        // DEBUG
        if (get_var_if_exists(unique_name) != null)
        {
            Util.logger_assert(
                "Already had an entry for variable trying to " +
                "insert into store.");
        }
        //#### END DEBUG
          
        name_to_var_map.put(unique_name, waldo_variable); 
    }

    /**
     *
     @param {String} unique_name --- 
     @returns {_WaldoVariable or None} --- None if variable does
     not exist, _WaldoVariable otherwise.
    */
    public LockedObject get_var_if_exists(String unique_name) 
    {
        return name_to_var_map.get(unique_name);
    }


    public VarStoreDeltas.Builder generate_deltas(
        LockedActiveEvent active_event,boolean force)
    {
        VarStoreDeltas.Builder vstore_builders = VarStoreDeltas.newBuilder();
        return generate_deltas(active_event,force,vstore_builders);
    }

    /**
     * 
     Create a map with an entry for each piece of peered data that
     was modified.  The entry should contain a
     _SerializationHelperNamedTuple that contains the delta
     representation of the object on the other side of the
     connection.

     @param {bool} force --- True if regardless of whether changed
     or not, we serialize and send its value.

     An example of when this would be used

     Sequence some_seq(Text a)
     {
     Side1.send_msg
     {
     }
     Side2.recv_msg
     {
     print (a);
     }
     }

     The first block does not actually modify a.  Therefore, it
     wouldn't be included in the message sent to Side2.recv_msg
     unless we force serialization of deltas for all sequence local
     data on the first message we send.
        
     @returns {VarStoreDeltas} @see varStoreDeltas.proto
        
     Should be deserializable and appliable from
     incorporate_deltas.

    */
    public VarStoreDeltas.Builder generate_deltas(
        LockedActiveEvent active_event,boolean force, VarStoreDeltas.Builder all_deltas)
    {
        all_deltas.setParentType(VarStoreDeltas.ParentType.VAR_STORE_DELTA);

        for (Entry<String, LockedObject> entry : name_to_var_map.entrySet())
        {
            LockedObject waldo_variable = entry.getValue();
            // only need to transfer actually peered data
            if (waldo_variable.is_peered())
            {
                waldo_variable.serializable_var_tuple_for_network(
                    all_deltas,entry.getKey(),active_event,force);
            }
        }
        return all_deltas;
    }
	
    /**
       @param {LockedActiveEvent} active_event  ---

       @param {varStoreDeltas.VarStoreDeltas message}
        
    */
    public void incorporate_deltas(
        LockedActiveEvent active_event, VarStoreDeltas var_store_deltas)
    {
        // FIXME: allow incorporating deltas on multi-threaded
        // variables.
		

        // incorporate all numbers
        for (SingleNumberDelta num_delta : var_store_deltas.getNumDeltasList())
        {
            String var_name = num_delta.getVarName();
            LockedObject existing_value = name_to_var_map.get(var_name);
            if (existing_value == null)
            {
                //means that the variable was not in the variable
                //store already.  This could happen for instance if we
                //are creating a sequence local variable for the first
                //time.
                name_to_var_map.put(
                    var_name,
                    new LockedVariables.SingleThreadedLockedNumberVariable(
                        host_uuid, true, num_delta.getVarData()));
            } else
            {
                // only incorporating data on single threaded objects
                try {
                    existing_value.write_if_different(active_event,num_delta.getVarData());
                } catch (BackoutException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
		
        // incorporate all texts
        for (SingleTextDelta text_delta : var_store_deltas.getTextDeltasList())
        {
            String var_name = text_delta.getVarName();
            LockedObject existing_value = name_to_var_map.get(var_name);
            if (existing_value == null)
            {
                //means that the variable was not in the variable
                //store already.  This could happen for instance if we
                //are creating a sequence local variable for the first
                //time.
                name_to_var_map.put(
                    var_name,
                    new LockedVariables.SingleThreadedLockedTextVariable(
                        host_uuid, true, text_delta.getVarData()));
            }
            else
            {
                // currently, only incorporating data on single-threaded objects
                try {
                    existing_value.write_if_different(active_event,text_delta.getVarData());
                } catch (BackoutException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
		
        // incorporate all true falses
        for (SingleTrueFalseDelta tf_delta : var_store_deltas.getTrueFalseDeltasList())
        {
            String var_name = tf_delta.getVarName();
            LockedObject existing_value = name_to_var_map.get(var_name);
            if (existing_value == null)
            {
                //means that the variable was not in the variable
                //store already.  This could happen for instance if we
                //are creating a sequence local variable for the first
                //time.
                name_to_var_map.put(
                    var_name,
                    new LockedVariables.SingleThreadedLockedTrueFalseVariable(
                        host_uuid, true, tf_delta.getVarData()));
            }
            else
            {
                // currently, only incorporating data on single-threaded objects
                try {
                    existing_value.write_if_different(active_event,tf_delta.getVarData());
                } catch (BackoutException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }


        // incorporate all maps
        for (SingleMapDelta map_delta : var_store_deltas.getMapDeltasList())
        {
            String var_name = map_delta.getVarName();
            LockedVariables.SingleThreadedLockedMapVariable existing_value = 
                (LockedVariables.SingleThreadedLockedMapVariable)name_to_var_map.get(var_name);
            if (existing_value == null)
            {
                // FIXME: Is there any way that this is legal java????  I don't need the template types????
                LockedVariables.SingleThreadedLockedMapVariable to_put_in = 
                    new LockedVariables.SingleThreadedLockedMapVariable(host_uuid,true);
				
                name_to_var_map.put(var_name,to_put_in);
                to_put_in.incorporate_deltas(map_delta, active_event);
            }
            else
                existing_value.incorporate_deltas(map_delta, active_event);
        }
		
        // FIXME: fill in the rest of the deserialization for other container types.
        Util.logger_warn("Note: still must print incorporate all non-value type deltas\n");
    }
}


