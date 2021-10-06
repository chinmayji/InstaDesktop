package cj.instawall;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    final String TAG = "CJ";
    String HWV_FLAG = "", HWV_LAST_VISIT = "", POST_DOWNLOAD_ACTION = "";
    String get_all_links, insert_dwn_btns, download_post;
    int DOWNLOAD_STACK_COUNT = 0;
    String username = "chinmayjain08";
    Random rnd = new Random();
    HashMap<String, HashSet<String>> url_to_name;
    ArrayList<String> recent_downloads = new ArrayList<>();
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor spEditor;
    WebView wv, hwv;
    WebChromeClient wvChromeClient;
    WebViewClient wvClient;
    Button run, hide_wv, sync, random;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        sharedPreferences = getPreferences(MODE_PRIVATE);
        spEditor = sharedPreferences.edit();
        wv = findViewById(R.id.webView);
        hwv = findViewById(R.id.hiddenWebView);
//        hwv = new WebView(getApplicationContext());
        hwv.setMinimumHeight(1920);
        hwv.setMinimumWidth(1080);
        run = findViewById(R.id.run);
        hide_wv = findViewById(R.id.hide_wv);
        sync = findViewById(R.id.sync);
        random = findViewById(R.id.random);
        readObjects();
        readScripts();
        setupWV();
        setupHWV();

        wv.loadUrl("https://www.instagram.com/chinmayjain08/saved");

        run.setOnClickListener(view -> {
            Log.d(TAG, String.valueOf(url_to_name.keySet().size()));

//            sendWallToDesktop("225828859_347655843521075_1510817421766449070_n.jpg");
        });
        run.setOnLongClickListener(view -> {
            saveObject(url_to_name, "url_to_name_backup");
            Log.d(TAG, "Backedup");
            return true;
        });
        hide_wv.setOnClickListener(view -> {
            if (wv.getVisibility() == View.VISIBLE) wv.setVisibility(View.INVISIBLE);
            else wv.setVisibility(View.VISIBLE);
        });
        sync.setOnClickListener(view -> {
            HWV_FLAG = "sync";
            hwv.loadUrl("https://www.instagram.com/chinmayjain08/saved");
        });

        random.setOnClickListener(view -> {
            String[] keys = url_to_name.keySet().toArray(new String[url_to_name.keySet().size()]);
            recent_downloads.clear();
            HWV_FLAG = "download_post";
            HWV_LAST_VISIT = keys[rnd.nextInt(url_to_name.keySet().size())];
            POST_DOWNLOAD_ACTION = "wallpaper";
            hwv.loadUrl("https://instagram.com" + keys[rnd.nextInt(url_to_name.keySet().size())]);
            random.setText("");
            random.setClickable(false);
        });

        random.setOnLongClickListener(view -> {
            String fnm = sharedPreferences.getString("current_wallpaper",null);
            if(fnm!=null){
                sendWallToDesktop(fnm);
            }
            return true;
        });

        // --------------------

//        setWallpaper("197849760_234294944779150_2759185063273606450_n.jpg");

        // --------------------
    }

    void saveObject(Object o, String n) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), n)));
            out.writeObject(o);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    void readObjects() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(getFilesDir(), "url_to_name")));
            url_to_name = (HashMap<String, HashSet<String>>) in.readObject();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "FileNotFound");
            url_to_name = new HashMap<>();
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
        if (url_to_name == null || url_to_name.keySet().size() == 0) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(getFilesDir(), "url_to_name_backup")));
                url_to_name = (HashMap<String, HashSet<String>>) in.readObject();
            } catch (Exception e) {
                Log.d(TAG, "errrrrrr");
            }
        }
        Toast.makeText(getApplicationContext(), String.valueOf(url_to_name.keySet().size()), Toast.LENGTH_SHORT).show();

    }

    void readScripts() {
        try {
            Scanner s = new Scanner(getAssets().open("scripts/get_all_links.js")).useDelimiter("\\A");
            get_all_links = s.hasNext() ? s.next() : "";
            s = new Scanner(getAssets().open("scripts/insert_dwn_btns.js")).useDelimiter("\\A");
            insert_dwn_btns = s.hasNext() ? s.next() : "";
            s = new Scanner(getAssets().open("scripts/download_post.js")).useDelimiter("\\A");
            download_post = s.hasNext() ? s.next() : "";

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setupHWV() {
        hwv.getSettings().setJavaScriptEnabled(true);
        hwv.getSettings().setDomStorageEnabled(true);
        hwv.setWebViewClient(wvClient);
        hwv.setWebChromeClient(wvChromeClient);
    }

    void setupWV() {
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setDomStorageEnabled(true);
        wvClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (view == wv) {
//                    if(wv.getUrl().equals("http://i"))
                    Log.d(TAG, wv.getUrl());
                    wv.evaluateJavascript(insert_dwn_btns, null);
                }
                if (view == hwv) {
                    if (HWV_FLAG.equals("sync")) {
                        HWV_FLAG = "";
                        hwv.evaluateJavascript(get_all_links, null);
                    } else if (HWV_FLAG.equals("download_post")) {
                        Log.d(TAG, "download_post");
                        HWV_FLAG = "";
                        hwv.evaluateJavascript(download_post, null);
                    }
                }
                super.onPageFinished(view, url);
            }
        };

        wvChromeClient = new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String action = "";
                String mess = consoleMessage.message();
                for (int i = 0; i < mess.length(); i++) {
                    if (mess.charAt(i) == ':') {
                        mess = mess.substring(i + 1);
                        break;
                    }
                    action += mess.charAt(i);
                }
                if (action.equals("flag_hwv")) {
                    HWV_FLAG = mess;
                }
                if (action.equals("post_link")) {
                    url_to_name.put(mess, new HashSet<>());
                }
                if (action.equals("visit")) {
                    HWV_LAST_VISIT = mess;
                    hwv.loadUrl("http://instagram.com" + mess);
                }
                if (action.equals("download_cnt")) {
                    DOWNLOAD_STACK_COUNT = Integer.parseInt(mess);
                }
                if (action.equals("download")) {
                    downloadFile(mess);
                }
                Log.d(TAG, action + " : " + mess);
                if (action.isEmpty())
                    Log.d(TAG, "onConsoleMessage: " + consoleMessage.message() + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        };
        wv.setWebViewClient(wvClient);
        wv.setWebChromeClient(wvChromeClient);
    }

    @Override
    protected void onDestroy() {
//        Toast.makeText(this, "destroy", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        saveObject(url_to_name, "url_to_name");
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (wv != null && wv.canGoBack()) {
            wv.goBack();
        } else {
            super.onBackPressed();
        }
    }

    String filenameFromUrl(String url) {
        Pattern p = Pattern.compile("\\w*.jpg");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return url.substring(m.start(), m.end());
        }
        return "none";
    }

    void sendWallToDesktop(String fnm) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL("http://192.168.29.229:8080").openConnection();
                conn.setRequestMethod("POST");
