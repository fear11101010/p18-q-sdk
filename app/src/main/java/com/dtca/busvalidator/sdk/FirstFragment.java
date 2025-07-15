package com.dtca.busvalidator.sdk;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.dtca.busvalidator.busvalidatorsdk.QRCodeReader;
import com.dtca.busvalidator.busvalidatorsdk.model.RideAndAlight;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.SamSyntaxError;
import com.dtca.busvalidator.sdk.databinding.FragmentFirstBinding;
import com.google.gson.Gson;

import java.util.Objects;

public class FirstFragment extends Fragment {

    private final HandlerThread backgroundThread = new HandlerThread("FelicaDetectionAndReadThread");
    private FragmentFirstBinding binding;
    private Handler handler;
    private Runnable runnable;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
//        MyApplication myApplication = (MyApplication) requireActivity().getApplication();
        /*try {
            long sTime = System.currentTimeMillis();
            myApplication.getSam().initSam();
            long eTime = System.currentTimeMillis();
            Log.d("sam_init_time", ((double)(eTime-sTime)/1000)+"");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
        backgroundThread.start();
        QRCodeReader qrCodeReader = QRCodeReader.getInstance();

        handler = new Handler(backgroundThread.getLooper());

        runnable = () -> {
            qrCodeReader.startQrCodeScaner();
            while (true){
                String s = qrCodeReader.readQrCode();
                if(s==null){
                    System.out.println("Qrcodedata error");
                }
                else{
                    System.out.println("q----->"+s);
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        handler.post(runnable);


        // Handle the message received
        /*backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                    Log.d("prepareLooper", "preparing looper");
                }
                handler = new Handler(Objects.requireNonNull(Looper.myLooper()));

                Looper.loop();
                Log.d("prepareLooper", "loop start");
            }
        });
        backgroundThread.start();*/

        /*try {
            backgroundThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }*/
        /*AtomicInteger i = new AtomicInteger(1);
        if(actionRunnable == null){
            actionRunnable = new Runnable() {
                @Override
                public void run() {
                    if(handler != null){
                        Message message = Message.obtain();
                        message.what = 1;
                        message.obj = "Message "+ i.get();
                        i.set(i.get()+1);
                        handler.sendMessage(message);
                    }
                }
            };
        }*/

        /*new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d("prepareLooper", "loop start");

        },2000);*/
    }

    private void rideOrAlight() throws InterruptedException {
        Log.d("looperLog", "rideOrAlight: ");
        /*Runnable r = () -> {
            Log.d("looper", "run: ");
            try {
                MyApplication myApplication = (MyApplication) requireActivity().getApplication();
                RideAndAlight rideAndAlight = new RideAndAlight(myApplication.getFelicaCard());
                if (myApplication.getFelicaCard().isStatusAlight()) {
                    rideAndAlight.setType(RideAndAlight.Type.RIDE);
                    myApplication.setType(RideAndAlight.Type.RIDE);
                } else {
                    rideAndAlight.setType(RideAndAlight.Type.ALIGHT);
                    myApplication.setType(RideAndAlight.Type.ALIGHT);
                }
                rideAndAlight.setDirection(RideAndAlight.Direction.UPSTREAM);
                rideAndAlight.setFareMatrix(myApplication.getUtils().fareMatrix);
                if (rideAndAlight.getType() == RideAndAlight.Type.RIDE) {
                    rideAndAlight.setStation(new Gson().toJson(myApplication.getUtils().fareMatrix.getStations().get(0)));
                } else if (rideAndAlight.getType() == RideAndAlight.Type.ALIGHT) {
                    rideAndAlight.setStation(new Gson().toJson(myApplication.getUtils().fareMatrix.getStations().get(5)));
                }
                try {
                    rideAndAlight.writeData((transactionData) -> {
                        Log.d("transaction", new Gson().toJson(transactionData));
                        requireActivity().runOnUiThread(() -> {
                            NavHostFragment.findNavController(FirstFragment.this)
                                    .navigate(R.id.action_FirstFragment_to_SecondFragment);
                        });
                    });
                } catch (Exception e) {
                    Log.d("child error", Objects.requireNonNull(e.getMessage()));
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log.d("parent error", Objects.requireNonNull(e.getMessage()));
                e.printStackTrace();
            }
        };

        Thread t = new Thread(r);

        t.start();*/
        try {
            MyApplication myApplication = (MyApplication) requireActivity().getApplication();
            RideAndAlight rideAndAlight = new RideAndAlight(myApplication.getFelicaCard());
            if (myApplication.getFelicaCard().isStatusAlight()) {
                rideAndAlight.setType(RideAndAlight.Type.RIDE);
                myApplication.setType(RideAndAlight.Type.RIDE);
            } else {
                rideAndAlight.setType(RideAndAlight.Type.ALIGHT);
                myApplication.setType(RideAndAlight.Type.ALIGHT);
            }
            rideAndAlight.setDirection(RideAndAlight.Direction.UPSTREAM);
            rideAndAlight.setFareMatrix(myApplication.getUtils().fareMatrix);
            if (rideAndAlight.getType() == RideAndAlight.Type.RIDE) {
                rideAndAlight.setStation(new Gson().toJson(myApplication.getUtils().fareMatrix.getStations().get(0)));
            } else if (rideAndAlight.getType() == RideAndAlight.Type.ALIGHT) {
                rideAndAlight.setStation(new Gson().toJson(myApplication.getUtils().fareMatrix.getStations().get(5)));
            }
            try {
                rideAndAlight.writeData((transactionData) -> {
                    Log.d("transaction", new Gson().toJson(transactionData));
                    if (rideAndAlight.getType() == RideAndAlight.Type.ALIGHT) {
                        Log.d("fare", rideAndAlight.getFare().toString());
                    }
                    requireActivity().runOnUiThread(() -> {
                        NavHostFragment.findNavController(FirstFragment.this)
                                .navigate(R.id.action_FirstFragment_to_SecondFragment);
                    });
                });
            } catch (Exception e) {
                Log.d("child error", Objects.requireNonNull(e.getMessage()));
                if (e instanceof SamSyntaxError) {
                    myApplication.getSam().initSam();
                }
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.d("parent error", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        destroyRunnable();
        binding = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        destroyRunnable();
    }

    @Override
    public void onStop() {
        super.onStop();
        destroyRunnable();
    }

    private void destroyRunnable() {
        backgroundThread.quit();
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

}