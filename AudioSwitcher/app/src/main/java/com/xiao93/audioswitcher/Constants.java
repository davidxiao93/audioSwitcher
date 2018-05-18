package com.xiao93.audioswitcher;

public class Constants {
    public interface ACTION {
        public static String START_MUSIC = "com.xiao93.foregroundservice.action.startmusic";
        public static String START_PODCAST = "com.xiao93.foregroundservice.action.startpodcast";
        public static String PAUSE = "com.xiao93.foregroundservice.action.pause";
        public static String STOP = "com.xiao93.foregroundservice.action.stop";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}