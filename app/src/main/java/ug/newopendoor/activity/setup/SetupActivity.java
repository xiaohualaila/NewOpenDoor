package ug.newopendoor.activity.setup;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ug.newopendoor.R;
import ug.newopendoor.activity.camera8.CameraActivity8;
import ug.newopendoor.util.ClearEditTextWhite;
import ug.newopendoor.util.FileUtil;
import ug.newopendoor.util.GetDataUtil;
import ug.newopendoor.util.SharedPreferencesUtil;
import ug.newpoendoor.greendaodemo.greendao.GreenDaoManager;
import ug.newpoendoor.greendaodemo.greendao.gen.WhiteListDao;


/**
 * Created by Administrator on 2017/12/11.
 */

public class SetupActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener  {
    @BindView(R.id.idcard_switch)
    Switch idcard_switch;
    @BindView(R.id.scan_switch)
    Switch scan_switch;
    @BindView(R.id.m1_ll)
    RelativeLayout m1_ll;
    @BindView(R.id.idcard_ll)
    RelativeLayout idcard_ll;

    @BindView(R.id.ip_address)
    ClearEditTextWhite ct_ip_address;
    @BindView(R.id.duankou)
    ClearEditTextWhite ct_duankou;
    @BindView(R.id.jieko)
    ClearEditTextWhite ct_jieko;
    @BindView(R.id.ct_secret)
    ClearEditTextWhite ct_secret;
    @BindView(R.id.ip_context)
    TextView ip_context;
    private boolean isUitralight = false;
    private boolean isScan = true;
    private boolean isIdcard = false;
    private String ip_address = "";
    private String duankou = "";
    private String jieko = "";
    public  String URL = "http://" + ip_address + ":" + duankou + "/ticket_checking/Api/" + jieko + "/";
    public  String URL1 = "http://" + ip_address + "/ticket_checking/Api/" + jieko + "/";
    private String path;
    private boolean isExcelSuccess = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        ButterKnife.bind(this);

        // 添加监听
        idcard_switch.setOnCheckedChangeListener(this);
        scan_switch.setOnCheckedChangeListener(this);
        setTextChangedListener(ct_ip_address,1);
        setTextChangedListener(ct_duankou,2);
        setTextChangedListener(ct_jieko,3);
    }

   public void setTextChangedListener(ClearEditTextWhite editTextWhite,int content){
       editTextWhite.addTextChangedListener(new TextWatcher() {
           @Override
           public void beforeTextChanged(CharSequence s, int start, int count, int after) {

           }

           @Override
           public void onTextChanged(CharSequence s, int start, int before, int count) {

           }

           @Override
           public void afterTextChanged(Editable s) {

               if(content == 1){
                   ip_address = s.toString();
                   if(TextUtils.isEmpty(duankou)){
                       URL1 = "http://" + ip_address + "/ticket_checking/Api/" + jieko + "/";
                       ip_context.setText(URL1);
                   } else {
                       URL = "http://" + ip_address + ":" + duankou + "/ticket_checking/Api/" + jieko + "/";
                       ip_context.setText(URL);
                   }
               }else if(content == 2){
                   duankou = s.toString();
                   if(TextUtils.isEmpty(duankou)){
                       URL1 = "http://" + ip_address + "/ticket_checking/Api/" + jieko + "/";
                       ip_context.setText(URL1);
                   } else {
                       URL = "http://" + ip_address + ":" + duankou + "/ticket_checking/Api/" + jieko + "/";
                       ip_context.setText(URL);
                   }
               }else if(content == 3){
                   jieko = s.toString();
                   if(TextUtils.isEmpty(duankou)){
                       URL1 = "http://" + ip_address + "/ticket_checking/Api/" + jieko + "/";
                       ip_context.setText(URL1);
                   } else {
                       URL = "http://" + ip_address + ":" + duankou + "/ticket_checking/Api/" + jieko + "/";
                       ip_context.setText(URL);
                   }
               }
           }
       });
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
                if(isExcelSuccess){
                    toActivity();
                }else {
                    Toast.makeText(this,"请先点击加载Excel！",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.add_excel:
                isExcelSuccess = false;
                getExcel();
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
        ip_context.getText().toString().trim();
        String  address = ip_context.getText().toString().trim();
        if(TextUtils.isEmpty(address)){
            Toast.makeText(this,"请设置IP",Toast.LENGTH_LONG).show();
            return;
        }
        String secret = ct_secret.getText().toString().trim();
        if(!isUitralight){
            if(TextUtils.isEmpty(secret)){
                Toast.makeText(this,"M1秘钥不能为空！",Toast.LENGTH_LONG).show();
                return;
            }
        }
        SharedPreferencesUtil.save("secret",secret,this);
        SharedPreferencesUtil.putBoolean(this,"uitralight", isUitralight);
        SharedPreferencesUtil.putBoolean(this,"scan", isScan);
        SharedPreferencesUtil.putBoolean(this,"idcard", isIdcard);
        Intent intent = new Intent(this, CameraActivity8.class);
        intent.putExtra("ip",address);
        startActivity(intent);
        finish();
    }

    //判断Excel文件是否存在
    private void getExcel() {
        path = FileUtil.getPath() + File.separator + "a.xls";
        File file = new File(path);
        if (!file.exists()) {
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
        } else {//存在
            Toast.makeText(this,"正在从Excel导入数据！",Toast.LENGTH_LONG).show();
            new ExcelDataLoader().execute(path);
        }
    }


    //在异步方法中 调用
    private class ExcelDataLoader extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... params) {

            return GetDataUtil.getXlsData(params[0], 0);
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {

            if (isSuccess) {
                WhiteListDao whiteListDao = GreenDaoManager.getInstance().getSession().getWhiteListDao();
                //加载成功
                Toast.makeText(SetupActivity.this,"加载成功！共" + whiteListDao.loadAll().size() + "条记录",Toast.LENGTH_LONG).show();
                isExcelSuccess =true;
            } else {
                //加载失败
                Toast.makeText(SetupActivity.this,"加载失败！",Toast.LENGTH_LONG).show();
                isExcelSuccess =false;
            }
        }
    }
}
