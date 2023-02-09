package mobile.indiatvshowz.desivideos;

import static mobile.indiatvshowz.utility.Utils.drawerFragmentPosition;
import static mobile.indiatvshowz.utility.Utils.lastVisitedFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import mobile.android.Utility.Classes.AppRater;
import mobile.android.Utility.Classes.DialogMaker;
import mobile.android.Utility.Classes.ProgressIndicator;
import mobile.android.Utility.Classes.UnCaughtException;
import mobile.android.sectionlistview.DrawerSectionListAdapter;
import mobile.android.sectionlistview.EntryItem;

import mobile.indiatvshowz.mores.MoreFragment;
import mobile.indiatvshowz.R;
import mobile.indiatvshowz.apis.GetCat;
import mobile.indiatvshowz.category.ChannelListFragment;
import mobile.indiatvshowz.featured.FeaturedFragment;
import mobile.indiatvshowz.help.HelpActivity;
import mobile.indiatvshowz.http.ConnectionDetector;
import mobile.indiatvshowz.http.Parser;
import mobile.indiatvshowz.movieschedule.ScheduleFragment;
import mobile.indiatvshowz.moviesfrag.MovieFragment;
import mobile.indiatvshowz.playlist.PlaylistFragment;
import mobile.indiatvshowz.search.SearchFragment;
import mobile.indiatvshowz.utility.BaseActionBarActivity;
import mobile.indiatvshowz.utility.Constant;
import mobile.indiatvshowz.utility.Debugger;
import mobile.indiatvshowz.utility.Globals;
import mobile.indiatvshowz.utility.NetworkUtil;
import mobile.indiatvshowz.utility.Utils;
import mobile.indiatvshowz.videolist.VideolistFragment;
import mobile.indiatvshowz.videolist.VideolistFragment.VideoType;
import mobile.android.sectionlistview.Item;


//To enable Inapp purchase plz uncomment this code in on create view.
//		set up IAB helper
//		getstartSetup(true);

//public class MainActivity extends InAppBillingActivity
public class MainActivity extends BaseActionBarActivity implements
		GetCat.GetCatListeners, DrawerSectionListAdapter.onRequestRefreshListener, OnItemClickListener{

	static Globals globals;
	private static final String SELECTED_DRAWER_ITEM = "com.drawer.selected.item";
	int mPosition = -1;


	public DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private LinearLayout mDrawer;
	
	public ActionBar actionbar;
	public static boolean isAppReady = false;

	DrawerSectionListAdapter mAdapter = null; 
	PrepareDrawerList prepareDrawerList;
	
	//For language Drop down
	boolean isLanguageChange = false;
	
	private boolean doubleBackToExitPressedOnce;
	ProgressIndicator mProgress;
	
	//google Analytics obj

	public static String TAG;
	
	//for Language change
	LinearLayout change_language;
	TextView ab_basemaps_title, ab_basemaps_subtitle;
	
	//bottom bar resources
	LinearLayout btnMore,btnFeatured,btnVideos,btnMovies,btnSchedule;
	String selectedVideoTitle = "";

	
	private BroadcastReceiver GCMRequestReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (mAdapter != null)
				mAdapter.notifyDataSetChanged();
		}
	};

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = NetworkUtil.getConnectivityStatusString(context);
            if (status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED){
                if (mAdapter != null)
                    mAdapter.notifyDataSetChanged();
            }
            else {
                if (mAdapter != null)
                    mAdapter.notifyDataSetChanged();
            }
        }
    };
	
	private BroadcastReceiver AppReadyReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(globals.isUpdateDialogNeeded()) {
				//initiate AppRate call to show Rate dialog.
		        AppRater.app_launched(MainActivity.this);
		        
		        //String newAppVersion = globals.getAppSettings().get(Constant.MM_SERVER_APP_VERSION);
                double newAppVersion = Double.parseDouble(globals.getAppSettings().get(Constant.MM_SERVER_APP_VERSION));
		        	Debugger.debugE(TAG, "App Version From Server : "+newAppVersion);
		        	Debugger.debugE(TAG, "Current App Version : "+globals.getApplicationVersion());
                    if (newAppVersion > globals.getApplicationVersion()){
                        showUpdateDialog(getString(R.string.app_name), Constant.MM_MSG_UPDATE_ALERT, true, newAppVersion);
		        }
		        globals.setUpdateDialogNeeded(false);
			}
			
			if(mDrawerList != null) {
				if(mAdapter != null) {
					mDrawerList.postDelayed(new Runnable() {
					    @Override
					    public void run() {
					    	mDrawerList.setSelection(0);
					    }
					},100);
				}
			}
			setNavigationListAdapter();
			Debugger.debugE(TAG, "Creating interstial ads");


			if(!isAppReady) {
				Debugger.debugE(TAG, "APP is Ready");
				//registerReceiver(mReceiver = GCMUtilities.mHandleMessageReceiver, new IntentFilter(GCMUtilities.DISPLAY_MESSAGE_ACTION));
			}
			isAppReady = true;
		}
	};


	

	ImageView iv_feature,iv_videos,iv_movie,iv_schedule,iv_more;
	TextView tv_tab_feature,tv_tab_videos,tv_tab_movie,tv_tab_schedule,tv_tab_more;

	public void tabClicker(ImageView ivRes,TextView tvRes){

		iv_feature.setColorFilter(getResources().getColor(R.color.white));
		iv_videos.setColorFilter(getResources().getColor(R.color.white));
		iv_movie.setColorFilter(getResources().getColor(R.color.white));
		iv_schedule.setColorFilter(getResources().getColor(R.color.white));
		iv_more.setColorFilter(getResources().getColor(R.color.white));

		tv_tab_feature.setTextColor(getResources().getColor(R.color.white));
		tv_tab_videos.setTextColor(getResources().getColor(R.color.white));
		tv_tab_movie.setTextColor(getResources().getColor(R.color.white));
		tv_tab_schedule.setTextColor(getResources().getColor(R.color.white));
		tv_tab_more.setTextColor(getResources().getColor(R.color.white));


		ivRes.setColorFilter(getResources().getColor(R.color.app_blue));
		tvRes.setTextColor(getResources().getColor(R.color.app_blue));
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MainActivity.this));
		globals = ((Globals)getApplicationContext());
		globals.setOrientation(MainActivity.this);
		globals.setSplasAds(false);

		setContentView(R.layout.main_layout);
		TAG = getClass().getName();

		iv_feature = findViewById(R.id.iv_feature);
		iv_videos = findViewById(R.id.iv_videos);
		iv_movie = findViewById(R.id.iv_movie);
		iv_schedule = findViewById(R.id.iv_schedule);
		iv_more = findViewById(R.id.iv_more);
		tv_tab_feature = findViewById(R.id.tv_tab_feature);
		tv_tab_videos = findViewById(R.id.tv_tab_videos);
		tv_tab_movie = findViewById(R.id.tv_tab_movie);
		tv_tab_schedule = findViewById(R.id.tv_tab_schedule);
		tv_tab_more = findViewById(R.id.tv_tab_more);

		tabClicker(iv_feature,tv_tab_feature);


		//set up IAB helper
