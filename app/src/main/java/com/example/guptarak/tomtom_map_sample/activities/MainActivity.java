package com.example.guptarak.tomtom_map_sample.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.guptarak.tomtom_map_sample.R;
import com.example.guptarak.tomtom_map_sample.model.Coordinate;
import com.example.guptarak.tomtom_map_sample.model.GeoResponse;
import com.example.guptarak.tomtom_map_sample.model.SearchResult;
import com.example.guptarak.tomtom_map_sample.request.SearchResultsRequest;
import com.example.guptarak.tomtom_map_sample.utils.SearchUtil;
import com.google.common.base.Optional;
import com.tomtom.online.sdk.common.location.LatLng;
import com.tomtom.online.sdk.map.CameraPosition;
import com.tomtom.online.sdk.map.Icon;
import com.tomtom.online.sdk.map.MapConstants;
import com.tomtom.online.sdk.map.MapFragment;
import com.tomtom.online.sdk.map.MarkerBuilder;
import com.tomtom.online.sdk.map.OnMapReadyCallback;
import com.tomtom.online.sdk.map.Route;
import com.tomtom.online.sdk.map.RouteBuilder;
import com.tomtom.online.sdk.map.TomtomMap;
import com.tomtom.online.sdk.map.model.MapTilesType;
import com.tomtom.online.sdk.routing.OnlineRoutingApi;
import com.tomtom.online.sdk.routing.RoutingApi;
import com.tomtom.online.sdk.routing.data.FullRoute;
import com.tomtom.online.sdk.routing.data.InstructionsType;
import com.tomtom.online.sdk.routing.data.Report;
import com.tomtom.online.sdk.routing.data.RouteQuery;
import com.tomtom.online.sdk.routing.data.RouteQueryBuilder;
import com.tomtom.online.sdk.routing.data.RouteResult;
import com.tomtom.online.sdk.routing.data.RouteType;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {
    private static final int NETWORK_THREADS_NUMBER = 4;
    private TomtomMap tomtomMap;
    private MapFragment mapFragment;
    private Location location;
    private TextInputEditText origin;
    private TextInputEditText name;
    private TextInputEditText destination;
    private RoutingApi routePlannerAPI;
    private TextView textCoordinates;
    protected Scheduler networkScheduler = Schedulers.from(Executors.newFixedThreadPool(NETWORK_THREADS_NUMBER));
    protected LocationManager locationManager;
    private List<LatLng> routeCoordinates;
    private RequestQueue requestQueue;
    private List<Location> bumpers;
    private LatLng nextLoc;
    private static final String BASE_URL = "http://192.168.43.109:8080/speedbumper/geocode";
    private TextToSpeech tts;
    private static final String MSG = "Alert breaker";
    private Button click;
    private boolean flag;
    private List<Location> mockCoordinates = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        tts = new TextToSpeech(this, onInitListener);
        mapFragment.getAsyncMap(onMapReadyCallback);
        requestQueue = Volley.newRequestQueue(this);
        destination = (TextInputEditText) findViewById(R.id.destination);
        origin = (TextInputEditText) findViewById(R.id.origin);
        name = (TextInputEditText) findViewById(R.id.name);
        click = (Button)findViewById(R.id.click);
        click.setText("start");
        click.setOnClickListener(buttonClick);
        destination.setOnEditorActionListener(onEditActionListner);
        routePlannerAPI = OnlineRoutingApi.create(getBaseContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        bumpers = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1.0f, locationListener);
    }


    private OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull TomtomMap map) {
            tomtomMap = map;
            tomtomMap.setMyLocationEnabled(true);
            tomtomMap.getUiSettings().setMapTilesType(MapTilesType.VECTOR);
            // tomtomMap.getUiSettings().getCurrentLocationView().hide();
            tomtomMap.setLanguage(Locale.getDefault().getLanguage());

            if (location != null) {
                tomtomMap.centerOn(
                        CameraPosition.builder(new LatLng(location.getLatitude(), location.getLongitude()))
                                .bearing(MapConstants.ORIENTATION_NORTH)
                                .zoom(30)
                                .build());
            }

        }
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        tomtomMap.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private EditText.OnEditorActionListener onEditActionListner = new EditText.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            boolean handle = false;
            if (i == EditorInfo.IME_ACTION_DONE) {
                String originValue = origin.getText().toString();
                String destinationValue = destination.getText().toString();
                if (originValue == null || originValue.length() == 0 || destinationValue == null || destinationValue.length() == 0) {
                    handle = false;
                } else {
                    showRoute();
                }
            }
            return handle;
        }
    };

    private void showRoute() {
        RouteQuery routeQuery = getRouteQuery();
        Disposable subscribe = routePlannerAPI.planRoute(routeQuery).subscribeOn(getWorkingScheduler())
                .observeOn(getResultScheduler())
                .subscribe(new Consumer<RouteResult>() {
                    @Override
                    public void accept(RouteResult routeResult) throws Exception {
                          //displayRoutes(routeResult);
                        displayRoutesMock(routeResult);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        System.out.println("Error in route");
                    }
                });

    }

    private RouteQuery getRouteQuery() {
        String origint[] = origin.getText().toString().split(",");
        String destT[] = destination.getText().toString().split(",");
        LatLng origniLatLng = new LatLng(Double.parseDouble(origint[0]),Double.parseDouble(origint[1]));
        LatLng destinationLatLng = new LatLng(Double.parseDouble(destT[0]),Double.parseDouble(destT[1]));
        // LatLng origin =  new LatLng(18.5554803,73.891771);//routeCoordinates.get(0);
        //LatLng first =   new LatLng(18.5559303,73.8918195);
        RouteQueryBuilder routeQueryBuilder = new RouteQueryBuilder(origniLatLng, destinationLatLng)
                .withMaxAlternatives(0)
                .withReport(Report.EFFECTIVE_SETTINGS)
                .withInstructionsType(InstructionsType.TEXT)
                .withRouteType(RouteType.FASTEST);
        return routeQueryBuilder;
    }

    private Scheduler getWorkingScheduler() {
        return networkScheduler;
    }

    private Scheduler getResultScheduler() {
        return AndroidSchedulers.mainThread();
    }


    private void displayRoutes(RouteResult routeResult) {
        tomtomMap.displayRoutesOverview();
        List<FullRoute> routes = routeResult.getRoutes();
        Optional<FullRoute> activeRoute = getActiveRoute(routes);

        if(activeRoute.isPresent()){
            routeCoordinates = activeRoute.get().getCoordinates();
        }

        for (FullRoute route : routes) {
            boolean isActiveRoute = activeRoute.isPresent() ? activeRoute.get().equals(route) : false;
            RouteBuilder routeBuilder = new RouteBuilder(route.getCoordinates())
                    .isActive(isActiveRoute);
            final Route mapRoute = tomtomMap.addRoute(routeBuilder);
        }

       LatLng origin = routeCoordinates.get(0);
       LatLng first = routeCoordinates.get(1);
       LatLng end =  routeCoordinates.get(routeCoordinates.size()-1);

       MarkerBuilder  markerBuilder = new MarkerBuilder(origin).icon(Icon.Factory.fromResources(getApplicationContext(),R.drawable.start_point))
               .tag("start");
        MarkerBuilder  markerBuilder1 = new MarkerBuilder(end).icon(Icon.Factory.fromResources(getApplicationContext(),R.drawable.end_point))
                .tag("end");
        tomtomMap.addMarker(markerBuilder);
        tomtomMap.addMarker(markerBuilder1);
       nextLoc = first;
       createRequest(origin,first);

    }


    private void displayRoutesMock(RouteResult routeResult) {
        tomtomMap.displayRoutesOverview();
        List<LatLng>routeCoordinateMock = new ArrayList();
        for(Location coordinate : mockCoordinates){
            routeCoordinateMock.add(SearchUtil.convertLocationToLatLng(coordinate));
        }
        routeCoordinates = routeCoordinateMock;

        LatLng origin =  routeCoordinates.get(0);
        LatLng first =   routeCoordinates.get(1);
        RouteBuilder routeBuilder = new RouteBuilder(routeCoordinates)
                    .isActive(true);
            final Route mapRoute = tomtomMap.addRoute(routeBuilder);
        LatLng end =  routeCoordinates.get(routeCoordinates.size()-1);
        MarkerBuilder  markerBuilder = new MarkerBuilder(origin).icon(Icon.Factory.fromResources(getApplicationContext(),R.drawable.start_point))
                .tag("start");
        MarkerBuilder  markerBuilder1 = new MarkerBuilder(end).icon(Icon.Factory.fromResources(getApplicationContext(),R.drawable.end_point))
                .tag("end");
        tomtomMap.addMarker(markerBuilder);
        tomtomMap.addMarker(markerBuilder1);
        nextLoc = first;
        createRequest(origin,first);

    }

    protected Optional<FullRoute> getActiveRoute(List<FullRoute> fullRoutes) {
        if (fullRoutes != null && fullRoutes.size() > 0) {
            return Optional.of(fullRoutes.get(0));
        }
        return Optional.absent();
    }


   private  LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            location = loc;
            name.setText(loc.getLatitude() + "," + loc.getLongitude());
            if (flag) {
                mockCoordinates.add(loc);
            } else {
                if (tomtomMap != null) {
                    tomtomMap.getMarkerSettings().removeMarkerByTag("loc");
                    MarkerBuilder markerBuilder = new MarkerBuilder(new LatLng(loc.getLatitude(), loc.getLongitude()))
                            .icon(Icon.Factory.fromResources(getApplicationContext(), R.drawable.navi))
                            .tag("loc");
                    tomtomMap.addMarker(markerBuilder);
                    tomtomMap.centerOn(new LatLng(loc.getLatitude(), loc.getLongitude()));
                }
                if (nextLoc != null) {
                    if (loc.distanceTo(SearchUtil.convertLatLngToLocation(nextLoc)) <= 10) {
                        int index = routeCoordinates.indexOf(nextLoc);
                        if (index + 1 != routeCoordinates.size() - 1) {
                            nextLoc = routeCoordinates.get(index + 1);
                            LatLng first = new LatLng(loc.getLatitude(), loc.getLongitude());
                            createRequest(SearchUtil.convertLocationToLatLng(loc), nextLoc);
                        }
                    } else if (SearchUtil.isWithinDistance(loc, bumpers)) {
                        speakOut(MSG);
                    }
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @Override
    protected void onPause(){
        super.onPause();
    }

    private void createRequest(final LatLng first, LatLng next){
    JSONObject jsonObject = SearchUtil.getJsonObject(first,next);
    if(jsonObject ==null)
        return;
    SearchResultsRequest searchResultsRequest = new SearchResultsRequest(Request.Method.POST, BASE_URL,jsonObject,GeoResponse.class,
            new Response.Listener<GeoResponse>() {
                @Override
                public void onResponse(GeoResponse response) {
                    for(SearchResult searchResult : response.getGeoResponse()){
                       for(Coordinate coordinate : searchResult.getBumpers().getLocation()){
                           Location loc = new Location("");
                           loc.setLatitude(coordinate.getLat());
                           loc.setLongitude(coordinate.getLon());
                           bumpers.add(loc);
                       }

                    }
                    for(Location coordinate : bumpers){
                        MarkerBuilder markerBuilder = new MarkerBuilder(new LatLng(coordinate.getLatitude(),coordinate.getLongitude()))
                                .icon(Icon.Factory.fromResources(getApplicationContext(),R.drawable.bump))
                                .tag("bumper");
                        tomtomMap.addMarker(markerBuilder);
                    }
                    Location firstLoc = new Location("");
                    firstLoc.setLatitude(first.getLatitude());
                    firstLoc.setLongitude(first.getLongitude());

                    if(SearchUtil.isWithinDistance(firstLoc,bumpers)){
                        speakOut(MSG);
                    }
                    tomtomMap.centerOn(
                            CameraPosition.builder(first)
                                    .bearing(MapConstants.ORIENTATION_NORTH)
                                    .zoom(30)
                                    .build());

                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //NetworkResponse response = error.networkResponse;
                    //String a = new String(response.data);
                    System.out.println("Error from web service");
                }
            }
    )
    {
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json; charset=utf8");
            return headers;
        }
    };


    searchResultsRequest.setRetryPolicy(new DefaultRetryPolicy(
            120000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    requestQueue.add(searchResultsRequest);
}

    TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status
        ) {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void speakOut(String text){
    tts.speak(text,TextToSpeech.QUEUE_FLUSH, null);
}

    View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
           if(!flag){
               click.setText("Stop");
               flag=true;
           }
           else{
               flag=false;
               click.setText("start");
           }
        }
    };


}

