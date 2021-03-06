package com.larson.remotedisplay.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author omerjerk
 * @date 14/8/14
 */
public class Utils {

    private static final int IP_MASK = 4;

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) {
                        continue;
                    }
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) {
                    return "";
                }
                StringBuilder buf = new StringBuilder();
                for (int idx=0; idx<mac.length; idx++) {
                    buf.append(String.format("%02X:", mac[idx]));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length()-1);
                }
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addressList = Collections.list(intf.getInetAddresses());
                for (InetAddress address : addressList) {
                    if (!address.isLoopbackAddress()) {
                        String sAddr = address.getHostAddress().toUpperCase();
                        boolean isIPv4 = IsIpv4(sAddr);
                        if (useIPv4) {
                            if (isIPv4) {
                                return sAddr;
                            }
                        } else {
                            if (!isIPv4) {
                                // drop ip6 port suffix
                                int delim = sAddr.indexOf('%');
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    /**
     * ???byte??????????????????bit
     */
    public static String byteToBit(byte bytes[]) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append("" + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
                    + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
                    + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                    + (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1));
        }
        return stringBuilder.toString();
    }

    /**
     * ?????????????????????IPV4??????
     */
    public static boolean IsIpv4(String ipv4) {
        if (ipv4 == null || ipv4.length() == 0) {
            return false;
        }
        //??????java doc???????????????, split????????????reg, ??????????????????, ?????????"|"??????, ????????????"\\|"
        String[] parts = ipv4.split("\\.");

        if (parts.length != IP_MASK) {
            //?????????????????????????????????4?????????
            return false;
        }
        for (String part : parts) {
            try {
                int n = Integer.parseInt(part);
                if (n < 0 || n > 255) {
                    //???????????????????????????
                    return false;
                }
            } catch (NumberFormatException e) {
                //?????????????????????
                return false;
            }
        }
        return true;
    }

}
