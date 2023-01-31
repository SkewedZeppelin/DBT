/*
Copyright (c) 2023 Divested Computing Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package us.spotco.directbootleaktest;

import static android.content.Context.CONNECTIVITY_SERVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class EventReceiver extends BroadcastReceiver {

    private static String randomID = UUID.randomUUID() + "";

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            sendRequest("LOCKED_BOOT_COMPLETED", context);
        }
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            sendRequest("BOOT_COMPLETED", context);
        }
    }

    public static void sendRequest(String status, Context context) {
        Thread request = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(!isNetworkAvailable(context)) {
                        Thread.sleep(1000);
                    }
                    HttpURLConnection connection = (HttpURLConnection) new URL("https://ntfy.sh/DirectBootTesting").openConnection();connection.setConnectTimeout(30000);
                    connection.addRequestProperty("User-Agent", "Direct Boot Tester/1.0");
                    connection.setRequestMethod("PUT");
                    connection.connect();
                    OutputStreamWriter out = new OutputStreamWriter(
                            connection.getOutputStream());
                    out.write(status + " - ID: " + getUniqueID(context) + ", VPN: " + isVPN(context));
                    out.close();
                    connection.getInputStream();
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        request.start();
    }

    private static String getUniqueID(Context context) {
        SharedPreferences prefs = context.createDeviceProtectedStorageContext().getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        if (prefs.getString("uniqueID", null) == null) {
            prefs.edit().putString("uniqueID", randomID.split("-")[0]).commit();
        }
        return prefs.getString("uniqueID", "INVALID");
    }

    //Credit (CC BY-SA 4.0): https://stackoverflow.com/a/69325252
    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Credit (CC BY-SA 4.0): https://stackoverflow.com/a/55609059
    private static boolean isVPN(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
    }
}
