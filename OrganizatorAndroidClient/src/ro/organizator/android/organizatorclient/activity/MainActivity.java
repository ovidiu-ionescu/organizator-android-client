package ro.organizator.android.organizatorclient.activity;

import ro.organizator.android.organizatorclient.ChatActivity;
import ro.organizator.android.organizatorclient.LoginActivity;
import ro.organizator.android.organizatorclient.OrganizatorMessagingService;
import ro.organizator.android.organizatorclient.OrganizatorSettingsActivity;
import ro.organizator.android.organizatorclient.R;
import ro.organizator.android.organizatorclient.fragments.ChatFragment;
import ro.organizator.android.organizatorclient.fragments.ChatSearchFragment;
import ro.organizator.android.organizatorclient.fragments.MemoSearchFragment;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Main class in the application, uses the drawers pattern.
 */
public class MainActivity extends ActionBarActivity {

	static final String LOG_TAG = MainActivity.class.getName();
	
	public OrganizatorMessagingService organizatorMessagingService;
	public boolean serviceBound;

	
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        doBindService();
        setContentView(R.layout.activity_main);

        mTitle = mDrawerTitle = getTitle();
        mPlanetTitles = getResources().getStringArray(R.array.activities_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mPlanetTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch(item.getItemId()) {
        	// no universal options yet
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
    	// for the time being use the memo search
    	
    	Fragment fragment = null;
    	switch(position) {
    	case 0:
    		fragment = new ChatFragment();
    		Log.d(LOG_TAG, "Create a chat fragment");
    		break;
    	case 1:
    		fragment = new ChatSearchFragment();
    		Log.d(LOG_TAG, "Create a chat search fragment");
    		break;
    	case 2:
    		fragment = new MemoSearchFragment();
    		Log.d(LOG_TAG, "Create a memo search fragment");
    		break;
    	case 3: 
    		startActivity(new Intent(this, OrganizatorSettingsActivity.class));
    		mDrawerLayout.closeDrawer(mDrawerList);
    		return;
    	case 4:
    		startActivity(new Intent(this, ChatActivity.class));
    		mDrawerLayout.closeDrawer(mDrawerList);
    		return;
    	case 5:
    		exitApp();
    		return;
    	default:
    	}
    	// Parameters for the fragment
        Bundle args = new Bundle();
        fragment.setArguments(args);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			organizatorMessagingService = ((OrganizatorMessagingService.LocalBinder)service).getService();
			Log.d(LOG_TAG, "Fetched the service into local field");
			if(!organizatorMessagingService.isStarted()) {
				// go to the login activity
				try {
					Intent k = new Intent(getApplicationContext(), LoginActivity.class);
					startActivity(k);
				} catch(Exception e) {
					
				}
				finish();
			} else {
				// tell the service to send a new data intent
				organizatorMessagingService.forceSendNewDataNotifications();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			organizatorMessagingService = null;
		}
	};	

	void doBindService() {
		bindService(new Intent(MainActivity.this, OrganizatorMessagingService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		serviceBound = true;
	}

	void doUnbindService() {
		if (serviceBound) {
			// Detach our existing connection.
			unbindService(serviceConnection);
			serviceBound = false;
		}
	}
	
	private void exitApp() {
		new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.exit_application)
        .setMessage(R.string.exit_application_are_you_sure)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Stop the activity
        		if(organizatorMessagingService != null) {
        			organizatorMessagingService.shutdown = true;
        			organizatorMessagingService.stopSelf();
        		}
        		MainActivity.this.finish();
            }

        })
        .setNegativeButton(R.string.no, null)
        .show();
	}


}
