package com.example.xiayuxi.myplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * 音乐列表适配器
 * 在列表的每一行显示歌曲名、歌手、时长
 */
public class MusicListAdapter extends BaseAdapter {

    private Context ctx; //上下文对象引用
    private ArrayList<MusicInfo> musicInfos;//存放音乐信息引用的集合
    private MusicInfo musicInfo;		//音乐信息对象引用
    private int pos = -1;			//列表位置

    /**
     * 构造函数
     */
    public MusicListAdapter(Context ctx, ArrayList<MusicInfo> musicInfos){
        this.ctx = ctx;
        this.musicInfos = musicInfos;
    }

    public ArrayList<MusicInfo> getmusicInfos() {
        return musicInfos;
    }

    public void setmusicInfos(ArrayList<MusicInfo> musicInfos) {
        this.musicInfos = musicInfos;
    }


    @Override
    public int getCount() {
        return musicInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return musicInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if(convertView==null){
            convertView = LayoutInflater.from(ctx).inflate(R.layout.music_item,null);
            vh = new ViewHolder();
            vh.title = (TextView) convertView.findViewById(R.id.title);
            vh.singer = (TextView) convertView.findViewById(R.id.singer);
            vh.time = (TextView) convertView.findViewById(R.id.time);
            convertView.setTag(vh);//表示给View添加一个格外的数据，
        }else {
            vh = (ViewHolder)convertView.getTag();//将数据取出来
        }

        MusicInfo musicInfo = musicInfos.get(position);
        vh.title.setText(musicInfo.getTitle());//显示歌曲名
        vh.singer.setText(musicInfo.getArtist());//显示歌手
        vh.time.setText(MediaUtils.formatTime(musicInfo.getDuration()));//显示时长

        return convertView;
    }

    /**
     * 内部类，声明相应的控件引用
     */
    static class ViewHolder{
        //所有控件对象引用
        TextView title;//歌曲名
        TextView singer;//歌手
        TextView time;//时长
    }
}