//                conn.setRequestProperty("Content-Type","image/jpeg");
                byte[] buffer = new byte[1024];
                OutputStream out = conn.getOutputStream();
                FileInputStream in = new FileInputStream(new File(getExternalFilesDir(null), fnm));
                int rd = 0;
                while((rd=in.read(buffer))>0){
                    out.write(buffer,0,rd);
                }
                out.flush();
                out.close();

                Log.d(TAG, "Written");

                rd = conn.getInputStream().read(buffer);
                Log.d(TAG, new String(Arrays.copyOfRange(buffer, 0, rd)));
//               BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
//               StringBuilder response = new StringBuilder();
//               String responseLine = null;
//               while ((responseLine = br.readLine()) != null) {
//                   response.append(responseLine.trim());
//               }
//               Log.d(TAG, response.toString());;
            } catch (Exception e) {
                Log.d(TAG, Log.getStackTraceString(e));
            }
        }).start();
    }

    void setWallpaper(String fnm) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(getExternalFilesDir(null).getAbsolutePath() + "/" + fnm);
            DisplayMetrics met = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(met);
            int w = met.widthPixels;
            int h = met.heightPixels;
            int sh = (int) ((float) w / (float) bitmap.getWidth() * (float) bitmap.getHeight());
            Bitmap background = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(background);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, sh, true);

            canvas.drawBitmap(bitmap, 0, (h - sh) / 2, new Paint());

            background.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(new File(getExternalFilesDir(null), "test.png")));
            Log.d(TAG, String.valueOf(sh));
            WallpaperManager manager = WallpaperManager.getInstance(getApplicationContext());
            manager.setBitmap(background);
            spEditor.putString("current_wallpaper",fnm);
            spEditor.commit();
            Log.d(TAG, "Wallpaper set");
            sendWallToDesktop(fnm);
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
        random.post(() -> {
            random.setClickable(true);
            random.setText("W");
            Toast.makeText(getApplicationContext(), "Wallpaper Changed", Toast.LENGTH_SHORT).show();
        });
    }

    void downloadFile(String url) {
        String filename = filenameFromUrl(url);
        Log.d(TAG, filename);
        new Thread(() -> {
            try {
                File f = new File(getExternalFilesDir(null), filename);
                if (f.exists()) {
                    Log.d(TAG, "already exists");
                } else {
                    BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
                    FileOutputStream out = new FileOutputStream(f);
                    byte[] buffer = new byte[1024];
                    int rd;
                    while ((rd = in.read(buffer)) > 0) {
                        out.write(buffer, 0, rd);
                    }
                    out.close();
                    in.close();
                }
                Log.d(TAG, "success");
                url_to_name.get(HWV_LAST_VISIT).add(filename);
                recent_downloads.add(filename);

            } catch (Exception e) {
                Log.d(TAG, Log.getStackTraceString(e));
            }
            DOWNLOAD_STACK_COUNT--;
            if (POST_DOWNLOAD_ACTION.equals("wallpaper")) {
                if (DOWNLOAD_STACK_COUNT == 0) {
                    int ri = rnd.nextInt(recent_downloads.size());
                    Log.d(TAG, "Set wallpaper" + recent_downloads.get(ri));
                    setWallpaper(recent_downloads.get(ri));
                }
            }
        }).start();
    }
}