package ug.newopendoor.usbtest;

import android.util.Log;

import com.decard.NDKMethod.BasicOper;

import java.util.ArrayList;
import java.util.List;

import ug.newopendoor.rx.RxBus;
import ug.newopendoor.util.ByteUtil;
import ug.newopendoor.util.Ticket;

/**
 * Created by hizha on 2017/7/31.
 */

public class M1CardModel {
    private M1CardListener mListener;

    public M1CardModel(M1CardListener listener){
        mListener = listener;
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
            b = false;
        }

    }
    public String bt_read_ticket(String newPasswordKey)
    {
        int keyType=0;
        int spinnerPosition=4;
       // int spinnerPosition=0;
       // newPasswordKey="FFFFFFFFFFFF";
        String[] pieceDatas = new String[4];
        String isSucceed;
        int i=0;
        int j=0;

        isSucceed=BasicOper.dc_card_hex(0);
        if (!MDSEUtils.isSucceed(isSucceed))
        {
            return isSucceed;

        }
        isSucceed=BasicOper.dc_load_key_hex(keyType, spinnerPosition , newPasswordKey);
        if (!MDSEUtils.isSucceed(isSucceed))
        {
            return isSucceed;
        }
        isSucceed=BasicOper.dc_authentication(keyType, spinnerPosition);
        if (!MDSEUtils.isSucceed(isSucceed))
        {
            return isSucceed;
        }
        //for ( i = 0; i < 3; i++) {

        isSucceed = BasicOper.dc_read_hex(spinnerPosition*4+0);
        BasicOper.dc_halt();
        return isSucceed;
    }


    public void bt_download(String btDownload, String sectorsNum, int keyType, String newPasswordKey, int spinnerPosition) {
        if ("All".equals(sectorsNum)) {
            List<String> loadKeyList = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                if (!MDSEUtils.isSucceed(BasicOper.dc_load_key_hex(keyType, i, newPasswordKey)))
                    loadKeyList.add(Integer.toString(i));
            }
            mListener.getM1CardResult(ConstUtils.BT_DOWNLOAD,loadKeyList,"","");

        } else {
            String result = BasicOper.dc_load_key_hex(keyType, spinnerPosition - 1, newPasswordKey);
            mListener.getM1CardResult(ConstUtils.BT_DOWNLOAD,null,result,"");
        }
    }

    public void bt_read_card(String btReadCard, int keyType, int firstVisibleItemPosition) {
        int currentSectors = firstVisibleItemPosition - 1;

        List<String> authFailList = new ArrayList<>();
        if (firstVisibleItemPosition == 0) {
            for (int i = 0; i < 16; i++) {
                if (MDSEUtils.isSucceed(BasicOper.dc_authentication(keyType, i))) {
//                    readSectorData(i);
                    mListener.getM1CardResult(btReadCard,null,i+"","");

                } else {
                    BasicOper.dc_card_hex(0);// 如果接口调用失败，需要进行复位才能进行后续的操作
                    authFailList.add(Integer.toString(i));
                }
            }
            mListener.getM1CardResult(btReadCard,authFailList,"","");

        } else {
            boolean authSucceed = MDSEUtils.isSucceed(BasicOper.dc_authentication(keyType, currentSectors));
            String isSucceed;
            if (authSucceed){
                isSucceed = "true";
            }else {
                isSucceed = "false";
            }
            Log.d("tag","isSucceed : " +isSucceed);
            mListener.getM1CardResult(btReadCard,null,isSucceed,currentSectors+"");
//            readSectorData(currentSectors);
        }
    }
}
