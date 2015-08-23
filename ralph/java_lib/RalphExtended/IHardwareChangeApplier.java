package RalphExtended;

public interface IHardwareChangeApplier<T>
{
    public boolean apply(T to_apply);
    public boolean undo(T to_undo);

    // Try to back out a failed hardware change. Argument
    // is fully apply instructions. If do not support partial
    // rollback, just return false.
    public boolean partial_undo(T what_had_tried_to_apply);
}