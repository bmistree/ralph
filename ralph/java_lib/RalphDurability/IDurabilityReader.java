package RalphDurability;

import ralph_protobuffs.DurabilityProto.Durability;

public interface IDurabilityReader
{
    /**
       @return null when stream to read events from is exhausted.
     */
    public Durability get_durability_msg();
}