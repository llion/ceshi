package com.color.home.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.HashSet;
import java.util.Random;

public class SwitchableImageView extends ItemImageView {

	protected float switchingPercent = 0;
	protected SwitchingStyle switchingStyle = SwitchingStyle.HORIZONTAL_STYLE;
	private static final String TAG = "SwitchableImageView";
    private static final boolean DBG = false;


	public SwitchableImageView(Context context) {
		super(context);

		if (DBG){
			Log.d(TAG,"SwitchableImageView(Context context)");
		}
	}

	public SwitchableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
        //setBackgroundColor(Color.GREEN);
		if (DBG){
			Log.d(TAG,"SwitchableImageView(Context context, AttributeSet attrs)");
		}
	}

	public SwitchableImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (DBG){
			Log.d(TAG,"SwitchableImageView(Context context, AttributeSet attrs, int defStyle)");
		}
		//

	}

	public SwitchingStyle getSwitchingStyle() {
		return switchingStyle;
	}

	public void setSwitchingStyle(SwitchingStyle switchingStyle) {
		this.switchingStyle = switchingStyle;

        // Default.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null); //--此方法不行，黑屏...

		if (DBG){
			Log.d(TAG,"setSwitchingStyle(SwitchingStyle switchingStyle)");
		}
	}

	public float getSwitchingPercent() {
		return switchingPercent;
	}

	public void setSwitchingPercent(float switchingPercent) {
		if (DBG) {
			Log.d(TAG, "setSwitchingPercent = " + switchingPercent);
		}

		this.switchingPercent = switchingPercent;
		switchingPercentChanged();
	}

	protected void switchingPercentChanged() {
		switchingStyle.switchingPercentChanged(this);
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
        switchingStyle.beforeDraw(canvas, switchingPercent);//限制新图出来的形状

		super.onDraw(canvas);//画图
		switchingStyle.onDraw(this, canvas);//处理图
		if (DBG){
			Log.d(TAG," onDraw(Canvas canvas)");
		}
	}

	public static enum SwitchingStyle {

		HORIZONTAL_STYLE {
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (DBG){
                    Log.d(TAG," come in..HORIZONTAL_STYLE...........................");
                }
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}
				float h = canvas.getHeight() / 10.f;
				int dh ;
					dh = (int)(h * (1 - thiz.switchingPercent ));
                    if (DBG){
                        Log.d(TAG," SwitchingStyle..thiz.switchingPercent < 0.5f..dh="+dh);
                    }
				for (int i=0; i<10; i++) {
					canvas.drawRect(0, i*h, canvas.getWidth(), i*h+dh, CLEARING_PAINT);//原图显示部分变小
				}
			}
			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
			}
		},
		VERTICAL_STYLE {
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (DBG){
                    Log.d(TAG," come in..VERTICAL_STYLE...........................");
                }
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}
				float w = canvas.getWidth() / 10.f;
				int dw;
				dw = (int)(w * (1 - thiz.switchingPercent ));
				for (int i=0; i<10; i++) {
					canvas.drawRect((i+1)*w-dw, 0, (i+1)*w, canvas.getHeight(), CLEARING_PAINT);
				}
			}
			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
            }
		},
        UP_DOWN_CLOSE_STYLE {//上下闭合--中间矩形变小
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (DBG){
                    Log.d(TAG," come in..UPDOWNCLOSE_STYLE...........................");
                }
                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }else{
                    int h = canvas.getHeight() ;
                    int dh;
                    dh = (int)(h * (1 - thiz.switchingPercent));
                    canvas.drawRect(0, h/2 - dh/2, canvas.getWidth(), h/2 + dh/2, CLEARING_PAINT);

                    if (DBG){
                        Log.d(TAG," SwitchingStyle..UPDOWNCLOSE_STYLE..dh="+dh);
                    }
                }

            }
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
            }
        },
        LEFT_RIGHT_OPEN_STYLE {//左右OPEN--左右两矩形变小
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (DBG){
                    Log.d(TAG," come in..LEFT_RIGHT_OPEN_STYLE...........................");
                }
                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }
                float w = canvas.getWidth() ;
                int dw;
                dw = (int)(w * (  thiz.switchingPercent ));

                canvas.drawRect(0, 0, w/2 - dw/2, canvas.getHeight(), CLEARING_PAINT);
                canvas.drawRect(w/2 + dw/2, 0, w, canvas.getHeight(), CLEARING_PAINT);
        }
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
            }
        },
        UP_DOWN_OPEN_STYLE {//上下对开--上下两个矩形变小
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (DBG){
                    Log.d(TAG," come in..UPDOWNOPEN_STYLE...........................");
                }
                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }else{
                    int h = canvas.getHeight() ;
                    int dh;
                    dh = (int)(h * (thiz.switchingPercent));
                    canvas.drawRect(0,0, canvas.getWidth(), (h - dh)/2, CLEARING_PAINT);
                    canvas.drawRect(0,(h + dh)/2, canvas.getWidth(), h, CLEARING_PAINT);
                    if (DBG){
                        Log.d(TAG," SwitchingStyle..UPDOWNOPEN_STYLE..dh="+dh);
                    }
                }

            }
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }

            }
        },
        LEFT_RIGHT_CLOSE_STYLE {//左右关闭--中间矩形宽度变化
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (DBG){
                    Log.d(TAG," come in..LEFT_RIGHT_CLOSE_STYLE...........................");
                }
                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }
                float w = canvas.getWidth() ;
                int dw;
                dw = (int)(w * (1.0f - thiz.switchingPercent ));
                canvas.drawRect((w - dw)/2, 0, (w + dw)/2, canvas.getHeight(), CLEARING_PAINT);
            }

            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
            }
        },
		ALL_RIGHT_ROTA_STYLE {//360度右旋--扇形减小
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				if (DBG){
					Log.d(TAG," come in..ALL_RIGHT_ROTA_STYLE...........................");
				}
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}
				float w = canvas.getWidth() ;
				float h=canvas.getHeight();
				float diagonal= (float) Math.sqrt(w*w+h*h);//屏对角线长度
				RectF bigRect=new RectF((0 - (diagonal - w)/2),(0 - (diagonal - h)/2),(w + (diagonal - w)/2),(h + (diagonal - h)/2));//扇形所在的大矩形
				float sweeAngle=360*(1.0f-thiz.switchingPercent);//扇形扫过角度
				if (DBG){
					Log.d(TAG,"sweeAngle== "+sweeAngle);
				}
				canvas.drawArc(bigRect,-90,-sweeAngle,true,CLEARING_PAINT);
			}

			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
			}
		},
		ALL_LEFT_ROTA_STYLE {//360度左旋--扇形减小
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				if (DBG){
					Log.d(TAG," come in..ALL_LEFT_ROTA_STYLE...........................");
				}
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}
				float w = canvas.getWidth() ;
				float h=canvas.getHeight();
				float diagonal= (float) Math.sqrt(w*w+h*h);//屏对角线长度
				RectF bigRect=new RectF((0 - (diagonal - w)/2),(0 - (diagonal - h)/2),(w + (diagonal - w)/2),(h + (diagonal - h)/2));//扇形所在的大矩形
				float sweeAngle=360*(1.0f-thiz.switchingPercent);//扇形扫过角度--逆时针为正
				if (DBG){
					Log.d(TAG,"sweeAngle== "+sweeAngle);
				}
				canvas.drawArc(bigRect,-90,sweeAngle,true,CLEARING_PAINT);
			}

			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
			}
		},
		HALF_RIGHT_ROTA_STYLE {//180度右旋--扇形减小
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				if (DBG){
					Log.d(TAG," come in..HALF_RIGHT_ROTA_STYLE...........................");
				}
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}
				float w = canvas.getWidth() ;
				float h=canvas.getHeight();
				float diagonal= (float) Math.sqrt(w*w+h*h);//屏对角线长度
				RectF bigRect=new RectF((0 - (diagonal - w)/2),(0 - (diagonal - h)/2),(w + (diagonal - w)/2),(h + (diagonal - h)/2));//扇形所在的大矩形
				float sweeAngle=180*(1.0f-thiz.switchingPercent);//扇形扫过角度
				if (DBG){
					Log.d(TAG,"sweeAngle== "+sweeAngle);
				}
				canvas.drawArc(bigRect,-90,-sweeAngle,true,CLEARING_PAINT);
				canvas.drawArc(bigRect,90,-sweeAngle,true,CLEARING_PAINT);
			}

			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
				if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
				}
			}
		},
		HALF_LEFT_ROTA_STYLE {//180度左旋--扇形减小
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				if (DBG){
					Log.d(TAG," come in..HALF_LEFT_ROTA_STYLE...........................");
				}
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}
				float w = canvas.getWidth() ;
				float h=canvas.getHeight();
				float diagonal= (float) Math.sqrt(w*w+h*h);//屏对角线长度
				RectF bigRect=new RectF((0 - (diagonal - w)/2),(0 - (diagonal - h)/2),(w + (diagonal - w)/2),(h + (diagonal - h)/2));//扇形所在的大矩形
				float sweeAngle=180*(1.0f-thiz.switchingPercent);//扇形扫过角度
				if (DBG){
					Log.d(TAG,"sweeAngle== "+sweeAngle);
				}
				canvas.drawArc(bigRect,-90,sweeAngle,true,CLEARING_PAINT);
				canvas.drawArc(bigRect,90,sweeAngle,true,CLEARING_PAINT);
			}

			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
			}
		},
		QUARTER_RIGHT_ROTA_STYLE {//90度右旋--扇形减小
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				if (DBG){
					Log.d(TAG," come in..QUARTER_RIGHT_ROTA_STYLE...........................");
				}
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}
				float w = canvas.getWidth() ;
				float h=canvas.getHeight();
				float diagonal= (float) Math.sqrt(w*w+h*h);//屏对角线长度
				RectF bigRect=new RectF((0 - (diagonal - w)/2),(0 - (diagonal - h)/2),(w + (diagonal - w)/2),(h + (diagonal - h)/2));//扇形所在的大矩形
				float sweeAngle=90*(1.0f-thiz.switchingPercent);//扇形扫过角度
				if (DBG){
					Log.d(TAG,"sweeAngle== "+sweeAngle);
				}
				canvas.drawArc(bigRect,0,-sweeAngle,true,CLEARING_PAINT);
				canvas.drawArc(bigRect,180,-sweeAngle,true,CLEARING_PAINT);
				canvas.drawArc(bigRect,-90,-sweeAngle,true,CLEARING_PAINT);
				canvas.drawArc(bigRect,90,-sweeAngle,true,CLEARING_PAINT);
			}

			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
			}
		},
		QUARTER_LEFT_ROTA_STYLE {//90度右旋--扇形减小
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				if (DBG){
					Log.d(TAG," come in..QUARTER_LEFT_ROTA_STYLE...........................");
				}
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}
				float w = canvas.getWidth() ;
				float h=canvas.getHeight();
				float diagonal= (float) Math.sqrt(w*w+h*h);//屏对角线长度
				RectF bigRect=new RectF((0 - (diagonal - w)/2),(0 - (diagonal - h)/2),(w + (diagonal - w)/2),(h + (diagonal - h)/2));//扇形所在的大矩形
				float sweeAngle=90*(1.0f-thiz.switchingPercent);//扇形扫过角度
				if (DBG){
					Log.d(TAG,"sweeAngle== "+sweeAngle);
				}
				canvas.drawArc(bigRect,0,sweeAngle,true,CLEARING_PAINT);
				canvas.drawArc(bigRect,180,sweeAngle,true,CLEARING_PAINT);
				canvas.drawArc(bigRect,-90,sweeAngle,true,CLEARING_PAINT);
				canvas.drawArc(bigRect,90,sweeAngle,true,CLEARING_PAINT);
			}

			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
			}
		},
		CENTER_AROUND_RECT_STYLE {//中间向四周--上下左右四个矩形变小
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				if (DBG){
					Log.d(TAG," come in..CENTER_AROUND_RECT_STYLE...........................");
				}
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}else{
					int h = canvas.getHeight() ;
					int w=canvas.getWidth();
					int dh,dw;
					dh = (int)(h * (thiz.switchingPercent));
					dw = (int)(w * (thiz.switchingPercent));
					canvas.drawRect(0,0, w, (h - dh)/2, CLEARING_PAINT);//上矩形
					canvas.drawRect(0,(h + dh)/2, w, h, CLEARING_PAINT);//下矩形
					canvas.drawRect(0,0, (w - dw)/2, h, CLEARING_PAINT);//左矩形
					canvas.drawRect((w + dw)/2,0, w, h, CLEARING_PAINT);//右矩形
				}
			}
			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
			}
		},

        DIAMOND_CENTER_OUTER_STYLE {
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
            }

            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (thiz.switchingPercent>=1.0f){
                    return;
                }
                canvas.restore();//恢复
            }

            public void beforeDraw(Canvas canvas, float switchingPercent) {
                canvas.save();
                if (switchingPercent<1.0f){
                    int h = canvas.getHeight() ;
                    int w=canvas.getWidth();
                    int dh,dw;
                    dh = (int)(h * switchingPercent);
                    dw = (int)(w * switchingPercent);
                    Path path = new Path();
                    path.moveTo(w/2,(h/2 - dh));//上
                    path.lineTo((w/2 + dw),h/2);//右
                    path.lineTo(w/2,(h/2 + dh));//下
                    path.lineTo((w/2 - dw),h/2);//左
                    path.close();
                   // path.addRect(0, 0, 100, 100, Path.Direction.CW);
                    canvas.clipPath(path);
                }
            }
        },
        DIAMOND_AROUND_CENTER_STYLE {//四周向中间--菱形
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
            }

            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (thiz.switchingPercent>=1.0f){
                    return;
                }
               // canvas.save();
                int h = canvas.getHeight() ;
                int w=canvas.getWidth();
                int dh,dw;
                dh = (int)(h * (1.0f - thiz.switchingPercent));
                dw = (int)(w * (1.0f - thiz.switchingPercent));

                Path path = new Path();
                path.moveTo(w/2,(h/2 - dh));//上
                path.lineTo((w/2 + dw),h/2);//右
                path.lineTo(w/2,(h/2 + dh));//下
                path.lineTo((w/2 - dw),h/2);//左
                path.close();
                //canvas.clipPath(path);
                canvas.drawPath(path,CLEARING_PAINT);
            }
        },

		AROUND_CENTER_RECT_STYLE {//四周向中间--中间矩形变小
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				if (DBG){
					Log.d(TAG," come in..AROUND_CENTER_RECT_STYLE...........................");
				}
				if (thiz.switchingPercent >= 1.0f) {
					return;
				}else{
					int h = canvas.getHeight() ;
					int w=canvas.getWidth();
					int dh,dw;
					dh = (int)(h * (1 - thiz.switchingPercent));
					dw = (int)(w * (1 - thiz.switchingPercent));
					canvas.drawRect((w - dw)/2,(h - dh)/2, (w + dw)/2, (h + dh)/2, CLEARING_PAINT);//中间矩形

				}
			}
			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
			}
		},
        AROUND_CENTER_CROSS_STYLE {//四周向中间--十字形，中间十字架变小
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (DBG){
                    Log.d(TAG," come in..AROUND_CENTER_CROSS_STYLE...........................");
                }
                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }else{
                    int h = canvas.getHeight() ;
                    int w=canvas.getWidth();
                    int dh,dw;
                    dh = (int)(h * (thiz.switchingPercent));
                    dw = (int)(w * (thiz.switchingPercent));
                    canvas.drawRect(dw/2,0, (w - dw/2), h, CLEARING_PAINT);//
                    canvas.drawRect(0,dh/2, w, (h - dh/2), CLEARING_PAINT);//
                }
            }
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
            }
        },
        CENTER_AROUND_CROSS_STYLE {//中间向四周--十字架，中间十字架变大
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(LAYER_TYPE_NONE, null);
                }
            }

            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (thiz.switchingPercent>=1.0f){
                    return;
                }
                int h = canvas.getHeight() ;
                int w=canvas.getWidth();
                int dh,dw;
                dh = (int)(h * thiz.switchingPercent);
                dw = (int)(w * thiz.switchingPercent);
                canvas.drawRect(0,0,(w- dw)/2,(h- dh)/2,CLEARING_PAINT);//左上
                canvas.drawRect((w + dw)/2,0,w,(h- dh)/2,CLEARING_PAINT);//右上
                canvas.drawRect(0,(h + dh)/2,(w- dw)/2,h,CLEARING_PAINT);//左下
                canvas.drawRect((w + dw)/2,(h + dh)/2,w,h,CLEARING_PAINT);//右下
            }

        },
		MOZAIC_STYLE {
			@Override
			protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
				thiz.drawMosaic(canvas);
			}
			@Override
			protected void switchingPercentChanged(SwitchableImageView thiz) {
				if (thiz.switchingPercent >= 1.0f) {
					thiz.setLayerType(View.LAYER_TYPE_NONE, null);
				}
			}
		},
        //覆盖
        LEFT_COVER_STYLE {//左覆盖
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {

                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }
                float w = canvas.getWidth() ;
                int dw;
                dw = (int)(w * thiz.switchingPercent);
                canvas.drawRect(dw, 0, w, canvas.getHeight(), CLEARING_PAINT);
            }

            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        },
        RIGHT_COVER_STYLE {//右覆盖
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {

                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }
                float w = canvas.getWidth() ;
                int dw;
                dw = (int)(w * thiz.switchingPercent);
                canvas.drawRect(0, 0, (w - dw), canvas.getHeight(), CLEARING_PAINT);
            }

            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        },
        OVER_COVER_STYLE {//上覆盖
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {

                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }
                float h = canvas.getHeight();
                int dh;
                dh = (int)(h * thiz.switchingPercent);
                canvas.drawRect(0, dh, canvas.getWidth(), h, CLEARING_PAINT);
            }

            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        },
        UNDER_COVER_STYLE {//下覆盖
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {

                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }
                float h = canvas.getHeight();
                int dh;
                dh = (int)(h * thiz.switchingPercent);
                canvas.drawRect(0, 0, canvas.getWidth(), (h - dh), CLEARING_PAINT);
            }

            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        },
        LEFT_TOP_DIAGONAL_STYLE {//左上覆盖--斜线，右下三角形变小
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {

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
                path.lineTo(w,h);
                path.lineTo((2 * dw -w),h);
                path.close();
                canvas.drawPath(path,CLEARING_PAINT);
            }
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        },
        RIGHT_TOP_DIAGONAL_STYLE {//右上覆盖--斜线，左下三角形变小
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {

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
                path.lineTo(2 * (w - dw),h);
                path.lineTo(0,h);
                path.close();
                canvas.drawPath(path,CLEARING_PAINT);
            }
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        },
        LEFT_BOTTOM_DIAGONAL_STYLE {//左下覆盖--斜线
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {

                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * thiz.switchingPercent);
                dw = (int) (w * thiz.switchingPercent);
                Path path = new Path();
                path.moveTo((2 * dw - w),0);
                path.lineTo(w,0);
                path.lineTo(w,2 * (h - dh));
                path.close();
                canvas.drawPath(path,CLEARING_PAINT);
            }
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        },
        RIGHT_BOTTOM_DIAGONAL_STYLE {//右下覆盖--斜线
            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (thiz.switchingPercent >= 1.0f) {
                    return;
                }
                int h = canvas.getHeight();
                int w = canvas.getWidth();
                int dh, dw;
                dh = (int) (h * thiz.switchingPercent);
                dw = (int) (w * thiz.switchingPercent);
                Path path = new Path();
                path.moveTo(0,0);
                path.lineTo(2 * (w - dw),0);
                path.lineTo(0,2 * (h - dh));
                path.close();
                canvas.drawPath(path,CLEARING_PAINT);
            }
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        },
        LEFT_TOP_LINE_STYLE {//左上角覆盖--直线
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }

            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (thiz.switchingPercent>=1.0f){
                    return;
                }
                canvas.restore();//恢复
            }

            public void beforeDraw(Canvas canvas, float switchingPercent) {
                canvas.save();
                if (switchingPercent<1.0f){
                    int h = canvas.getHeight() ;
                    int w=canvas.getWidth();
                    int dh,dw;
                    dh = (int)(h * switchingPercent);
                    dw = (int)(w * switchingPercent);
                    Rect rect=new Rect(0,0,dw,dh);
                    canvas.clipRect(rect);

                }
            }
        },
        RIGHT_TOP_LINE_STYLE {//右上角覆盖--直线
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }

            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (thiz.switchingPercent>=1.0f){
                    return;
                }
                canvas.restore();//恢复
            }

            public void beforeDraw(Canvas canvas, float switchingPercent) {
                canvas.save();
                if (switchingPercent<1.0f){
                    int h = canvas.getHeight() ;
                    int w=canvas.getWidth();
                    int dh,dw;
                    dh = (int)(h * switchingPercent);
                    dw = (int)(w * switchingPercent);
                    Rect rect=new Rect((w - dw),0,dw,dh);
                    canvas.clipRect(rect);
                }
            }
        },
        LEFT_BOTTOM_LINE_STYLE {//左下角覆盖--直线
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }

            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (thiz.switchingPercent>=1.0f){
                    return;
                }
                canvas.restore();//恢复
            }

            public void beforeDraw(Canvas canvas, float switchingPercent) {
                canvas.save();
                if (switchingPercent<1.0f){
                    int h = canvas.getHeight() ;
                    int w=canvas.getWidth();
                    int dh,dw;
                    dh = (int)(h * switchingPercent);
                    dw = (int)(w * switchingPercent);
                    Rect rect=new Rect(0,(h - dh),dw,h);
                    canvas.clipRect(rect);
                }
            }
        },
        RIGHT_BOTTOM_LINE_STYLE {//右下角覆盖--直线
            @Override
            protected void switchingPercentChanged(SwitchableImageView thiz) {
                if (thiz.switchingPercent >= 1.0f) {
                    thiz.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }

            @Override
            protected void onDraw(SwitchableImageView thiz, Canvas canvas) {
                if (thiz.switchingPercent>=1.0f){
                    return;
                }
                canvas.restore();//恢复
            }

            public void beforeDraw(Canvas canvas, float switchingPercent) {
                canvas.save();
                if (switchingPercent<1.0f){
                    int h = canvas.getHeight() ;
                    int w=canvas.getWidth();
                    int dh,dw;
                    dh = (int)(h * switchingPercent);
                    dw = (int)(w * switchingPercent);
                    Rect rect=new Rect((w - dw),(h - dh),w,h);
                    canvas.clipRect(rect);
                }
            }
        },
		;

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

		protected abstract void switchingPercentChanged(SwitchableImageView thiz);
		protected abstract void onDraw(SwitchableImageView thiz, Canvas canvas);

        public void beforeDraw(Canvas canvas, float switchingPercent) {}
    }

	// 以下是马赛克相关的东西
	private int mosaicSize = 0;
	private int xCount = 0, yCount = 0;
	private HashSet<Integer> revealedMosaicSet = new HashSet<Integer>();
	private Random random = new Random();

	private void initMosaicData(int width, int height) {
		int[] xyCount = { 0, 0 };
		mosaicSize = calcMosaicSize(width, height, xyCount);
		xCount = xyCount[0];
		yCount = xyCount[1];
		revealedMosaicSet.clear();
	}

	private static int calcMosaicSize(int width, int height, int[] outXyCount) {
		int dimen = width > height ? width : height;
		int result = dimen / 10;
		if (outXyCount != null && outXyCount.length >= 2) {
			outXyCount[0] = Math.round((float)width / (float)result);
			outXyCount[1] = Math.round((float)height / (float)result);
		}
		return result;
	}

	private void drawMosaic(Canvas canvas) {
		if (switchingPercent == 0.0f) {
			initMosaicData(canvas.getWidth(), canvas.getHeight());
		}
		int expectedRevealedCount = Math.round(xCount * yCount * switchingPercent);
		while (revealedMosaicSet.size() < expectedRevealedCount) {
			revealOneMosaic();
		}
		for (int y=0; y < yCount; y++) {
			for (int x=0; x < xCount; x++) {
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
							x * mosaicSize + w, y * mosaicSize + h, SwitchingStyle.CLEARING_PAINT);
				}
			}
		}
	}

	private void revealOneMosaic() {
		int r = random.nextInt(xCount * yCount - revealedMosaicSet.size());
		for (int i=0; i < xCount * yCount; i++) {
			if (!revealedMosaicSet.contains(i)) {
				if (r == 0) {
					revealedMosaicSet.add(i);
					break;
				}
				r--;
			}
		}
	}
    //设置百叶窗每格宽度
    public static float setShadesCellWidth(int canvasHeight){
        float height=0.f;

        return height;
    }

}
