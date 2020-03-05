package com.example;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PrintTime {
    public static String printTime() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