//		getstartSetup(true);
		
		//Create Analytics object
		//analytics = new Analytics(MainActivity.this);
		
		//init start app


		// Register to receive messages.
		//Push notification reg-unreg request handler
		LocalBroadcastManager.getInstance(this).registerReceiver(GCMRequestReceiver, new IntentFilter("GCM-on-off-event"));
		//app ready and show rater & update dialog handler
		LocalBroadcastManager.getInstance(this).registerReceiver(AppReadyReceiver, new IntentFilter("App-Ready"));
		
		mPosition = (savedInstanceState==null) ? 0 : savedInstanceState.getInt(SELECTED_DRAWER_ITEM, 0);
//		doMainOpration(mPosition);
		
		actionbar = getSupportActionBar();

		// Getting a reference to the drawer listview
		mDrawerList = (ListView) findViewById(R.id.drawer_list);
		// Getting a reference to the sidebar drawer ( Title + ListView )
		mDrawer = (LinearLayout) findViewById(R.id.drawer);
		// Getting reference to DrawerLayout
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		// set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        change_language = (LinearLayout) findViewById(R.id.change_language_layout);
        ab_basemaps_title = (TextView) findViewById(R.id.ab_basemaps_title);
        ab_basemaps_subtitle = (TextView) findViewById(R.id.ab_basemaps_subtitle);


		btnMore = findViewById(R.id.btn_more);
		btnSchedule = findViewById(R.id.btn_schedule);
		btnVideos = findViewById(R.id.btn_videos);
		btnFeatured = findViewById(R.id.btn_featured);
		btnMovies = findViewById(R.id.btn_movies);


		//To save fragment state
		FragmentStateSaver fragmentStateSaver = new FragmentStateSaver(findViewById(R.id.content_frame), getSupportFragmentManager()) {
			@Override
			public Fragment getItem(int position) {
				switch (position) {
					case 2:
						actionbar.setTitle("Movies");
						return new MovieFragment();
					case 3:
						actionbar.setTitle("Schedule");
						return new ScheduleFragment();
					case 0:
						actionbar.setTitle("Featured");
						return new FeaturedFragment();
					case 1:
						actionbar.setTitle("Songs");
						Fragment fragmentva;
						if (lastVisitedFragment != null) {
							fragmentva = lastVisitedFragment;
						}else {
							//here we set default song fragment

							doVideosOpration(drawerFragmentPosition);
							mAdapter.setPositionSelected(drawerFragmentPosition);
							fragmentva = lastVisitedFragment;
						}
						return fragmentva;
					case 4:

						return new MoreFragment();
					default:
						return new MoreFragment();
				}
			}
		};

		//default featured category selected
		fragmentStateSaver.changeFragment(0);

		btnFeatured.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tabClicker(iv_feature,tv_tab_feature);

