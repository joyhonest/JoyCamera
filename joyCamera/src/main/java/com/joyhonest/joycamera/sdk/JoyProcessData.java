package com.joyhonest.joycamera.sdk;

import org.simple.eventbus.EventBus;

import java.util.ArrayList;

class JoyProcessData {

    public static boolean PressData(byte[] data) {
        int nStartInx, nEndInx, inx, nLen;
        if (GP4225_Device.MacAddress == null) {
            GP4225_Device.MacAddress = new byte[6];
            for (int i = 0; i < 6; i++) {
                GP4225_Device.MacAddress[0] = 0;
            }
        }

        if (data == null)
            return false;
        if (data.length <= 10)
            return false;

        if ((data[0] & 0xFF) != 'F' ||
                (data[1] & 0xFF) != 'D' ||
                (data[2] & 0xFF) != 'W' ||
                (data[3] & 0xFF) != 'N') {
            return false;
        }

        String sFileName;
        byte nStatus;

        int m_cmd = (data[4] & 0xFF) + (data[5] & 0xFF) * 0x100;
        int s_cmd = (data[6] & 0xFF) + (data[7] & 0xFF) * 0x100;
        int n_len = (data[8] & 0xFF) + (data[9] & 0xFF) * 0x100;
        if (n_len == 0)
            return false;
        if (n_len + 10 > data.length)
            return false;

        if (m_cmd == 0x0000 && s_cmd == 0x0001) {   //Device Status
            GP4225_Device.nMode = data[10] & 0xFF;
            GP4225_Device.bSD = ((data[11] & 0x01) == 0); // 0 have SD  1 NoSD
            GP4225_Device.bSDRecording = ((data[11] & 0x02) != 0);

            GP4225_Device.VideosCount = ((data[12] & 0xFF) | (data[13] & 0xFF) * 0x100 | (data[14] & 0xFF) * 0x10000 | (data[15] & 0xFF) * 0x1000000);
            GP4225_Device.LockedCount = ((data[16] & 0xFF) | (data[17] & 0xFF) * 0x100 | (data[18] & 0xFF) * 0x10000 | (data[19] & 0xFF) * 0x1000000);
            GP4225_Device.PhotoCount = ((data[20] & 0xFF) | (data[21] & 0xFF) * 0x100 | (data[22] & 0xFF) * 0x10000 | (data[23] & 0xFF) * 0x1000000);

            if (n_len >= 0x1A) {
                GP4225_Device.nSDAllSize = ((data[24] & 0xFF) | (data[25] & 0xFF) * 0x100 | (data[26] & 0xFF) * 0x10000 | (data[27] & 0xFF) * 0x1000000L | (data[34] & 0xFF) * 0x100000000L);
                GP4225_Device.nSDAvaildSize = ((data[28] & 0xFF) | (data[29] & 0xFF) * 0x100 | (data[30] & 0xFF) * 0x10000 | (data[31] & 0xFF) * 0x1000000L | (data[35] & 0xFF) * 0x100000000L);
            }
            if (data.length >= 34) {
                GP4225_Device.nBattery = data[32] & 0xFF;
                GP4225_Device.bAdjfocus = (data[33] != 0);

                if (data.length >= 35) {
                    GP4225_Device.nFuncMask = data[34] & 0xFF;
                }
                if (data.length >= 40) {
                    GP4225_Device.nSDRecordTime = (data[36] & 0xFF) | (data[37] & 0xFF) * 0x100 | (data[38] & 0xFF) * 0x10000 | (data[39] & 0xFF) * 0x1000000;
                } else {
                    GP4225_Device.nSDRecordTime = 0;
                }
                if (data.length >= 48) {
                    System.arraycopy(data, 40, GP4225_Device.MacAddress, 0, 6);
                    GP4225_Device.CustermerType =   data[46];
                    GP4225_Device.DeviceType =   data[47];
                }
            } else {
                GP4225_Device.nFuncMask = 0;
                GP4225_Device.nBattery = 4;
                GP4225_Device.bAdjfocus = false;
                GP4225_Device.nSDRecordTime = 0;
            }

            Integer nb = GP4225_Device.nBattery;
            EventBus.getDefault().post(nb, "onGetBattery");
            EventBus.getDefault().post("", "GP4225_GetStatus");
            return true;
        }
        if(m_cmd == 0xFFFF)
        {
            byte[] buffer = new byte[n_len+6];
            System.arraycopy(data,4,buffer,0,n_len+6);
            EventBus.getDefault().post(buffer,"onGetCustomData");
            return true;
        }

        if (m_cmd == 0x0002)  //GetFileList
        {
            if (s_cmd == 0x0001 || s_cmd == 0x0002 || s_cmd == 0x0003) {  //VideoList   LockFileList  //图片文件
                nStartInx = (data[10] & 0xFF) | (data[11] & 0xFF) * 0x100;
                nEndInx = (data[12] & 0xFF) | (data[13] & 0xFF) * 0x100;
                JoyGetFiles FF = new JoyGetFiles();
                FF.files = new ArrayList<>();
                for (int ii = 0; ii <= nEndInx - nStartInx; ii++) {
                    inx = 14 + 32 + (ii * 68);
                    nLen = (data[inx] & 0xFF) | (data[inx + 1] & 0xFF) * 0x100 | (data[inx + 2] & 0xFF) * 0x10000 | (data[inx + 3] & 0xFF) * 0x1000000;
                    inx += 4;
                    int da = 0;
                    for (int xx = 0; xx < 32; xx++) {
                        if (data[inx + xx] != 0) {
                            da++;
                        }
                    }
                    sFileName = "";
                    if (da != 0) {
                        byte[] bytes = new byte[da];
                        System.arraycopy(data, inx, bytes, 0, da);
                        sFileName = new String(bytes, 0, da);
                    }
                    JoyFile file = new JoyFile("", sFileName, nLen);
                    FF.files.add(file);
                }
                EventBus.getDefault().post(FF, "onGetSdFliesList");
            }
            return true;
        }

        if (m_cmd == 0x0009)  //Delete File
        {
            nStatus = data[10];
            sFileName = "";
            if (n_len > 64) {
                nLen = 0;
                for (int xx = 0; xx < 32; xx++) {
                    if (data[xx + 11 + 32] == 0) {
                        break;
                    } else {
                        nLen++;
                    }
                }
                if (nLen != 0)
                    sFileName = new String(data, 11 + 32, nLen);
            }

            switch (s_cmd) {
                case 0x00001:          //delete a file
                    JoyFile file = new JoyFile("", sFileName,  nStatus);
                    EventBus.getDefault().post(file, "GP4225_DeleteFile");
                    break;
                case 0x0002:             //delete all file
                    Integer i = (int) nStatus;
                    EventBus.getDefault().post(i, "GP4225_DeleteAllFile");
                    break;
                default:
                    break;
            }
            return true;
        }
        if (m_cmd == 0x0021) {
            if (s_cmd == 0x0001) {   //透传数据
                byte[] buffer = new byte[n_len];
                System.arraycopy(data, 10, buffer, 0, n_len);
                EventBus.getDefault().post(buffer, "GP4225_Get_Transfer_data");
                return true;
            }
            return false;
        }
        if (m_cmd == 0x0020) {
            boolean bOK = true;
            switch (s_cmd) {
                case 0x0001: //时间
                {
                    byte[] buffer = new byte[n_len];
                    System.arraycopy(data, 10, buffer, 0, n_len);
                    EventBus.getDefault().post(buffer, "GP4225_GetDeviceDateTime");
                }

                break;
                case 0x0002: //水印开关
                {
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_GetDeviceOsdStatus");
                }

                break;
                case 0x0003:  //图像翻转
                {
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_GetDeviceReversaltatus");
                }

                break;
                case 0x0004: //录像分段时间
                {
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_GetDeviceRecordTime");
                }
                break;
                case 0x0005:  //返回SSID
                {
                    byte[] da = new byte[n_len];
                  //  da[n_len] = 0;
                    System.arraycopy(data, 10, da, 0, n_len);
                    try {
                        String str = new String(da);
                        EventBus.getDefault().post(str, "onGetWiFiSSID");
                    } catch (Exception ignored) {

                    }
                }
                    break;
                case 0x0006:  //wifi password
                {
                    byte[] da = new byte[n_len];
                //    da[n_len] = 0;
                    System.arraycopy(data, 10, da, 0, n_len);
                    try {
                        String str = new String(da);
                        EventBus.getDefault().post(str, "onGetWiFiPassword");
                    } catch (Exception ignored) {

                    }
                }
                    break;
                case 0x0007: //WifiChannel
                {
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_WifiChannel");
                }
                break;
                case 0x0008:  //format SD卡
                {
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_FormatSD_Status");
                }
                break;
                case 0x0009:  //Ver
                {
                    byte[] buffer = new byte[n_len];
                    System.arraycopy(data, 10, buffer, 0, n_len);
                    GP4225_Device.sVer = new String(buffer);

                    EventBus.getDefault().post(GP4225_Device.sVer, "GP4225_GetFireWareVersion");
                    EventBus.getDefault().post(GP4225_Device.sVer, "onGetFirmwareVersion");
                }
                break;
                case 0x000A: {
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_Reset_Status");
                }
                break;
                case 0x000B: {
                    int val = (data[10] & 0xFF) + (data[11] & 0xFF) * 0x100;
                    val >>= 4;
                    val &= 0x3FF;
                    Integer aa = val;
                    EventBus.getDefault().post(aa, "GP4225_34_GetAdjFocus");
                }
                break;
                case 0x000C: {
                    //int val = (data[10] & 0xFF) + (data[11] & 0xFF) * 0x100;
                    Integer aa = ((data[10] & 0xFF) + (data[11] & 0xFF) * 0x100);
                    EventBus.getDefault().post(aa, "GP4225_GetVcm");
                }
                break;
                case 0x000E:  //GP4225_GetLed
                {
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "onGetLed");
                    EventBus.getDefault().post(aa, "GP4225_GetLed");
                }
                break;
                case 0x0010: //GP4225_GetResolution
                {
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_GetResolution");
                }
                break;
//                case 0x0012:    //G-Sensor 数据
//                {
//                     在native 里面处理
//                }
//                break;

                case 0x0013:    //AC检测 数据
                {
                    //byte a = data[10];
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_GetAC_Data");
                }
                break;
                case 0x0015: {
                    //byte a = data[10];
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_GetIR_Status");
                }
                break;
                case 0x0016: {
                    //byte a = data[10];
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_GetPIR_Status");
                }
                break;
                case 0x0017: {
                    //byte a = data[10];
                    Integer aa = (int) (data[10]);
                    EventBus.getDefault().post(aa, "GP4225_GetLed_Status");
                }
                break;
                case 0x0018:   //WIFI 板主动发送按键指令
                {
                    if (n_len == 4) {
                        byte[] da = new byte[4];
                        System.arraycopy(data, 10, da, 0, 4);
                        EventBus.getDefault().post(da, "GP4225_GetKey");
                        EventBus.getDefault().post(da, "onGetKey_new");
                    } else {
                        bOK = false;
                    }
                }
                    break;
                case 0x0019: //雷达数据发送(设备发起或应答)
                {
                    if (n_len != 0x000D) {
                        bOK = false;
                        break;
                    } else {
                        byte[] da = new byte[0x0D];
                        System.arraycopy(data, 10, da, 0, 0x0D);
                        EventBus.getDefault().post(da, "GP4225_GetRadarData");
                    }
                }
                    break;
                case 0x0020: {
                    //byte a = data[11];
                    Integer aa = (int) (data[11]);
                    EventBus.getDefault().post(aa, "onGetLedMode");
                }
                break;

                case 0x0021:  //获取当前变焦参数
                {
                    byte[] da = new byte[0x04];
                    System.arraycopy(data, 10, da, 0, 0x04);
                    EventBus.getDefault().post(da, "onGetZoom_res");
                }
                break;
                case 0x0022:  //设置当前变焦参数 的返回，一般这个结果可以不用理会，使用 0x21 即可读取
                {
                    byte[] da = new byte[0x04];
                    System.arraycopy(data, 10, da, 0, 0x04);
                    EventBus.getDefault().post(da, "onSetZoom_res");
                }
                break;
                case 0x0028:   //摄像头温度报警，固件每2sec回传一次
                {
                    byte[] da = new byte[0x08];
                    System.arraycopy(data, 10, da, 0, 0x08);
                    EventBus.getDefault().post(da, "onSensorWarn_res");
                }
                    break;


                case 0x0050: {
                    byte[] aa;
                    aa = new byte[n_len];
                    System.arraycopy(data, 10, aa, 0, n_len);
                    EventBus.getDefault().post(aa, "GP4225_GetDeviceInfo");
                }
                break;
                default:
                    bOK = false;
                    break;
            }
            return bOK;
        }
        return false;
    }
}
