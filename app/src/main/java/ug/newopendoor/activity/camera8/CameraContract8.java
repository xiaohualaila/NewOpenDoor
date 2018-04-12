package ug.newopendoor.activity.camera8;


import java.io.File;

import ug.newopendoor.activity.base.IBasePresenter;
import ug.newopendoor.activity.base.IBaseView;

/**
 * Created by Administrator on 2017/6/3.
 */

public interface CameraContract8 {
    interface View extends IBaseView<Presenter> {

        void doCommonError(String text, int num, String Face_path);

        void doSuccess(String Face_path, String ticket_no, String seat_info);

    }

    interface Presenter extends IBasePresenter {
        void load(int projectId,String chipId,String qrCodeId,String doorType);
    }
}
