package ralph;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import RalphExceptions.BackoutException;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.ArgumentContainerDeltas;
import ralph_protobuffs.UtilProto.UUID;

/**
   When an ralph object is being serialized part of an rpc call, that
   object may point to another object.  We can either recursively
   serialize that internal value when we encounter it, or we can add a
   note to the context to also serialize the internal value its
   holding.
 */
public class SerializationContext
{
    protected final Map<String,RalphObject> still_to_serialize =
        new HashMap<String,RalphObject>();
    protected final Map<String,RalphObject> serialized =
        new HashMap<String,RalphObject>();

    /**
       If deep-copying data for an rpc, then when call
       serialize_contents on each, internallists and internalmaps add
       all of their values as individual deltas, which ultimately get
       added to message.
     */
    protected final List<ArgumentContainerDeltas.Builder> container_deltas_list =
        new ArrayList<ArgumentContainerDeltas.Builder>();
    
    protected final List<RalphObject> arguments_list;
    /**
       Whether containers, etc. should copy internal values.
     */
    public final boolean deep_copy;
    
    public SerializationContext(boolean _deep_copy)
    {
        this(null,_deep_copy);

    }
    
    public SerializationContext(
        List<RalphObject> _arguments_list,boolean _deep_copy)
    {
        arguments_list = _arguments_list;
        deep_copy = _deep_copy;
        if (arguments_list != null)
        {
            for (RalphObject ro : arguments_list)
                still_to_serialize.put(ro.uuid(),ro);
        }
    }

    /**
       When serializing 
     */
    public void add_argument_container_delta(
        ArgumentContainerDeltas.Builder arg_container_delta)
    {
        container_deltas_list.add(arg_container_delta);
    }
    
    public void add_to_serialize(RalphObject ro)
    {
        if (serialized.containsKey(ro.uuid()))
            return;
        still_to_serialize.put(ro.uuid(),ro);        
    }

    public Arguments.Builder serialize_all(ActiveEvent active_event)
        throws BackoutException
    {
        Arguments.Builder to_return = Arguments.newBuilder();
        // set argument uuid list
        if (arguments_list != null)
        {
            for (RalphObject ro : arguments_list)
            {
                UUID.Builder arg_uuid_builder = UUID.newBuilder();
                arg_uuid_builder.setData(ro.uuid());
                to_return.addArgumentUuids(arg_uuid_builder);
            }
        }

        // now populate contents: note the reason for the outside
        // while loop is that calls to serialize_contents can add
        // additional entries to still_to_serialize.
        while(! still_to_serialize.isEmpty())
        {
            for (Iterator<Entry<String,RalphObject>> it =
                     still_to_serialize.entrySet().iterator();
                 it.hasNext();)
            {
                Entry<String,RalphObject> entry = it.next();
                RalphObject ro = entry.getValue();

                ObjectContents obj_contents = ro.serialize_contents(
                    active_event,null,this);
                to_return.addContextContents(obj_contents);

                it.remove();
            }
        }

        for (ArgumentContainerDeltas.Builder arg_container_deltas :
                 container_deltas_list)
        {
            to_return.addContainerDeltas(arg_container_deltas);
        }
        
        return to_return;
    }
}