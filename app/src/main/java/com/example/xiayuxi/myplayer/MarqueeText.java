package com.example.xiayuxi.myplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 实现音乐播放界面中歌曲名的跑马灯效果
 */
public class MarqueeText extends TextView {
    public MarqueeText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    public boolean isFocused() {
        //判断TextView是不是在一个被选中的状态上。
        return true;
    }
    public MarqueeText(Context context) {
        super(context);
    }
    public MarqueeText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
