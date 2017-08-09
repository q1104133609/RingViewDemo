package com.example.mycircleview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangb on 2017/8/9.
 */

public class RingView extends ViewGroup {
    /**
     * //上一次滑动的坐标
     */
    private float mLastX;
    private float mLastY;
    /**
     * 检测按下到抬起时使用的时间
     */
    private long mDownTime;
    /**
     * 自动滚动线程
     */
    private AngleRunnable mAngleRunnable;
    /**
     * 检测按下到抬起时旋转的角度
     */
    private float mTmpAngle;
    /**
     * 每秒最大移动角度
     */
    private int mMax_Speed;
    /**
     * 如果移动角度达到该值，则屏蔽点击
     */
    private int mMin_Speed;
    /**
     * 圆的直径
     */
    private int mRadius;
    /**
     * 判断是否正在自动滚动
     */
    private boolean isMove;
    /**
     * 布局滚动角度
     */
    private int mStartAngle = 0;
    /**
     * 中间条的宽度
     */
    private int mCircleLineStrokeWidth;
    /**
     * 画圆所在的距形区域
     */
    private final RectF mRectF;
    /**
     * 画笔
     */
    private final Paint mPaint;
    /**
     * 外侧圆宽度
     */
    private int outCirWidth;
    /**
     * 内侧圆宽度
     */
    private int intCirWidth;
    /**
     * 图片内容偏移角度
     */
    private int mImageAngle;
    /**
     * 是否初始化布局
     */
    private boolean isChekc = false;
    /**
     * 外圆颜色
     */
    private int mOutColor;
    /**
     * 内圆颜色
     */
    private int mInColor;
    /**
     * 布局view
     */
    private List<Integer> mImageList = new ArrayList<>();
    /**
     * 是否可点击
     */
    private boolean isCanClick = true;

    /**
     * 图片与环之间的padding
     */
    private int mPadding;

    //是否能转动
    private boolean mCanScrool;

    public RingView(Context context) {
        this(context, null, 0);

    }

