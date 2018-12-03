package com.morningdip.mjpegstreamer.upnp;

public class UpnpConstant {
    public static final String IGD = "urn:schemas-upnp-org:device:InternetGatewayDevice:1";

    public static String externalIpAddress = null;
    public static String internalIpAddress = null;


    public interface SERVICE_TYPE {
        public static final String WANIPConnection = "urn:schemas-upnp-org:service:WANIPConnection:1";
    }

    public interface ACTION {
        public static final String AddPortMapping = "AddPortMapping";
        public static final String DeletePortMapping = "DeletePortMapping";
        public static final String GetExternalIPAddress = "GetExternalIPAddress";
        public static final String GetGenericPortMappingEntry = "GetGenericPortMappingEntry";

    }

    public static final String NewExternalIPAddress = "NewExternalIPAddress";

    public interface MSG {
        public static final int find_fail = -1;
        public static final int find_ok = 0;
        public static final int find_start = 2;
        public static final int find_end = 1;

        public static final int ip_done = 5;

        public static final int add_port_ok = 3;
        public static final int add_port_fail = 4;
    }
}
