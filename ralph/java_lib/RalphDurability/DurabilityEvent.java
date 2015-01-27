package RalphDurability;

import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta;

public class DurabilityEvent
{
    public enum DurabilityEventType
    {
        // message had a pair that completed.
        COMPLETED,
        // unknown whether message completed.
        OUTSTANDING,
        // message is a service factory
        SERVICE_FACTORY
    }

    
    final public DurabilityEventType event_type;
    final public DurabilityPrepare prepare_msg;
    final public ServiceFactoryDelta service_factory_msg;
    
    public DurabilityEvent(
        DurabilityEventType event_type, DurabilityPrepare prepare_msg)
    {
        this.event_type = event_type;
        this.prepare_msg = prepare_msg;
        this.service_factory_msg = null;
    }

    public DurabilityEvent(ServiceFactoryDelta service_factory_msg)
    {
        this.event_type = DurabilityEventType.SERVICE_FACTORY;
        this.prepare_msg = null;
        this.service_factory_msg = service_factory_msg;
    }
}