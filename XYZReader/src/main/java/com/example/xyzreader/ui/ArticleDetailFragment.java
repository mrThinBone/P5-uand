package com.example.xyzreader.ui;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.transition.Slide;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.example.xyzreader.R;

import java.lang.ref.WeakReference;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements ObservableScrollView.OnScrollChangedListener {
//    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ARTICLE_CONTENT = "article_content";

    private ObservableScrollView mRootView;
    private String mContent;
    private TextViewLazyLoad textViewLazyLoad;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(String content) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_ARTICLE_CONTENT, content);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ARTICLE_CONTENT)) {
            mContent = getArguments().getString(ARG_ARTICLE_CONTENT);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = (ObservableScrollView) inflater.inflate(R.layout.fragment_article_detail, container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRootView.setCallbacks(this);
    }

    private void animate(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide(Gravity.BOTTOM);
            slide.addTarget(view);
            slide.setInterpolator(AnimationUtils.loadInterpolator(getActivity(), android.R.interpolator.linear_out_slow_in));
            slide.setDuration(300);
            getActivity().getWindow().setEnterTransition(slide);
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);


        bodyView.setTypeface(((ArticleDetailActivity)getActivity()).getContentFont());

        if (mContent != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);


            bodyView.setText(getString(R.string.content_loading));
            animate(bodyView);

            textViewLazyLoad = new TextViewLazyLoad(bodyView);
//            textViewLazyLoad.execute(mContent);
            // allow running in parallel
            textViewLazyLoad.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mContent);
        } else {
            bodyView.setText("N/A");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(textViewLazyLoad != null) {
            textViewLazyLoad.release();
            textViewLazyLoad = null;
        }
    }

    @Override
    public void onScrollChanged(int scrollY) {
        ArticleDetailActivity activity = (ArticleDetailActivity) getActivity();
        if(scrollY > 0) {
            activity.mFAB.show();
        } else {
            activity.mFAB.hide();
        }
    }

    private static class TextViewLazyLoad extends AsyncTask<String, CharSequence, Void> {

        private static final int LIMIT_LENGTH = 2000;
        private WeakReference<TextView> mRef;
        private boolean cleanup = false;

        public TextViewLazyLoad(TextView textView) {
            mRef = new WeakReference<>(textView);
        }

        @Override
        protected Void doInBackground(String... strings) {
            if(TextUtils.isEmpty(strings[0])) return null;

            SpannableStringBuilder strData = (SpannableStringBuilder) Html.fromHtml(strings[0].replaceAll("(\r\n|\n)", "<br />"));
            int length = strData.length();
            if(length <= LIMIT_LENGTH) {
                publishProgress(strData);
                return null;
            }

            int offset = 0;
            while(offset + LIMIT_LENGTH < length) {
                int end = offset + LIMIT_LENGTH;
                publishProgress(strData.subSequence(offset, end));
                offset = end;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            publishProgress(strData.subSequence(offset, length));
            return null;
        }

        @Override
        protected void onProgressUpdate(CharSequence... values) {
            super.onProgressUpdate(values);
            if(isCancelled()) return;
            TextView textView = mRef.get();
            if(!cleanup) {
                textView.setText("");
                cleanup = true;
            }
            mRef.get().append(values[0]);
        }

        void release() {
            cancel(true);
            mRef.clear();
            mRef = null;
        }
    }
}