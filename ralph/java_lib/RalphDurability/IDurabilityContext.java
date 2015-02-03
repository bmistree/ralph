package RalphDurability;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.DurabilityProto.Durability;

public interface IDurabilityContext
{
    /**
       Makes a deep copy of the state of the current durability
       context with a different event_uuid (new_event_uuid).  Basic
       idea is that from a non-atomic event, when we encounter an
       atomic event, we want to copy its durability context.  We only
       do stable logging when we complete atomic events.
     */
    public IDurabilityContext clone(String new_event_uuid);

    public void add_endpt_created_info(
        String endpt_uuid,String endpt_constructor_name);
    
    public void add_rpc_arg(
        PartnerRequestSequenceBlock arg, String endpoint_uuid);

    public Durability prepare_proto_buf();
    public Durability complete_proto_buf(boolean succeeded);
}