package com.iot.smartsole.control.ui.data;

import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.iot.smartsole.control.model.Data;

public class DataViewModel extends ViewModel {
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Intent> selectedIntent;

    public DataViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        this.selectedIntent = new MutableLiveData<Intent>();
    }

    public void selectIntent(Intent intent) {
        selectedIntent.setValue(intent);
    }

    public LiveData<Intent> getSelectedIntent() {
        return selectedIntent;
    }

    public Data getPersistentField(String fieldTag) {
        return savedStateHandle.get(fieldTag);
    }

    public void setPersistentField(String fieldTag, Data field) {
        savedStateHandle.set(fieldTag, field);
    }
}
