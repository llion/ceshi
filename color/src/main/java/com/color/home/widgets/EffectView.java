package com.color.home.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by Administrator on 2016/8/16.
 */
public class EffectView extends ImageView{

    protected float switchingPercent = 0;
    protected EffectStyle effectStyle = EffectStyle.HORIZONTAL_STYLE;
    private static final String TAG = "EffectView";
    private static final boolean DBG = false;
    private static final boolean DBG_PERCENT = false;
    public boolean effect2 = false;



    public EffectView(Context context) {
        super(context);
    }

    public EffectView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EffectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //

    public EffectStyle getEffectStyle() {
        return effectStyle;
    }

   

    public float getSwitchingPercent() {
        return switchingPercent;
    }

    public void setSwitchingPercent(float switchingPercent) {
        if (DBG_PERCENT) {
            Log.d(TAG, "setSwitchingPercent = " + switchingPercent);
        }

        this.switchingPercent = switchingPercent;
        switchingPercentChanged();
    }

    protected void switchingPercentChanged() {
        effectStyle.switchingPercentChanged(this);
        invalidate();
    }

//    @Override
//    protected void onDraw(Canvas canvas) {
//        effectStyle.beforeDraw(canvas, switchingPercent);//限制新图出来的形状
//
//        super.onDraw(canvas);//画图
//        effectStyle.onDraw(this, canvas);//处理图
//        if (DBG){
//            Log.d(TAG," onDraw(Canvas canvas)");
//        }
//    }

    public void setEffectStyle(int animationType) {
        this.effect2 = true;
        // Default.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        if (DBG){
            Log.d(TAG,"setEffectStyle(EffectStyle effectStyle)");
        }

        if (animationType == 2)//左覆盖
            this.effectStyle = EffectStyle.LEFT_COVER_STYLE;
        else if (animationType == 3)//右覆盖
            this.effectStyle = EffectStyle.RIGHT_COVER_STYLE;
        else if (animationType == 4)//上覆盖
            this.effectStyle = EffectStyle.OVER_COVER_STYLE;
        else if (animationType == 5)//下覆盖
            this.effectStyle = EffectStyle.UNDER_COVER_STYLE;
        else if (animationType == 6)//左上覆盖--斜线
            this.effectStyle = EffectStyle.LEFT_TOP_DIAGONAL_STYLE;
        else if (animationType == 7)//右上覆盖--斜线
            this.effectStyle = EffectStyle.RIGHT_TOP_DIAGONAL_STYLE;
        else if (animationType == 8)//左下覆盖--斜线
            this.effectStyle = EffectStyle.LEFT_BOTTOM_DIAGONAL_STYLE;
        else if (animationType == 9)//右下覆盖--斜线
            this.effectStyle = EffectStyle.RIGHT_BOTTOM_DIAGONAL_STYLE;
        else if (animationType == 10)//左上角覆盖--直线
            this.effectStyle = EffectStyle.LEFT_TOP_LINE_STYLE;
        else if (animationType == 11)//右上角覆盖--直线
            this.effectStyle = EffectStyle.RIGHT_TOP_LINE_STYLE;
        else if (animationType == 12)//左下角覆盖--直线
            this.effectStyle = EffectStyle.LEFT_BOTTOM_LINE_STYLE;
        else if (animationType == 13)//右下角覆盖--直线
            this.effectStyle = EffectStyle.RIGHT_BOTTOM_LINE_STYLE;
        else if (animationType == 14)//水平百叶
            this.effectStyle = EffectStyle.HORIZONTAL_STYLE;
        else if (animationType == 15)//垂直百叶
            this.effectStyle = EffectStyle.VERTICAL_STYLE;
        else if (animationType == 16) //左右对开
            this.effectStyle = EffectStyle.LEFT_RIGHT_OPEN_STYLE;
        else if (animationType == 17) //上下对开
            this.effectStyle = EffectStyle.UP_DOWN_OPEN_STYLE;
        else if (animationType == 18) //左右闭合
            this.effectStyle = EffectStyle.LEFT_RIGHT_CLOSE_STYLE;
        else if (animationType == 19) //上下闭合
            this.effectStyle = EffectStyle.UP_DOWN_CLOSE_STYLE;
        else if (animationType == 28 || animationType == 29 || animationType == 30)
            this.effectStyle = EffectStyle.MOZAIC_STYLE;
        else if (animationType == 32) //右旋360
            this.effectStyle = EffectStyle.ALL_RIGHT_ROTA_STYLE;
        else if (animationType == 33) //左旋360
            this.effectStyle = EffectStyle.ALL_LEFT_ROTA_STYLE;
        else if (animationType == 34) //右旋180
            this.effectStyle = EffectStyle.HALF_RIGHT_ROTA_STYLE;
        else if (animationType == 35) //左旋180
            this.effectStyle = EffectStyle.HALF_LEFT_ROTA_STYLE;
        else if (animationType == 36) //右旋90
            this.effectStyle = EffectStyle.QUARTER_RIGHT_ROTA_STYLE;
        else if (animationType == 37) //左旋90
            this.effectStyle = EffectStyle.QUARTER_LEFT_ROTA_STYLE;
        else if (animationType == 43) //中间向四周--矩形
            this.effectStyle = EffectStyle.CENTER_AROUND_RECT_STYLE;
        else if (animationType == 44) //四周向中间--矩形
            this.effectStyle = EffectStyle.AROUND_CENTER_RECT_STYLE;
        else if (animationType == 45) //中间向四周--菱形
            this.effectStyle = EffectStyle.DIAMOND_CENTER_OUTER_STYLE;
        else if (animationType == 46) //四周向中间--菱形
            this.effectStyle = EffectStyle.DIAMOND_AROUND_CENTER_STYLE;
        else if (animationType == 47) //中间向四周--十字
            this.effectStyle = EffectStyle.CENTER_AROUND_CROSS_STYLE;
        else if (animationType == 48) //四周向中间--十字
            this.effectStyle = EffectStyle.AROUND_CENTER_CROSS_STYLE;


    }
    
    //设置百叶窗每格宽度
    public static int getShadesCellSize(int totalSize){
        int height ;
        if (totalSize <= 64){
            height = 10;
        }else if (totalSize <= 128){
            height = 40;
        }else if (totalSize <= 512){
            height = 60;
        }else if (totalSize <= 1280){
            height = 80;
        }else{
            height = 100;
        }

        return height;
    }
    



}
