package RalphExtended;

import RalphVersions.IVersionListener;
import RalphServiceActions.ServiceAction;

import ralph.SpeculativeFuture;
import ralph.CommitMetadata;

public class WrapApplyToHardware<T> extends ServiceAction
{
    private final T to_apply;
    private final boolean undo_changes;
    private final ExtendedObjectStateController state_controller;
    /**
       Note: using constructor of SpeculativeFuture here out of
       convenience: it's the only class that implements
       ICancellableFuture, and don't want to create a dummy
       implementation.  That said, it's very important that only stick
       to ICancellableFuture interface, as will easily get things like
       NullPointerExceptions if try to operate on its event.
     */
    public final SpeculativeFuture to_notify_when_complete =
        new SpeculativeFuture(null,false);
    private final IHardwareChangeApplier<T> hardware_change_applier;

    /**
       May be null.  Ie., if do not want to track version history of
       object.
     */
    private final IVersionListener<T> version_listener;
    private final CommitMetadata commit_metadata;
    
    public WrapApplyToHardware(
        T _to_apply, boolean _undo_changes,
        ExtendedObjectStateController _state_controller,
        IHardwareChangeApplier<T> _applier,
        IVersionListener<T> _version_listener,CommitMetadata _commit_metadata)
    {
        to_apply = _to_apply;
        undo_changes = _undo_changes;
        state_controller = _state_controller;
        hardware_change_applier = _applier;
        version_listener = _version_listener;
        commit_metadata = _commit_metadata;
    }
    
    @Override
    public void run()
    {
        boolean application_successful = false;
        if (undo_changes)
            application_successful = hardware_change_applier.undo(to_apply);
        else
            application_successful = hardware_change_applier.apply(to_apply);

        state_controller.get_state_hold_lock();
        if (! application_successful)
            state_controller.move_state_failed();
        else if (undo_changes) // undo succeeded
            state_controller.move_state_clean();
        else // apply succeeded
            state_controller.move_state_staged_changes();
        state_controller.release_lock();

        // notify version listener.
        if (version_listener != null)
        {
            if (undo_changes)
                version_listener.backout_delta(commit_metadata);
            else
                version_listener.complete_delta(commit_metadata);
        }

        
        if (application_successful)
            to_notify_when_complete.succeeded();
        else
            to_notify_when_complete.failed();
    }
}