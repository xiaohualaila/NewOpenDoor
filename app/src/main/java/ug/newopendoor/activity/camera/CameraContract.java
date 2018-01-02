package ug.newopendoor.activity.camera;


import java.io.File;

import ug.newopendoor.activity.base.IBasePresenter;
import ug.newopendoor.activity.base.IBaseView;

/**
 * Created by Administrator on 2017/6/3.
 */

public interface CameraContract {
    interface View extends IBaseView<Presenter> {
        void doError();
        void doSuccess(String Face_path);
    }

    interface Presenter extends IBasePresenter {
       void load(boolean isNetAble, String device_id, int type, String ticketNum, File newFile);

    }
}
