package com.example.xiayuxi.myplayer;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * 播放页面
 * 已经实现的功能有：显示当前时间和总时间、进度条
 * 播放、暂停、上一首、下一首、快进、快退、切换播放模式、收藏
 */
public class MusicPlay extends BaseActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private SeekBar sb; //进度条
    private TextView musicName; //音乐名称
    private TextView music_progress; //当前时间
    private TextView music_duration; //总时长
    private ImageView play_pause_music; //开始暂停按钮
    private ImageView stop; //停止
    private ImageView pre; //上一首
    private ImageView next; //下一首
    private ImageView rewind; //快进
    private ImageView fast; //快退
    private ImageView mode; //播放模式
    private ImageView like; //喜欢
    //private ImageView background; //显示背景图片

    //private int position;

    private static final int UPDATE_TIME = 0x10;//更新播放事件的标记

    private PlayerApp app;//取出全局对象 方便调用

    //摇一摇
    private SensorManager sensorManager;
    private Sensor sensor; //传感器
    private Vibrator vibrator; //振动器
    private static final int UPTATE_INTERVAL_TIME = 50;
    private static final int SPEED_SHRESHOLD = 30;//调节灵敏度的数值
    private long lastUpdateTime;
    private float lastX;
    private float lastY;
    private float lastZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_play);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); //传感器
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE); //振动器

        app = (PlayerApp) getApplication();//取出全局对象 方便调用

        initView(); //初始化界面

        myHandler = new MyHandler(this);
    }

    /*初始化控件*/
    private void initView() {
        //UI
        sb = (SeekBar)findViewById(R.id.seekBar);
        musicName = (TextView) findViewById(R.id.name);
        music_progress = (TextView) findViewById(R.id.currentTime);
        music_duration = (TextView) findViewById(R.id.totalTime);
        play_pause_music = (ImageView) findViewById(R.id.play_pause);
        stop = (ImageView) findViewById(R.id.stop);
        pre = (ImageView) findViewById(R.id.previous);
        next = (ImageView) findViewById(R.id.next);
        rewind = (ImageView) findViewById(R.id.rewind);
        fast = (ImageView) findViewById(R.id.fastforward);
        mode = (ImageView) findViewById(R.id.mode);
        like = (ImageView) findViewById(R.id.like);

        //设置监听器
        play_pause_music.setOnClickListener(this);
        stop.setOnClickListener(this);
        pre.setOnClickListener(this);
        next.setOnClickListener(this);
        rewind.setOnClickListener(this);
        fast.setOnClickListener(this);
        mode.setOnClickListener(this);
        like.setOnClickListener(this);
        sb.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindPlayService();//绑定服务

        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //获取某种类型的感应器
        }
        if (sensor != null) {
            sensorManager.registerListener(sensorEventListener, //注册监听，获取传感器变化值
                    sensor,
                    SensorManager.SENSOR_DELAY_GAME);//选择感应频率
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //playService.stop();
        unbindPlayService();//解绑服务
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //playService.stop();
        unbindPlayService();//解绑服务
    }

    private static MyHandler myHandler; //用于更新已经播放时间

    //进度条改变 (fromUser 是否来自用户的改变 , 而不是程序自身控制的改变)
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            playService.seekTo(progress);//寻找指定的时间位置 ,跳到某个时间点进行播放
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    static class MyHandler extends Handler {
        private MusicPlay musicplay;
        private WeakReference<MusicPlay> weak;//弱引用

        public MyHandler(MusicPlay musicplay) {
            weak = new WeakReference<MusicPlay>(musicplay);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            musicplay = weak.get();
            if (musicplay != null) {
                switch (msg.what) {
                    case UPDATE_TIME://更新时间(已经播放时间)
                        musicplay.music_progress.setText(MediaUtils.formatTime((int) msg.obj));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void publish(int progress) {
        myHandler.obtainMessage(UPDATE_TIME, progress).sendToTarget();
        sb.setProgress(progress);
    }

    @Override
    public void change(int position) {//初始化,播放界面的歌曲切换后的初始化界面上的歌曲信息
        Log.i("Info","调用了change，初始化音乐信息");
        //position = playService.getCurrentPosition();
        Log.i("INFO","play当前位置："+position);
        MusicInfo musicInfo = playService.musicInfos.get(position);
        musicName.setText(musicInfo.getTitle());//设置歌名

        music_duration.setText(MediaUtils.formatTime(musicInfo.getDuration()));//设置总时间
        play_pause_music.setImageResource(R.drawable.pause);//设置暂停图片
        sb.setProgress(0);//设置当前进度为0
        sb.setMax((int) musicInfo.getDuration());//设置进度条最大值为总时间
        if (playService.isPlaying()) {
            play_pause_music.setImageResource(R.drawable.pause);
        } else {
            play_pause_music.setImageResource(R.drawable.play);
        }

        switch (playService.getPlay_mode()) {
            case PlayService.ORDER_PLAY://顺序播放
                mode.setImageResource(R.drawable.order);
                break;
            case PlayService.RANDOM_PLAY://随机播放
                mode.setImageResource(R.drawable.random);
                break;
            case PlayService.SINGLE_PLAY://单曲循环
                mode.setImageResource(R.drawable.single);
                break;
            default:
                break;
        }

        //初始化收藏状态
        try {
            MusicInfo loveMusicInfo = app.dbUtils.findFirst(Selector.from(MusicInfo.class).where("musicInfoId","=",getId(musicInfo)));//查出歌曲,SQL语句
            Log.i("Info","初始化收藏状态"+loveMusicInfo);
            if (loveMusicInfo != null) {
                Log.i("Info","是否收藏："+loveMusicInfo.getIsLove());
                if (loveMusicInfo.getIsLove() == 0) {//返回值不为null,且,isLove为0时,也显示为'未收藏'
                    like.setImageResource(R.drawable.like);
                }else {//返回值为null,且,isLove为1时,显示为'已收藏'
                    like.setImageResource(R.drawable.love);
                }
            } else {//返回值为null,显示为'未收藏'
                like.setImageResource(R.drawable.like);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    //如果是本地音乐,id就是id;如果是收藏音乐,id则是musicInfoId，提供给 收藏按钮 点击事件时调用.
    private long getId(MusicInfo musicInfo){
        //初始收藏状态
        long id = 0;
        switch (playService.getChangePlayList()){
            case PlayService.MY_MUSIC_LIST:
                id = musicInfo.getId();
                Log.i("Info","当前为本地音乐 id="+id);
                break;
            case PlayService.LOVE_MUSIC_LIST:
                id = musicInfo.getMusicInfoId();
                Log.i("Info","当前为收藏音乐 id="+id);
                break;
            default:
                break;
        }
        return id;
    }

    //点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_pause: {//播放暂停按钮
                if (playService.isPlaying()) {//如果是播放状态
                    play_pause_music.setImageResource(R.drawable.play);//设置播放图片
                    playService.pause();
                } else {
                    if (playService.isPause()) {
                        play_pause_music.setImageResource(R.drawable.pause);//设置暂停图片
                        playService.start();//播放事件
                    } else {
                        playService.play(playService.getCurrentPosition());
                    }
                }
                break;
            }
            case R.id.next: {
                playService.next();//下一首
                break;
            }
            case R.id.previous: {
                playService.prev();//上一首
                break;
            }
            case R.id.fastforward: {
                playService.fastforward();//快进
                break;
            }
            case R.id.rewind: {
                playService.rewind();//快退
                break;
            }
            case R.id.stop: {
                sb.setProgress(0);//设置当前进度为0
                music_progress.setText("00:00");
                playService.stop();//停止
                //playService.setCurrentPosition(playService.getCurrentPosition()-1);
                play_pause_music.setImageResource(R.drawable.play);//设置播放图片
                break;
            }
            case R.id.mode: {//循环模式按钮
                switch (playService.getPlay_mode()) {
                    case PlayService.ORDER_PLAY:
                        mode.setImageResource(R.drawable.random);
                        playService.setPlay_mode(PlayService.RANDOM_PLAY);
                        Toast.makeText(this, getString(R.string.random_play), Toast.LENGTH_SHORT).show();
                        break;
                    case PlayService.RANDOM_PLAY:
                        mode.setImageResource(R.drawable.single);
                        playService.setPlay_mode(PlayService.SINGLE_PLAY);
                        Toast.makeText(this, getString(R.string.single_play), Toast.LENGTH_SHORT).show();
                        break;
                    case PlayService.SINGLE_PLAY:
                        mode.setImageResource(R.drawable.order);
                        playService.setPlay_mode(PlayService.ORDER_PLAY);
                        Toast.makeText(this, getString(R.string.order_play), Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
            }
            case R.id.like: {//收藏按钮  //在vo.MusicInfo里  private long musicInfoId;//在收藏音乐时用于保存原始ID
                MusicInfo musicInfo = playService.musicInfos.get(playService.getCurrentPosition());//查出歌曲
                Log.i("Info","歌曲信息：" + musicInfo);
                try {
                    /*MusicInfo loveMusicInfo = app.dbUtils.findFirst(Selector.from(MusicInfo.class)
                            .where("mp3InfoId","=",musicInfo.getMusicInfoId()));//查出歌曲,SQL语句  
                    if (loveMusicInfo==null){//不在音乐收藏数据库中  
                        musicInfo.setMusicInfoId(musicInfo.getId());
                        //在音乐收藏数据库 保存音乐  
                        app.dbUtils.save(musicInfo);
                        like.setImageResource(R.drawable.like);
                        Toast.makeText(MusicPlay.this, "已收藏", Toast.LENGTH_SHORT).show();
                    }else {//在音乐收藏数据库中  
                        //在音乐收藏数据库 删除音乐  
                        app.dbUtils.deleteById(MusicInfo.class,loveMusicInfo.getId());
                        like.setImageResource(R.drawable.love);
                        Toast.makeText(MusicPlay.this, "取消收藏", Toast.LENGTH_SHORT).show();
                    }  */
                    MusicInfo loveMusicInfo = app.dbUtils.findFirst(Selector.from(MusicInfo.class).where("musicInfoId","=",getId(musicInfo)));//查出歌曲,SQL语句
                    Log.i("Info","收藏音乐信息："+loveMusicInfo);
                    if (loveMusicInfo==null){//返回值为null,则需要save
                        Log.i("Info","不在音乐收藏数据库中 保存音乐数据 原始数据: " + musicInfo);
                        musicInfo.setMusicInfoId(musicInfo.getId());
                        musicInfo.setIsLove(1);
                        Log.i("Info","歌曲信息：" + musicInfo);
                        app.dbUtils.save(musicInfo);//在音乐收藏数据库 保存音乐
                        Log.i("Info","保存");
                        like.setImageResource(R.drawable.love);
                        //检验结果
                        loveMusicInfo = app.dbUtils.findFirst(Selector.from(MusicInfo.class).where("musicInfoId","=",getId(musicInfo)));//查出歌曲,SQL语句
                        Log.i("Info","最新音乐信息："+loveMusicInfo);
                    }else {//返回值不为null,则需要更新
                        Log.i("Info","在音乐收藏数据库中 更新音乐数据 原始数据: " + loveMusicInfo);
                        int isLove = loveMusicInfo.getIsLove();
                        if (isLove==1){//返回值不为null,且,isLove为1时;设置isLove为0,同时显示为'未收藏'
                            loveMusicInfo.setIsLove(0);
                            like.setImageResource(R.drawable.like);
                        }else {//返回值不为null,且,isLove为0时;设置isLove为1,同时显示为'已收藏'
                            loveMusicInfo.setIsLove(1);
                            like.setImageResource(R.drawable.love);
                        }
                        Log.i("Info","更新");
                        app.dbUtils.update(loveMusicInfo,"isLove");//更新loveMusicInfo数据
                        //检验结果
                        loveMusicInfo = app.dbUtils.findFirst(Selector.from(MusicInfo.class).where("musicInfoId","=",getId(musicInfo)));//查出歌曲,SQL语句
                        Log.i("Info","最新音乐信息："+loveMusicInfo);
                    }
                } catch (DbException e) {
                    e.printStackTrace();
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * 重力感应监听
     */
    private SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentUpdateTime = System.currentTimeMillis();
            long timeInterval = currentUpdateTime - lastUpdateTime;
            if (timeInterval < UPTATE_INTERVAL_TIME) {
                return;
            }
            lastUpdateTime = currentUpdateTime;// 传感器信息改变时执行该方法
            float[] values = event.values;
            float x = values[0]; // x轴方向的重力加速度，向右为正
            float y = values[1]; // y轴方向的重力加速度，向前为正
            float z = values[2]; // z轴方向的重力加速度，向上为正
            float deltaX = x - lastX;
            float deltaY = y - lastY;
            float deltaZ = z - lastZ;

            lastX = x;
            lastY = y;
            lastZ = z;
            double speed = (Math.sqrt(deltaX * deltaX + deltaY * deltaY
                    + deltaZ * deltaZ) / timeInterval) * 100;
            if (speed >= SPEED_SHRESHOLD) {
                vibrator.vibrate(300); //震动

                playService.next(); //切换到下一首
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
}
