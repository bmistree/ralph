package RalphExtended;

public interface IHardwareChangeApplier<T>
{
    public boolean apply(T to_apply);
    public boolean undo(T to_undo);
}