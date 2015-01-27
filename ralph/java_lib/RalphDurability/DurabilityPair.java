package RalphDurability;

import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;

public class DurabilityPair
{
    public enum DurabilityType
    {
        // message had a pair that completed.
        COMPLETED,
        // unknown whether message completed.
        OUTSTANDING;
    }

    final public DurabilityType complete_type;
    final public DurabilityPrepare prepare_msg;

    public DurabilityPair(
        DurabilityType complete_type, DurabilityPrepare prepare_msg)
    {
        this.complete_type = complete_type;
        this.prepare_msg = prepare_msg;
    }
}