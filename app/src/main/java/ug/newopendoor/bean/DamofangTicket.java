package ug.newopendoor.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by Administrator on 2018/1/2.
 */
@Entity
public class DamofangTicket {
    @Id
    private Long _id;
    private String ticket_id;//id
    private String leval;//等级
    private String price_name;//票价名称
    private String price;//票价
    private String floor;//楼层
    private String area;//区域
    private String row;//排

    public DamofangTicket() {
    }

    @Generated(hash = 1584681666)
    public DamofangTicket(Long _id, String ticket_id, String leval,
            String price_name, String price, String floor, String area,
            String row) {
        this._id = _id;
        this.ticket_id = ticket_id;
        this.leval = leval;
        this.price_name = price_name;
        this.price = price;
        this.floor = floor;
        this.area = area;
        this.row = row;
    }

    public Long get_id() {
        return this._id;
    }

    public void set_id(Long _id) {
        this._id = _id;
    }

    public String getTicket_id() {
        return this.ticket_id;
    }

    public void setTicket_id(String ticket_id) {
        this.ticket_id = ticket_id;
    }

    public String getLeval() {
        return this.leval;
    }

    public void setLeval(String leval) {
        this.leval = leval;
    }

    public String getPrice_name() {
        return this.price_name;
    }

    public void setPrice_name(String price_name) {
        this.price_name = price_name;
    }

    public String getPrice() {
        return this.price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getFloor() {
        return this.floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getArea() {
        return this.area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getRow() {
        return this.row;
    }

    public void setRow(String row) {
        this.row = row;
    }
}
