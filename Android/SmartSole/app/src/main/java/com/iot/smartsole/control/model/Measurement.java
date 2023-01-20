package com.iot.smartsole.control.model;

import android.icu.util.Measure;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Date;

public class Measurement implements Parcelable {

    public final static String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final int FIELDS = 4;

    private static final int MAC_ADDRESS_IDX = 0;
    private static final int INTERIOR_FRONT_IDX = 1;
    private static final int EXTERIOR_FRONT_IDX = 2;
    private static final int BACK_IDX = 3;

    private String macAddress;
    private String interiorFrontValue;
    private String exteriorFrontValue;
    private String backValue;
    private Date createdAt;

    public Measurement() {

    }

    public Measurement(String interiorFrontValue, String exteriorFrontValue,
                       String backValue) {
        this.interiorFrontValue = interiorFrontValue;
        this.exteriorFrontValue = exteriorFrontValue;
        this.backValue = backValue;
    }

    public Measurement(String macAddress, String interiorFrontValue, String exteriorFrontValue,
                       String backValue, Date createdAt) {
        this.macAddress = macAddress;
        this.interiorFrontValue = interiorFrontValue;
        this.exteriorFrontValue = exteriorFrontValue;
        this.backValue = backValue;
        this.createdAt = createdAt;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    public String getInteriorFrontValue() {
        return interiorFrontValue;
    }

    public void setInteriorFrontValue(String interiorFrontValue) {
        this.interiorFrontValue = interiorFrontValue;
    }

    public String getExteriorFrontValue() {
        return exteriorFrontValue;
    }

    public void setExteriorFrontValue(String exteriorFrontValue) {
        this.exteriorFrontValue = exteriorFrontValue;
    }

    public String getBackValue() {
        return backValue;
    }

    public void setBackValue(String backValue) {
        this.backValue = backValue;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void populateField(Data data) {
        if (data.getDataName().equals(Data.INTERIOR_FRONT)) {
            interiorFrontValue = data.getDataValue();
        } else if (data.getDataName().equals(Data.EXTERIOR_FRONT)) {
            exteriorFrontValue = data.getDataValue();
        } else if (data.getDataName().equals(Data.BACK)) {
            backValue = data.getDataValue();
        }
    }

    public boolean isPopulated() {
        return interiorFrontValue != null && exteriorFrontValue != null && backValue != null;
    }

    public static Creator getCREATOR() {
        return CREATOR;
    }

    public Measurement(Parcel in) {
        String[] measurement = new String[FIELDS];
        in.readStringArray(measurement);

        this.macAddress = measurement[MAC_ADDRESS_IDX];
        this.interiorFrontValue = measurement[INTERIOR_FRONT_IDX];
        this.exteriorFrontValue = measurement[EXTERIOR_FRONT_IDX];
        this.backValue = measurement[BACK_IDX];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{this.macAddress, this.interiorFrontValue, this.exteriorFrontValue, this.backValue});
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new Measurement(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new Measurement[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return String.format(macAddress + " " + interiorFrontValue + " " + exteriorFrontValue + " " +
                backValue + " " + createdAt.toString());
    }

    @Override
    public boolean equals(Object obj) {
        return macAddress.equals(((Measurement) obj).getMacAddress());
    }
}
