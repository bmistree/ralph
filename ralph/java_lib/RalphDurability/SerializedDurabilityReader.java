package RalphDurability;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import ralph.Util;

import ralph_protobuffs.DurabilityProto.Durability;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete;

/**
   Runs through full stream of durability updates.  For each prepare
   message, discard those that were not committed.  Keep track of
   order of those that did complete and those that are logged.

   For a stream of commands:
   AA'BCC'B'DD"E

   were A' indicates that A has been successfully commited, D"
   indicates that D was backed out and E may or may not be still
   outstanding (because it has no match)

   Must maintain order that if some action was committed before
   another, it gets returned first.
 */
public class SerializedDurabilityReader implements ISerializedDurabilityReader
{
    final private IDurabilityReader durability_reader;
    /**
       Contains unmatched commit messages.  If we reach the end of
       durability_reader and there are still elements in this map,
       these are outstanding commits, and calls to
       next_durability_message can return them in any order as

       Keys are a combination of host uuid (if it exists) and event uuids.
     */
    final private Map<String,DurabilityPrepare> outstanding_request_map =
        new HashMap<String,DurabilityPrepare> ();
    
    public SerializedDurabilityReader(IDurabilityReader durability_reader)
    {
        this.durability_reader = durability_reader;
    }

    /**
       Gets any outstanding message from the request map and returns
       it wrapped in a DurabilityEvent.  Note, we've already checked
       that we're out of other messages to read from the reader so
       that any messages left in outstanding_request_map will be
       outstanding.
     */
    private DurabilityEvent get_outstanding_if_exists()
    {
        String key_to_remove = null;
        
        for (Entry<String,DurabilityPrepare> entry :
                 outstanding_request_map.entrySet())
        {
            key_to_remove = entry.getKey();
            break;
        }

        // no elements left in map: return null.
        if (key_to_remove == null)
            return null;

        DurabilityPrepare durability_prepare =
            outstanding_request_map.remove(key_to_remove);

        return new DurabilityEvent(
            DurabilityEvent.DurabilityEventType.OUTSTANDING,
            durability_prepare);
    }

    private String key_from_combination(String host_uuid, String event_uuid)
    {
        if (host_uuid != null)
            return host_uuid + ":" + event_uuid;
        return event_uuid;
    }
    
    /**
       @return null if out of durability messsages to replay.
     */
    @Override
    public DurabilityEvent next_durability_event()
    {
        // keep reading messages from reader until run out, or we can
        // return a complete message.
        while(true)
        {
            Durability msg = durability_reader.get_durability_msg();
            if (msg == null)
                return get_outstanding_if_exists();

            
            // 1) if message is a prepare message, add it to map, and
            // keep reading.
            // 2) if message is a complete message without backout,
            // remove from map and return durability pair.
            // 3) if message is a complete message with backout, then
            // remove from map, and keep reading.
            // 4) message is a serialized service factory: wrap it and
            // return it.
            if (msg.hasPrepare())
            {
                // case 1
                DurabilityPrepare prep = msg.getPrepare();

                String event_uuid = prep.getEventUuid().getData();
                String host_uuid =  null;
                if (prep.hasHostUuid())
                    host_uuid = prep.getHostUuid().getData();
                String outstanding_map_key =
                    key_from_combination(host_uuid,event_uuid);
                
                outstanding_request_map.put(outstanding_map_key,prep);
            }
            else if (msg.hasComplete())
            {
                DurabilityComplete comp = msg.getComplete();

                String event_uuid = comp.getEventUuid().getData();
                String host_uuid =  null;
                if (comp.hasHostUuid())
                    host_uuid = comp.getHostUuid().getData();
                String outstanding_map_key =
                    key_from_combination(host_uuid,event_uuid);
                
                DurabilityPrepare prep =
                    outstanding_request_map.remove(outstanding_map_key);
                //// DEBUG
                if (prep == null)
                {
                    Util.logger_assert(
                        "Got complete message for an event that " +
                        "hadn't begun to commit yet.");
                }
                //// END DEBUG
                
                if (comp.getSucceeded())
                {
                    // case 2
                    DurabilityEvent to_return =
                        new DurabilityEvent(
                            DurabilityEvent.DurabilityEventType.COMPLETED,
                            prep);

                    return to_return;
                }
                // case 3 is else of this condition.
            }
            else if (msg.hasServiceFactory())
            {
                // case 4: message is a service factory
                DurabilityEvent to_return =
                    new DurabilityEvent(msg.getServiceFactory());
                return to_return;
            }
            //// DEBUG: shouldn't be any other message types.
            else
            {
                Util.logger_assert(
                    "Got uknown durability message when replaying.");
            }
            //// END DEBUG
        }
    }
}