//					addFragment(new FeaturedFragment());
					actionbar.setTitle("Featured");
					actionbar.setDisplayHomeAsUpEnabled(false);
					mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
					fragmentStateSaver.changeFragment(0);

			}
		});
		btnVideos.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tabClicker(iv_videos,tv_tab_videos);

					actionbar.setTitle(selectedVideoTitle);
					actionbar.setDisplayHomeAsUpEnabled(true);
					mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
					fragmentStateSaver.changeFragment(1);

			}
		});
		btnMovies.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tabClicker(iv_movie,tv_tab_movie);

					actionbar.setTitle("Movies");
					actionbar.setDisplayHomeAsUpEnabled(false);
					mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
					fragmentStateSaver.changeFragment(2);
//					addFragment(new MovieFragment());


			}
		});
		btnSchedule.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tabClicker(iv_schedule,tv_tab_schedule);

					actionbar.setTitle("Schedule");
					fragmentStateSaver.changeFragment(3);
					mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
					actionbar.setDisplayHomeAsUpEnabled(false);
//					addFragment(new ScheduleFragment());

			}
		});
		btnMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tabClicker(iv_more,tv_tab_more);


					actionbar.setTitle("More");
					actionbar.setDisplayHomeAsUpEnabled(false);
					mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
					fragmentStateSaver.changeFragment(4);
					//addFragment(new MoreFragment());

			}
		});
        
        change_language.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLanguageSelectDialog();
			}
		});
        getCurrentLanguageIndex();
        
        // Creating a ToggleButton for NavigationDrawer with drawer event listener
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close){
        	
        	private CharSequence Title;

			/** Called when drawer is closed */
            public void onDrawerClosed(View view) {
            	
            	getCurrentLanguageIndex();
            	setActionBarArrowDependingOnFragmentsBackStack();
            	if(mPosition != -1 ) {
            		Item.Group currentGroup = ((EntryItem)prepareDrawerList.getItem(mPosition)).group;
            		if(currentGroup == Item.Group.Main)
            			doMainOpration(mPosition-1);
            		else if(currentGroup == Item.Group.Videos) {
						doVideosOpration(mPosition);
						fragmentStateSaver.removeFragment(1);
						fragmentStateSaver.changeFragment(1);
					}
            		else if(currentGroup == Item.Group.More)
            			doMoreOpration(mPosition - (globals.getCategoryList().size() + 4 + 3));
            		actionbar.setTitle(((EntryItem)prepareDrawerList.getItem(mAdapter.getSelectedPosition())).title);
            	} else 
            		actionbar.setTitle(Title);
            	
            	if(isLanguageChange) {
            		mAdapter.setPositionSelected(0);
            		if(globals.getGCM_DeviceToken() != null)
            			GCMRqgTask(true);
            		doMainOpration(0);
            		isLanguageChange = false;
            	}
            }

            /** Called when a drawer is opened */
            public void onDrawerOpened(View drawerView) {       
            	
            	getCurrentLanguageIndex();
            	Title = actionbar.getTitle();
            	actionbar.setTitle("Category");

            	mDrawerToggle.setDrawerIndicatorEnabled(true);
                mPosition = -1;
                globals.hideKeyboard(MainActivity.this);
            }
        };

		// Setting event listener for the drawer
	//	mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerLayout.addDrawerListener(mDrawerToggle);
		getSupportFragmentManager().addOnBackStackChangedListener(
				new FragmentManager.OnBackStackChangedListener() {
					@Override
					public void onBackStackChanged() {
						setActionBarArrowDependingOnFragmentsBackStack();
					}
				});
        
        // ItemClick event handler for the drawer items
        mDrawerList.setOnItemClickListener(this);
        
        // Enabling Up navigation
        actionbar.setDisplayHomeAsUpEnabled(true);     
        actionbar.setDisplayShowHomeEnabled(true);  
	}
	
	public void showLanguageSelectDialog() {
		
		ArrayList<HashMap<String, String>> language = globals.getLanguageList();
		int lcount = language.size();
		
		List<String> listItems = new ArrayList<String>();
		int currentIndex = 0;
		for (int i = 0; i < lcount; i++) {
			listItems.add(language.get(i).get(Constant.MM_LANGUAGE_NAME));
			if(language.get(i).get(Constant.MM_LANGUAGE_ID).equalsIgnoreCase(globals.getLanguageID())) {
				currentIndex = i;
			}
		}

		final CharSequence[] charSequenceItems = listItems.toArray(new CharSequence[listItems.size()]);
		
        ab_basemaps_title.setText(language.get(currentIndex).get(Constant.MM_LANGUAGE_NAME));
        ab_basemaps_subtitle.setText("Total Language: "+ language.size());
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Language");
		builder.setSingleChoiceItems(charSequenceItems, currentIndex, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				
				if(!globals.getLanguageList().get(position).get(Constant.MM_LANGUAGE_ID).equalsIgnoreCase(globals.getLanguageID())) {
					isLanguageChange = true;
					
					//recreate application and reset all saved values
					((Globals)getApplicationContext()).resetOnLanguageChange();
					//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_CHANGE_LANGUAGE, globals.getLanguageList().get(position).get(Constant.MM_LANGUAGE_NAME));
					globals.setLanguageID(globals.getLanguageList().get(position).get(Constant.MM_LANGUAGE_ID));
				}
				dialog.dismiss();
				mDrawerLayout.closeDrawer(mDrawer);
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public int getCurrentLanguageIndex() {
		
		int index = 0;
		ArrayList<HashMap<String, String>> languages = globals.getLanguageList();
		int lcount = languages.size();
		
		if(lcount > 1) {
			change_language.setVisibility(View.VISIBLE);
			for(index = 0; index < lcount; index++) {
				if(languages.get(index).get(Constant.MM_LANGUAGE_ID).equalsIgnoreCase(globals.getLanguageID())) {
					break;
				}
			}
			
	        ab_basemaps_title.setText(languages.get(index).get(Constant.MM_LANGUAGE_NAME));
	        ab_basemaps_subtitle.setText("Total Language: "+ lcount);
		} else {
			change_language.setVisibility(View.GONE);
		}
		
		if(mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
        return index;
	}
	
	public void setNavigationListAdapter() {

		Debugger.debugE("AAA","total items in drawer - "+globals.getCategoryList().size());
		prepareDrawerList = new PrepareDrawerList();
		if(mAdapter == null)
			mAdapter = new DrawerSectionListAdapter(this, prepareDrawerList.getDrawerList(globals.getCategoryList()), this);
		else
			mAdapter.refreshAdapter(prepareDrawerList.getDrawerList(globals.getCategoryList()));
		
		// Setting the adapter to the listView
		if(mDrawerList.getAdapter() == null)
			mDrawerList.setAdapter(mAdapter);
	}
	
	public void showUpdateDialog(String pTitle, final String pMsg, boolean isCancelable, final double newAppVersion)
	{
		//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_UPDATE_DIALOG, globals.getApplicationVersion()+" > "+ newAppVersion);
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	
		builder.setInverseBackgroundForced(true);
		builder.setCancelable(isCancelable);
		builder.setTitle(pTitle);
		builder.setMessage(pMsg);
		builder.setIcon(R.mipmap.ic_launcher);
		
		builder.setNegativeButton("Update Now",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_UPDATE_NOW_BUTTON, globals.getApplicationVersion()+" > "+ newAppVersion);
						if(ConnectionDetector.internetCheck(MainActivity.this))
							AppRater.rateNow(MainActivity.this);
					}
				});
	
		AlertDialog alert = builder.create();
		alert.show();
	}



	public void GCMRqgTask(boolean isRegRequest) {
		//globals.new SendGCMTokenRequest(isRegRequest).execute(isRegRequest);
	}
	
	@Override
	protected void onResume() {
		super.onResume();


        registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}
	
	@Override
	protected void onPause() {
		super.onPause();


        unregisterReceiver(receiver);
	}
    
    @Override
	public void onBackPressed() {
    	if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
        	mDrawerLayout.closeDrawer(GravityCompat.START);
    	} else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
        	super.onBackPressed();
            return;
        } else {
	    	if (doubleBackToExitPressedOnce) {
	    		((Globals)this.getApplicationContext()).clearCache();

	    		//startAppAd.onBackPressed();
	            super.onBackPressed();
	            return;
	        }
	        this.doubleBackToExitPressedOnce = true;
	        Toast.makeText(this, Constant.Exit_message, Toast.LENGTH_SHORT).show();
	        
	        new Handler().postDelayed(new Runnable() {
	            @Override
	            public void run() {
	             doubleBackToExitPressedOnce=false;   
	            }
	        }, 2000);
        }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Unregister since the activity is about to be closed.
		isAppReady = false;
		LocalBroadcastManager.getInstance(this).unregisterReceiver(GCMRequestReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(AppReadyReceiver);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//save current showing position
		outState.putInt(SELECTED_DRAWER_ITEM, mPosition);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}
	
