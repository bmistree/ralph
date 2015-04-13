package ralph.Install;

import ralph.InternalServiceReference;
import ralph.SettableFuture;

import ralph_protobuffs.InstallProto.Install.Reply;
import ralph_protobuffs.DeltaProto.Delta.ServiceReferenceDelta;

public class InstallSettableFuture
    extends SettableFuture<InternalServiceReference>
{
    public InstallSettableFuture()
    {}

    /**
       Set internal value for any waiting calls on futures.
     */
    public void handle_install_reply(Reply install_reply)
    {
        ServiceReferenceDelta service_reference_delta =
            install_reply.getServiceReference();
        InternalServiceReference deserialized_service_reference =
            InternalServiceReference.deserialize_delta(service_reference_delta);
        set(deserialized_service_reference);
    }
}
