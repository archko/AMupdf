package cn.archko.pdf.widgets;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import cn.archko.mupdf.R;
import cn.archko.pdf.utils.Utils;

/**
 * @author: archko 2018/12/21 :17:23
 */
public class ListMoreView implements IMoreView {

    private ViewGroup mHostListView;

    private ViewGroup mRootView;
    private ImageView mProgressImage;
    private TextView mLoadingView;
    private Animation animation;
    private int state = IMoreView.STATE_NORMAL;
    private int layoutId = R.layout.item_list_more;
    private String[] text = {"加载更多信息", "正在加载...", "加载失败，点击重试", "没有更多了"};

    private int[] noneMoreDrawable;
    private int[] failedDrawable;

    public ListMoreView(ViewGroup listView) {
        mHostListView = listView;
    }

    public ListMoreView(ViewGroup mHostListView, int layoutId) {
        this.mHostListView = mHostListView;
        this.layoutId = layoutId;
    }

    public void setText(String[] text) {
        this.text = text;
    }

    public void setTextNormal(String textNormal) {
        text[0] = textNormal;
    }

    public void setTextLoading(String textLoading) {
        text[1] = textLoading;
    }

    public void setTextLoadFailed(String textLoadFailed) {
        text[2] = textLoadFailed;
    }

    public void setTextNoneMore(String textNoneMore) {
        text[3] = textNoneMore;
    }

    public void setLoadingText(String loadingText) {
        if (!TextUtils.isEmpty(loadingText)) {
            mLoadingView.setText(loadingText);
        }
    }

    public void setNoneMoreDrawable(int[] noneMoreDrawable) {
        this.noneMoreDrawable = noneMoreDrawable;
    }

    public void setFailedDrawable(int[] failedDrawable) {
        this.failedDrawable = failedDrawable;
    }

    public void showMoreView() {
        if (mRootView == null) {
            return;
        }
        //mRootView.setVisibility(View.VISIBLE);
        mRootView.setMinimumHeight(Utils.dipToPixel(mRootView.getContext(), 80f));
        mRootView.getLayoutParams().height = Utils.dipToPixel(mRootView.getContext(), 80f);
    }

    public void hideMoreView() {
        if (mRootView == null) {
            return;
        }
        //mRootView.setVisibility(View.GONE);
        mRootView.setMinimumHeight(0);
        mRootView.getLayoutParams().height = 0;
        mRootView.requestLayout();
    }

    public TextView getLoadingView() {
        return mLoadingView;
    }

    public ImageView getProgressImage() {
        return mProgressImage;
    }

    @Override
    public ViewGroup getLoadMoreView() {
        if (mRootView == null) {
            mRootView = (ViewGroup) LayoutInflater.from(mHostListView.getContext()).inflate(layoutId, mHostListView, false);
            mLoadingView = (TextView) mRootView.findViewById(R.id.txt_more);
            mProgressImage = (ImageView) mRootView.findViewById(R.id.img_progress);
            animation = AnimationUtils.loadAnimation(mHostListView.getContext(), R.anim.loading_animation);
            mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onLoadMore();
                }
            });
        }
        return mRootView;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setBackgroundRes(int resId) {
        ViewGroup loadMoreView = getLoadMoreView();
        if (loadMoreView != null) {
            loadMoreView.setBackgroundResource(resId);
        }
    }

    @Override
    public void onLoadMore() {
        // empty
    }

    @Override
    public void onLoadingStateChanged(int state) {
        this.state = state;
        switch (state) {
            case IMoreView.STATE_NORMAL:
                if (mProgressImage != null) {
                    mProgressImage.setVisibility(View.GONE);
                    mProgressImage.clearAnimation();
                }
                mLoadingView.setText(text[0]);
                break;
            case IMoreView.STATE_LOADING:
                if (mProgressImage != null) {
                    mProgressImage.setVisibility(View.VISIBLE);
                    mProgressImage.startAnimation(animation);
                }
                mLoadingView.setText(text[1]);
                break;
            case IMoreView.STATE_LOAD_FAIL:
                if (mProgressImage != null) {
                    mProgressImage.setVisibility(View.GONE);
                    mProgressImage.clearAnimation();
                }
                if (failedDrawable != null && failedDrawable.length == 4) {
                    mLoadingView.setCompoundDrawablesWithIntrinsicBounds(failedDrawable[0],
                            failedDrawable[1], failedDrawable[2], failedDrawable[3]);
                }
                mLoadingView.setText(text[2]);
                break;
            case IMoreView.STATE_NO_MORE:
                if (mProgressImage != null) {
                    mProgressImage.setVisibility(View.GONE);
                    mProgressImage.clearAnimation();
                }
                if (noneMoreDrawable != null && noneMoreDrawable.length == 4) {
                    mLoadingView.setCompoundDrawablesWithIntrinsicBounds(noneMoreDrawable[0],
                            noneMoreDrawable[1], noneMoreDrawable[2], noneMoreDrawable[3]);
                }
                mLoadingView.setText(text[3]);
                break;
            default:
                if (mProgressImage != null) {
                    mProgressImage.setVisibility(View.GONE);
                    mProgressImage.clearAnimation();
                }
                mLoadingView.setText(text[0]);
        }
    }
}
