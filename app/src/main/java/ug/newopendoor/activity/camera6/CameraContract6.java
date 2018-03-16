package ug.newopendoor.activity.camera6;


import java.io.File;

import ug.newopendoor.activity.base.IBasePresenter;
import ug.newopendoor.activity.base.IBaseView;

/**
 * Created by Administrator on 2017/6/3.
 */

public interface CameraContract6 {
    interface View extends IBaseView<Presenter> {
        void requestFail();

        void doError();

        void doFaceError(String Face_path);

        void doTimeError(String Face_path);

        void doManyError(String Face_path);

        void doNoFaceError(String Face_path);

        void doSuccess(String Face_path);

    }

    interface Presenter extends IBasePresenter {
        void load(String device_id, int type, String ticketNum, File newFile);
    }
}
