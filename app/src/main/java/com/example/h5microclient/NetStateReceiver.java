package com.example.h5microclient;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.widget.Toast;

import com.example.h5microclient.util.LogUtil;

public class NetStateReceiver extends BroadcastReceiver {
    private AlertDialog dialog;
    private NetworkObserver observer;

    @Override
    public void onReceive(Context context, Intent intent){
        checkNetwork(context);
    }

    public void checkNetwork(Context context){
        ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable()){
            Toast.makeText(context, "网络连接成功", Toast.LENGTH_SHORT).show();
            closeDialog();
            this.observer.networkSuccess();
            LogUtil.d("network succuess");
        }else{
            showDialog(context);
            this.observer.networkFail();
            LogUtil.d("network fail");
        }
    }

    public void showDialog(final Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("网络异常");
        builder.setMessage("网络异常，请检查网络！！！");
        builder.setCancelable(false);
        builder.setPositiveButton("网络设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//            context.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("重试一次", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            checkNetwork(context);
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    public void closeDialog(){
        if (dialog != null){
            dialog.dismiss();
            dialog = null;
        }
    }

    public interface NetworkObserver{
        public void networkSuccess();
        public void networkFail();
    }

    public void setNetworkObserver(NetworkObserver observer) {
        this.observer = observer;
    }
}
