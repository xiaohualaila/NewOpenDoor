package ug.newopendoor.activity.setup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ug.newopendoor.R;
import ug.newopendoor.activity.camera8.CameraActivity8;
import ug.newopendoor.util.ClearEditTextWhite;
import ug.newopendoor.util.SharedPreferencesUtil;

/**
 * Created by Administrator on 2017/12/11.
 */

public class SetupActivity extends AppCompatActivity{
    @BindView(R.id.ct_no)
    ClearEditTextWhite ct_no;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup2);
        ButterKnife.bind(this);

    }

    @OnClick({R.id.finish})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.finish:
                toActivity();
                break;
        }
    }

    private void toActivity() {
        String  no = ct_no.getText().toString().trim();
        if(TextUtils.isEmpty(no)){
            Toast.makeText(this,"入场口编号不能为空！",Toast.LENGTH_LONG).show();
            return;
        }
        if(no.equals("1")||no.equals("0")||no.equals("2")){
            SharedPreferencesUtil.save("no",no,this);
            Intent intent = new Intent(this, CameraActivity8.class);
            startActivity(intent);
            finish();
        }else {
            Toast.makeText(this,"请输入正确的入场口编号0,1,2",Toast.LENGTH_LONG).show();
            return;
        }

    }

}
