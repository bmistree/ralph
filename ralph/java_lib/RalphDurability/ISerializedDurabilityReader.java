package RalphDurability;

public interface ISerializedDurabilityReader
{
    public DurabilityPair next_durability_message();
}