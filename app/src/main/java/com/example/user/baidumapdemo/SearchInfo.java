package com.example.user.baidumapdemo;

/**
 * Created by user on 2018/2/28.
 * 存储jsno数据类
 */

public class SearchInfo {
    private String name, address, streetIds, uids;
    private double lat,lng;
    public SearchInfo(String name,double lat,double lng, String address,String streetIds,String uids){
        this.name=name;
        this.lat=lat;
        this.lng=lng;
        this.address=address;
        this.streetIds=streetIds;
        this.uids=uids;
    }

    public String getDesname() {
        return name;
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongtiude() {
        return lng;
    }

    public String getAddress() {
        return address;
    }

    public String getStreetIds() {
        return streetIds;
    }

    public String getUid(){
        return uids;
    }


}
