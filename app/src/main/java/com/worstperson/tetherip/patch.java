package com.worstperson.tetherip;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedHelpers;

public class patch implements IXposedHookLoadPackage {

    private String getAddress(int interfaceType) {
        final int TETHERING_WIFI      = 0;
        final int TETHERING_USB       = 1;
        final int TETHERING_BLUETOOTH = 2;
        final int TETHERING_WIFI_P2P = 3;
        final int TETHERING_NCM = 4;
        final int TETHERING_ETHERNET = 5;
        final int TETHERING_WIGIG = 6;

        switch (interfaceType) {
            case TETHERING_WIFI:
                return "192.168.43.1/24";
            case TETHERING_USB:
                return "192.168.42.1/24";
            case TETHERING_BLUETOOTH:
                return "192.168.44.1/24";
            case TETHERING_WIFI_P2P:
                return "192.168.49.1/24";
            case TETHERING_NCM:
                return "192.168.42.1/24";
            case TETHERING_ETHERNET:
                return "192.168.42.1/24";
            case TETHERING_WIGIG:
                return "192.168.43.1/24";
        }
        return null;
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.networkstack.tethering"))
            return;

        final Class<?> LinkAddress = XposedHelpers.findClass("android.net.LinkAddress", lpparam.classLoader);
        final Class<?> IpServer = XposedHelpers.findClass("android.net.ip.IpServer", lpparam.classLoader);

        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU){
            // Android 12-13
            XposedHelpers.findAndHookMethod("com.android.networkstack.tethering.PrivateAddressCoordinator", lpparam.classLoader, "requestDownstreamAddress", IpServer, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int interfaceType = (Integer) XposedHelpers.callMethod(param.args[0],"interfaceType");
                    String overrideAddress = getAddress(interfaceType);
                    if (overrideAddress != null) {
                        param.setResult(XposedHelpers.newInstance(LinkAddress, overrideAddress));
                    }
                }
            });
        } else {
            // Android 14-15+
            XposedHelpers.findAndHookMethod("com.android.networkstack.tethering.PrivateAddressCoordinator", lpparam.classLoader, "requestDownstreamAddress", IpServer, int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int interfaceType = (Integer) XposedHelpers.callMethod(param.args[0],"interfaceType");
                    String overrideAddress = getAddress(interfaceType);
                    if (overrideAddress != null) {
                        param.setResult(XposedHelpers.newInstance(LinkAddress, overrideAddress));
                    }
                }
            });
        }
    }
}