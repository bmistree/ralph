package RalphDurability;

public class DurabilityPair
{
    public enum DurabilityType
    {
        // message had a pair that completed.
        COMPLETED,
        // unknown whether message completed.
        OUTSTANDING;
    }
}