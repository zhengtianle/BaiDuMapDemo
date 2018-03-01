package com.example.user.baidumapdemo;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.http.HttpClient;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 主函数
 * 结合定位SDK实现定位，并使用MyLocationOverlay绘制定位位置
 * 同时使用自定义图标绘制并点击时弹出泡泡
 */

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private BaiduMap mBaiduMap=null;
    private MapView mMapView = null;

    // 定位相关
    LocationClient mLocClient;
    public MyLocationListener myListener = new MyLocationListener();

    private MyLocationConfiguration.LocationMode mCurrentMode;////////////
    BitmapDescriptor mCurrentMarker;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;
    private SensorManager mSensorManager;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;

    // UI相关
    RadioGroup.OnCheckedChangeListener radioButtonListener;////////////
    Button requestLocButton;
    boolean isFirstLoc = true; // 是否首次定位
    private MyLocationData locData;
    private float direction;

    private EditText searchEdit;
    private Button okToSearch;
    private List<SearchInfo> searchInfoLists;
    private String uid;


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0x1234) {
                JSONObject object = (JSONObject) msg.obj;
                //toast("json:----->"+object.toString());
                //解析开始:然后把每一个地点信息封装到SearchInfo类中
                try {
                    JSONArray array = object.getJSONArray("results");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject joObject = array.getJSONObject(i);
                        String name = joObject.getString("name");
                        JSONObject object2 = joObject.getJSONObject("location");
                        double lat = object2.getDouble("lat");
                        double lng = object2.getDouble("lng");
                        String address = joObject.getString("address");
                        String streetIds = joObject.getString("street_id");
                        String uids = joObject.getString("uid");
                        SearchInfo mInfo = new SearchInfo(name, lat, lng, address, streetIds, uids);
                        searchInfoLists.add(mInfo);
                    }

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            displayInDialog();
        }

        /**
         * @author mikyou
         * 显示搜索后信息的自定义列表项对话框，以及对话框点击事件的处理
         * */
        private void displayInDialog() {
            if (searchInfoLists!=null) {
                AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                builder.setIcon(R.drawable.arrow).setTitle("请选择你查询到的地点")
                        .setAdapter(new myDialogListAdapter(), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final SearchInfo mInfos=searchInfoLists.get(which);
                                uid=mInfos.getUid();
                                addPnoramaLayout(mInfos);//
                            }

                        }).show();
            }else{
                toast("未查询到相关地点");
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 设置标题栏不可用
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        selectLocation();
        initSearchDestination();

        mapStart();


    }//OnCreate()

    /**
     * 搜索
     */
    private void initSearchDestination() {
        searchEdit=(EditText) findViewById(R.id.search_panorama);
        okToSearch=(Button) findViewById(R.id.ok_to_search);
        okToSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchInfoLists=new ArrayList<SearchInfo>();
                Intent intent = new Intent(MainActivity.this,PoiSearchDemo.class);
                startActivity(intent);
                getSearchDataFromNetWork();
            }

        });
    }

    /**
     * 根据输入搜索的信息，从网络获得的JSON数据
     * 开启一个子线程去获取网络数据
     */
    private void getSearchDataFromNetWork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    org.apache.http.client.HttpClient httpClient = new DefaultHttpClient();
                    //用的百度的一个人的url，自己的包产生的url相关服务被禁用，没找出什么原因
                    HttpGet httpGet = new HttpGet("http://api.map.baidu.com/place/v2/search?ak=nylibTfKmZw9mGe3juhT3U9x&output=json&query="+searchEdit.getText().toString()+"&page_size=10&page_num=0&scope=1&region=全国&mcode=F5:3E:06:3E:FC:E8:ED:19:60:2E:99:63:D8:78:85:2E:EB:12:9D:BE;com.mikyou.maptest");
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    //连接成功
                    if(httpResponse.getStatusLine().getStatusCode() == 200){
                        HttpEntity entity = httpResponse.getEntity();
                        String response = EntityUtils.toString(entity,"utf-8");
                        JSONObject jsonObject = new JSONObject(response);
                        //获取result节点下的位置信息
                        JSONArray resultArray = jsonObject.getJSONArray("result");
                        if(resultArray.length() > 0){
                            Message msg=new Message();
                            msg.obj=jsonObject;
                            msg.what=0x1234;
                            handler.sendMessage(msg);
                        }
                    }

                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 将我们搜索的信息来自网络的JSON数据解析后，封装在一个SearchInfo类中
     * 然后将这些数据展示在一个自定义的列表项的对话框中，以下就为定义列表项的适配器
     * ListAdapter
     */
    class myDialogListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return searchInfoLists.size();
        }

        @Override
        public Object getItem(int position) {
            return getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SearchInfo mSearchInfo=searchInfoLists.get(position);
            View view=View.inflate(MainActivity.this, R.layout.dialog_list_item, null);
            TextView desnameTv=(TextView) view.findViewById(R.id.desname);
            TextView addressTv=(TextView) view.findViewById(R.id.address);
            desnameTv.setText(mSearchInfo.getDesname());
            addressTv.setText(mSearchInfo.getAddress());
            return view;
        }

    }

    /**
     * 添加全景覆盖物，即全景的图标，迅速定位到该地点在地图上的位置
     */
    public void addPnoramaLayout(SearchInfo mInfos) {
        mBaiduMap.clear();
        LatLng latLng=new LatLng(mInfos.getLatitude(), mInfos.getLongtiude());
        Marker pnoramaMarker=null;
        OverlayOptions options;
        BitmapDescriptor mPnoramaIcon=BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
        options=new MarkerOptions().position(latLng).icon(mPnoramaIcon).zIndex(6);
        pnoramaMarker=(Marker) mBaiduMap.addOverlay(options);
        MapStatusUpdate msu=MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.animateMapStatus(msu);
    }


    /**
     * 地图定位初始化
     */
    public void mapStart(){
        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();

    }

    /**
     * 定位模式
     */
    public void selectLocation(){
        requestLocButton = (Button) findViewById(R.id.button1);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//获取传感器管理服务
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;

        requestLocButton.setTextColor(Color.BLACK);
        View.OnClickListener btnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                switch (mCurrentMode) {
                    case NORMAL:
                        requestLocButton.setText("跟随");
                        mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                                mCurrentMode, true, mCurrentMarker));
                        MapStatus.Builder builder = new MapStatus.Builder();
                        builder.overlook(0);
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                        break;
                    case COMPASS:
                        requestLocButton.setText("普通");
                        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
                        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                                mCurrentMode, true, mCurrentMarker));
                        MapStatus.Builder builder1 = new MapStatus.Builder();
                        builder1.overlook(0);
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder1.build()));
                        break;
                    case FOLLOWING:
                        requestLocButton.setText("罗盘");
                        mCurrentMode = MyLocationConfiguration.LocationMode.COMPASS;
                        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                                mCurrentMode, true, mCurrentMarker));
                        break;
                    default:
                        break;
                }
            }
        };
        requestLocButton.setOnClickListener(btnClickListener);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;

    }//onSensorChanged()

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }




    /**
     * 定位SDK监听函数
     */
    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }


    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();

    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
        //为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onStop() {
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    public void toast(String str){
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }



}
