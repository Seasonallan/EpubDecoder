package com.season.bookreader.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.season.bookreader.anim.AbsVerGestureAnimController;
import com.season.bookreader.bookmarks.BookMark;
import com.season.bookreader.bookmarks.BookMarkDatas;
import com.season.bookreader.digests.AbsTextSelectHandler;
import com.season.bookreader.view.AbsReadView;
import com.season.bookreader.view.BaseReadView2;
import com.season.bookreader.view.EpubReadView;
import com.season.bookreader.view.IReaderView;
import com.season.bookreader.view.PullRefreshLayout;
import com.season.lib.bookformats.BookInfo;
import com.season.lib.bookformats.Catalog;
import com.season.lib.bookformats.Chapter;
import com.season.lib.bookformats.FormatPlugin;
import com.season.lib.bookformats.PluginManager;
import com.season.lib.bookformats.epub.Resource;
import com.season.lib.os.SyncThreadPool;
import com.season.lib.text.html.CssProvider;
import com.season.lib.text.html.CssProvider.ICssLoader;
import com.season.lib.text.html.DataProvider;
import com.season.lib.text.html.HtmlParser.TagHandler;
import com.season.lib.text.html.ICssProvider;
import com.season.lib.util.EncryptUtils;
import com.season.lib.util.FileUtil;
import com.season.lib.util.LogUtil;
import com.season.bookreader.R;
import com.season.bookreader.fragment.CatalogView;
import com.season.bookreader.fragment.ReaderMenuPopWin;
import com.season.bookreader.model.Book;
import com.season.bookreader.tagspan.ExpandTagHandler;
import com.season.bookreader.tagspan.ReaderMediaPlayer;
import com.season.bookreader.view.BaseReadView;
import com.season.lib.util.ScreenUtil;
import com.season.lib.util.StatusBarUtil;
import com.season.lib.util.ToastUtil;

