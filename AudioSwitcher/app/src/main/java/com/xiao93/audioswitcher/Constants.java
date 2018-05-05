package com.xiao93.audioswitcher;

public class Constants {
    public interface ACTION {
        public static String STARTFOREGROUND_ACTION_MUSIC = "com.xiao93.foregroundservice.action.startforegroundmusic";
        public static String STARTFOREGROUND_ACTION_PODCAST = "com.xiao93.foregroundservice.action.startforegroundpodcast";
        public static String STOPFOREGROUND_ACTION = "com.xiao93.foregroundservice.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}