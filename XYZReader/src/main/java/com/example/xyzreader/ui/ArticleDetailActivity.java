package com.example.xyzreader.ui;

import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.util.Utility;

import java.util.Date;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    static final String EXTRA_CURSOR_POSITION = "cur_pos";

    private Cursor mCursor;
    private long mStartId;
    private int mStartPos;
    private String mTitle = " ";
    private boolean showTitle = false;

    private ViewPager mPager;
    private AppBarLayout mAppBar;
    private MyPagerAdapter mPagerAdapter;

    private CollapsingToolbarLayout mCollapsingToolbar;
    private DynamicHeightNetworkImageView mPhotoView;
    private LinearLayout mMetaBar;
    private TextView mTitleView;
    private TextView mBylineView;
    FloatingActionButton mFAB;

    private Typeface mContentFont;
    private boolean isTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }*/
        setContentView(R.layout.activity_article_detail);
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapse_toolbar);
        mPhotoView = (DynamicHeightNetworkImageView) findViewById(R.id.photo);
        mFAB = (FloatingActionButton) findViewById(R.id.share_fab);
        mMetaBar = (LinearLayout) findViewById(R.id.meta_bar);
        mTitleView = (TextView) findViewById(R.id.article_title);
        mBylineView = (TextView) findViewById(R.id.article_byline);
        mPager = (ViewPager) findViewById(R.id.pager);
        mCollapsingToolbar.setCollapsedTitleTextColor(Color.WHITE);
        mContentFont = Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf");
        isTablet = mCollapsingToolbar.findViewById(R.id.toolbar) == null;

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        mPager.setOffscreenPageLimit(1);

        findViewById(R.id.action_up).setOnClickListener(this);
        mFAB.setOnClickListener(this);
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        mAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            int scrollRange = -1;
            float scrollRangeF;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                    scrollRangeF = scrollRange;
                }
                int scroll = scrollRange + verticalOffset;
                if (scrollRange + verticalOffset < 70) {
                    if(showTitle) return;
                    mCollapsingToolbar.setTitle(mTitle);
                    mTitleView.setVisibility(View.INVISIBLE);
                    showTitle = true;
                } else if(showTitle) {
                    mTitleView.setVisibility(View.VISIBLE);
                    mCollapsingToolbar.setTitle(" ");
                    showTitle = false;
                }
                float alpha = scroll/scrollRangeF;
                mTitleView.setAlpha(alpha);
                mTitleView.setTextScaleX(alpha);
            }
        });
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mFAB.show();
                    mMetaBar.setAlpha(0f);
                    mMetaBar.setScaleY(0f);
                    mMetaBar.animate()
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(300);
                    mCursor.moveToPosition(position);
                    bindViews();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if(state == ViewPager.SCROLL_STATE_DRAGGING) {
                    mMetaBar.setAlpha(0f);
                    mMetaBar.setScaleY(0f);
                    mFAB.hide();
                } else {
                    mFAB.show();
                    mMetaBar.setAlpha(1f);
                    mMetaBar.setScaleY(1f);
                }
            }
        });

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent != null) {
                if(intent.getData() != null)mStartId = ItemsContract.Items.getItemId(intent.getData());
                mStartPos = intent.getIntExtra(EXTRA_CURSOR_POSITION, -1);
            }
        }
    }

    private void bindViews() {
        mTitle = mCursor.getString(ArticleLoader.Query.TITLE);
        mTitleView.setText(mTitle);
        if(showTitle) mCollapsingToolbar.setTitle(mTitle);
        String publishDateString = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
        Date publishedDate = Utility.parseDate(publishDateString);
        if (!Utility.beforeEpochTime(publishedDate)) {

            mBylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        } else {
            mBylineView.setText(Html.fromHtml(
                    Utility.dateFormat(publishedDate)
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        }
        ImageLoaderHelper.getInstance(this).getImageLoader()
                .get(mCursor.getString(ArticleLoader.Query.THUMB_URL), new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                        Bitmap bitmap = imageContainer.getBitmap();
                        if (bitmap != null) {
                            Palette p = Palette.generate(bitmap, 12);
                            int muteColor = p.getMutedColor(0xFF333333);
                            int darkMuteColor = p.getDarkMutedColor(0xFF333333);
                            mFAB.setBackgroundTintList(ColorStateList.valueOf(muteColor));
                            updateStatusBar(darkMuteColor);
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {}
                });
        mPhotoView.setImageUrl(
                mCursor.getString(ArticleLoader.Query.PHOTO_URL),
                ImageLoaderHelper.getInstance(this).getImageLoader());
    }

    private void updateStatusBar(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if(isTablet) {
            /*ArticleDetailFragment fragment = (ArticleDetailFragment) getFragmentManager()
                    .findFragmentByTag("android:switcher:" + R.id.pager + ":" + mPager.getCurrentItem());
            if(fragment != null)
            fragment.scrollUp();*/
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mAppBar.getLayoutParams();
            final AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
            if (behavior != null) {
                ValueAnimator valueAnimator = ValueAnimator.ofInt();
                valueAnimator.setInterpolator(new DecelerateInterpolator());
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        behavior.setTopAndBottomOffset((Integer) animation.getAnimatedValue());
                        mAppBar.requestLayout();
                    }
                });
                valueAnimator.setIntValues(0, -200);
                valueAnimator.setDuration(300);
                valueAnimator.setStartDelay(1000);
                valueAnimator.start();
            }
        }
    }

    public Typeface getContentFont() {
        return mContentFont;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.action_up:
                onBackPressed();
                break;
            case R.id.share_fab:
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
                break;
            default:
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        if(mStartPos >= 0 && mCursor.moveToPosition(mStartPos)) {
            mPager.setCurrentItem(mStartPos, false);
            bindViews();
            return;
        }
        if (mStartId > 0) {
            mCursor.moveToFirst();
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
            bindViews();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {
        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getString(ArticleLoader.Query.BODY));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
