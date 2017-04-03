package com.example.xyzreader.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by DELL-INSPIRON on 4/3/2017.
 */

public class Utility {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.US);
    // Use default locale format
    private static final DateFormat outputFormat = SimpleDateFormat.getDateTimeInstance();

    private static final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    public static Date parseDate(String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return new Date();
//            e.printStackTrace();
        }
    }

    public static String dateFormat(Date date) {
        return outputFormat.format(date);
    }

    public static boolean beforeEpochTime(Date date) {
        return date.before(START_OF_EPOCH.getTime());
    }
}
