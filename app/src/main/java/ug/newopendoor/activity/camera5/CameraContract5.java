package ug.newopendoor.activity.camera5;


import java.io.File;

import ug.newopendoor.activity.base.IBasePresenter;
import ug.newopendoor.activity.base.IBaseView;

/**
 * Created by Administrator on 2017/6/3.
 */

public interface CameraContract5 {
    interface View extends IBaseView<Presenter> {

        void doSuccess(String Face_path);

        void doCommonError(String text, int num);
    }

    interface Presenter extends IBasePresenter {
        void load(String device_id, int type, String ticketNum, File newFile);

    }
}
