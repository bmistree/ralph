package ralph.MessageManager;

import ralph_protobuffs.InstallProto.Install;

public interface IInstallMessageListener
{
    public void recv_install_msg(String from_host_uuid, Install msg);
}