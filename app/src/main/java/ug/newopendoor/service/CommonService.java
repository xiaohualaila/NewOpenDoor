package ug.newopendoor.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.decard.NDKMethod.BasicOper;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ug.newopendoor.usbtest.ConstUtils;
import ug.newopendoor.usbtest.M1CardListener;
import ug.newopendoor.usbtest.M1CardModel;
import ug.newopendoor.usbtest.MDSEUtils;
import ug.newopendoor.usbtest.SectorDataBean;
import ug.newopendoor.usbtest.UltralightCardListener;
import ug.newopendoor.usbtest.UltralightCardModel;
import ug.newopendoor.util.ByteUtil;
import ug.newopendoor.util.SharedPreferencesUtil;

/**
 * Created by Administrator on 2017/12/13.
 */

public class CommonService extends Service implements UltralightCardListener, M1CardListener {
    private final int TIME = 1000;

    //身份证
    private Thread thread;
    private boolean isAuto = true;
    private boolean choose = false;//false标准协议,true公安部协议
    private static Lock lock = new ReentrantLock();
    //UltralightCard读卡
    private UltralightCardModel model;

    //M1
    private M1CardModel model2;
    private boolean isHaveOne = false;

    public boolean uitralight = true;
    public boolean idcard = false;
    private String newPasswordKey;

    private OnDataListener onDataListener;

    public void setOnProgressListener(OnDataListener onProgressListener) {
        this.onDataListener = onProgressListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //身份证
        thread = new Thread(task);
        thread.start();

        if (uitralight) {
            //UltralightCard
            model = new UltralightCardModel(this);
        } else {
            //M1
            String secret = SharedPreferencesUtil.getStringByKey("secret", this);
            Log.i("sss", "secret>> " + secret);
            model2 = new M1CardModel(this);
            //以下是后来添加读取M1秘钥部分代码
            newPasswordKey = ByteUtil.convertStringToHex(secret);//设置秘钥12位  安卓是16进制 电脑是ascii码
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isAuto = false;
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
                        Thread.sleep(TIME);
                    } else {
                        //M1
                        model2.bt_download(ConstUtils.BT_DOWNLOAD, "All", 0, newPasswordKey, 0);
                        if (MDSEUtils.isSucceed(BasicOper.dc_card_hex(1))) {
                            final int keyType = 0;// 0 : 4; 密钥套号 0(0套A密钥)  4(0套B密钥)
                            isHaveOne = true;
                            model2.bt_read_card(ConstUtils.BT_READ_CARD, keyType, 5);
                        }
                        Log.i("sss", ">>>>>>>>>>>>>>>>>>>>>>M1");
                        Thread.sleep(TIME);
                    }
                    //身份证
                    if (idcard) {
                        Log.i("sss", ">>>>>>>>>>>>>>>>>>>>>>身份证");
                        com.decard.entitys.IDCard idCardData;
                        if (!choose) {
                            //标准协议
                            idCardData = BasicOper.dc_get_i_d_raw_info();
                        } else {
                            //公安部协议
                            idCardData = BasicOper.dc_SamAReadCardInfo(1);
                        }
                        if (idCardData != null) {
                            onDataListener.onIDCardMsg(idCardData);
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

    @Override
    public void getUltralightCardResult(String cmd, String result) {
        if (!result.equals("1003|无卡或无法寻到卡片")) {
            if (!result.equals("0001|操作失败")) {
                if (!result.equals("FFFF|操作失败")) {
                    if (!result.equals("1001|设备未打开")) {
                        onDataListener.onBackMsg(1, result);
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
            String string = sectorDataBean.pieceZero.substring(0, 24);//
            String num = ByteUtil.decode(string);
            onDataListener.onBackMsg(4, num);
            b = false;
        }
    }


    public class MyBinder extends Binder {
        public CommonService getService() {
            return CommonService.this;
        }

        public void setIntentData(boolean uitralight, boolean idcard) {
            CommonService.this.uitralight = uitralight;
            CommonService.this.idcard = idcard;
        }

        public void stopThread() {
            thread.interrupt();
        }
    }

    public interface OnDataListener {
        void onIDCardMsg(com.decard.entitys.IDCard data);

        void onBackMsg(int type, String result);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }
}
