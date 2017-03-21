package kartaskup.samuki.me.kartaskup;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity {
    final static String url = "https://portal.kartaskup.pl/web/info/zaloguj";
    final static String cookieURL = "https://portal.kartaskup.pl/pl/web/info/karty";

    static SharedPreferences prefs;
    static String cookies;
    static String login, password;
    WebView webView;
    //LOADING
    Timer loading;
    boolean timerRunning;
    ProgressBar loadingCircle;
    int howManyTimesWentWrong;
    //TICKETS
    static String wallet;
    static Elements ticketsElements;
    //NO TICKETS
    LinearLayout[] noTickets;
    //IN USE
    private LinearLayout ticketLayoutInUse;
    LinearLayout[] ticketItemContainerInUse;
    LinearLayout[] ticketItemExpandInUse;
    int ticketsCountInUse;
    boolean[] cutInUse;
    //NOT IN USE
    private LinearLayout ticketLayoutNotInUse;
    LinearLayout[] ticketItemContainerNotInUse;
    LinearLayout[] ticketItemExpandNotInUse;
    int ticketsCountNotInUse;
    boolean[] cutNotInUse;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timerRunning = false;
        loadingCircle = (ProgressBar) this.findViewById(R.id.loading);
        //SET WEBVIEW
        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        MainActivity.MyWebViewClient webViewClient = new MainActivity.MyWebViewClient();

        webView.setWebViewClient(webViewClient);
        webView.layout(100, 100, 100, 100);
        webView.setVisibility(View.GONE);

        prefs = MainActivity.this.getPreferences(MODE_PRIVATE);
        cookies = null;
        //TICKETS SETTINGS
        ticketsCountInUse = 0;
        ticketLayoutInUse = (LinearLayout) findViewById(R.id.tickets_in_use);
        ticketsCountNotInUse = 0;
        ticketLayoutNotInUse = (LinearLayout) findViewById(R.id.tickets_not_in_use);

        noTickets = new LinearLayout[2];
        noTickets[0] = (LinearLayout) getLayoutInflater().inflate(R.layout.no_tickets, null);
        noTickets[1] = (LinearLayout) getLayoutInflater().inflate(R.layout.no_tickets, null);

        login = prefs.getString("Login", "");
        password = prefs.getString("Password", "");

        if(prefs.getBoolean("CardWasPin", false))
            new GetThingsFromWebsite().execute();
        howManyTimesWentWrong = 0;

    }

    //ADD LOGIN AND PASSWORD
    public void pinCard(View view) {
        if(timerRunning) {
            loading.cancel();
            timerRunning = false;
        }
        Intent intent = new Intent(this, CardAddActivity.class);
        startActivity(intent);
    }
    //WEBVIEW
    public void addCard() {
        System.out.println("ADD CARD");
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
        }
        else {
            cookieManager.removeAllCookie();
        }
        TimerTask loadingTask = new TimerTask() {
            @Override
            public void run() {
                wentWrong();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingCircle.setVisibility(View.GONE);
                    }
                });
            }
        };
        loading = new Timer();
        timerRunning = true;
        loading.schedule(loadingTask, 15000);

        webView.loadUrl(MainActivity.url);
    }
    //SOMETHING WENT WRONG
    public void wentWrong() {
        DialogFragment newFragment = new WentWrongDialog();
        newFragment.show(getSupportFragmentManager(), "missiles");
    }
    //GET THINGS FROM TICKETS
    public void getLists() {
        TextView walletView = (TextView) findViewById(R.id.wallet);
        walletView.setText(wallet);
        ticketItemContainerInUse = new LinearLayout[ticketsCountInUse];
        ticketItemExpandInUse = new LinearLayout[ticketsCountInUse];
        cutInUse = new boolean[ticketsCountInUse];

        if(ticketsCountInUse == 0) {
            ticketLayoutInUse.addView(noTickets[0]);
        }
        else {
            for (int i = 0; i < ticketsCountInUse; i++) {
                ticketItemContainerInUse[i] = (LinearLayout) getLayoutInflater().inflate(R.layout.ticket_items_container, null);
                ticketLayoutInUse.addView(ticketItemContainerInUse[i]);

                LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.ticket_list_item, null);
                TextView view0 = (TextView) item.findViewById(R.id.time_type_ticket);
                TextView view1 = (TextView) item.findViewById(R.id.ticket_type);
                TextView view2 = (TextView) item.findViewById(R.id.time_left);
                TextView view3 = (TextView) item.findViewById(R.id.ticket_id);

                ticketItemExpandInUse[i] = (LinearLayout) getLayoutInflater().inflate(R.layout.ticket_list_item_expand, null);
                TextView[] expandView = new TextView[6];
                expandView[0] = (TextView) ticketItemExpandInUse[i].findViewById(R.id.ticket_description_0);
                expandView[1] = (TextView) ticketItemExpandInUse[i].findViewById(R.id.ticket_description_1);
                expandView[2] = (TextView) ticketItemExpandInUse[i].findViewById(R.id.ticket_description_2);
                expandView[3] = (TextView) ticketItemExpandInUse[i].findViewById(R.id.ticket_description_3);
                expandView[4] = (TextView) ticketItemExpandInUse[i].findViewById(R.id.ticket_description_4);
                expandView[5] = (TextView) ticketItemExpandInUse[i].findViewById(R.id.ticket_description_5);

                boolean countTickets = false;
                int ticketInfo = -1;
                for (Element element : MainActivity.ticketsElements) {
                    if (element.hasText()) {
                        if (element.text().equals("Liczba przejazdów do wykorzystania"))
                            countTickets = true;
                        else if (element.text().equals("Opis"))
                            countTickets = false;
                        if (ticketInfo > -1 && ticketInfo < 6) {
                            expandView[ticketInfo].setText(element.text());
                        }
                        if (countTickets)
                            ticketInfo++;
                    }
                }

                String startDateString = expandView[3].getText().toString();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date startDate;
                try {
                    startDate = df.parse(startDateString);

                    Calendar now = Calendar.getInstance();
                    Calendar date = Calendar.getInstance();
                    date.setTime(startDate);

                    long milliseconds1 = date.getTimeInMillis();
                    long milliseconds2 = now.getTimeInMillis();
                    long diff = milliseconds1 - milliseconds2;
                    long diffDays = diff / (24 * 60 * 60 * 1000)+1;

                    remainder(milliseconds1 - (86400000*2));

                    if(diffDays == 1)
                        view1.setText("Ostatni dzień!");
                    else
                        view1.setText(diffDays + "");
                } catch (ParseException e) {
                    e.printStackTrace();
                    view1.setText("Error");
                }

                view0.setText(expandView[4].getText());
                view2.setText(expandView[5].getText());
                view3.setText("" + i);

                ticketItemContainerInUse[i].addView(item);

                cutInUse[i] = false;
            }
        }if(ticketsCountNotInUse == 0) {
            ticketLayoutNotInUse.addView(noTickets[0]);
        }
        else {
            for (int i = 0; i < ticketsCountNotInUse; i++) {
                ticketItemContainerNotInUse[i] = (LinearLayout) getLayoutInflater().inflate(R.layout.ticket_items_container, null);
                ticketLayoutNotInUse.addView(ticketItemContainerNotInUse[i]);

                LinearLayout item = (LinearLayout) getLayoutInflater().inflate(R.layout.ticket_list_item, null);
                TextView view0 = (TextView) item.findViewById(R.id.time_type_ticket);
                TextView view1 = (TextView) item.findViewById(R.id.ticket_type);
                TextView view2 = (TextView) item.findViewById(R.id.time_left);
                TextView view3 = (TextView) item.findViewById(R.id.ticket_id);

                ticketItemExpandNotInUse[i] = (LinearLayout) getLayoutInflater().inflate(R.layout.ticket_list_item_expand, null);
                TextView[] expandView = new TextView[6];
                expandView[0] = (TextView) ticketItemExpandNotInUse[i].findViewById(R.id.ticket_description_0);
                expandView[1] = (TextView) ticketItemExpandNotInUse[i].findViewById(R.id.ticket_description_1);
                expandView[2] = (TextView) ticketItemExpandNotInUse[i].findViewById(R.id.ticket_description_2);
                expandView[3] = (TextView) ticketItemExpandNotInUse[i].findViewById(R.id.ticket_description_3);
                expandView[4] = (TextView) ticketItemExpandNotInUse[i].findViewById(R.id.ticket_description_4);
                expandView[5] = (TextView) ticketItemExpandNotInUse[i].findViewById(R.id.ticket_description_5);

                boolean countTickets = false;
                int ticketInfo = -1;
                int justCount = 0;
                for (Element element : MainActivity.ticketsElements) {
                    justCount++;
                    if(justCount < 7) {
                        continue;
                    }
                    if (element.hasText()) {
                        if (element.text().equals("Liczba przejazdów do wykorzystania"))
                            countTickets = true;
                        else if (element.text().equals("Opis"))
                            countTickets = false;
                        if (ticketInfo > -1 && ticketInfo < 6) {
                            expandView[ticketInfo].setText(element.text());
                        }
                        if (countTickets)
                            ticketInfo++;
                    }
                }

                String startDateString = expandView[3].getText().toString();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date startDate;
                try {
                    startDate = df.parse(startDateString);

                    Calendar now = Calendar.getInstance();
                    Calendar date = Calendar.getInstance();
                    date.setTime(startDate);

                    long milliseconds1 = date.getTimeInMillis();
                    long milliseconds2 = now.getTimeInMillis();
                    long diff = milliseconds1 - milliseconds2;
                    long diffDays = diff / (24 * 60 * 60 * 1000)+1;

                    if(diffDays == 1)
                        view1.setText("Ostatni dzień!");
                    else
                        view1.setText(diffDays + "");
                } catch (ParseException e) {
                    e.printStackTrace();
                    view1.setText("Error");
                }

                view0.setText(expandView[4].getText());
                view2.setText(expandView[5].getText());
                view3.setText("" + i);

                ticketItemContainerNotInUse[i].addView(item);

                cutNotInUse[i] = false;
            }
        }

    }
    //EXPAND VIEW
    public void onClick(View view) {
        TextView textViewId = (TextView) view.findViewById(R.id.ticket_id);
        int ticketId = Integer.parseInt(textViewId.getText().toString());
        if(!cutInUse[ticketId]) {
            ticketItemContainerInUse[ticketId].addView(ticketItemExpandInUse[ticketId]);
            cutInUse[ticketId] = !cutInUse[ticketId];
        }
        else {
            ticketItemContainerInUse[ticketId].removeViewAt(1);
            cutInUse[ticketId] = !cutInUse[ticketId];
        }
    }
    //NOTIFICATION
    private void remainder(long when) {
        Intent remainderIntent = new Intent(this, TicketExpireNotify.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, remainderIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, when, pendingIntent);
    }

    //WEBVIEWCLIENT
    private class MyWebViewClient extends WebViewClient {
        boolean done = false;
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            System.out.println("NOICE");
            if(timerRunning) {
                loading.cancel();
                timerRunning = false;
            }
            return super.shouldOverrideUrlLoading(view, request);
        }
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            System.out.println("NOICE");
            if(timerRunning) {
                loading.cancel();
                timerRunning = false;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            System.out.println(view.getTitle());
            long startTime = System.currentTimeMillis();
            long timeRightNow = System.currentTimeMillis();
            if(view.getTitle().equals("Zarządzanie Kartami ŚKUP - ŚKUP")) {
                CookieSyncManager.getInstance().sync();
                CookieManager cM = CookieManager.getInstance();
                MainActivity.cookies = cM.getCookie(MainActivity.cookieURL);
                System.out.println(cookies);
                new GetThingsFromWebsite().execute();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                view.evaluateJavascript(
                        "javascript:document.getElementById('A2227:formularzLogowania:login').value = '"+login+"';" +
                        "javascript:document.getElementById('A2227:formularzLogowania:haslo').value = '"+password+"';" +
                        "javascript:document.getElementById('A2227:formularzLogowania:zalogujUserBtn').click();", null);
            }
            else {
                view.loadUrl(
                        "javascript:document.getElementById('A2227:formularzLogowania:login').value = '"+login+"';" +
                        "javascript:document.getElementById('A2227:formularzLogowania:haslo').value = '"+password+"';" +
                        "javascript:document.getElementById('A2227:formularzLogowania:zalogujUserBtn').click();");
            }
            while(!done) {
                if(timeRightNow > startTime+1000) {
                    done = true;
                }
                timeRightNow = System.currentTimeMillis();
            }
        }
    }
    //JSOUP!!!
    private class GetThingsFromWebsite extends AsyncTask<Void, Void, Void> {
        String title;
        Element justForWallet;
        @Override
        protected void onPreExecute(){
            loadingCircle.setVisibility(View.VISIBLE);
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Document doc;
                if(MainActivity.cookies != null) {
                    doc = Jsoup.connect(cookieURL)
                            .header("Cookie", MainActivity.cookies)
                            .get();
                    System.out.println("Tu będzie działać  "+doc.title());
                }
                else {
                    doc = Jsoup.connect(cookieURL)
                            .get();
                    System.out.println("TAK");
                }
                title = doc.title();

                MainActivity.ticketsElements = doc.getElementsByClass("ui-dt-c");
                justForWallet = doc.getElementById("A6408:idSzczegolyKarty:danePodstatoweTableBody");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(title != null) {
                if (!title.equals("Zarządzanie Kartami ŚKUP - ŚKUP")) {
                    System.out.println(title+"     TAKJSTBGDIHG");
                    howManyTimesWentWrong++;
                    if(howManyTimesWentWrong < 3)
                        addCard();
                } else {
                    wallet = justForWallet.child(0).child(1).child(1).text();
                    boolean countTicketsInUse = false;
                    boolean countTicketsNotInUse = false;
                    boolean nowNotInUse = false;
                    int ticketsInUse = 0;
                    int ticketsNotInUse = 0;
                    System.out.println("Jakoś się udało");
                    if (MainActivity.ticketsElements != null) {
                        for (Element element : MainActivity.ticketsElements) {
                            if (element.hasText()) {
                                if (countTicketsInUse) {
                                    ticketsInUse++;
                                    nowNotInUse = true;
                                    System.out.println(element.text());
                                }
                                else if(countTicketsNotInUse) {
                                    ticketsNotInUse++;
                                    System.out.println(element.text());
                                }
                                if (element.text().equals("Liczba przejazdów do wykorzystania") && !nowNotInUse)
                                    countTicketsInUse = true;
                                else if (element.text().equals("Opis")) {
                                    countTicketsInUse = false;
                                }
                                else if(element.text().equals("Liczba przejazdów do wykorzystania") && nowNotInUse)
                                    countTicketsNotInUse = true;
                            }
                        }
                        ticketsCountInUse = ticketsInUse / 6;
                        ticketsCountNotInUse = ticketsNotInUse / 6;
                        System.out.println(ticketsInUse+"        "+ticketsNotInUse);
                        getLists();
                    }
                    System.out.println(ticketsCountInUse);
                    loadingCircle.setVisibility(View.GONE);
                }
            }
            else
                System.out.println("NO CO JEST?");
        }
    }
}
