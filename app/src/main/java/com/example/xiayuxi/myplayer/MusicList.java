package com.example.xiayuxi.myplayer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;

import java.util.ArrayList;
import java.util.Map;
/**
 * 程序从一开始的MusicPlayer到MyMusicPlayer再到现在的Myplayer，已经是3.0坂本，但是功能仍然不够完善
 * 本程序为本地音乐播放器，能够自动获取本地音乐，并实现播放，具体播放功能在MusicPlay类中有说明
 * 音乐列表页面，首次进入打开列表页
 * 搜索本地音乐显示在列表中
 * 已实现的功能有显示本地音乐，点击列表中项目进入播放页面，搜索功能，收藏功能
 */
public class MusicList extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener, SearchView.OnQueryTextListener {
    private ListView my_music;
    private SearchView sv;
    private ImageView lovemusic;
    private ImageView renew;
    //private TextView empty;
    private ArrayList<MusicInfo> musicInfos;
    private ArrayList<MusicInfo> searchInfo = new ArrayList<MusicInfo>();
    private MusicListAdapter musicListAdapter;
    public PlayerApp app;//取出全局对象

    private ArrayList<Map<String, Object>> listems; //需要显示在listview里的信息

    private boolean isSearch = false;
    //protected int mposition = 0;//当前播放的位置,提供给播放页面

