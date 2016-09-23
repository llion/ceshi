package com.color.home.widgets;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import java.util.HashSet;
import java.util.Random;

/**
 * Created by Administrator on 2016/8/22.
 */
public enum EffectStyle {
    HORIZONTAL_STYLE {
        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            //float h = canvas.getHeight() / 10.f;
            int h = EffectView.getShadesCellSize(canvas.getHeight());
            int dh, count, remain;
            dh = (int) (h * (1 - thiz.switchingPercent));
            count = canvas.getHeight() / h;
            remain = canvas.getHeight() % h;

            for (int i = 0; i < count; i++) {
                if (i == (count - 1) && remain > 0) {
                    canvas.drawRect(0, i * h, canvas.getWidth(), i * h + dh + remain * (1 - thiz.switchingPercent), CLEARING_PAINT);
                } else
                    canvas.drawRect(0, i * h, canvas.getWidth(), i * h + dh, CLEARING_PAINT);//原图显示部分变小
            }

        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    VERTICAL_STYLE {
        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            //float w = canvas.getWidth() / 10.f;
            int w = EffectView.getShadesCellSize(canvas.getWidth());
            int dw, count, remain;
            dw = (int) (w * (1 - thiz.switchingPercent));
            count = canvas.getWidth() / w;
            remain = canvas.getWidth() % w;

            for (int i = 0; i < count; i++) {
                if (i == (count - 1) && remain > 0) {
                    canvas.drawRect(canvas.getWidth() - (dw + remain * (1 - thiz.switchingPercent)), 0, canvas.getWidth(), canvas.getHeight(), CLEARING_PAINT);
                } else
                    canvas.drawRect((i + 1) * w - dw, 0, (i + 1) * w, canvas.getHeight(), CLEARING_PAINT);
            }
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    UP_DOWN_CLOSE_STYLE {//上下闭合--中间矩形变小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            } else {
                int h = canvas.getHeight();
                int w = canvas.getWidth();

                int dh;
                dh = (int) (h * (1 - thiz.switchingPercent));
                canvas.drawRect(0, h / 2 - dh / 2, canvas.getWidth(), h / 2 + dh / 2, CLEARING_PAINT);
//					if (DBG)
//						Log.d(TAG,"left_top = ( 0,"+( h/2 - dh/2 )+"   right_bottom = ("+);

            }

        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    LEFT_RIGHT_OPEN_STYLE {//左右OPEN--左右两矩形变小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            int dw;
            dw = (int) (w * (thiz.switchingPercent));

            canvas.drawRect(0, 0, w / 2 - dw / 2, canvas.getHeight(), CLEARING_PAINT);
            canvas.drawRect(w / 2 + dw / 2, 0, w, canvas.getHeight(), CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    UP_DOWN_OPEN_STYLE {//上下对开--上下两个矩形变小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            } else {
                int h = canvas.getHeight();
                int dh;
                dh = (int) (h * (thiz.switchingPercent));
                canvas.drawRect(0, 0, canvas.getWidth(), (h - dh) / 2, CLEARING_PAINT);
                canvas.drawRect(0, (h + dh) / 2, canvas.getWidth(), h, CLEARING_PAINT);
            }

        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }

        }
    },
    LEFT_RIGHT_CLOSE_STYLE {//左右关闭--中间矩形宽度变化

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            int dw;
            dw = (int) (w * (1.0f - thiz.switchingPercent));
            canvas.drawRect((w - dw) / 2, 0, (w + dw) / 2, canvas.getHeight(), CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    ALL_RIGHT_ROTA_STYLE {//360度右旋--扇形减小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            float h = canvas.getHeight();
            float diagonal = (float) Math.sqrt(w * w + h * h);//屏对角线长度
            RectF bigRect = new RectF((0 - (diagonal - w) / 2), (0 - (diagonal - h) / 2), (w + (diagonal - w) / 2), (h + (diagonal - h) / 2));//扇形所在的大矩形
            float sweeAngle = 360 * (1.0f - thiz.switchingPercent);//扇形扫过角度
            canvas.drawArc(bigRect, -90, -sweeAngle, true, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    ALL_LEFT_ROTA_STYLE {//360度左旋--扇形减小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            float h = canvas.getHeight();
            float diagonal = (float) Math.sqrt(w * w + h * h);//屏对角线长度
            RectF bigRect = new RectF((0 - (diagonal - w) / 2), (0 - (diagonal - h) / 2), (w + (diagonal - w) / 2), (h + (diagonal - h) / 2));//扇形所在的大矩形
            float sweeAngle = 360 * (1.0f - thiz.switchingPercent);//扇形扫过角度--逆时针为正
            canvas.drawArc(bigRect, -90, sweeAngle, true, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    HALF_RIGHT_ROTA_STYLE {//180度右旋--扇形减小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            float h = canvas.getHeight();
            float diagonal = (float) Math.sqrt(w * w + h * h);//屏对角线长度
            RectF bigRect = new RectF((0 - (diagonal - w) / 2), (0 - (diagonal - h) / 2), (w + (diagonal - w) / 2), (h + (diagonal - h) / 2));//扇形所在的大矩形
            float sweeAngle = 180 * (1.0f - thiz.switchingPercent);//扇形扫过角度
            canvas.drawArc(bigRect, -90, -sweeAngle, true, CLEARING_PAINT);
            canvas.drawArc(bigRect, 90, -sweeAngle, true, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    HALF_LEFT_ROTA_STYLE {//180度左旋--扇形减小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            float h = canvas.getHeight();
            float diagonal = (float) Math.sqrt(w * w + h * h);//屏对角线长度
            RectF bigRect = new RectF((0 - (diagonal - w) / 2), (0 - (diagonal - h) / 2), (w + (diagonal - w) / 2), (h + (diagonal - h) / 2));//扇形所在的大矩形
            float sweeAngle = 180 * (1.0f - thiz.switchingPercent);//扇形扫过角度
            canvas.drawArc(bigRect, -90, sweeAngle, true, CLEARING_PAINT);
            canvas.drawArc(bigRect, 90, sweeAngle, true, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    QUARTER_RIGHT_ROTA_STYLE {//90度右旋--扇形减小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            float h = canvas.getHeight();
            float diagonal = (float) Math.sqrt(w * w + h * h);//屏对角线长度
            RectF bigRect = new RectF((0 - (diagonal - w) / 2), (0 - (diagonal - h) / 2), (w + (diagonal - w) / 2), (h + (diagonal - h) / 2));//扇形所在的大矩形
            float sweeAngle = 90 * (1.0f - thiz.switchingPercent);//扇形扫过角度
            canvas.drawArc(bigRect, 0, -sweeAngle, true, CLEARING_PAINT);
            canvas.drawArc(bigRect, 180, -sweeAngle, true, CLEARING_PAINT);
            canvas.drawArc(bigRect, -90, -sweeAngle, true, CLEARING_PAINT);
            canvas.drawArc(bigRect, 90, -sweeAngle, true, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    QUARTER_LEFT_ROTA_STYLE {//90度右旋--扇形减小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            float h = canvas.getHeight();
            float diagonal = (float) Math.sqrt(w * w + h * h);//屏对角线长度
            RectF bigRect = new RectF((0 - (diagonal - w) / 2), (0 - (diagonal - h) / 2), (w + (diagonal - w) / 2), (h + (diagonal - h) / 2));//扇形所在的大矩形
            float sweeAngle = 90 * (1.0f - thiz.switchingPercent);//扇形扫过角度
            canvas.drawArc(bigRect, 0, sweeAngle, true, CLEARING_PAINT);
            canvas.drawArc(bigRect, 180, sweeAngle, true, CLEARING_PAINT);
            canvas.drawArc(bigRect, -90, sweeAngle, true, CLEARING_PAINT);
            canvas.drawArc(bigRect, 90, sweeAngle, true, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    CENTER_AROUND_RECT_STYLE {//中间向四周--上下左右四个矩形变小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            } else {
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * (thiz.switchingPercent));
                dw = (int) (w * (thiz.switchingPercent));
                canvas.drawRect(0, 0, w, (h - dh) / 2, CLEARING_PAINT);//上矩形
                canvas.drawRect(0, (h + dh) / 2, w, h, CLEARING_PAINT);//下矩形
                canvas.drawRect(0, 0, (w - dw) / 2, h, CLEARING_PAINT);//左矩形
                canvas.drawRect((w + dw) / 2, 0, w, h, CLEARING_PAINT);//右矩形
            }
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },

    DIAMOND_CENTER_OUTER_STYLE {
        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            canvas.restore();//恢复
        }

        public void beforeDraw(Canvas canvas, float switchingPercent) {
            canvas.save();
            if (switchingPercent < 1.0f) {
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * switchingPercent);
                dw = (int) (w * switchingPercent);
                Path path = new Path();
                path.moveTo(w / 2, (h / 2 - dh));//上
                path.lineTo((w / 2 + dw), h / 2);//右
                path.lineTo(w / 2, (h / 2 + dh));//下
                path.lineTo((w / 2 - dw), h / 2);//左
                path.close();
                // path.addRect(0, 0, 100, 100, Path.Direction.CW);
                canvas.clipPath(path);
            }
        }
    },
    DIAMOND_AROUND_CENTER_STYLE {//四周向中间--菱形

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            // canvas.save();
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dh, dw;
            dh = (int) (h * (1.0f - thiz.switchingPercent));
            dw = (int) (w * (1.0f - thiz.switchingPercent));

            Path path = new Path();
            path.moveTo(w / 2, (h / 2 - dh));//上
            path.lineTo((w / 2 + dw), h / 2);//右
            path.lineTo(w / 2, (h / 2 + dh));//下
            path.lineTo((w / 2 - dw), h / 2);//左
            path.close();
            //canvas.clipPath(path);
            canvas.drawPath(path, CLEARING_PAINT);
        }
    },

    AROUND_CENTER_RECT_STYLE {//四周向中间--中间矩形变小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {

            if (thiz.switchingPercent >= 1.0f) {
                return;
            } else {
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * (1 - thiz.switchingPercent));
                dw = (int) (w * (1 - thiz.switchingPercent));
                canvas.drawRect((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2, CLEARING_PAINT);//中间矩形

            }
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    AROUND_CENTER_CROSS_STYLE {//四周向中间--十字形，中间十字架变小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            } else {
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * (thiz.switchingPercent));
                dw = (int) (w * (thiz.switchingPercent));
                canvas.drawRect(dw / 2, 0, (w - dw / 2), h, CLEARING_PAINT);//
                canvas.drawRect(0, dh / 2, w, (h - dh / 2), CLEARING_PAINT);//
            }
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    CENTER_AROUND_CROSS_STYLE {//中间向四周--十字架，中间十字架变大

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dh, dw;
            dh = (int) (h * thiz.switchingPercent);
            dw = (int) (w * thiz.switchingPercent);
            canvas.drawRect(0, 0, (w - dw) / 2, (h - dh) / 2, CLEARING_PAINT);//左上
            canvas.drawRect((w + dw) / 2, 0, w, (h - dh) / 2, CLEARING_PAINT);//右上
            canvas.drawRect(0, (h + dh) / 2, (w - dw) / 2, h, CLEARING_PAINT);//左下
            canvas.drawRect((w + dw) / 2, (h + dh) / 2, w, h, CLEARING_PAINT);//右下
        }

    },
    MOZAIC_STYLE {
        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            drawMosaic(canvas, thiz.switchingPercent);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    //覆盖
    LEFT_COVER_STYLE {//左覆盖

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {

            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            int dw;
            dw = (int) (w * thiz.switchingPercent);
            canvas.drawRect(dw, 0, w, canvas.getHeight(), CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    RIGHT_COVER_STYLE {//右覆盖

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {

            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float w = canvas.getWidth();
            int dw;
            dw = (int) (w * thiz.switchingPercent);
            canvas.drawRect(0, 0, (w - dw), canvas.getHeight(), CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    OVER_COVER_STYLE {//上覆盖

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {

            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float h = canvas.getHeight();
            int dh;
            dh = (int) (h * thiz.switchingPercent);
            canvas.drawRect(0, dh, canvas.getWidth(), h, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    UNDER_COVER_STYLE {//下覆盖

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {

            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            float h = canvas.getHeight();
            int dh;
            dh = (int) (h * thiz.switchingPercent);
            canvas.drawRect(0, 0, canvas.getWidth(), (h - dh), CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    LEFT_TOP_DIAGONAL_STYLE {//左上覆盖--斜线，右下三角形变小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {

            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dh, dw;
            dh = (int) (h * thiz.switchingPercent);
            dw = (int) (w * thiz.switchingPercent);
            Path path = new Path();
            path.moveTo(w, (2 * dh - h));
            path.lineTo(w, h);
            path.lineTo((2 * dw - w), h);
            path.close();
            canvas.drawPath(path, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    RIGHT_TOP_DIAGONAL_STYLE {//右上覆盖--斜线，左下三角形变小

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {

            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dh, dw;
            dh = (int) (h * thiz.switchingPercent);
            dw = (int) (w * thiz.switchingPercent);
            Path path = new Path();
            path.moveTo(0, (2 * dh - h));
            path.lineTo(2 * (w - dw), h);
            path.lineTo(0, h);
            path.close();
            canvas.drawPath(path, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    LEFT_BOTTOM_DIAGONAL_STYLE {//左下覆盖--斜线

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {

            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dh, dw;
            dh = (int) (h * thiz.switchingPercent);
            dw = (int) (w * thiz.switchingPercent);
            Path path = new Path();
            path.moveTo((2 * dw - w), 0);
            path.lineTo(w, 0);
            path.lineTo(w, 2 * (h - dh));
            path.close();
            canvas.drawPath(path, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    RIGHT_BOTTOM_DIAGONAL_STYLE {//右下覆盖--斜线

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dh, dw;
            dh = (int) (h * thiz.switchingPercent);
            dw = (int) (w * thiz.switchingPercent);
            Path path = new Path();
            path.moveTo(0, 0);
            path.lineTo(2 * (w - dw), 0);
            path.lineTo(0, 2 * (h - dh));
            path.close();
            canvas.drawPath(path, CLEARING_PAINT);
        }

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    },
    LEFT_TOP_LINE_STYLE {//左上角覆盖--直线

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            canvas.restore();//恢复
        }

        public void beforeDraw(Canvas canvas, float switchingPercent) {
            canvas.save();
            if (switchingPercent < 1.0f) {
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * switchingPercent);
                dw = (int) (w * switchingPercent);
                Rect rect = new Rect(0, 0, dw, dh);
                canvas.clipRect(rect);

            }
        }
    },
    RIGHT_TOP_LINE_STYLE {//右上角覆盖--直线

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {


            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            canvas.restore();//恢复
        }

        public void beforeDraw(Canvas canvas, float switchingPercent) {
            canvas.save();
            if (switchingPercent < 1.0f) {
                int h = canvas.getHeight();
                int w = canvas.getWidth();

                int dh, dw;
                dh = (int) (h * switchingPercent);
                dw = (int) (w * switchingPercent);
                Rect rect = new Rect((w - dw), 0, w, dh);
                canvas.clipRect(rect);
            }
        }
    },
    LEFT_BOTTOM_LINE_STYLE {//左下角覆盖--直线

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            canvas.restore();//恢复
        }

        public void beforeDraw(Canvas canvas, float switchingPercent) {
            canvas.save();
            if (switchingPercent < 1.0f) {
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * switchingPercent);
                dw = (int) (w * switchingPercent);
                Rect rect = new Rect(0, (h - dh), dw, h);
                canvas.clipRect(rect);
            }
        }
    },
    RIGHT_BOTTOM_LINE_STYLE {//右下角覆盖--直线

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            canvas.restore();//恢复
        }

        public void beforeDraw(Canvas canvas, float switchingPercent) {
            canvas.save();
            if (switchingPercent < 1.0f) {
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * switchingPercent);
                dw = (int) (w * switchingPercent);
                Rect rect = new Rect((w - dw), (h - dh), w, h);
                canvas.clipRect(rect);
            }
        }
    }, LEFT_TRANSLATE_STYLE {//左移

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dw;
            dw = (int) (w * thiz.switchingPercent);
            Rect rect = new Rect(0, 0, w - dw, h);
            canvas.drawRect(rect, CLEARING_PAINT);
        }
    }, RIGHT_TRANSLATE_STYLE {//右移

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dw;
            dw = (int) (w * thiz.switchingPercent);
            Rect rect = new Rect(dw, 0, w, h);
            canvas.drawRect(rect, CLEARING_PAINT);
        }
    }, UP_TRANSLATE_STYLE {//上移

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dh;
            dh = (int) (h * thiz.switchingPercent);
            Rect rect = new Rect(0, 0, w, h - dh);
            canvas.drawRect(rect, CLEARING_PAINT);
        }
    }, DOWN_TRANSLATE_STYLE {//下移

        @Override
        protected void switchingPercentChanged(EffectView thiz) {
            if (thiz.switchingPercent >= 1.0f) {
                thiz.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }

        @Override
        public void onDraw(EffectView thiz, Canvas canvas) {
            if (thiz.switchingPercent >= 1.0f) {
                return;
            }
            int h = canvas.getHeight();
            int w = canvas.getWidth();
            int dh;
            dh = (int) (h * thiz.switchingPercent);
            Rect rect = new Rect(0, dh, w, h);
            canvas.drawRect(rect, CLEARING_PAINT);
        }
    },;

    private static final Paint CLEARING_PAINT;
    private static final Paint NORMAL_PAINT;
    private static final Paint SRC_OVER_PAINT;

    static {
        // 预先创建，尽量别在onDraw中创建东西
        CLEARING_PAINT = new Paint();
        CLEARING_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        SRC_OVER_PAINT = new Paint();
        SRC_OVER_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        NORMAL_PAINT = new Paint();
        NORMAL_PAINT.setColor(Color.BLACK);
    }

    // 以下是马赛克相关的东西
    private int mosaicSize = 0;
    private int xCount = 0, yCount = 0;
    private HashSet<Integer> revealedMosaicSet = new HashSet<Integer>();
    private Random random = new Random();

    private void initMosaicData(int width, int height) {
        int[] xyCount = {0, 0};
        mosaicSize = calcMosaicSize(width, height, xyCount);
        xCount = xyCount[0];
        yCount = xyCount[1];
        revealedMosaicSet.clear();
    }

    private static int calcMosaicSize(int width, int height, int[] outXyCount) {
        int dimen = width > height ? width : height;
        int result = dimen / 10;
        if (outXyCount != null && outXyCount.length >= 2) {
            outXyCount[0] = Math.round((float) width / (float) result);
            outXyCount[1] = Math.round((float) height / (float) result);
        }
        return result;
    }

    public void drawMosaic(Canvas canvas, float switchingPercent) {
        if (switchingPercent == 0.0f) {
            initMosaicData(canvas.getWidth(), canvas.getHeight());
        }
        int expectedRevealedCount = Math.round(xCount * yCount * switchingPercent);
        while (revealedMosaicSet.size() < expectedRevealedCount) {
            revealOneMosaic();
        }
        for (int y = 0; y < yCount; y++) {
            for (int x = 0; x < xCount; x++) {
                int w = mosaicSize;
                int h = mosaicSize;
                if (x == xCount - 1) {
                    w = canvas.getWidth() - x * mosaicSize;
                }
                if (y == yCount - 1) {
                    h = canvas.getHeight() - y * mosaicSize;
                }
                if (!revealedMosaicSet.contains(x + y * xCount)) {
                    canvas.drawRect(x * mosaicSize, y * mosaicSize,
                            x * mosaicSize + w, y * mosaicSize + h, EffectStyle.CLEARING_PAINT);
                }
            }
        }
    }

    private void revealOneMosaic() {
        int r = random.nextInt(xCount * yCount - revealedMosaicSet.size());
        for (int i = 0; i < xCount * yCount; i++) {
            if (!revealedMosaicSet.contains(i)) {
                if (r == 0) {
                    revealedMosaicSet.add(i);
                    break;
                }
                r--;
            }
        }
    }


    protected abstract void switchingPercentChanged(EffectView thiz);

    public abstract void onDraw(EffectView thiz, Canvas canvas);

    public void beforeDraw(Canvas canvas, float switchingPercent) {
    }
}
