package com.lrony.mread.presentation.book.detail;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lrony.mread.AppManager;
import com.lrony.mread.AppRouter;
import com.lrony.mread.R;
import com.lrony.mread.data.net.BookDetailPackage;
import com.lrony.mread.data.net.RecommendBooksPackage;
import com.lrony.mread.mvp.MvpActivity;
import com.lrony.mread.ui.help.BaseRecyclerAdapter;
import com.lrony.mread.ui.help.ProgressCancelListener;
import com.lrony.mread.ui.help.ProgressDialogHandler;
import com.lrony.mread.ui.help.ToolbarHelper;
import com.lrony.mread.ui.widget.ShapeTextView;
import com.lrony.mread.util.Constant;
import com.lrony.mread.util.ImageLoader;
import com.lrony.mread.util.ScreenUtil;
import com.lrony.mread.util.StringUtils;

import java.text.ParseException;

public class BookDetailActivity extends MvpActivity<BookDetailContract.Presenter> implements BookDetailContract.View, View.OnClickListener, ProgressCancelListener {

    private static final String TAG = "BookDetailActivity";

    private static final String K_EXTRA_BOOK = "book";

    private BookDetailPackage mBook;
    private RecommendBooksPackage mRecommendBooks;

    private RecommendBookAdapter mRecommendAdapter;

    private String mBookId;

    private ImageView mIvCover;
    private TextView mTvReadCount;
    private ShapeTextView mTvIsFinish;
    private TextView mTvAuthor;
    private TextView mTvWordCount;
    private TextView mTvCatalogTitle;
    private TextView mTvUpdateTime;
    private TextView mTvDescribe;
    private RecyclerView mRvRecommendBook;
    private TextView mTvWordCountCopyright;
    private TextView mTvCreateDateCopyright;

    private ProgressDialogHandler mDialogHandler;

    private boolean mInfoLoadOK = false;
    private boolean mRecommendLoadOK = false;

    public static Intent newIntent(Context context, String bookid) {
        Intent intent = new Intent(context, BookDetailActivity.class);
        intent.putExtra(K_EXTRA_BOOK, bookid);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(R.layout.activity_book_detail);
        getPresenter().start();

        initView();
        initListener();

        getPresenter().loadBookInfo(mBookId);
        getPresenter().loadRecommendBookList(mBookId);
    }

    @NonNull
    @Override
    public BookDetailContract.Presenter createPresenter() {
        return new BookDetailPresenter();
    }

    private void init() {
        mBookId = getIntent().getStringExtra(K_EXTRA_BOOK);
        Log.d(TAG, "init: " + mBookId);
    }

    private void initView() {
        Log.d(TAG, "initView");
        mDialogHandler = new ProgressDialogHandler(this, this, true);
        mDialogHandler.obtainMessage(ProgressDialogHandler.SHOW_PROGRESS_DIALOG).sendToTarget();

        mIvCover = findViewById(R.id.iv_cover);
        mTvReadCount = findViewById(R.id.tv_read_count);
        mTvIsFinish = findViewById(R.id.tv_is_finish);
        mTvAuthor = findViewById(R.id.tv_author);
        mTvWordCount = findViewById(R.id.tv_word_count);
        mTvCatalogTitle = findViewById(R.id.tv_catalog_title);
        mTvDescribe = findViewById(R.id.tv_describe);
        mTvWordCountCopyright = findViewById(R.id.tv_word_count_copyright);
        mTvCreateDateCopyright = findViewById(R.id.tv_create_date_copyright);
        mTvUpdateTime = findViewById(R.id.tv_update_time);
        mRvRecommendBook = findViewById(R.id.rv_recommend_book);
        mRvRecommendBook.setNestedScrollingEnabled(false);
        mRvRecommendBook.setLayoutManager(new GridLayoutManager(this, ScreenUtil.isLAndscape(this) ? 4 : 3));
        mRecommendAdapter = new RecommendBookAdapter(this, mRecommendBooks, ScreenUtil.isLAndscape(this) ? 8 : 6);
        mRvRecommendBook.setAdapter(mRecommendAdapter);
    }

    private void initListener() {
        Log.d(TAG, "initListener");
        bindOnClickLister(this, R.id.fl_add_bookcase, R.id.fl_download_book
                , R.id.fl_open_book, R.id.ll_book_detail_catalog, R.id.rl_recommend_more);

        mRecommendAdapter.setItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                AppRouter.showBookDetailActivity(BookDetailActivity.this, mRecommendBooks.getBooks().get(position).get_id());
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private void refreshBookInfo() {
        Log.d(TAG, "refreshBookInfo");
        if (mBook == null) return;

        ImageLoader.load(this, Constant.IMG_BASE_URL + mBook.getCover(), mIvCover);
        ToolbarHelper.initToolbar(this, R.id.toolbar, true, mBook.getTitle());
        mTvReadCount.setText(StringUtils.formatCount(mBook.getPostCount()) + "人看过");
        String isFinished = !mBook.isIsSerial() ? getString(R.string.bookdetail_finished) : getString(R.string.bookdetail_not_finished);
        mTvAuthor.setText(mBook.getMinorCate() + " | " + mBook.getAuthor());
        mTvIsFinish.setText(isFinished);
        mTvWordCount.setText(isFinished + " | " + StringUtils.formatCount(mBook.getWordCount()) + "字");
        mTvWordCountCopyright.setText(mTvWordCountCopyright.getText() + StringUtils.formatCount(mBook.getWordCount()) + "字");
        mTvCatalogTitle.setText("最新章节: " + mBook.getLastChapter());
        try {
            mTvUpdateTime.setText(StringUtils.formatSomeAgo(StringUtils.dealDateFormat(mBook.getUpdated())) + "更新");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        mTvCreateDateCopyright.setText(mTvCreateDateCopyright.getText().toString() + mBook.getRetentionRatio() + "%");
        mTvDescribe.setText(mBook.getLongIntro());

    }

    private void jugeCloseDialog() {
        Log.d(TAG, "jugeCloseDialog mInfoLoadOK: " + mInfoLoadOK + ",mRecommendLoadOK: " + mRecommendLoadOK);
        if (mInfoLoadOK == true && mRecommendLoadOK == true) {
            if (mDialogHandler != null) {
                mDialogHandler.obtainMessage(ProgressDialogHandler.DISMISS_PROGRESS_DIALOG).sendToTarget();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fl_add_bookcase:
                break;
            case R.id.fl_download_book:
                break;
            case R.id.fl_open_book:
                break;
            case R.id.ll_book_detail_catalog:
                break;
            case R.id.rl_recommend_more:
                AppRouter.showRecommendActivity(this, mBookId);
                break;
        }
    }

    @Override
    public void finshLoadBookInfo(BookDetailPackage book) {
        Log.d(TAG, "finshLoadBookInfo");
        mBook = book;
        refreshBookInfo();
        mInfoLoadOK = true;
        jugeCloseDialog();
    }

    @Override
    public void finshLoadRecommendBookList(RecommendBooksPackage books) {
        Log.d(TAG, "finshLoadRecommendBookList");
        mRecommendBooks = books;
        mRecommendAdapter.refresh(mRecommendBooks);
        mRecommendLoadOK = true;
        jugeCloseDialog();
    }

    @Override
    public void onCancelProgress() {
        Log.d(TAG, "onCancelProgress");
        if (mDialogHandler != null) {
            mDialogHandler.obtainMessage(ProgressDialogHandler.DISMISS_PROGRESS_DIALOG).sendToTarget();
        }
        AppManager.getInstance().finishActivity();
    }
}