    public RingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public RingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //获取自定义控件设置的值
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ringview, 0, 0);
        mMax_Speed = array.getInteger(R.styleable.ringview_max_speed, 300);
        mMin_Speed = array.getInteger(R.styleable.ringview_min_speed, 3);
        outCirWidth = array.getInteger(R.styleable.ringview_out_circle_width, 10);
        intCirWidth = array.getInteger(R.styleable.ringview_in_circle_width, 20);
        mImageAngle = array.getInteger(R.styleable.ringview_image_angle, 0);
        mPadding = array.getInteger(R.styleable.ringview_image_padding, 0);
        mOutColor = array.getColor(R.styleable.ringview_out_circle_color, 0xffEFF0EB);
        mInColor = array.getColor(R.styleable.ringview_in_circle_color, 0xffEFF0EB);
        mCanScrool = array.getBoolean(R.styleable.ringview_can_scroll, true);
        //获取xml定义的资源文件
        TypedArray mList = context.getResources().obtainTypedArray(array.getResourceId(R.styleable.ringview_list, 0));
        int len = mList.length();
        if (len > 0) {
            for (int i = 0; i < len; i++)
                mImageList.add(mList.getResourceId(i, 0));
        } else {
            mImageList.add(R.mipmap.ic_launcher);
            mImageList.add(R.mipmap.ic_launcher);
            mImageList.add(R.mipmap.ic_launcher);
        }
        mList.recycle();
        array.recycle();
        mRectF = new RectF();
        mPaint = new Paint();
        imagelogo();
        /**
         *  因为默认不走ondraw 所以设置背景透明
         */
        setBackgroundColor(0x00000000);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!isChekc) {
            initView();
            mRadius = getWidth();
            isChekc = true;
        }

    }

    /**
     * 测量
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            this.measureChild(child, widthMeasureSpec, heightMeasureSpec);
            child.getMeasuredWidth();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = this.getWidth();
        int height = this.getHeight();

        if (width != height) {
            int min = Math.min(width, height);
            width = min;
            height = min;
        }
        // 设置画笔相关属性
        mPaint.setAntiAlias(true);
        mPaint.setColor(0xfffafafa);
        //设置画布颜色
        canvas.drawColor(Color.TRANSPARENT);
        //设置圆环宽度
        mPaint.setStrokeWidth(mCircleLineStrokeWidth);
        //设置为空心圆
        mPaint.setStyle(Paint.Style.STROKE);
        // 设置位置
        mRectF.left = mCircleLineStrokeWidth / 2; // 左上角x
        mRectF.top = mCircleLineStrokeWidth / 2; // 左上角y
        mRectF.right = width - mCircleLineStrokeWidth / 2; // 左下角x
        mRectF.bottom = height - mCircleLineStrokeWidth / 2; // 右下角y
        //画三个弧形
        mPaint.setColor(0xfff89326);
        canvas.drawArc(mRectF, -90, 120, false, mPaint);
        mPaint.setColor(0xff4daae7);
        canvas.drawArc(mRectF, 30, 120, false, mPaint);
        mPaint.setColor(0xff687df2);
        canvas.drawArc(mRectF, 150, 120, false, mPaint);
        //每根线的弧长
        int mVale = (15 / (int) (width / 2 * Math.PI / 180));
        //画三条分割线线
        mPaint.setColor(0xfffafafa);
        canvas.drawArc(mRectF, -90, mVale, false, mPaint);
        canvas.drawArc(mRectF, 30, mVale, false, mPaint);
        canvas.drawArc(mRectF, 150, mVale, false, mPaint);
        //画外圆
        mPaint.setColor(mOutColor);
        mPaint.setStrokeWidth(outCirWidth);
        canvas.drawCircle(width / 2, width / 2, (width - outCirWidth) / 2, mPaint);
        //画内圆
        mPaint.setStrokeWidth(intCirWidth);
        mPaint.setColor(mInColor);
        canvas.drawCircle(width / 2, width / 2, width / 2 - mCircleLineStrokeWidth, mPaint);
        // drawLogo(canvas);
    }


    /**
     * 用canvas 画图标
     *
     * @param canvas
     */
    private void drawLogo(Canvas canvas) {
        int width = this.getWidth();
        int height = this.getHeight();
        if (width != height) {
            int min = Math.min(width, height);
            width = min;
            height = min;
        }
        //图标半径
        int mContent = getWidth() / 2 - mCircleLineStrokeWidth / 2;
        //直接画图标
        for (int i = 1; i < mImageList.size(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),
                    mImageList.get(i));
            //获取圆点上 xy坐标 mAngle 为图片圆半径和大圆半径差值
            float x = (float) (width / 2 + mContent * Math.cos((360 / mImageList.size() * (i + 1) + mImageAngle) * Math.PI / 180)) - bitmap.getWidth() / 2;
            float y = (float) (height / 2 + mContent * Math.sin((360 / mImageList.size() * (i + 1) + mImageAngle) * Math.PI / 180)) - bitmap.getHeight() / 2;
            canvas.drawBitmap(bitmap, null, new RectF(x, y, x + bitmap.getWidth(), y + bitmap.getHeight()), new Paint());
            bitmap.recycle();
        }
    }

    /**
     * 排版布局
     */
    private void initView() {
        int width = this.getWidth();
        int height = this.getHeight();
        if (width != height) {
            int min = Math.min(width, height);
            width = min;
            height = min;
        }
        //图片摆放的圆弧半径
        mCircleLineStrokeWidth = getChildAt(0).getMeasuredHeight() + outCirWidth + mPadding;
        //计算图片圆的半径
        final int mContent = width / 2 - mCircleLineStrokeWidth / 2;
        for (int i = 0; i < getChildCount(); i++) {
            View child = this.getChildAt(i);
            //计算每个图片摆放的角度
            int mAnGle = 360 / mImageList.size() * (i + 1) + mImageAngle;
            //获取每个图片摆放的左上角的x和y坐标
            float left = (float) (width / 2 + mContent * Math.cos(mAnGle * Math.PI / 180)) - child.getMeasuredWidth() / 2;
            float top = (float) (height / 2 + mContent * Math.sin(mAnGle * Math.PI / 180)) - child.getMeasuredHeight() / 2;
            /**
             * 一四象限
             */
            if (getQuadrantByAngle(mAnGle) == 1 || getQuadrantByAngle(mAnGle) == 4) {
                child.setRotation(mAnGle - 270);
                /**
                 * 二三象限
                 */
            } else {
                child.setRotation(mAnGle + 90);
            }
            child.layout((int) left, (int) top, (int) left + child.getMeasuredWidth(), (int) top + child.getMeasuredHeight());
        }
    }


    /**
     * 添加子控件
     */
    private void imagelogo() {
        for (int i = 1; i < mImageList.size() + 1; i++) {
            //新建imageview
            ImageView mImageView = new ImageView(getContext());
            mImageView.setImageResource(mImageList.get(i - 1));
            mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mImageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            final int finalI = i;
            //添加点击事件
            mImageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isCanClick) {
                        Toast.makeText(getContext(),finalI + "   ---",Toast.LENGTH_SHORT).show();
                        if (mOnLogoItemClick != null)
                            mOnLogoItemClick.onItemClick(view, finalI - 1);
                    }

                }
            });
            //添加view
            addView(mImageView);
        }
        //添加view点击事件
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isCanClick) {
                }
            }
        });

    }

    /**
     * 触摸监听
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mCanScrool) {
            float x = event.getX();
            float y = event.getY();


            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    mLastX = x;
                    mLastY = y;
                    mDownTime = System.currentTimeMillis();
                    mTmpAngle = 0;

                    // 如果当前已经在快速滚动
                    if (isMove) {
                        // 移除快速滚动的回调
                        removeCallbacks(mAngleRunnable);
                        isMove = false;
                        return true;
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    /**
                     * 获得开始的角度
                     */
                    float start = getAngle(mLastX, mLastY);
                    /**
                     * 获得当前的角度
                     */
                    float end = getAngle(x, y);
                    Log.e("TAG", "start = " + start + " , end =" + end);
                    // 一四象限
                    if (getQuadrant(x, y) == 1 || getQuadrant(x, y) == 4) {
                        mStartAngle += end - start;
                        mTmpAngle += end - start;
                        //二三象限
                    } else {
                        mStartAngle += start - end;
                        mTmpAngle += start - end;
                    }
                    // 重新布局
                    getCheck();

                    break;
                case MotionEvent.ACTION_UP:
                    // 获取每秒移动的角度
                    float anglePerSecond = mTmpAngle * 1000
                            / (System.currentTimeMillis() - mDownTime);
                    // 如果达到最大速度
                    if (Math.abs(anglePerSecond) > mMax_Speed && !isMove) {
                        // 惯性滚动
                        post(mAngleRunnable = new AngleRunnable(anglePerSecond));
                        return true;
                    }

                    // 如果当前旋转角度超过minSpeed屏蔽点击
                    if (Math.abs(mTmpAngle) > mMin_Speed) {
                        return true;
                    }

                    break;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * 获取移动的角度
     */
    private float getAngle(float xTouch, float yTouch) {
        double x = xTouch - (mRadius / 2d);
        double y = yTouch - (mRadius / 2d);
        return (float) (Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
    }

    /**
     * 根据当前位置计算象限
     */
    private int getQuadrant(float x, float y) {
        int tmpX = (int) (x - mRadius / 2);
        int tmpY = (int) (y - mRadius / 2);
        if (tmpX >= 0) {
            return tmpY >= 0 ? 4 : 1;
        } else {
            return tmpY >= 0 ? 3 : 2;
        }

    }


    /**
     * 通过角度判断象限
     */
    private int getQuadrantByAngle(int angle) {
        if (angle <= 90) {
            return 4;
        } else if (angle <= 180) {
            return 3;
        } else if (angle <= 270) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 惯性滚动
     */
    private class AngleRunnable implements Runnable {

        private float angelPerSecond;

        public AngleRunnable(float velocity) {
            this.angelPerSecond = velocity;
        }

        public void run() {
            //小于20停止
            if ((int) Math.abs(angelPerSecond) < 20) {
                isMove = false;
                return;
            }
            isMove = true;
            // 滚动时候不断修改滚动角度大小
            mStartAngle += (angelPerSecond / 30);
            //逐渐减小这个值
            angelPerSecond /= 1.0666F;
            postDelayed(this, 30);
            // 重新布局
            getCheck();
        }
    }


    /**
     * 点击事件接口
     */
    public interface OnLogoItemClick {
        void onItemClick(View view, int pos);
    }

    private OnLogoItemClick mOnLogoItemClick;

    /**
     * 设置点击事件
     *
     * @param mOnLogoItemClick
     */
    public void addOnItemClick(OnLogoItemClick mOnLogoItemClick) {
        this.mOnLogoItemClick = mOnLogoItemClick;
    }


    /**
     * 旋转圆盘
     */
    private void getCheck() {
        mStartAngle %= 360;
        setRotation(mStartAngle);
    }

    /**
     * 设置是否可点击
     */
    public void setCanClick(boolean canClick) {
        isCanClick = canClick;
    }

}