package com.example.chap03.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DataUtil {

    public static String getNowTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:MM:SS");
        return sdf.format(new Date());
    }
}