    private final int EXTERNAL_STORAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_list);

        //请求权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            //若需要向用户解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                //在这里向用户解释
                Log.i("APP","READ_EXTERNAL_STORAGE");
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        EXTERNAL_STORAGE_REQUEST);
            }else{
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        EXTERNAL_STORAGE_REQUEST);
            }
        }
        Intent intent = new Intent(this,PlayService.class);
        startService(intent);

        app = (PlayerApp) getApplication();

        //初始化控件
        my_music = (ListView)findViewById(R.id.musiclist);
        lovemusic = (ImageView) findViewById(R.id.lovemusic);
        renew = (ImageView) findViewById(R.id.renew);
        sv = (SearchView) findViewById(R.id.search);
        sv.setOnQueryTextListener(this);// 为该SearchView组件设置事件监听器
        lovemusic.setOnClickListener(this);
        renew.setOnClickListener(this);
        my_music.setTextFilterEnabled(true);
        //加载数据
        loadData();

    }

    @Override
    //权限请求的结果
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[]grantResults){
        switch (requestCode){
            case EXTERNAL_STORAGE_REQUEST:{
                //若请求被用户取消，grantResults数组为空
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i("INFO","Permission granted");
                }
            }
        }
    }

    @Override
    public void publish(int progress) {

    }

    @Override
    public void change(int progress) {
        loadData();
    }

    /**
     * 初始化数据,加载本地音乐列表
     */
    public void loadData(){
        isSearch = false;
        searchInfo = new ArrayList<MusicInfo>();
        musicInfos = MediaUtils.getMusicInfos(this);
        if (!musicInfos.isEmpty()) {
            Log.i("Info","NOT EMPTY");
            musicListAdapter = new MusicListAdapter(this, musicInfos);
            my_music.setAdapter(musicListAdapter);
            my_music.setOnItemClickListener(this);
        }else {
            Log.i("Info","IS EMPTY");
            //my_music.setEmptyView(empty);
            //empty.setText("无音乐");
            String [] empty = {"无音乐","请在本地添加"};
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,empty);
            my_music.setAdapter(adapter);
        }
        /*listems = new ArrayList<Map<String, Object>>();
        for (Iterator iterator = musicInfos.iterator(); iterator.hasNext();) {
            Map<String, Object> map = new HashMap<String, Object>();
            MusicInfo mp3Info = (MusicInfo) iterator.next();
            map.put("name", mp3Info.getTitle());
            map.put("artist", mp3Info.getArtist());
            map.put("duration", mp3Info.getDuration());
            listems.add(map);
        }

        SimpleAdapter mSimpleAdapter = new SimpleAdapter(
                this,
                listems,
                R.layout.music_item,
                new String[] {"name","artist","album","duration"},
                new int[] {R.id.title,R.id.singer,R.id.time}
        );
        my_music.setAdapter(mSimpleAdapter);*/
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //如果当前播放列表不是本地音乐列表
        if (isSearch){
            playService.setMusicInfos(searchInfo);
        }
        else if (playService.getChangePlayList() != PlayService.MY_MUSIC_LIST){
            playService.setMusicInfos(musicInfos);//播放列表切换为本地音乐列表
            playService.setChangePlayList(PlayService.MY_MUSIC_LIST);
            Log.i("Info","播放列表切换为本地列表");
        }
        Log.i("Info","是否为搜索"+isSearch);

        //mposition = position;
        //playService.setCurrentPosition(position);
        playService.play(position);
        Log.i("INFO","开始播放");
        MusicInfo musicInfo = musicInfos.get(position);
        Log.i("Info","本地列表:"+musicInfo);
        Log.i("INFO","list当前位置："+playService.getCurrentPosition());
        savePlayRecord(); //保存播放时间
        Intent intent = new Intent(MusicList.this,MusicPlay.class);//跳转到播放页面
        //intent.putExtra("position", position);
        //如果该Activity已经启动了，就不产生新的Activity，而是将现实的实例显示到前端
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    //保存播放记录
    private void savePlayRecord(){
        //获取当前正在播放的音乐对象
        MusicInfo MusicInfo = playService.getMusicInfos().get(playService.getCurrentPosition());
        try {
            MusicInfo playRecordmusicInfo = app.dbUtils.findFirst(Selector.from(MusicInfo.class).where("musicInfoId", "=", MusicInfo.getId()));//查出歌曲,SQL语句
            if (playRecordmusicInfo==null){
                MusicInfo.setMusicInfoId(MusicInfo.getId());
                app.dbUtils.save(MusicInfo);
            }
        }catch (DbException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.renew:
                loadData();
                break;
            case R.id.lovemusic://点击我的收藏
                Intent intent = new Intent(MusicList.this,LoveMusicList.class);//跳转到音乐收藏页面
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindPlayService();//绑定服务
        Log.i("INFO","绑定服务");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindPlayService();//解绑服务
        Log.i("INFO","解绑服务");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPlayService();//解绑服务
        //保存当前播放的一些状态值
        PlayerApp app = (PlayerApp) getApplication();
        SharedPreferences.Editor editor = app.sp.edit();

        editor.putInt("currentPosition", playService.getCurrentPosition());//保存当前正在播放的歌曲的位置

        editor.putInt("play_mode", playService.getPlay_mode());//保存播放模式

        editor.commit();//提交

        Log.i("INFO","已保存状态值");
    }

    @Override
    //搜索结果
    public boolean onQueryTextSubmit(String query) {
        //Toast.makeText(this, "搜索："+query, Toast.LENGTH_SHORT).show();
        if (!musicInfos.isEmpty()) {
            Log.i("INFO","音乐列表不空");
            if (query != null) {
                //ArrayList<MusicInfo> searchInfo = new ArrayList<MusicInfo>();
                for (MusicInfo info : musicInfos) {//历遍歌曲列表，找到匹配歌曲名或歌手的音乐
                    //Log.i("INFO","遍历音乐："+info);
                    /*if (info.getTitle().equals(query) || info.getArtist().equals(query)) {
                        searchInfo.add(info);
                    }*/
                    if (search(query,info.getTitle())||search(query,info.getArtist())){
                        searchInfo.add(info);
                    }
                }
                Log.i("INFO","符合搜索结果的："+searchInfo);
                if (searchInfo != null) {
                    //musicInfos = searchInfo;
                    //musicListAdapter.notifyDataSetChanged();//更新列表
                    musicListAdapter = new MusicListAdapter(this, searchInfo);
                    my_music.setAdapter(musicListAdapter);
                    my_music.setOnItemClickListener(this);
                    isSearch = true;
                    Log.i("INFO","列表已更新");
                }
                else {
                    Toast.makeText(this, "搜索："+query+" 无结果", Toast.LENGTH_SHORT).show();
                }
            } else if(!query.isEmpty()){
                Log.i("INFO","未输入关键字");
                loadData();
            }
        }
        sv.clearFocus();  //收起键盘
        sv.onActionViewCollapsed();    //收起SearchView视图
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
/*        if (TextUtils.isEmpty(newText)) {
            // 清除ListView的过滤
            my_music.clearTextFilter();
        } else {
            // 使用用户输入的内容对ListView的列表项进行过滤
            my_music.setFilterText(newText);
        }*/
        return false;
    }

    //搜索的模板文本是否匹配正文文本
    public static Boolean search(String pat, String txt) {
        int M = pat.length();
        int N = txt.length();

        // 用j来跟踪模板文本
        int j = 0;
        // 用i来跟踪正文文本
        for(int i = 0; i < N - M; i++) {
            // 依次匹配，一个字符一个字符的匹配
            // 如果匹配成功，就返回true
            for(j = 0; j < M; j++) {
                if (txt.charAt(i+j) != pat.charAt(j)) {
                    break;
                }
            }
            if (j == M) {
                return true;
            }
        }
        return false;
    }
}
