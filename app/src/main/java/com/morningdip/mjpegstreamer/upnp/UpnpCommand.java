package com.morningdip.mjpegstreamer.upnp;

import com.morningdip.mjpegstreamer.utils.MappingEntity;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.Device;

public class UpnpCommand {
    public static String GetExternalIPAddress(Device device) {
        String externalIp = "";
        org.cybergarage.upnp.Service wanIPConnectionSer = device.getService(UpnpConstant.SERVICE_TYPE.WANIPConnection);

        if (wanIPConnectionSer != null) {
            Action action = wanIPConnectionSer.getAction(UpnpConstant.ACTION.GetExternalIPAddress);
            if (action != null) {
                if (action.postControlAction()) {
                    ArgumentList out = action.getOutputArgumentList();
                    if (out != null) {
                        Argument argument = out.getArgument(UpnpConstant.NewExternalIPAddress);
                        if (argument != null) {
                            return argument.getValue();
                        }
                    }
                }
            } else {

            }
        }
        return externalIp;
    }

    public static MappingEntity GetGenericPortMappingEntry(Device device, int NewPortMappingIndex) {
        MappingEntity entity = null;
        org.cybergarage.upnp.Service wanIPConnectionSer = device.getService(UpnpConstant.SERVICE_TYPE.WANIPConnection);

        if (wanIPConnectionSer != null) {
            Action portMappingAction = wanIPConnectionSer.getAction(UpnpConstant.ACTION.GetGenericPortMappingEntry);
            if (portMappingAction != null) {
                ArgumentList argumentList = portMappingAction.getArgumentList();
                argumentList.getArgument("NewPortMappingIndex").setValue(Integer.toString(NewPortMappingIndex));
                if (portMappingAction.postControlAction()) {
                    ArgumentList out = portMappingAction.getOutputArgumentList();
                    if (out != null) {
                        entity = new MappingEntity();
                        for (int i = 0; i < out.size(); i++) {
                            String key = ((Argument) out.get(i)).getName();
                            String value = ((Argument) out.get(i)).getValue();
                            if (key.equals("NewRemoteHost"))
                                entity.NewRemoteHost = value;
                            if (key.equals("NewExternalPort"))
                                entity.NewExternalPort = value;
                            if (key.equals("NewProtocol"))
                                entity.NewProtocol = value;
                            if (key.equals("NewInternalPort"))
                                entity.NewInternalPort = value;
                            if (key.equals("NewInternalClient"))
                                entity.NewInternalClient = value;
                            if (key.equals("NewEnabled"))
                                entity.NewEnabled = value;
                            if (key.equals("NewPortMappingDescription"))
                                entity.NewPortMappingDescription = value;
                            if (key.equals("NewLeaseDuration"))
                                entity.NewLeaseDuration = value;
                        }
                    }
                }
            }
        }
        return entity;
    }

    public static boolean addPortMapping(Device dev, String port, String ip) {
        boolean success = false;
        if (dev != null) {
            org.cybergarage.upnp.Service wanIPConnectionSer = dev.getService(UpnpConstant.SERVICE_TYPE.WANIPConnection);

            if (wanIPConnectionSer != null) {
                Action addPortMappingAction = wanIPConnectionSer.getAction(UpnpConstant.ACTION.AddPortMapping);

                if (addPortMappingAction != null) {
                    ArgumentList argumentList = addPortMappingAction.getArgumentList();
                    argumentList.getArgument("NewRemoteHost").setValue("");
                    argumentList.getArgument("NewExternalPort").setValue(port);
                    argumentList.getArgument("NewProtocol").setValue("TCP");
                    argumentList.getArgument("NewInternalPort").setValue(port);
                    argumentList.getArgument("NewInternalClient").setValue(ip);
                    argumentList.getArgument("NewEnabled").setValue("1");
                    argumentList.getArgument("NewPortMappingDescription").setValue("miniupnpd");
                    argumentList.getArgument("NewLeaseDuration").setValue("0");

                    if (addPortMappingAction.postControlAction()) {
                        success = true;
                    }
                }
            }
        }
        return success;
    }

    public static boolean DeletePortMapping(Device dev, String external_port) {
        boolean success = false;
        if (dev != null) {
            org.cybergarage.upnp.Service wanIPConnectionSer = dev.getService(UpnpConstant.SERVICE_TYPE.WANIPConnection);
            if (wanIPConnectionSer != null) {
                Action delPortMappingAction = wanIPConnectionSer.getAction(UpnpConstant.ACTION.DeletePortMapping);
                if (delPortMappingAction != null) {
                    ArgumentList argumentList = delPortMappingAction.getArgumentList();
                    argumentList.getArgument("NewExternalPort").setValue(external_port);
                    argumentList.getArgument("NewProtocol").setValue("TCP");
                    argumentList.getArgument("NewRemoteHost").setValue("");
                    if (delPortMappingAction.postControlAction()) {
                        success = true;
                    }
                }
            }
        }
        return success;
    }
}