public class BaseReaderActivity extends Activity implements ReaderMediaPlayer.PlayerListener,
        IReaderView.IReadCallback, AbsTextSelectHandler.ITouchEventDispatcher,
        PullRefreshLayout.OnPullListener, PullRefreshLayout.OnPullStateListener, AbsVerGestureAnimController.IVertialTouchEventDispatcher {
	/**内容密钥*/
	private String secretKey = null;
	private static final String TAG = BaseReaderActivity.class.getSimpleName();
	private BaseReaderActivity this_ = this;
    private FrameLayout mReadContainerView;
    private BaseReadView mReadView;
	private CatalogView mCatalogView;
	private RelativeLayout mCatalogLay;
	private FormatPlugin mPlugin;
	private Book mBook;
	private SyncThreadPool mSyncThreadPool;
	private ReaderMenuPopWin mReaderMenuPopWin;
	private int toolbarLP;
	private int toolbarRP;
	private int toolbarTP;
	private int toolbarBP;
	private int screenWidth;
	private int screenHeight;
	private int toolTouchAreaW;
	private int toolTouchAreaH;
	private boolean isInit;
	private boolean hasAddBookMark = false;
	private ClickDetector mClickDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent == null) {
			finish();
			return;
		}
        new ScreenUtil(this).hideNavigationBar();
        StatusBarUtil.setColor(this, 0xff30302E);
       // StatusBarUtil.setTranslucentForCoordinatorLayout(this, 122);

        ReaderMediaPlayer.init(mDataProvider);
		mBook = new Book();
		mBook.setBookId("00000");
		mSyncThreadPool = new SyncThreadPool();
		DisplayMetrics dm = getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		toolTouchAreaW = screenWidth >> 1;
		toolTouchAreaH = screenHeight >> 1;
		toolbarLP = toolTouchAreaW >> 1;
		toolbarRP = screenWidth - toolbarLP;
		toolbarTP = toolTouchAreaH >> 1;
		toolbarBP = screenHeight - toolbarTP;
		setContentView(R.layout.activity_reader_lay);
        mReadContainerView = findViewById(R.id.read_view);
		mCatalogLay =  findViewById(R.id.content_lay);
		initReaderCatalogView();
		showReaderContentView();
		initClickDetector();
		init();
        initPullView();
		ReaderMediaPlayer.getInstance().addPlayerListener(this);

        overridePendingTransition(0, 0);
        LogUtil.e("status  onCreated");
	}

    private Animation mRotateUpAnimation;
    private Animation mRotateDownAnimation;
    private PullRefreshLayout mPullLayout;
    private TextView mActionText;
    private TextView mTimeText;
    private ImageView mBookMarkSignImage;
    private View mProgress;
    private View mActionImage;
    protected void initPullView() {
        mRotateUpAnimation = AnimationUtils.loadAnimation(this_,
                R.anim.rotate_up);
        mRotateDownAnimation = AnimationUtils.loadAnimation(this_,
                R.anim.rotate_down);

        mPullLayout = (PullRefreshLayout) findViewById(R.id.pull_container);
        mPullLayout.setOnActionPullListener(this);
        mPullLayout.setOnPullStateChangeListener(this);

        mProgress = findViewById(android.R.id.progress);
        mActionImage = findViewById(android.R.id.icon);
        mActionText = (TextView) findViewById(R.id.pull_note);
        mTimeText = (TextView) findViewById(R.id.refresh_time);
        mBookMarkSignImage = (ImageView) findViewById(R.id.iv_book_mark_sign);

        mTimeText.setText(R.string.note_not_update);
        mActionText.setText(R.string.note_pull_down);
    }
    private AbsVerGestureAnimController mAbsVerGestureAnimController = new AbsVerGestureAnimController();

	@Override
	protected void onDestroy() {
		super.onDestroy();
        LogUtil.e("status  onDestroy");
		if(mSyncThreadPool != null){
			mSyncThreadPool.destroy();
			ReaderMediaPlayer.getInstance().release();
			mReadView.onDestroy();
		}
	}

	private void initClickDetector(){
		mClickDetector = new ClickDetector(new ClickDetector.OnClickCallBack() {
			@Override
			public boolean onLongClickCallBack(MotionEvent event) {
				return false;
			}
			
			@Override
			public boolean onClickCallBack(MotionEvent ev) {
                LogUtil.e("key>> onClickCallBack "+ "11" );
				float x = ev.getX();
				float y = ev.getY();
				if (mReadView.dispatchClickEvent(ev)) {
                    LogUtil.e("key>> onClickCallBack "+ "22" );
					return true;
				} else if (x > toolbarLP && x < toolbarRP && 
						y > toolbarTP && y < toolbarBP) {
                    LogUtil.e("key>> onClickCallBack "+ "44" );
					showMenu();
					return true;
				}
				return false;
			}
			
			@Override
			public void dispatchTouchEventCallBack(MotionEvent event) {
                LogUtil.e("key>> onClickCallBack "+ "4444" );
				onTouchEvent(event);
			}
		},false);
	}
	
	private void initMenu() {
		mReaderMenuPopWin = new ReaderMenuPopWin(mReadView, this, mBook,
				new ReaderMenuPopWin.IActionCallback() {
					@Override
					public void onShowReaderCatalog() {
						showReaderCatalogView();
					}

					@Override
					public void onSaveUserBookmark() {
						// TODO Auto-generated method stub
					}

					@Override
					public void onGotoPage(int pageNum) {
						mReadView.gotoPage(pageNum, true);
					}

					@Override
					public void onGotoBuyBook() {
						// TODO Auto-generated method stub

					}

					@Override
					public void onDeleteUserBookmark() {
						// TODO Auto-generated method stub

					}

					@Override
					public boolean isNeedBuy() {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public int getPageNums() {
						return mReadView.getMaxReadProgress();
					}

					@Override
					public int getCurPage() {
						return mReadView.getCurReadProgress();
					}

					@Override
					public boolean canAddUserBookmark() {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public void gotoPreChapter() {
						mReadView.gotoPreChapter();
					}

					@Override
					public void gotoNextChapter() {
						mReadView.gotoNextChapter();
					}

					@Override
					public boolean hasPreChapter() {
						return mReadView.hasPreChapter();
					}

					@Override
					public boolean hasNextChapter() {
						return mReadView.hasNextChapter();
					}

					@Override
					public int getLayoutChapterProgress() {
						return mReadView.getLayoutChapterProgress();
					}

					@Override
					public int getLayoutChapterMax() {
						return mReadView.getLayoutChapterMax();
					}
				});
	}

	private void initReaderCatalogView() {
		mCatalogView = new CatalogView(this_, new CatalogView.IActionCallBack() {

			@Override
			public void showReaderContentView() {
				this_.showReaderContentView();
			}

			@Override
			public void selectCatalog(Catalog catalog) {
				mReadView.gotoChapter(mPlugin.getChapterIndex(catalog), true);
			}

			@Override
			public void reflashCurrentPageBookmark() {
				this_.reflashCurrentPageBookmark();
			}

			@Override
			public void onEditModeChange(boolean isEdit) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isTextSelectHandlEenabled() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isHasNetWork() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Catalog getCurrentCatalog() {
				return mReadView.getCurrentCatalog();
			}

			@Override
			public BookInfo getBookInfo() {
				return mPlugin.getBookInfo();
			}
		});
	}

	private void showMenu() {
		if (!mReaderMenuPopWin.isShowing() && !mCatalogLay.isShown()) {
			mReaderMenuPopWin.showAtLocation();
		}
	}

	private void dismissMenu() {
		if (mReaderMenuPopWin.isShowing()) {
			mReaderMenuPopWin.dismiss();
		}
	}

	protected void showReaderContentView() {
		if(mCatalogView != null && mCatalogView.isShown()){
			dismissReaderCatalogView();
		}
	}

	protected void showReaderCatalogView() {
		mReadView.onHideContentView();
		if (mCatalogView != null && mPlugin != null && mCatalogView.isShowing == false) {
			if (mCatalogView.getParent() == null) {
				mCatalogLay.addView(mCatalogView);
			}
			mCatalogView.setVisibility(View.VISIBLE);
			mCatalogView.isShowing = true;
			mCatalogLay.setVisibility(View.VISIBLE);
			mCatalogView.setCatalogData(mPlugin.getCatalog());
			Animation trans1 = new TranslateAnimation(
					Animation.ABSOLUTE, -screenWidth, Animation.ABSOLUTE,
					0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, 0.0f);
			trans1.setDuration(600);
			trans1.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					mCatalogView.setAnimation(null);
					mCatalogView.isShowing = false;
				}
			});
			mCatalogView.startAnimation(trans1);	
		}
	}
	
	protected void dismissReaderCatalogView() {
		if (mCatalogView != null && mCatalogView.isDismissing == false) {
			mCatalogView.isDismissing = true;
			mCatalogView.setVisibility(View.INVISIBLE);
			Animation trans1 = new TranslateAnimation(Animation.ABSOLUTE,
					0.0f, Animation.ABSOLUTE, -screenWidth,
					Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, 0.0f);
			trans1.setDuration(350);
			trans1.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationEnd(Animation animation) {
					mCatalogView.isDismissing = false;
					mReadView.setVisibility(View.VISIBLE);
					mCatalogLay.setVisibility(View.GONE);
				}
			});
			mCatalogView.startAnimation(trans1);
		}
	}

	protected void reflashCurrentPageBookmark() {
		if (Build.VERSION.SDK_INT >= 10) {
			// saveSystemBookmarkInUi();
		}
	};

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
        LogUtil.e("key>>dispatchKeyEvent  "+ "11" );
		if(mReadView.onActivityDispatchKeyEvent(event)){
			return true;
		}
        LogUtil.e("key>>dispatchKeyEvent  "+ "22" );
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
        LogUtil.e("key>>"+ "11" );

		if (!isInit) {
			return false;
		}
        LogUtil.e("key>>"+ "22" );
		if(mReadView.onActivityDispatchTouchEvent(ev)){
			return false;
		}
        LogUtil.e("key>>"+ "33" );
		if(mCatalogLay.isShown()){
			return super.dispatchTouchEvent(ev);
		}
        LogUtil.e("key>>"+ "44" );
        if(mReadView.handlerSelectTouchEvent(ev, this)){
            return false;
        }
        LogUtil.e("key>>"+ "55" );
		if(mClickDetector.onTouchEvent(ev)){
			return false;
		}
        LogUtil.e("key>>"+ "66" );
		return super.dispatchTouchEvent(ev);
	}

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
	public boolean onTouchEvent(MotionEvent ev) {
        LogUtil.e("key>>onTouchEvent  "+ "66" );
        if(isPullEnabled() && mAbsVerGestureAnimController.handlerTouch(ev, this)){
            return false;
        }
		return mReadView.handlerTouchEvent(ev);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("menu");// 必须创建一项
		return super.onCreateOptionsMenu(menu);
	}


	private String getBookFielPath(){
		String pathDir = getCacheDir() + File.separator;
        String path =pathDir + "book.epub";
		File fileDir = new File(pathDir);
		if(!fileDir.exists()){
			fileDir.mkdirs();
		}
		return path;
	}
	
	private void init() {
       new Thread() {
			int requestPageCharIndex = 0;
			int requestCatalogIndex = 0;
			@Override
			public void run() {
				try {
					InputStream is = getResources().openRawResource(R.drawable.book);
					mBook.setPath(getBookFielPath());
					if(!FileUtil.copyFileToFile(mBook.getPath(), is)){
                        LogUtil.e("status  error");
			        	finish();
			        	return;
					}
					mPlugin = PluginManager.instance().getPlugin(
							mBook.getPath(),secretKey);
				// 书籍信息
					final BookInfo bookInfo = mPlugin.getBookInfo();
					bookInfo.id = mBook.getBookId();
					mBook.setAuthor(bookInfo.author);
					mBook.setBookName(bookInfo.title);
					requestCatalogIndex = 0;
					if(requestCatalogIndex == 0 && requestPageCharIndex == 0){
						requestPageCharIndex = 0;
					}
					// 读章节信息
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
                            mReadView = new EpubReadView(BaseReaderActivity.this, mBook, BaseReaderActivity.this);
                            mReadContainerView.addView(mReadView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            isInit = true;
                            mReadView.onCreate(null);
							mCatalogView.setBookInfo(bookInfo.title, bookInfo.author);

                            initMenu();
							mReadView.onInitReaderInBackground(requestCatalogIndex, requestPageCharIndex, secretKey);
						}
					});
				} catch (Exception e) {
					LogUtil.e(TAG, e);
				}
			}
		}.start();
	}

    @Override
    public void onChapterChange(ArrayList<Catalog> chapters) {
        if(mCatalogView != null){
            mCatalogView.setCatalogData(chapters);
            mCatalogView.refreshCatalog();
        }
    }

    @Override
	public void onPageChange(int totalPageIndex,int max) {
		mReaderMenuPopWin.setJumpSeekBarProgress(totalPageIndex, max);
	}

    @Override
    public void onLayoutProgressChange(int progress, int max) {

    }


	@Override
	public void onNotPreContent() {
		ToastUtil.showToast(this, "已经是第一页了");// TODO
	}
	
	@Override
	public void onNotNextContent() {
		ToastUtil.showToast(this, "已经是最后一页了");// TODO
	}

    @Override
    public boolean checkNeedBuy(int catalogIndex, boolean isNeedBuy) {
        return false;
    }

    @Override
    public void showLoadingDialog(int resId) {

    }

    @Override
    public void hideLoadingDialog() {

    }

    @Override
    public boolean hasShowBookMark(int chapterId, int pageStart, int pageEnd) {
        return BookMarkDatas.getInstance().isPageMarked(chapterId, pageStart, pageEnd);
    }

    @Override
    public boolean setFreeStart_Order_Price(int feeStart, boolean isOrdered, String price, String limitPrice) {
        return false;
    }

    @Override
    public void setCebBookId(String cebBookId) {

    }

	@Override
	public void onPlayStateChange(int state, String voiceSrc) {
		if(state == ReaderMediaPlayer.STATE_COMPLETION){
			String newPlaySrc = null;
			if(!TextUtils.isEmpty(voiceSrc)){
				try {
					int start = voiceSrc.lastIndexOf("-") + 1;
					int end = voiceSrc.lastIndexOf(".");
					String indexStr = voiceSrc.substring(start, end);
					if(TextUtils.isDigitsOnly(indexStr)){
						int voiceIndex = Integer.valueOf(indexStr);
						if(voiceIndex >= 0){
							newPlaySrc = voiceSrc.substring(0, start) 
									+ (voiceIndex + 1)
									+ voiceSrc.substring(end, voiceSrc.length());
							if(!mDataProvider.hasData(newPlaySrc)){
								newPlaySrc = null;
							}
						}
					}
				} catch (Exception e) {}
			}
			if(newPlaySrc != null){
				ReaderMediaPlayer.getInstance().startVioce(newPlaySrc);
			}else{
				ReaderMediaPlayer.getInstance().stop();
			}
		}
	}
	
	@Override
	public void onProgressChange(long currentPosition, long maxPosition,
			String voiceSrc) {
	}
	
	private CssProvider mCssProvider = new CssProvider(new ICssLoader() {
		@Override
		public String load(String path) {
			try {
				Resource resource = mPlugin.findResource(path);
				byte[] data = null;
				if(resource != null){
					if (TextUtils.isEmpty(secretKey)) {
						data = resource.getData();
					}else {
						data = EncryptUtils.decryptByAES(resource.getData(),secretKey);
					}
				}
				if (data != null) {
					return new String(data);
				}
			} catch (Exception e) {
			}
			return null;
		}
	});
	
	private DataProvider mDataProvider = new DataProvider() {
		
		@Override
		public Drawable getDrawable(final String source,
				final DrawableContainer drawableContainer) {
			Drawable drawable = new ColorDrawable(Color.TRANSPARENT);
			mSyncThreadPool.addTask(new Runnable() {
				Bitmap bitmap = null;
				@Override
				public void run() {
					if(drawableContainer.isInvalid()){
						return;
					}
					try {
						InputStream is = getDataStream(source);
						if (is != null) {
							Options opts = new BitmapFactory.Options();
							opts.inPreferredConfig = Bitmap.Config.RGB_565;
							opts.inScaled = false;
							opts.inPurgeable = true;
							bitmap = BitmapFactory.decodeStream(is, null, opts);
						}
					} catch (Exception e) {
					}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							BitmapDrawable bitmapDrawable = null;
							if (bitmap != null) {
								bitmapDrawable = new BitmapDrawable(
										bitmap);
								bitmapDrawable
										.setTargetDensity(getResources()
												.getDisplayMetrics().densityDpi);
							}
							drawableContainer
									.setDrawable(bitmapDrawable);
						}
					});
				}
			});
			return drawable;
		}

		@Override
		public Context getContext() {
			return getApplicationContext();
		}

		@Override
		public InputStream getDataStream(String source) throws IOException {
			Resource resource = mPlugin.findResource(source);
			if(resource != null){
				if (TextUtils.isEmpty(secretKey)) {
					return resource.getDataStream();
				}else {
					return EncryptUtils.decryptByAES(resource.getDataStream(),secretKey);
				}
			}
			return null;
		}

		@Override
		public boolean hasData(String source) {
			Resource resource = mPlugin.findResource(source);
			return resource != null;
		}
	};

    @Override
    public void dispatchTouchEventCallBack(MotionEvent ev) {
        dispatchTouchEvent(ev);
    }




    /**
     * 当前页面是否书签
     */
    private boolean isCurrentBookMarked(){
        return BookMarkDatas.getInstance().findBookMarkPosition(mReadView.newUserBookmark()) != -1 ;
    }

    private boolean mInLoading = false;
    @Override
    public void onPullOut() {
        if (!mInLoading) {
            if(isCurrentBookMarked()){
                mBookMarkSignImage.setImageResource(R.drawable.book_mark_unsign);
                mActionText.setText(R.string.book_label_del_pull_refresh);
            }else{
                mBookMarkSignImage.setImageResource(R.drawable.book_mark_sign);
                mActionText.setText(R.string.book_label_add_pull_refresh);
            }
            mActionImage.clearAnimation();
            mActionImage.startAnimation(mRotateUpAnimation);
        }

    }

    @Override
    public void onPullIn() {
        if (!mInLoading) {
            if(isCurrentBookMarked()){
                mBookMarkSignImage.setImageResource(R.drawable.book_mark_sign);
                mActionText.setText(R.string.book_label_del_pull);
            }else{
                mBookMarkSignImage.setImageResource(R.drawable.book_mark_unsign);
                mActionText.setText(R.string.book_label_add_pull);
            }

            mActionImage.clearAnimation();
            mActionImage.startAnimation(mRotateDownAnimation);
        }
    }

    @Override
    public void onSnapToTop() {
        if (!mInLoading) {
            mInLoading = true;
            mPullLayout.setEnableStopInActionView(false);
            mActionImage.clearAnimation();
            mActionImage.setVisibility(View.GONE);
            mProgress.setVisibility(View.VISIBLE);
            if(isCurrentBookMarked()){
                mBookMarkSignImage.setImageResource(R.drawable.book_mark_unsign);
                mActionText.setText(R.string.book_label_del_pull_loading);
            }else{
                mBookMarkSignImage.setImageResource(R.drawable.book_mark_sign);
                mActionText.setText(R.string.book_label_add_pull_loading);
            }
            mHandler.sendEmptyMessage(MSG_LOADING);
            mTimeText.setText(this_.getString(R.string.note_update_at, getCurrentTimeByMDHM()));
        }

    }

    public static String getCurrentTimeByMDHM() {
        SimpleDateFormat var0 = new SimpleDateFormat("MM-dd HH:mm");
        Calendar var1 = Calendar.getInstance();
        return var0.format(var1.getTime());
    }

    private final static int MSG_LOADING = 0x1;
    private final static int MSG_LOADED = 0x2;
    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_LOADING:
                    //下拉后做的网路操作
                    if(isCurrentBookMarked()){
                        delBookLabel();
                    }else{
                        addBookLabel();
                    }
                    break;
                case MSG_LOADED:
                    dataLoaded();
                    break;

                default:
                    break;
            }
        };
    };

    /**
     * 添加书签
     */
    private void addBookLabel(){
        BookMark userMark = mReadView.newUserBookmark();
        if(BookMarkDatas.getInstance().addBookMark(userMark)){
            ToastUtil.showToast(this_, R.string.book_label_add_success);
            mReadView.getContentView().invalidate();
        }
        dataLoaded();
    }

    /**
     * 删除用户书签
     */
    private void delBookLabel(){
        BookMark userMark = mReadView.newUserBookmark();
        if(BookMarkDatas.getInstance().deleteBookMark(userMark)){
            ToastUtil.showToast(this_, R.string.book_label_del_success);
            mReadView.getContentView().invalidate();
        }
        dataLoaded();
    }
    /**刷新加载结束*/
    private void dataLoaded() {
        if (mInLoading) {
            mInLoading = false;
            mPullLayout.setEnableStopInActionView(false);
            mPullLayout.hideActionView();
            mActionImage.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.GONE);

            if (mPullLayout.isPullOut()) {
                mActionText.setText(R.string.note_pull_refresh);
                mActionImage.clearAnimation();
                mActionImage.startAnimation(mRotateUpAnimation);
            } else {
                mActionText.setText(R.string.note_pull_down);
            }
        }
    }
    @Override
    public void onShow() {

    }

    @Override
    public void onHide() {

    }

    @Override
    public boolean isPullEnabled() {
        return mReadView.getTextSelectHandler() != null;
    }

    @Override
    public void verticalTouchEventCallBack(MotionEvent ev) {
        mPullLayout.dispatchTouchEvent(ev);
    }

    @Override
    public void unVerticalTouchEventCallBack(MotionEvent ev) {
        onTouchEvent(ev);
    }

}
