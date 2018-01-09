package ug.newopendoor.activity;

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
import ug.newopendoor.activity.camera.CameraActivity;
import ug.newopendoor.activity.camera2.CameraActivity2;
import ug.newopendoor.util.FileUtil;


/**
 * Created by Administrator on 2017/12/11.
 */

public class SetupActivity  extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    @BindView(R.id.idcard_switch)
    Switch idcard_switch;
    @BindView(R.id.scan_switch)
    Switch scan_switch;
    @BindView(R.id.choose_switch)
    Switch choose_switch;
    @BindView(R.id.m1_ll)
    RelativeLayout m1_ll;
    @BindView(R.id.idcard_ll)
    RelativeLayout idcard_ll;
    @BindView(R.id.add_excel)
    TextView add_excel;
    private boolean isUitralight = true;
    private boolean isScan = true;
    private boolean isIdcard = false;
    private boolean isHaveThree = false;

    private String path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        ButterKnife.bind(this);
        m1_ll.setVisibility(View.GONE);
        // 添加监听
        idcard_switch.setOnCheckedChangeListener(this);
        scan_switch.setOnCheckedChangeListener(this);
        choose_switch.setOnCheckedChangeListener(this);
        FileUtil.getPath();
        //getExcel();
    }


    @OnClick({R.id.rb_uitralight,R.id.rb_m1,R.id.finish,R.id.add_excel})
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
            case R.id.add_excel:

                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.idcard_switch:
                isIdcard = isChecked;
                break;
            case R.id.scan_switch:
                isScan = isChecked;
                break;
            case R.id.choose_switch:
                if(isChecked){
                    m1_ll.setVisibility(View.VISIBLE);
                   // idcard_ll.setVisibility(View.VISIBLE);
                    isHaveThree = true;
                }else {
                    m1_ll.setVisibility(View.GONE);
                  //  idcard_ll.setVisibility(View.GONE);
                    isHaveThree = false;
                }
                break;
        }

    }

    //判断Excel文件是否存在
    private void getExcel() {
        path = FileUtil.getPath()+ File.separator +"door.xls";
        File file = new File(path);
        if(!file.exists()){
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_msg)//dialog_msg
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
            dialog.show();
            return;
        }else {//存在
            add_excel.setText("正在导入Excel表！");
            add_excel.setEnabled(false);

        }
    }

    private void toActivity() {
        Intent intent = new Intent(this,CameraActivity2.class);
        intent.putExtra("uitralight",isUitralight);
        intent.putExtra("scan",isScan);
        intent.putExtra("idcard",isIdcard);
        intent.putExtra("isHaveThree",isHaveThree);
        startActivity(intent);
        finish();
    }
}