//	@Override
//	public boolean onNavigationItemSelected(int position, long arg1) {
//		if(!isFirstTime) {
//			isLanguageChange = true;
//			//recreate application and reset all saved values
//			mDrawerLayout.closeDrawer(mDrawer);
//	analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_CHANGE_LANGUAGE, globals.getLanguageList().get(position).get(Constant.MM_LANGUAGE_NAME));
//	}
//		globals.setLanguageID(globals.getLanguageList().get(position).get(Constant.MM_LANGUAGE_ID));
//      isFirstTime = false;
//		spinnerAdapter.refresh();
//      return false;
//	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		EntryItem item = (EntryItem) prepareDrawerList.getItem(position);
		
		if(item.group == Item.Group.VideoMore) {
			if(mAdapter.isAllActive()) {
				mAdapter.setAllActive(false);
				mDrawerList.smoothScrollToPosition(4);
			} else 
				mAdapter.setAllActive(true);
			mAdapter.notifyDataSetChanged();
		} else if (item.group == Item.Group.Notification) {

            ToggleButton btn_toggle = (ToggleButton)findViewById(R.id.btn_toggle_notification);

            Debugger.debugE(TAG, "Notification click");
		} else if (item.group == Item.Group.More){
			mPosition = position;
			mDrawerLayout.closeDrawer(mDrawer);
		} else if(mAdapter.getSelectedPosition() != position) {
			mAdapter.setPositionSelected(position);
			mPosition = position;
			mDrawerLayout.closeDrawer(mDrawer);
		}
	}
	
	private void setActionBarArrowDependingOnFragmentsBackStack() {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        boolean shouldEnableDrawerIndicator = backStackEntryCount == 0;
        mDrawerToggle.setDrawerIndicatorEnabled(shouldEnableDrawerIndicator);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		
        case android.R.id.home:
        	if (mDrawerToggle.isDrawerIndicatorEnabled() && mDrawerToggle.onOptionsItemSelected(item)) {
    	        return true;
    	    } else if (item.getItemId() == android.R.id.home && getSupportFragmentManager().popBackStackImmediate()) {
    	        return true;
    	    } 
        	
        default:
        	return super.onOptionsItemSelected(item);
		}
	}

	public void getCategory() {
		//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_REFRESH_CATEGORY, null);
		String URL = getResources().getString(R.string.ServerName)+ String.format(getResources().getString(R.string.URL_CategoryList), globals.getLanguageID(), globals.getApplicationVersion(), globals.getApplicationPackageName());
		if (ConnectionDetector.internetCheck(MainActivity.this)) {
			/*GetCategory task = new GetCategory(MainActivity.this, this);
			task.execute(URL);*/
            GetCat getCat = new GetCat(MainActivity.this, this);
            getCat.getResult(URL);
		}
	}

	/*@Override
	public void onGetCategorySucceed(JSONArray jCategory, JSONArray jFeatured, JSONObject jAppSetting) throws JSONException {
		globals.setAppSettings(Parser.getAppSettings(jAppSetting));
		globals.setCategoryList(Parser.getCategoryList(jCategory));
//		Extractor.getInstance(MainActivity.this, null).getVersion();
		setNavigationListAdapter();
	}

	@Override
	public void onGetCategoryFaild(JSONObject error) throws JSONException {
		
		HashMap<DialogKeys, String> params = new HashMap<DialogMaker.DialogKeys, String>();
		params.put(DialogKeys.btnPositive, "OK");
		params.put(DialogKeys.sMessage, error.getString(Constant.MM_API_ERROR_INFO));
		params.put(DialogKeys.sTitle, Constant.MM_ALERT_TITLE_ERROR);
		params.put(DialogKeys.isCancelable, "true");
		
		new DialogMaker(MainActivity.this, params, DialogType.NEUTRAL);
	}*/

	@Override
	public void doRefreshDrawerList() {
		getCategory();
	}
	
	public void doMainOpration(int position){
		globals.hideKeyboard(MainActivity.this);
		switch (position) {
		case 0:
			addFragment(new FeaturedFragment());
			break;

//		case 1:
//			addFragment(new MoviesFragment());
//			break;

		case 1:
			addFragment(new SearchFragment());
			break;

		case 2:
			addFragment(new PlaylistFragment());
			break;
		}
	}


    protected void doVideosOpration(int position) {

		drawerFragmentPosition = position;
		globals.hideKeyboard(MainActivity.this);
		HashMap<String, String> categoryItem = globals.getCategoryList().get(position);
		//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_SELECT_CATEGORY, categoryItem.get(Constant.MM_CATEGORY_NAME));
		
		if(!categoryItem.get(Constant.MM_PLAYLIST_URL).toString().trim().equalsIgnoreCase("")) {
//			addFragment(
//					VideolistFragment.newInstance(
//						categoryItem.get(Constant.MM_CATEGORY_ID),
//						categoryItem.get(Constant.MM_CATEGORY_NAME).toString(),
//						categoryItem.get(Constant.MM_CATEGORY_NAME).toString(),
//						categoryItem.get(Constant.MM_PLAYLIST_URL).toString(),
//						VideoType.OTHERS,
//						null,
//						(categoryItem.get(Constant.MM_IS_YOUTUBE).equalsIgnoreCase("1")) ? true : false)
//					);
			lastVisitedFragment = VideolistFragment.newInstance(
					categoryItem.get(Constant.MM_CATEGORY_ID),
					categoryItem.get(Constant.MM_CATEGORY_NAME).toString(),
					categoryItem.get(Constant.MM_CATEGORY_NAME).toString(),
					categoryItem.get(Constant.MM_PLAYLIST_URL).toString(),
					VideoType.OTHERS,
					null,
					(categoryItem.get(Constant.MM_IS_YOUTUBE).equalsIgnoreCase("1")) ? true : false);



			Debugger.debugE("FFF",categoryItem.get(Constant.MM_CATEGORY_ID));
			Debugger.debugE("FFF",categoryItem.get(Constant.MM_CATEGORY_NAME));
			Debugger.debugE("FFF",categoryItem.get(Constant.MM_CATEGORY_NAME));
			Debugger.debugE("FFF",categoryItem.get(Constant.MM_PLAYLIST_URL));
			Debugger.debugE("FFF",categoryItem.get(Constant.MM_IS_YOUTUBE));

		} else {
//			addFragment(ChannelListFragment.newInstance(position, categoryItem.get(Constant.MM_CATEGORY_NAME).toString()));

			lastVisitedFragment = ChannelListFragment.newInstance(position, categoryItem.get(Constant.MM_CATEGORY_NAME).toString());
		}
		actionbar.setTitle(categoryItem.get(Constant.MM_CATEGORY_NAME));
		selectedVideoTitle = categoryItem.get(Constant.MM_CATEGORY_NAME);
	}
	
	protected void doMoreOpration(int position) {
		globals.hideKeyboard(MainActivity.this);
		 switch (position) {
		 
		 	case 0:
		 		//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_more_apps, null);
				if(ConnectionDetector.internetCheck(MainActivity.this)) {
					try {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constant.MORE_APPS + Constant.PUBLISHER_NAME)));
					} catch (Exception e) {
						HashMap<DialogMaker.DialogKeys, String> params = new HashMap<DialogMaker.DialogKeys, String>();
						params.put(DialogMaker.DialogKeys.btnPositive, "OK");
						params.put(DialogMaker.DialogKeys.sTitle, Constant.MM_ACTION_FAIL_TITLE);
						params.put(DialogMaker.DialogKeys.sMessage, Constant.MM_ACTION_FAIL_MSG);
						params.put(DialogMaker.DialogKeys.isCancelable, "true");
						new DialogMaker(MainActivity.this, params, DialogMaker.DialogType.NEUTRAL);
					}
				}
				break;
				
			case 1:
				//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_about_us, null);
				Intent about_us_actiivty = new Intent(MainActivity.this, AboutUsActivity.class);
				startActivity(about_us_actiivty);
				break;
				
			case 2:
				//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_conatct_us, null);
				if(ConnectionDetector.internetCheck(MainActivity.this)) 
					ContactUs();
				break;
				
			case 3:
				//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_tell_a_friend, null);
				doShare(MainActivity.this, null, null);
				break;
				
			case 4:
				//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_APP_TOUR, null);
				Intent help = new Intent(MainActivity.this, HelpActivity.class);
				help.putExtra("isIntentFromMainActivity", true);
				startActivity(help);
				break;
				
			case 5:
				//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_rate_app, null);
				if(ConnectionDetector.internetCheck(MainActivity.this)) 
					AppRater.rateNow(MainActivity.this);
				break;
		 
			case 6:
				//analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EA_like_us, null);
				Intent fb_like = getOpenFacebookIntent();
				startActivity(fb_like);
				break;
				
