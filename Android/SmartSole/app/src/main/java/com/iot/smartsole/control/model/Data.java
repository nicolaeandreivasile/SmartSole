package com.iot.smartsole.control.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Data implements Parcelable {

    public final static String INTERIOR_FRONT = "InteriorFront";
    public final static String EXTERIOR_FRONT = "ExteriorFront";
    public final static String BACK = "Back";

    public final static int FIELDS = 2;
    public final static int NAME_IDX = 0;
    public final static int VALUE_IDX = 1;

    private String dataName;
    private String dataValue;

    public Data(String dataName, String dataValue) {
        this.dataName = dataName;
        this.dataValue = dataValue;
    }

    public void setDataName(String dataType) {
        this.dataName = dataType;
    }

    public String getDataName() {
        return dataName;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public String getDataValue() {
        return dataValue;
    }

    public Data(Parcel in) {
        String[] data = new String[FIELDS];
        in.readStringArray(data);

        this.dataName = data[NAME_IDX];
        this.dataValue = data[VALUE_IDX];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{this.dataName, this.dataValue});
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new Data(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new Data[size];
        }
    };

    @Override
    public boolean equals(Object obj) {
        return this.dataName.equals(((Data) obj).getDataName());
    }
}
