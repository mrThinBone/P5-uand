package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
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

    private Cursor mCursor;
    private long mStartId;
    private String mTitle = " ";
    private boolean showTitle = false;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;

    private CollapsingToolbarLayout mCollapsingToolbar;
    private DynamicHeightNetworkImageView mPhotoView;
    private TextView mTitleView;
    private TextView mBylineView;

    private Typeface mContentFont;

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
        mTitleView = (TextView) findViewById(R.id.article_title);
        mBylineView = (TextView) findViewById(R.id.article_byline);
        mPager = (ViewPager) findViewById(R.id.pager);
        mCollapsingToolbar.setCollapsedTitleTextColor(Color.WHITE);
        mContentFont = Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf");

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        mPager.setOffscreenPageLimit(1);

        findViewById(R.id.action_up).setOnClickListener(this);
        findViewById(R.id.share_fab).setOnClickListener(this);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
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
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                /*mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);*/
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    bindViews();
                }
//                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
            }
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
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
                            updateStatusBar(bitmap);
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {}
                });
        mPhotoView.setImageUrl(
                mCursor.getString(ArticleLoader.Query.PHOTO_URL),
                ImageLoaderHelper.getInstance(this).getImageLoader());
    }

    private void updateStatusBar(Bitmap bmp) {
        if(bmp == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Palette p = Palette.generate(bmp, 12);
            int darkMuteColor = p.getDarkMutedColor(0xFF333333);

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(darkMuteColor);
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
                finish();
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

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
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
        public MyPagerAdapter(FragmentManager fm) {
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