//			case 7:
//				analytics.Event_Tracking(AnalyticsConstant.EC_START, AnalyticsConstant.EC_REMOVE_ADS, null);
//				if(ConnectionDetector.internetCheck(MainActivity.this))
//					performPurchase(Constant.PurchaseType.RemoveAds);
//
////				Intent i = new Intent(MainActivity.this, IABTestActivity.class);
////				startActivity(i);
//				break;
		 }
	}

	public void addFragment(Fragment fragment) {
		getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.content_frame, fragment, null);
        ft.commit();
    }
	
	public Intent getOpenFacebookIntent() {
		
		String facebookUrl = "https://www.facebook.com/IndiaTVShowz";
		try {
			int versionCode = getPackageManager().getPackageInfo("com.facebook.katana", 0).versionCode;
			Debugger.debugE(TAG, "versionCode : "+versionCode);
	        if (versionCode >= 3002842) {
	        	Debugger.debugE(TAG, "new fb");
	            Uri uri = Uri.parse("fb://facewebmodal/f?href=" + facebookUrl);
	            return new Intent(Intent.ACTION_VIEW, uri);
	        } else {
	        	Debugger.debugE(TAG, "old fb");
				return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/447381858691506"));
	        }
		} catch (Exception e) {
			return new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl));
		}
	}
	
	public static void doShare(Context context, String songName, String videoURL) {
		
	    List<Intent> targetShareIntents=new ArrayList<Intent>();
	    Intent shareIntent=new Intent();
	    shareIntent.setAction(Intent.ACTION_SEND);
	    shareIntent.setType("text/plain");
	    
	 // 1. Retrieve all apps for our intent. If there are no apps - return usual already created intent.
	    List<ResolveInfo> resInfos = context.getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resInfos.isEmpty()) {
        	Toast.makeText(context, "Action Could Not Be Completed", Toast.LENGTH_SHORT).show();
        	return;
        }
        
     // 2. setting up app specific Extra Text.
	    if(!resInfos.isEmpty()) {
	        for(ResolveInfo resInfo : resInfos) {
	        	
	        	if(resInfo.activityInfo == null)
	        		continue;
	        	
	            String packageName=resInfo.activityInfo.packageName;
	            Debugger.debugI(TAG, "Package Name : "+packageName);
	            
	            boolean isRequired = true;
	            Intent intent=new Intent();
                intent.setComponent(new ComponentName(packageName, resInfo.activityInfo.name));
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TITLE, context.getResources().getString(R.string.app_name)+"!");
                intent.setType("text/plain");
                
	            if(packageName.contains("com.facebook.katana")) {
	            	//on facebook app you can share only link not text.
	            	String link = null;
	            	if(songName == null) {
	        			link = Constant.LongLink + globals.getPackageName();
	        		} else {
	        			link = videoURL;
	        		}
	            	intent.putExtra(Intent.EXTRA_TEXT, link);
	            	
	            } else if(packageName.contains("twitter")) {
	            	
	            	String msg;
					if(songName == null) {
	        			int available_space = 140 - Constant.ShortLink.length();
	        			String sName = Constant.MM_SHARE_SHORT_MESSAGE.length() > available_space ? Constant.MM_SHARE_SHORT_MESSAGE.substring(0, available_space)+"..." : Constant.MM_SHARE_SHORT_MESSAGE;
	        			msg = sName+"\n" + Constant.ShortLink;
	        		} else {
	        			int available_space = 140 - new String("... "+Constant.MM_VIDEO_SHARE_POSTFIX_MESSAGE+" "+Constant.ShortLink_Aboutus+"\n"+videoURL).length();
	        			String sName = songName.length() > available_space ? songName.substring(0, available_space)+"..." : songName;
	        			msg = sName+Constant.MM_VIDEO_SHARE_POSTFIX_MESSAGE+" "+Constant.ShortLink_Aboutus+"\n"+videoURL;
	        		}
	            	intent.putExtra(Intent.EXTRA_TEXT, msg);
	            	
	            } else if(packageName.contains("mms")) {
	            	
	            	String message;
	            	if(songName == null) {
	    				message = Constant.MM_SHARE_MESSAGE+ " " + Constant.ShortLink_Aboutus;
	    			} else {
	    				int available_space = 160 - new String(Constant.MM_VIDEO_SHARE_POSTFIX_MESSAGE+" "+Constant.ShortLink_Aboutus).length();
	    				String vName = songName.length() > available_space ? songName.substring(0, available_space)+"..." : songName;
	    				
	    				message = vName+Constant.MM_VIDEO_SHARE_POSTFIX_MESSAGE+" "+Constant.ShortLink_Aboutus;
	    			}
	            	intent.putExtra(Intent.EXTRA_TEXT, message);
	            	
	            } else if(packageName.contains("mail") || packageName.contains("android.gm")) {
	            	
	            	String subject;
	        		Spanned Body;
	            	if(songName == null) {
	        			subject = Constant.MM_SHARE_SUBJECT;
	        			
	        			String msg = Constant.MM_SHARE_SHORT_MESSAGE;
	        			Body = Html.fromHtml(
	        					new StringBuilder()
	        						.append("<a href=\""+Constant.LongLink + context.getPackageName()+"\">"+Constant.MM_APP_NAME+"</a>")
	        						.append("<br>"+msg)
	        						.append("<br><br><br><br>Check more details here, "+Constant.ShortLink_Aboutus)
	        						.append("<br>Download "+Constant.MM_IOS_APP+Constant.ShortLink_IOS)
	        						.append("<br>Download "+Constant.MM_ANDROID_APP+Constant.ShortLink)
	        						.toString());
	        		} else {
	        			subject = "Watching "+ songName + "at #IndiaTVShowz";
	        			Body = Html.fromHtml(
	        					new StringBuilder()
	        					.append(Constant.MM_VIDEO_SHARE_PREFIX_MESSAGE+"\"")
	        					.append(songName+"\" at #IndiaTVShowz mobile App.<br>"+videoURL)
	        					.append("<br><br><br><br>Check more details here, "+Constant.ShortLink_Aboutus)
	        			        .append("<br>Download "+Constant.MM_IOS_APP+Constant.ShortLink_IOS)
	        			        .append("<br>Download "+Constant.MM_ANDROID_APP+Constant.ShortLink)
	        			        .toString());
	        		}
	        		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
	            	intent.putExtra(Intent.EXTRA_TEXT, Body);
	                intent.setType("message/rfc822");
	                
	            } else if(packageName.contains("wifi") || packageName.contains("bluetooth") || packageName.contains("FileTransferClient")){
	            	isRequired = false;
	            } else {
	            	
	            	String message;
	    			if(songName == null) {
	    				message = Constant.MM_SHARE_MESSAGE
	    						+"\nCheck more details here, "+Constant.ShortLink_Aboutus
	    						+"\n"+Constant.MM_IOS_APP+Constant.ShortLink_IOS
	    						+"\n"+Constant.MM_ANDROID_APP+Constant.LongLink + context.getPackageName();
	    			} else {
	    				message = songName+Constant.MM_VIDEO_SHARE_POSTFIX_MESSAGE
	    						+"\n"+Constant.MM_IOS_APP+Constant.ShortLink_IOS
	    						+"\n"+Constant.MM_ANDROID_APP+Constant.LongLink + context.getPackageName();
	    			}
	            	intent.putExtra(Intent.EXTRA_TEXT, message);
	            }
	            
	            if(isRequired){
		            intent.setPackage(packageName);
	                targetShareIntents.add(intent);
	            }
	        }
	        if(!targetShareIntents.isEmpty()){
	            Intent chooserIntent=Intent.createChooser(targetShareIntents.remove(0), "Choose app to share");
	            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetShareIntents.toArray(new Parcelable[]{}));
	            context.startActivity(chooserIntent);
	        }else{
	            Toast.makeText(context, "Action Could Not Be Completed", Toast.LENGTH_SHORT).show();
	        }
	    }
	}
	
	private void ContactUs() {
		
		String DeviceInfo = "("+((Globals)getApplicationContext()).getDeviceName()+" v"+((Globals)getApplicationContext()).getOsVersion()+")";
		String subject = getResources().getString(R.string.app_name)+" Android Feedback v"+globals.getApplicationVersion();
		
		subject = subject+" "+DeviceInfo;
		Debugger.debugE(TAG, "subject :"+subject);

		Intent intent = new Intent(Intent.ACTION_SENDTO); 
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, "");
		intent.setData(Uri.parse("mailto:"+Constant.MAIL_ID)); 
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
		startActivity(intent);
	}

    @Override
    public void onGetCategorySucceed(JSONArray jCategory, JSONArray jFeatured, JSONObject jAppSetting) throws JSONException {
        globals.setAppSettings(Parser.getAppSettings(jAppSetting));
        globals.setCategoryList(Parser.getCategoryList(jCategory));
//		Extractor.getInstance(MainActivity.this, null).getVersion();
        setNavigationListAdapter();
    }

    @Override
    public void onGetCategoryFaild(JSONObject error) throws JSONException {
        HashMap<DialogMaker.DialogKeys, String> params = new HashMap<DialogMaker.DialogKeys, String>();
        params.put(DialogMaker.DialogKeys.btnPositive, "OK");
        params.put(DialogMaker.DialogKeys.sMessage, error.getString(Constant.MM_API_ERROR_INFO));
        params.put(DialogMaker.DialogKeys.sTitle, Constant.MM_ALERT_TITLE_ERROR);
        params.put(DialogMaker.DialogKeys.isCancelable, "true");

        new DialogMaker(MainActivity.this, params, DialogMaker.DialogType.NEUTRAL);
    }
}