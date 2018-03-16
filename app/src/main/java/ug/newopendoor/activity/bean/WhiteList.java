package ug.newopendoor.activity.bean;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;

/**
 * Created by Administrator on 2017/11/30.
 */
@Entity
public class WhiteList {
    @Id(autoincrement = true)
    private Long _id;
    private String xin_id;//芯片id
    private String name;//姓名
    private String company;//公司名称

    @Keep
    @Generated(hash = 577612888)
    public WhiteList( String xin_id,String name, String company) {
        this.xin_id = xin_id;
        this.name = name;
        this.company = company;

    }
    @Keep
    @Generated(hash = 697618186)
    public WhiteList() {
    }
    @Keep
    @Generated(hash = 577612888)
    public WhiteList(Long _id, String name, String xin_id, String code_id,
                     String num) {
        this._id = _id;
        this.name = name;
        this.xin_id = xin_id;
    }
    @Generated(hash = 1588867102)
    public WhiteList(Long _id, String xin_id, String name, String company) {
        this._id = _id;
        this.xin_id = xin_id;
        this.name = name;
        this.company = company;
    }
    public Long get_id() {
        return this._id;
    }
    public void set_id(Long _id) {
        this._id = _id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getXin_id() {
        return this.xin_id;
    }
    public void setXin_id(String xin_id) {
        this.xin_id = xin_id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

}
