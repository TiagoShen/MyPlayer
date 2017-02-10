package com.example.xiayuxi.myplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

/**
 * 服务的绑定和解除
 * 其他的播放和列表类全都是基于BaseActivity，onResume时绑定服务，onDestroy时解绑服务
 */
public abstract class BaseActivity extends Activity {

    protected PlayService playService;

    private boolean isBound = false;//是否已经绑定

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    //绑定Service
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {//服务连接
            PlayService.PlayBinder playBinder = (PlayService.PlayBinder) service;//转换
            playService = playBinder.getPlayService();//绑定播放服务
            playService.setMusicUpdatrListener(musicUpdatrListener);//设置监听器
            musicUpdatrListener.onChange(playService.getCurrentPosition());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {//服务断开
            playService = null;
            isBound = false;
        }
    };

    //方法在子类中实现
    private PlayService.MusicUpdatrListener musicUpdatrListener = new PlayService.MusicUpdatrListener() {
        @Override
        public void onPublish(int progress) {
            publish(progress);
        }

        @Override
        public void onChange(int progress) {
            change(progress);
        }
    };

    //抽象类
    public abstract void publish(int progress);
    public abstract void change(int progress);

    //绑定服务
    public void bindPlayService(){
        if(!isBound) {
            Intent intent = new Intent(this, PlayService.class);
            bindService(intent, conn, Context.BIND_AUTO_CREATE);
            isBound = true;
        }
    }

    //解绑服务
    public void unbindPlayService(){
        if(isBound) {
            unbindService(conn);
            isBound = false;
        }
    }
}
