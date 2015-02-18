package RalphDurability;

public interface IDurabilitySaverFactory
{
    public IDurabilitySaver construct(String filename);
}