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

/**
 * Created by Administrator on 2017/12/13.
 */

public class CommonService extends Service implements UltralightCardListener,M1CardListener {
    private int flag = 3;
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

    private boolean uitralight = true;
    private boolean idcard = false;
    private boolean three = false;

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
        //UltralightCard
        model = new UltralightCardModel(this);
        //M1
        model2 = new M1CardModel(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isAuto =false;
        Log.i("sss","service onDestroy");
    }

    Runnable task = new Runnable() {
        @Override
        public void run() {
                while (isAuto) {
                    lock.lock();
                    try {
                        if(flag == 1){//UltralightCard
                                if(uitralight){
                                    model.bt_seek_card(ConstUtils.BT_SEEK_CARD);
                                    Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>UltralightCard");
                                    Thread.sleep(TIME);
                                }else {//M1
                                    if (MDSEUtils.isSucceed(BasicOper.dc_card_hex(1))) {
                                        final int keyType = 0;// 0 : 4; 密钥套号 0(0套A密钥)  4(0套B密钥)
                                        isHaveOne = true;
                                        model2.bt_read_card(ConstUtils.BT_READ_CARD,keyType,0);
                                    }
                                    Thread.sleep(TIME);
                                }
                                if(idcard){
                                    flag = 2;
                                }
                        }else if(flag == 2){//身份证
                            if(idcard){
                                Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>身份证");
                                com.decard.entitys.IDCard idCardData;
                                if (!choose) {
                                    //标准协议
                                    idCardData = BasicOper.dc_get_i_d_raw_info();
                                } else {
                                    //公安部协议
                                    idCardData = BasicOper.dc_SamAReadCardInfo(1);
                                }
                                if(idCardData!= null){
                                    onDataListener.onIDCardMsg(idCardData);
                                }
                                Thread.sleep(TIME);
                            }
                            if(three){
                                flag = 1;
                            }
                        }else if(flag == 3){
                            if(three){
                                flag = 1;
                            }else {
                                flag = 2;
                            }
                            Thread.sleep(4000);
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
        if(!result.equals("1003|无卡或无法寻到卡片")){
            if(!result.equals("0001|操作失败")){
                if(!result.equals("FFFF|操作失败")){
                    if(!result.equals("1001|设备未打开")){
                        Log.i("sss",">>>>result"+result);
                        onDataListener.onBackMsg(1,result);
                    }
                }
            }
        }
    }

    @Override
    public void getM1CardResult(String cmd, List<String> list, String result, String resultCode) {
        if(isHaveOne){
            isHaveOne = false;
            if (list == null){
                if (result.length() > 2){
                    readSectorData(Integer.parseInt(resultCode));
                }else {
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

        if (b){
            String string = sectorDataBean.pieceZero.substring(0,8);
            onDataListener.onBackMsg(4,string);
            b = false;
        }

    }


    public class MyBinder extends Binder {
        public CommonService getService(){
            return CommonService.this;
        }

        public void setIntentData(boolean three,boolean uitralight, boolean idcard) {
            CommonService.this.three = three;
            CommonService.this.uitralight = uitralight;
            CommonService.this.idcard = idcard;
        }

        public void stopThread() {
            thread.interrupt();
        }
    }

    public interface OnDataListener {
        void onIDCardMsg(com.decard.entitys.IDCard data);
        void onBackMsg(int type,String result);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }
}
