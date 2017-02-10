package com.example.xiayuxi.myplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音乐播放服务
 * 提供基本的播放功能
 */
public class PlayService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private MediaPlayer mPlayer;
    private int currentPosition;//当前正在播放的歌曲的位置

    public int getCurrentPosition() {
        return currentPosition;
    }

/*    public void setCurrentPosition(int position) {
        this.currentPosition = position;
    }*/

    ArrayList<MusicInfo> musicInfos;

    private MusicUpdatrListener musicUpdatrListener;

    //单实力线程,用于更新音乐信息
    private ExecutorService es = Executors.newSingleThreadExecutor();

    //播放模式
    public static final int ORDER_PLAY = 1;//顺序播放
    public static final int RANDOM_PLAY = 2;//随机播放
    public static final int SINGLE_PLAY = 3;//单曲循环
    private int play_mode = ORDER_PLAY;//播放模式,默认为顺序播放
    //set方法
    public void setPlay_mode(int play_mode) {
        this.play_mode = play_mode;
    }
    //get方法
    public int getPlay_mode() {
        return play_mode;
    }

    private boolean isPause = false;//歌曲播放中的暂停状态
    public boolean isPause() {
        return isPause;
    }

    //切换播放列表,默认本地音乐
    public static final int MY_MUSIC_LIST = 1;//本地音乐
    public static final int LOVE_MUSIC_LIST = 2;//收藏音乐
    private int listName = MY_MUSIC_LIST;
    public int getChangePlayList() {//get方法
        return listName;
    }
    public void setChangePlayList(int changePlayList) {//set方法
        this.listName = changePlayList;
    }

    public PlayService() {
    }

    public void setMusicInfos(ArrayList<MusicInfo> musicInfos) {
        this.musicInfos = musicInfos;
    }

    private Random random = new Random();//创建随机对象

    //播放完成以后,判断播放模式,播放下一首
    @Override
    public void onCompletion(MediaPlayer mp) {
        switch (play_mode) {
            case ORDER_PLAY://顺序播放
                next();//下一首
                break;
            case RANDOM_PLAY://随机播放
                play(random.nextInt(musicInfos.size()));
                break;
            case SINGLE_PLAY://单曲循环
                play(currentPosition);
                break;
            default:
                break;
        }
    }

    //播放错误，处理实现播放下一首功能出现的错误
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();//重启
        return false;
    }

    //内部类PlayBinder实现Binder,得到当前PlayService对象
    class PlayBinder extends Binder {
        public PlayService getPlayService() {
            return PlayService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new PlayBinder();//通过PlayBinder拿到PlayService,给Activity调用
    }

    public ArrayList<MusicInfo> getMusicInfos() {
        return musicInfos;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //恢复状态值
        PlayerApp app = (PlayerApp) getApplication();
        currentPosition = app.sp.getInt("currentPosition", 0);
        play_mode = app.sp.getInt("play_mode", PlayService.ORDER_PLAY);
        //在PlayerApp的onCreate中 实例化 SharedPreferences
        //在Musiclist的onDestroy中 保存状态值
        //在PlayService的onCreate中 恢复状态值
        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(this);//播放完成事件
        mPlayer.setOnErrorListener(this);//播放错误事件
        musicInfos = MediaUtils.getMusicInfos(this);//获取音乐列表
        es.execute(updateSteatusRunnable);//更新进度值
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //回收线程
        if (es != null && !es.isShutdown()) {//当进度值等于空,并且,进度值没有关闭
            es.shutdown();
            es = null;
        }
    }

    //利用Runnable来实现多线程
    Runnable updateSteatusRunnable = new Runnable() {//更新状态
        @Override
        public void run() {
            //不断更新进度值
            while (true) {
                //音乐更新监听不为空且媒体播放不为空且媒体播放为播放状态
                if (musicUpdatrListener != null && mPlayer != null && mPlayer.isPlaying()) {
                    musicUpdatrListener.onPublish(getCurrentProgress());//获取当前的进度值
                }
                try {
                    Thread.sleep(500);//500毫秒更新一次
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //播放
    public void play(int position) {
        //musicInfos = MediaUtils.getMusicInfos(this);//获取音乐列表
        MusicInfo musicInfo;
        if (position < 0 || position >= musicInfos.size()) {
            position = 0;
        }
        if (musicInfos == null){
            return;
        }
        musicInfo = musicInfos.get(position);//获取音乐信息对象
        //进行播放,播放前判断
        try {
            mPlayer.reset();//重启
            mPlayer.setDataSource(this, Uri.parse(musicInfo.getUrl()));//资源解析,音乐地址
            mPlayer.prepare();//准备
            mPlayer.start();//开始(播放)
            currentPosition = position;//保存当前位置到currentPosition
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (musicUpdatrListener != null) {
            musicUpdatrListener.onChange(currentPosition);//更新当前位置
        }
    }

    //暂停
    public void pause() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            isPause = true;
        }
    }

    //停止
    public void stop() {
        if (mPlayer.isPlaying()) {
            mPlayer.stop();
            isPause = true;
        }
    }

    //下一首
    public void next() {
        if (currentPosition >= musicInfos.size() - 1) {//如果超出最大值,(因为第一首是0),说明已经是最后一首
            currentPosition = 0;//回到第一首
        } else {
            currentPosition++;//下一首
        }
        play(currentPosition);
    }

    //上一首
    public void prev() {
        if (currentPosition - 1 < 0) {//如果上一首小于0,说明已经是第一首
            currentPosition = musicInfos.size() - 1;//回到最后一首
        } else {
            currentPosition--;//上一首
        }
        play(currentPosition);
    }

    //快进15秒
    public void fastforward() {
        if (mPlayer != null){
            mPlayer.seekTo(mPlayer.getCurrentPosition()+15000);
        }
    }

    //快进15秒
    public void rewind() {
        if (mPlayer != null){
            mPlayer.seekTo(mPlayer.getCurrentPosition()-15000);
        }
    }

    //默认开始播放
    public void start() {
        if (mPlayer != null && !mPlayer.isPlaying()) {//判断当前歌曲不等于空,并且没有在播放的状态
            mPlayer.start();
        }
    }

    //获取当前是否为播放状态,提供给MyMusicListFragment的播放暂停按钮点击事件判断状态时调用
    public boolean isPlaying() {
        if (mPlayer != null) {
            return mPlayer.isPlaying();
        }
        return false;
    }

    //获取当前的进度值
    public int getCurrentProgress() {
        if (mPlayer != null && mPlayer.isPlaying()) {//mPlayer不为空,并且,为播放状态
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    //获取文件的持续时间
    public int getDuration() {
        return mPlayer.getDuration();
    }

    //寻找指定的时间位置 (跳到某个时间点进行播放)
    public void seekTo(int msec) {
        mPlayer.seekTo(msec);
    }

    //更新状态的内部接口
    public interface MusicUpdatrListener {//音乐更新监听器
        public void onPublish(int progress);//发表进度事件(更新进度条)
        public void onChange(int position); //更新歌曲位置.按钮的状态等信息
    }

    //set方法
    public void setMusicUpdatrListener(MusicUpdatrListener musicUpdatrListener) {
        this.musicUpdatrListener = musicUpdatrListener;
    }
}
