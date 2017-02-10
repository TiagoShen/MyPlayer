package com.example.xiayuxi.myplayer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.lidroid.xutils.DbUtils;

/**
 * 存储类和数据库，方便保存和提取信息
 * 老师要求的数据库基本上体现在这里
 * SharedPreferences用于保存播放模式和收藏状态
 * DbUtils用于储存收藏音乐的信息
 */
public class PlayerApp extends Application {

    //轻量级的存储类，保存常用配置
    public static SharedPreferences sp;
    //音乐收藏数据库
    public static DbUtils dbUtils;

    @Override
    public void onCreate() {
        super.onCreate();

        sp = getSharedPreferences("MyPlayer", Context.MODE_PRIVATE);
        //退出Activity时，保存循环模式，歌曲位置(第几首歌曲)，保存进度值

        dbUtils = DbUtils.create(getApplicationContext(),"MyPlayerDB.db");
    }
}
