package com.yaleed.vpnresearch.shizuku;

interface IShizukuService {
    int uid();
    String run(in String[] cmd);
    String readAndroidId();
    int writeAndroidId(String value);
}