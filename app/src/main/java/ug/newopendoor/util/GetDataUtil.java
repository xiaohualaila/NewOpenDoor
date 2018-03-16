package ug.newopendoor.util;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import jxl.Sheet;
import jxl.Workbook;
import ug.newopendoor.activity.bean.WhiteList;
import ug.newpoendoor.greendaodemo.greendao.GreenDaoManager;
import ug.newpoendoor.greendaodemo.greendao.gen.WhiteListDao;


/**
 * Created by Administrator on 2017/11/20.
 */

public class GetDataUtil {

    /**
     * 获取 excel 表格中的数据,不能在主线程中调用
     *
     * @param xlsName excel 表格的名称
     * @param index   第几张表格中的数据
     */
    public static Boolean getXlsData(String xlsName, int index) {
        boolean saveSuccess = false;
        try {
            File file =new File(xlsName);
            InputStream in=new FileInputStream(file);
            Workbook workbook = Workbook.getWorkbook(in);
            Sheet sheet = workbook.getSheet(index);
            int sheetNum = workbook.getNumberOfSheets();
            int sheetRows = sheet.getRows();
            int sheetColumns = sheet.getColumns();

//            Log.d(TAG, "the num of sheets is " + sheetNum);
//            Log.d(TAG, "the name of sheet is  " + sheet.getName());
//            Log.d(TAG, "total rows is 行=" + sheetRows);
//            Log.d(TAG, "total cols is 列=" + sheetColumns);
            WhiteList whiteList =null;

             String xin_id;//芯片id
             String name;//姓名
             String company;//单位名称

            WhiteListDao whiteListDao = GreenDaoManager.getInstance().getSession().getWhiteListDao();
            whiteListDao.deleteAll();
            for (int i = 0; i < sheetRows; i++) {
                xin_id = sheet.getCell(0, i).getContents();
                name = sheet.getCell(1, i).getContents();
                company = sheet.getCell(2, i).getContents();


                Log.i("xxx", xin_id +" " + name + " " + company);
                if(TextUtils.isEmpty(name) && TextUtils.isEmpty(xin_id)&& TextUtils.isEmpty(company)){
                    break;
                }
                whiteList = new WhiteList(xin_id,name,company);
                whiteListDao.insert(whiteList);
            }
            workbook.close();
            saveSuccess = true;
        } catch (Exception e) {
            Log.i("sss",">>>"+e.toString());
            saveSuccess =false;
        }
        return saveSuccess;
    }


    public  static WhiteList getXinDataBooean (String code){
        WhiteListDao whiteListDao = GreenDaoManager.getInstance().getSession().getWhiteListDao();
        WhiteList whiteList =  whiteListDao.queryBuilder().where(WhiteListDao.Properties.Xin_id.eq(code)).build().unique();
        if(whiteList != null){
            return whiteList;
        }else {
            return null;
        }
    }

}
