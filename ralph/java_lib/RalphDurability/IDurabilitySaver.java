package RalphDurability;

import java.util.List;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;

public interface IDurabilitySaver
{
    /**
       Blocks until prepare and complete have been appended to log.
     */
    public void prepare_operation(
        String event_uuid, List<PartnerRequestSequenceBlock> rpc_args);
    public void complete_operation(String event_uuid, boolean succeeded);
}