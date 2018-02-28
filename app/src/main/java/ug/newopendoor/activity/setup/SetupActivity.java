package ug.newopendoor.activity.setup;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ug.newopendoor.R;
import ug.newopendoor.activity.camera2.CameraActivity2;
import ug.newopendoor.activity.camera6.CameraActivity6;
import ug.newopendoor.util.FileUtil;
import ug.newopendoor.util.SharedPreferencesUtil;


/**
 * Created by Administrator on 2017/12/11.
 */

public class SetupActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.idcard_switch)
    Switch idcard_switch;
    @BindView(R.id.scan_switch)
    Switch scan_switch;
    @BindView(R.id.m1_ll)
    RelativeLayout m1_ll;
    @BindView(R.id.idcard_ll)
    RelativeLayout idcard_ll;
    @BindView(R.id.add_excel)
    TextView add_excel;
    private boolean isUitralight = true;
    private boolean isScan = true;
    private boolean isIdcard = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        ButterKnife.bind(this);
        // 添加监听
        idcard_switch.setOnCheckedChangeListener(this);
        scan_switch.setOnCheckedChangeListener(this);

    }


    @OnClick({R.id.rb_uitralight, R.id.rb_m1, R.id.finish, R.id.add_excel})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rb_uitralight:
                isUitralight = true;
                break;
            case R.id.rb_m1:
                isUitralight = false;
                break;
            case R.id.finish:
                toActivity();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.idcard_switch:
                isIdcard = isChecked;
                break;
            case R.id.scan_switch:
                isScan = isChecked;
                break;
        }
    }


    private void toActivity() {
        SharedPreferencesUtil.putBoolean(this,"uitralight", isUitralight);
        SharedPreferencesUtil.putBoolean(this,"scan", isScan);
        SharedPreferencesUtil.putBoolean(this,"idcard", isIdcard);
        Intent intent = new Intent(this, CameraActivity6.class);
        startActivity(intent);
        finish();
    }
}
