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
            Toast.makeText(this,"秘钥不能为空！",Toast.LENGTH_LONG).show();
            return;
        }
            SharedPreferencesUtil.save("m1",no,this);
            Intent intent = new Intent(this, CameraActivity8.class);
            startActivity(intent);
            finish();


    }

}
