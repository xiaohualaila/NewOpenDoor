package ug.newopendoor.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import ug.newopendoor.R;


/**
 * Created by Administrator on 2017/12/27.
 */

public class SoundPoolUtil {
    private static SoundPoolUtil soundPoolUtil;
    private static SoundPool soundPool;


    //单例模式
    public static SoundPoolUtil getInstance(Context context) {
        if (soundPoolUtil == null)
            soundPoolUtil = new SoundPoolUtil(context);
        return soundPoolUtil;
    }

    private SoundPoolUtil(Context context) {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 10);
        //加载音频文件
        soundPool.load(context, R.raw.please_remake, 1);//请对准摄像头，再次拍照
        soundPool.load(context, R.raw.repeat_enter, 2);//重复刷卡，请退出通道
        soundPool.load(context, R.raw.ticket_error, 3);//无效票证，请退出通道
        soundPool.load(context, R.raw.ticket_right, 4);//验证通过，请通行
        soundPool.load(context, R.raw.enter_time_error, 5);//入场时间错误
        soundPool.load(context, R.raw.net_error, 6);//网络异常
        soundPool.load(context, R.raw.error_num, 7);//入场次数已用完
        soundPool.load(context, R.raw.error_door, 8);//入场口错误
    }

    public static void play(int number) {
        Log.d("tag", "number " + number);
        //播放音频
        soundPool.play(number, 0.9f, 0.9f, 0, 0, 1);
    }
}
