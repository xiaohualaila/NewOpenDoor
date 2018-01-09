package ug.newopendoor.util;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/11/10.
 */

public class MyMessage implements Serializable{
    private int  num;

    public MyMessage(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
