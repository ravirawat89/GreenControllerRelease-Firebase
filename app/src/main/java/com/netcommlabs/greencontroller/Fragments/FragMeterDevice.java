package com.netcommlabs.greencontroller.Fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.netcommlabs.greencontroller.R;
import com.netcommlabs.greencontroller.activities.MainActivity;
import com.netcommlabs.greencontroller.constant.TagConstant;
import com.netcommlabs.greencontroller.model.ModalDeviceModule;
import com.netcommlabs.greencontroller.sqlite_db.DatabaseHandler;

import java.util.List;

/**
 * Created by Android on 12/7/2017.
 */

public class FragMeterDevice extends Fragment {

        private MainActivity mContext;
        private View view;
        private TextView auto_Mode, timer_Mode;
        boolean isClick_auto = true;
        boolean isClick_timer = true;
        private DatabaseHandler databaseHandler;
        private volatile int timer = 0;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mContext = (MainActivity) context;
        }


    @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            view = inflater.inflate(R.layout.frag_meter_device, null);

            findViews();
            initBaseNdListeners();
            return view;
        }

    private void findViews()
    {
        auto_Mode =  (TextView) view.findViewById(R.id.autoMode);
        timer_Mode = (TextView) view.findViewById(R.id.timerMode);
    }

    private void initBaseNdListeners()
    {
        //auto_Mode.setClickable(true);
        //timer_Mode.setClickable(true);
        if(timer == 1)
            timer_Mode.setBackgroundResource(R.drawable.round_back_shadow_green_small_t);

        auto_Mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isClick_auto) {
                    timer_Mode.setClickable(false);
                    auto_Mode.setBackgroundResource(R.drawable.round_back_shadow_green_small_t);
                    isClick_auto = false;
                    Toast.makeText(mContext, "Pump Auto Mode activated", Toast.LENGTH_SHORT).show();
                    timer = 0;
                }
                else
                    {
                        timer_Mode.setClickable(true);
                        auto_Mode.setBackgroundResource(R.drawable.round_back_shadow_small);
                        isClick_auto = true;
                        Toast.makeText(mContext, "Pump Auto Mode deactivated", Toast.LENGTH_SHORT).show();
                        timer = 0;
                    }

            }
        });

        timer_Mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isClick_timer)
                {
                    timer = 1;
                    auto_Mode.setClickable(false);
                    timer_Mode.setBackgroundResource(R.drawable.round_back_shadow_green_small_t);
                    isClick_timer = false;
                    Toast.makeText(mContext, "Pump Timer Mode selected", Toast.LENGTH_SHORT).show();
                   // MyFragmentTransactions.replaceFragment(mContext, new FragAddEditSesnPlan(), TagConstant.ADD_EDIT, mContext.frm_lyt_container_int, true);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run()
                        {
                            databaseHandler = DatabaseHandler.getInstance(mContext);
                            List<ModalDeviceModule> listAllDevices = databaseHandler.getDeviceDataForIMap("");
                            if (listAllDevices.size() > 0) {
                                //Adding Fragment(FragDeviceMAP)
                                MyFragmentTransactions.replaceFragment(mContext, new FragDeviceMAP(), TagConstant.DEVICE_MAP, mContext.frm_lyt_container_int, true);
                            } else {
                                //Adding Fragment(FragDontHvDevice)
                                MyFragmentTransactions.replaceFragment(mContext, new FragDontHvDevice(), TagConstant.DO_NOT_HAVE_DEVICE, mContext.frm_lyt_container_int, true);
                            }
                        }
                    }, 2000);

                }
                else
                {
                    timer = 0;
                    auto_Mode.setClickable(true);
                    timer_Mode.setBackgroundResource(R.drawable.round_back_shadow_small);
                    isClick_timer = true;
                    Toast.makeText(mContext, "Pump Timer Mode deactivated", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}
