package com.example.xiayuxi.myplayer;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * 音乐获取
 * 从本地文件中搜索
 */
public class MediaUtils {
    /**
     * 从数据库中查询歌曲的信息,保存在列表中
     */
    public static ArrayList<MusicInfo> getMusicInfos(Context context) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DURATION + ">=30000", null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        ArrayList<MusicInfo> musicInfos = new ArrayList<MusicInfo>();
        if(cursor != null) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                MusicInfo musicInfo = new MusicInfo();
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));//音乐id
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));//歌曲名
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));//歌手
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));//专辑
                long albumid = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));//专辑id
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));//时长
                long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));//文件大小
                String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));//文件路径
                int isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));//是否为音乐
                if (isMusic != 0) {
                    musicInfo.setId(id);
                    musicInfo.setTitle(title);
                    musicInfo.setArtist(artist);
                    musicInfo.setAlbum(album);
                    musicInfo.setAlbumId(albumid);
                    musicInfo.setDuration(duration);
                    musicInfo.setSize(size);
                    musicInfo.setUrl(url);
                    musicInfos.add(musicInfo);
                }
            }
        }
        else {
            musicInfos = null;
        }
        cursor.close();
        return musicInfos;
    }

    /**
     * 格式化时间,将毫秒转换为分:秒格式
     */
    public static String formatTime(long time){
        long seconds = time/1000;
        long second = seconds%60;
        long munite = (seconds-second)/60;
        DecimalFormat decimalFormat = new DecimalFormat("00");
        return decimalFormat.format(munite)+":"+decimalFormat.format(second);
    }
}
