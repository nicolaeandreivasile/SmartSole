package com.iot.smartsole.control.ui.data;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.iot.smartsole.R;
import com.iot.smartsole.control.ControlActivity;
import com.iot.smartsole.control.model.Data;

import org.jetbrains.annotations.NotNull;

public class DataFragment extends Fragment {

    DataViewModel dataViewModel;

    private Data interiorFrontPressure;
    private Data exteriorFrontPressure;
    private Data backPressure;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data,
                container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView gasTextValue = (TextView) view.findViewById(R.id.interior_front);
        TextView temperatureTextValue = (TextView) view.findViewById(R.id.exterior_front);
        TextView humidityTextValue = (TextView) view.findViewById(R.id.back);

        /* Get the DataViewModel and retrieve saved values */
        dataViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);
        if (!retrieveData())
            return;

        /* Set the change listener for the DataViewModel intent */
        dataViewModel.getSelectedIntent().removeObservers(getViewLifecycleOwner());
        dataViewModel.getSelectedIntent().observe(getViewLifecycleOwner(), new Observer<Intent>() {
            @Override
            public void onChanged(Intent intent) {
                Data receivedData =
                        (Data) intent.getParcelableExtra(ControlActivity.CHARACTERISTIC);
                if (receivedData == null)
                    return;

                if (receivedData.getDataName().equals(Data.INTERIOR_FRONT)) {
                    interiorFrontPressure = receivedData;
                    gasTextValue.setText(interiorFrontPressure.getDataValue() + "%");
                } else if (receivedData.getDataName().equals(Data.EXTERIOR_FRONT)) {
                    exteriorFrontPressure = receivedData;
                    temperatureTextValue.setText(exteriorFrontPressure.getDataValue() + "%");
                } else if (receivedData.getDataName().equals(Data.BACK)) {
                    backPressure = receivedData;
                    humidityTextValue.setText(backPressure.getDataValue() + "%");
                }
            }
        });

        /* Update the fragment's arguments accordingly */
        if (!isDataAvailable()) {
            startPostponedEnterTransition();
        } else {
            gasTextValue.setText(interiorFrontPressure.getDataValue() + "%");
            temperatureTextValue.setText(exteriorFrontPressure.getDataValue() + "%");
            humidityTextValue.setText(backPressure.getDataValue() + "%");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveData();
    }

    /* Save data using DataViewModel */
    public void saveData() {
        if (dataViewModel == null)
            return;

        dataViewModel.setPersistentField(Data.INTERIOR_FRONT, interiorFrontPressure);
        dataViewModel.setPersistentField(Data.EXTERIOR_FRONT, exteriorFrontPressure);
        dataViewModel.setPersistentField(Data.BACK, backPressure);
    }

    /* Retrieve data from the DataViewModel */
    public boolean retrieveData() {
        if (dataViewModel == null)
            return false;

        interiorFrontPressure = dataViewModel.getPersistentField(Data.INTERIOR_FRONT);
        exteriorFrontPressure = dataViewModel.getPersistentField(Data.EXTERIOR_FRONT);
        backPressure = dataViewModel.getPersistentField(Data.BACK);

        return true;
    }

    /* Check if data is available */
    private boolean isDataAvailable() {
        return interiorFrontPressure != null && exteriorFrontPressure != null && backPressure != null;
    }
}
