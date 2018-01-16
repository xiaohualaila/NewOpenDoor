package ug.newopendoor.activity.camera3;

import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import butterknife.BindView;
import ug.newopendoor.R;
import ug.newopendoor.activity.base.BaseAppCompatActivity;
import ug.newopendoor.rx.RxBus;
import ug.newopendoor.service.CommonThreeService;
import ug.newopendoor.service.ScreenService;
import ug.newopendoor.usbtest.ComBean;
import ug.newopendoor.usbtest.SPUtils;
import ug.newopendoor.usbtest.SerialHelper;
import ug.newopendoor.usbtest.Utils;
import ug.newopendoor.util.MyMessage;
import ug.newopendoor.util.Ticket;

/**
 * Created by Administrator on 2018/1/10.
 */

public class FragmentActivity extends BaseAppCompatActivity {
    @BindView(R.id.fl_content)
    FrameLayout fl_content;

    private Fragment mCurrentFrag;
    private FragmentManager fm;
    private Fragment cameraFragment;
    private Fragment mainFragment;

    private SPUtils settingSp;
    private String USB = "";
    //串口
    SerialControl ComA;
    DispQueueThread DispQueue;
    private boolean isScan = true;

    ////////////////////////////////////屏保部分代码
    private Handler mHandler01 = new Handler();
    private Handler mHandler02 = new Handler();

    /* 上一次User有动作的Time Stamp */
    private Date lastUpdateTime;
    /* 计算User有几秒没有动作的 */
    private long timePeriod;

    /* 静止超过N秒将自动进入屏保 */
    private float mHoldStillTime = 30;
    /*标识当前是否进入了屏保*/
    private boolean isRunScreenSaver;

    /*时间间隔*/
    private long intervalScreenSaver = 1000;
    private long intervalKeypadeSaver = 1000;
    //////////////////////////////////////////


    @Override
    protected void init() {
        initShebei();
        fm = getSupportFragmentManager();
        cameraFragment = new CameraFragment();
        mainFragment = new MainFragment();
        switchContent(cameraFragment);//切换页面
         /* 初始取得User可触碰屏幕的时间 */
        lastUpdateTime = new Date(System.currentTimeMillis());
        RxBus.getDefault().toObserverable(MyMessage.class).subscribe(myMessage -> {
            int num = myMessage.getNum();
            if (num == 0) {
                updateUserActionTime();
                switchContent(cameraFragment);
                isScan = true;
            }
        });
    }

    private void initShebei() {
        Utils.init(getApplicationContext());
        settingSp = new SPUtils(getString(R.string.settingSp));
        USB = settingSp.getString(getString(R.string.usbKey), getString(R.string.androidUsb));
        rkGpioControlNative.init();
        //串口
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();
        openErWeiMa();

        startService(new Intent(this, CommonThreeService.class));
        startService(new Intent(this, ScreenService.class));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_fragment;
    }


    public void switchContent(Fragment to) {
        if (mCurrentFrag != to) {
            if (!to.isAdded()) {// 如果to fragment没有被add则增加一个fragment
                if (mCurrentFrag != null) {
                    fm.beginTransaction().hide(mCurrentFrag).commit();
                }
                fm.beginTransaction()
                        .add(R.id.fl_content, to)
                        .commit();
            } else {
                fm.beginTransaction().hide(mCurrentFrag).show(to).commit(); // 隐藏当前的fragment，显示下一个
            }
            mCurrentFrag = to;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        onOpenConnectPort();
        ///////////////////////////////////
        mHandler01.postAtTime(mTask01, intervalKeypadeSaver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        onDisConnectPort();
          /*activity不可见的时候取消线程*/
        mHandler01.removeCallbacks(mTask01);
        mHandler02.removeCallbacks(mTask02);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, CommonThreeService.class));
        stopService(new Intent(this, ScreenService.class));


        adcNative.close(0);
        adcNative.close(2);
        rkGpioControlNative.close();
        closeErWeiMa();
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

        } else {
            Toast.makeText(this, "设备没有连接上！", Toast.LENGTH_LONG).show();
        }
    }

    //关闭设备
    public void onDisConnectPort() {
        int close_status = BasicOper.dc_exit();
        if (close_status >= 0) {
            //   Log.i("sss","设备关闭");
        } else {
            //  Log.i("sss","Port has closed");
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
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                if (isScan) {
                                    String ticketNum = new String(ComData.bRec).trim();
                                    Ticket ticket = new Ticket(2, ticketNum);
                                    RxBus.getDefault().post(ticket);
                                    updateUserActionTime();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    try {
                        Thread.sleep(1000);
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


    /////////////////////////////////////////////////////////屏保部分代码

    /**
     * 计时线程
     */
    private Runnable mTask01 = new Runnable() {

        @Override
        public void run() {
            Date timeNow = new Date(System.currentTimeMillis());
            /* 计算User静止不动作的时间间距 */
            /**当前的系统时间 - 上次触摸屏幕的时间 = 静止不动的时间**/
            timePeriod = (long) timeNow.getTime() - (long) lastUpdateTime.getTime();

            /*将静止时间毫秒换算成秒*/
            float timePeriodSecond = ((float) timePeriod / 1000);

            if (timePeriodSecond > mHoldStillTime) {
                if (isRunScreenSaver == false) {  //说明没有进入屏保
                    /* 启动线程去显示屏保 */
                    mHandler02.postAtTime(mTask02, intervalScreenSaver);
                    /*显示屏保置为true*/
                    isRunScreenSaver = true;
                } else {
                    /*屏保正在显示中*/
                }
            } else {
                /*说明静止之间没有超过规定时长*/
                isRunScreenSaver = false;
            }
            /*反复调用自己进行检查*/
            mHandler01.postDelayed(mTask01, intervalKeypadeSaver);
        }
    };

    /**
     * 持续屏保显示线程
     */
    private Runnable mTask02 = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (isRunScreenSaver == true) {  //如果屏保正在显示，就计算不断持续显示
//              hideOriginalLayout();
                showScreenSaver();
                mHandler02.postDelayed(mTask02, intervalScreenSaver);
            } else {
                mHandler02.removeCallbacks(mTask02);  //如果屏保没有显示则移除线程
            }
        }
    };

    /**
     * 显示屏保
     */
    private void showScreenSaver() {
        isScan = false;
        switchContent(mainFragment);
    }

    /*用户有操作的时候不断重置静止时间和上次操作的时间*/
    public void updateUserActionTime() {
        Date timeNow = new Date(System.currentTimeMillis());
        timePeriod = timeNow.getTime() - lastUpdateTime.getTime();
        lastUpdateTime.setTime(timeNow.getTime());
    }

}
