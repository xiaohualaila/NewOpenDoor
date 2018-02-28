package ug.newopendoor.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;


import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ug.newopendoor.R;
import ug.newopendoor.rx.RxBus;
import ug.newopendoor.usbtest.ComBean;
import ug.newopendoor.usbtest.ConstUtils;
import ug.newopendoor.usbtest.M1CardListener;
import ug.newopendoor.usbtest.M1CardModel;
import ug.newopendoor.usbtest.MDSEUtils;
import ug.newopendoor.usbtest.SPUtils;
import ug.newopendoor.usbtest.SectorDataBean;
import ug.newopendoor.usbtest.SerialHelper;
import ug.newopendoor.usbtest.UltralightCardListener;
import ug.newopendoor.usbtest.UltralightCardModel;
import ug.newopendoor.usbtest.Utils;
import ug.newopendoor.util.ByteUtil;
import ug.newopendoor.util.SharedPreferencesUtil;
import ug.newopendoor.util.Ticket;


public class Service2 extends Service implements UltralightCardListener, M1CardListener {

    private final int TIME = 500;
    //身份证
    private Thread thread;
    private boolean isAuto = true;
    private static Lock lock = new ReentrantLock();
    //UltralightCard读卡
    private UltralightCardModel model;
    //M1
    private M1CardModel model2;
    private boolean isHaveOne = false;
    private String ticketNum;
    private SPUtils settingSp;
    private String USB = "";

    private boolean uitralight = true;//设置为false m1读卡
    private boolean idcard = true;
    private boolean scan = true;
    //串口
    SerialControl ComA;
    DispQueueThread DispQueue;
    private String newPasswordKey;
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(getApplicationContext());
        settingSp = new SPUtils(getString(R.string.settingSp));
        USB = settingSp.getString(getString(R.string.usbKey), getString(R.string.androidUsb));
        rkGpioControlNative.init();
        onOpenConnectPort();
        uitralight = SharedPreferencesUtil.getBoolean(this,"uitralight", true);
        idcard = SharedPreferencesUtil.getBoolean(this,"scan", true);
        scan =  SharedPreferencesUtil.getBoolean(this,"idcard", true);
        if(scan){
            //串口
            ComA = new SerialControl();
            openErWeiMa();
            DispQueue = new DispQueueThread();
            DispQueue.start();
        }


        //身份证
        thread = new Thread(task);
        thread.start();

        if(uitralight){
            //UltralightCard
            model = new UltralightCardModel(this);
        }else {
            //M1
            model2 = new M1CardModel(this);
            //以下是后来添加读取M1秘钥部分代码
            newPasswordKey =  ByteUtil.convertStringToHex("123123");//设置秘钥12位  安卓是16进制 电脑是ascii码
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isAuto = false;
        onDisConnectPort();
        adcNative.close(0);
        adcNative.close(2);
        rkGpioControlNative.close();
        closeErWeiMa();
    }

    Runnable task = new Runnable() {
        @Override
        public void run() {
            while (isAuto) {
                lock.lock();
                try {
                    //UltralightCard
                    if (uitralight) {
                        model.bt_seek_card(ConstUtils.BT_SEEK_CARD);
                    //    Log.i("sss", ">>>>>>>>>>>>>>>>>>>>>>UltralightCard");
                        Thread.sleep(TIME);
                    } else {
                        //M1
                        model2.bt_download(ConstUtils.BT_DOWNLOAD,"All",0,newPasswordKey,0);
                        if (MDSEUtils.isSucceed(BasicOper.dc_card_hex(1))) {
                            final int keyType = 0;// 0 : 4; 密钥套号 0(0套A密钥)  4(0套B密钥)
                            isHaveOne = true;
                            model2.bt_read_card(ConstUtils.BT_READ_CARD, keyType, 0);
                        }
                     //   Log.i("sss", ">>>>>>>>>>>>>>>>>>>>>>M1");
                        Thread.sleep(TIME);
                    }

                    //身份证
                    if (idcard) {
                     //   Log.i("sss", ">>>>>>>>>>>>>>>>>>>>>>身份证");
                        com.decard.entitys.IDCard idCardData;
                        //标准协议
                        idCardData = BasicOper.dc_get_i_d_raw_info();
                        if (idCardData != null) {
                            RxBus.getDefault().post(idCardData);
                        }
                        Thread.sleep(TIME);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void getUltralightCardResult(String cmd, String result) {
        if (!result.equals("1003|无卡或无法寻到卡片")) {
            if (!result.equals("0001|操作失败")) {
                if (!result.equals("FFFF|操作失败")) {
                    if (!result.equals("1001|设备未打开")) {
                        Ticket ticket = new Ticket(1, result);
                        RxBus.getDefault().post(ticket);
                    }
                }
            }
        }
    }

    @Override
    public void getM1CardResult(String cmd, List<String> list, String result, String resultCode) {
        if (isHaveOne) {
            isHaveOne = false;
            if (list == null) {
                if (result.length() > 2) {
                    readSectorData(Integer.parseInt(resultCode));
                } else {
                    readSectorData(Integer.parseInt(result));
                }
            }
        }
    }

    private void readSectorData(int currentSectors) {
        boolean b = true;
        int piece = (currentSectors + 1) * 4;
        SectorDataBean sectorDataBean = new SectorDataBean();
        String[] pieceDatas = new String[4];
        for (int i = piece - 4, j = 0; i < piece; i++, j++) {
            String pieceData = MDSEUtils.returnResult(BasicOper.dc_read_hex(i));
            pieceDatas[j] = pieceData;
        }
        sectorDataBean.pieceZero = pieceDatas[0];

        if (b) {
            String string = sectorDataBean.pieceZero.substring(0, 24);
            String num =  ByteUtil. decode(string);
            Ticket ticket = new Ticket(4, num);
            RxBus.getDefault().post(ticket);
            b = false;
        }

    }

    //打开串口
    public void openErWeiMa() {
        ComA.setPort("/dev/ttyS4");
        ComA.setBaudRate("115200");
        OpenComPort(ComA);
    }

    private void OpenComPort(SerialHelper ComPort) {
        try {
            ComPort.open();
        } catch (SecurityException e) {
            Log.i("xxx", "SecurityException" + e.toString());
        } catch (IOException e) {
            Log.i("xxx", "IOException" + e.toString());
        } catch (InvalidParameterException e) {
            Log.i("xxx", "InvalidParameterException" + e.toString());
        }
    }

    public void closeErWeiMa() {
        CloseComPort(ComA);
    }

    private void CloseComPort(SerialHelper ComPort) {
        if (ComPort != null) {
            ComPort.stopSend();
            ComPort.close();
        }
    }

    //打开设备
    public void onOpenConnectPort() {
        BasicOper.dc_AUSB_ReqPermission(this);
        int portSate = BasicOper.dc_open(USB, this, "", 0);
        if (portSate >= 0) {
            BasicOper.dc_beep(5);
        }
    }

    //关闭设备
    public void onDisConnectPort() {
        BasicOper.dc_exit();
    }

    private class SerialControl extends SerialHelper {

        public SerialControl() {
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData) {
            DispQueue.AddQueue(ComRecData);
        }
    }

    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    try {
                        ticketNum = new String(ComData.bRec).trim();
                      //  Log.i("sss",">>>>>>>>>>>>>>>>"+ticketNum);
                        Ticket ticket = new Ticket(2, ticketNum);
                        RxBus.getDefault().post(ticket);
                        Thread.sleep(800);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }

}
