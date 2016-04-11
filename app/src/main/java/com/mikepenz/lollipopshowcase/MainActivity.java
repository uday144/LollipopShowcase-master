package com.mikepenz.lollipopshowcase;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mikepenz.lollipopshowcase.adapter.ApplicationAdapter;
import com.mikepenz.lollipopshowcase.entity.AppInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends Activity {


    private List<AppInfo> applicationList = new ArrayList<AppInfo>();



    private ApplicationAdapter mAdapter;

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Handle ProgressBar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);


        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

      //  mRecyclerView.setItemAnimator(new CustomItemAnimator());
      //  mRecyclerView.setItemAnimator(new ReboundItemAnimator());

        mAdapter = new ApplicationAdapter(new ArrayList<AppInfo>(), R.layout.row_application, MainActivity.this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.theme_accent));
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new InitializeApplicationsTask().execute();
            }
        });

        new InitializeApplicationsTask().execute();



        //show progress
        mRecyclerView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }




    /**
     * helper class to start the new detailActivity animated
     *
     * @param appInfo
     * @param appIcon
     */
    public void animateActivity(AppInfo appInfo, View appIcon) {

        new GenerateApk(appInfo).execute();


    }

    private void extractApk(AppInfo appInfo) {
        final ResolveInfo info = appInfo.getResolveInfo();
        File f1 =new File( info.activityInfo.applicationInfo.publicSourceDir);

        Log.v("file--", " "+f1.getName().toString()+"----"+info.loadLabel(getPackageManager()));
        try {

            String file_name = info.loadLabel(getPackageManager()).toString();
            Log.d("file_name--", " " + file_name);

            // File f2 = new File(Environment.getExternalStorageDirectory().toString()+"/Folder/"+file_name+".apk");
            // f2.createNewFile();

            File f2 = new File(Environment.getExternalStorageDirectory().toString() + "/BlackPanther");
            f2.mkdirs();
            f2 = new File(f2.getPath() + "/" + file_name + ".apk");
            f2.createNewFile();

            InputStream in = new FileInputStream(f1);

            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

        }
        catch(FileNotFoundException ex){
            System.out.println(ex.getMessage() + " in the specified directory.");
        }
        catch(IOException e){
            System.out.println(e.getMessage());
        }
    }


    /**
     * A simple AsyncTask to load the list of applications and display them
     */
    private class InitializeApplicationsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mAdapter.clearApplications();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            applicationList.clear();

            //Query the applications
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> ril = getPackageManager().queryIntentActivities(mainIntent, 0);
            for (ResolveInfo ri : ril) {
                applicationList.add(new AppInfo(MainActivity.this, ri));
            }
            Collections.sort(applicationList);

            for (AppInfo appInfo : applicationList) {
                //load icons before shown. so the list is smoother
                appInfo.getIcon();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //handle visibility
            mRecyclerView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            //set data for list
            mAdapter.addApplications(applicationList);
            mSwipeRefreshLayout.setRefreshing(false);

            super.onPostExecute(result);
        }
    }
    /**
     * A simple AsyncTask to load the list of applications and display them
     */
    private class GenerateApk extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;
        AppInfo appInfo;
        GenerateApk(AppInfo appInfo)
        {
            this.appInfo =  appInfo;
        }
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this,
                    "Extracting Apk",
                    "Please Wait...");
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            extractApk(appInfo);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(MainActivity.this,"Done",Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();

            super.onPostExecute(result);
        }
    }
}
