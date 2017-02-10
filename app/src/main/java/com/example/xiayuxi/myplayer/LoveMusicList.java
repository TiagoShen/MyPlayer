package com.example.xiayuxi.myplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏音乐页面
 * 通过点击播放页面的心形收藏按钮来添加到收藏
 */
public class LoveMusicList extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    private ListView love_list;
    private ImageView refurbish;
    private PlayerApp app;//取出全局对象 方便调用
    private ArrayList<MusicInfo> loveMusicInfos;
    private MusicListAdapter adapter;
    private boolean isChange = false;//表示当前播放列表是否为收藏列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_list_love);//绑定布局

        app = (PlayerApp) getApplication();
        love_list = (ListView) findViewById(R.id.love_list);//实例化布局
        refurbish = (ImageView) findViewById(R.id.refurbish);
        love_list.setOnItemClickListener(this);
        refurbish.setOnClickListener(this);
        initData();//初始化数据
    }

    private void initData() {//初始化数据
        try {
            List<MusicInfo> list = app.dbUtils.findAll(Selector.from(MusicInfo.class).where("isLove","=","1"));//查找数据库中所有已收藏音乐
            if (list==null || list.size()==0){
                return;
            }
            loveMusicInfos = (ArrayList<MusicInfo>) list;
            adapter = new MusicListAdapter(this,loveMusicInfos);
            love_list.setAdapter(adapter);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindPlayService();//绑定服务
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindPlayService();//解绑服务
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPlayService();//解绑服务
    }

    @Override
    public void publish(int progress) {

    }

    @Override
    public void change(int progress) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //如果当前播放列表不是收藏列表
        if (playService.getChangePlayList() != playService.LOVE_MUSIC_LIST){
            playService.setMusicInfos(loveMusicInfos);//播放列表切换为收藏列表
            playService.setChangePlayList(playService.LOVE_MUSIC_LIST);
            Log.i("Info","播放列表切换为收藏列表");
        }
        //playService.setCurrentPosition(position);
        playService.play(position);
        Log.i("INFO","lovelist当前位置："+playService.getCurrentPosition());
        MusicInfo loveMusicInfo = loveMusicInfos.get(position);
        Log.i("Info","收藏列表:"+loveMusicInfo);
        //保存播放时间
        savePlayRecord();
        Intent intent = new Intent(LoveMusicList.this,MusicPlay.class);//跳转到播放页面
        startActivity(intent);
    }

    //保存播放记录
    private void savePlayRecord(){
        //获取当前正在播放的音乐对象
        MusicInfo musicInfo = playService.getMusicInfos().get(playService.getCurrentPosition());
        try {
            MusicInfo playRecordMusicInfo = app.dbUtils.findFirst(Selector.from(MusicInfo.class).where("musicInfoId", "=", musicInfo.getMusicInfoId()));//查出歌曲
            if (playRecordMusicInfo==null){
                musicInfo.setMusicInfoId(musicInfo.getId());
                app.dbUtils.save(musicInfo);
            }
        }catch (DbException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.refurbish:
                initData();
                Log.i("INFO","已刷新");
                break;
            default:
                break;
        }
    }
}
