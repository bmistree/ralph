package RalphDurability;

public interface IDurabilitySaver
{
    /**
       Blocks until prepare and complete have been appended to log.
     */
    public void prepare_operation(DurabilityContext dc);
    public void complete_operation(DurabilityContext dc, boolean succeeded);
}