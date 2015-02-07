package com.appathon.smartola;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity implements ConnectionCallbacks,
		OnConnectionFailedListener, LocationListener,
		OnMyLocationButtonClickListener, OnClickListener {

	private GoogleApiClient mGoogleApiClient;

	private Marker marker;

	private GoogleMap map;

	private Button ride, plan, explore, mini, sedan, rideNow, pool;

	private int centerX, centerY;

	private final double degreeChangePlace = 0.009046499 * 5;

	private final double degreeChangeCab = 0.009046499;

	private Marker[] places, cabs;

	private boolean riding = true, planning = false, exploring = false,
			sedanCab = false, miniCab = false, booking = false,
			pooling = false, booked = false;

	private LinearLayout categoryLayout, bookingLayout;

	private Context context;

	private Activity activity;

	private static final LocationRequest REQUEST = LocationRequest.create()
			.setInterval(5000) // 5 seconds
			.setFastestInterval(16) // 16ms = 60fps
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	private Geocoder geocoder;
	private List<Address> addresses = null;

	// Planning Variables
	private LatLng planSource;
	private ArrayList<LatLng> planIntermediate;
	private LatLng planDestination;
	private LatLng temp;
	private ProgressDialog busyDialog;
	private AlertDialog.Builder alert;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		context = getApplicationContext();
		activity = this;
		ride = (Button) findViewById(R.id.btnRide);
		plan = (Button) findViewById(R.id.btnPlan);
		this.registerForContextMenu(plan);
		explore = (Button) findViewById(R.id.btnExplore);
		mini = (Button) findViewById(R.id.btnMini);
		sedan = (Button) findViewById(R.id.btnSedan);
		rideNow = (Button) findViewById(R.id.btnRideNow);
		pool = (Button) findViewById(R.id.btnPool);
		bookingLayout = (LinearLayout) findViewById(R.id.layoutBooking);
		categoryLayout = (LinearLayout) findViewById(R.id.layoutCategories);
		categoryLayout.setVisibility(View.VISIBLE);
		ride.setOnClickListener(this);
		plan.setOnClickListener(this);
		explore.setOnClickListener(this);
		mini.setOnClickListener(this);
		sedan.setOnClickListener(this);
		rideNow.setOnClickListener(this);
		pool.setOnClickListener(this);
		planIntermediate = new ArrayList<LatLng>();

		busyDialog = new ProgressDialog(activity);
		busyDialog.setTitle("Please Wait!");
		busyDialog.setMessage("Loading....");
		busyDialog.setCancelable(false);
		geocoder = new Geocoder(this, Locale.getDefault());
		android.app.FragmentManager fm = getFragmentManager();
		MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(LocationServices.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();
		mGoogleApiClient.connect();
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int width = metrics.widthPixels;
		int height = metrics.heightPixels;
		centerX = width / 2;
		centerY = height / 2;
		mapFragment.getMapAsync(new OnMapReadyCallback() {
			public void onMapReady(GoogleMap googleMap) {
				map = googleMap;
				map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				map.setMyLocationEnabled(true);
				marker = map.addMarker(new MarkerOptions().position(new LatLng(
						0, 0)));

				map.setOnMapLoadedCallback(new OnMapLoadedCallback() {

					@Override
					public void onMapLoaded() {
						// TODO Auto-generated method stub
						zoomToMyLocation();
					}
				});

				map.setOnMarkerClickListener(new OnMarkerClickListener() {

					@Override
					public boolean onMarkerClick(Marker marker) {
						// TODO Auto-generated method stub
						marker.getPosition();
						if (exploring) {
							Toast.makeText(
									context,
									"A Cab Will Pick You In 10 Minutes. Pack Your Bags. :D",
									Toast.LENGTH_LONG).show();
						}
						return true;
					}
				});

				map.setOnCameraChangeListener(new OnCameraChangeListener() {

					@Override
					public void onCameraChange(CameraPosition position) {
						// TODO Auto-generated method stub
						marker.remove();
						LatLng post = map.getProjection().fromScreenLocation(
								new Point(centerX, centerY));
						post = new LatLng(post.latitude, post.longitude);
						marker = map.addMarker(new MarkerOptions().position(
								post).title("PickUp"));
						if (exploring) {
							removePlaces();
							setPlaces();
						}
						if (riding && post.latitude != 0 && post.longitude != 0) {
							removeSedanCabs();
							removeMiniCabs();
							if (sedanCab)
								setSedanCabs();
							if (miniCab)
								setMiniCabs();
						}
						Log.e("Location", "Latitude: " + post.latitude
								+ " Longitude: " + post.longitude);
					}
				});

				map.setOnMapLongClickListener(new OnMapLongClickListener() {

					@Override
					public void onMapLongClick(LatLng point) {
						// TODO Auto-generated method stub
						if (planning) {
							Log.e("Long Click", "Captured");
							temp = point;
							activity.openContextMenu(plan);
						}
					}
				});

				map.setOnMyLocationButtonClickListener(new OnMyLocationButtonClickListener() {

					@Override
					public boolean onMyLocationButtonClick() {
						marker.remove();
						zoomToMyLocation();
						LatLng location = new LatLng(
								LocationServices.FusedLocationApi
										.getLastLocation(mGoogleApiClient)
										.getLatitude(),
								LocationServices.FusedLocationApi
										.getLastLocation(mGoogleApiClient)
										.getLongitude());
						// map.addMarker(new
						// MarkerOptions().position(location));
						LocationServices.FusedLocationApi
								.getLastLocation(mGoogleApiClient);
						return false;
					}
				});
			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Select The Action");
		menu.add(0, v.getId(), 0, "Source");// groupId, itemId, order, title
		menu.add(0, v.getId(), 0, "Intermediate Point");
		menu.add(0, v.getId(), 0, "Destination");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getTitle() == "Source") {
			planSource = temp;
		} else if (item.getTitle() == "Intermediate Point") {
			planIntermediate.add(planIntermediate.size(), temp);
		} else if (item.getTitle() == "Destination") {
			planDestination = temp;
			findRoute();
		} else {
			return false;
		}
		return true;
	}

	private void findRoute() {
		busyDialog.show();
		alert = new AlertDialog.Builder(this);
		new LoadLocations().execute();
	}

	private class LoadLocations extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			alert.setTitle("Fill the Halts (in Minutes)");

			LinearLayout layout = new LinearLayout(context);
			LinearLayout.LayoutParams params = new LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT);
			layout.setLayoutParams(params);
			layout.setOrientation(LinearLayout.VERTICAL);
			final EditText[] input = new EditText[planIntermediate.size()];
			final TextView[] places = new TextView[(planIntermediate.size() + 1) * 2];
			final LinearLayout[] rows = new LinearLayout[planIntermediate
					.size() + 1];
			try {
				addresses = geocoder.getFromLocation(planSource.latitude,
						planSource.longitude, 1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			places[0] = new TextView(context);
			// places[0].setText(planSource.latitude + "," +
			// planSource.longitude);
			places[0].setText(addresses.get(0).getAddressLine(0));
			places[0].setBackgroundColor(Color.GREEN);

			places[((planIntermediate.size() + 1) * 2) - 1] = new TextView(
					context);
			try {
				addresses = geocoder.getFromLocation(planDestination.latitude,
						planDestination.longitude, 1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			places[((planIntermediate.size() + 1) * 2) - 1].setText(addresses
					.get(0).getAddressLine(0));
			places[((planIntermediate.size() + 1) * 2) - 1]
					.setBackgroundColor(Color.GREEN);
			int j = 0;
			for (int i = 1; i <= (((planIntermediate.size() + 1) * 2) - 2); i++) {
				places[i] = new TextView(context);
				try {
					addresses = geocoder.getFromLocation(
							planIntermediate.get(j).latitude,
							planIntermediate.get(j).longitude, 1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				places[i].setText(addresses.get(0).getAddressLine(0));
				places[i].setBackgroundColor(Color.GREEN);
				// places[i].setText(planIntermediate.get(j).latitude + ","
				// + planIntermediate.get(j).longitude);
				if (i % 2 == 0) {
					j++;
				}
			}
			j = 0;
			for (int i = 0; i <= planIntermediate.size(); i++) {

				rows[i] = new LinearLayout(context);
				rows[i].setLayoutParams(new LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT));
				rows[i].setOrientation(LinearLayout.HORIZONTAL);
				LayoutParams par = new LayoutParams(0,
						RelativeLayout.LayoutParams.WRAP_CONTENT, 1);
				par.topMargin = 10;
				par.bottomMargin = 10;
				par.leftMargin = 3;
				par.rightMargin = 3;
				places[j].setLayoutParams(par);
				places[j].setTextColor(Color.BLACK);
				rows[i].addView(places[j++]);
				places[j].setTextColor(Color.BLACK);
				places[j].setLayoutParams(par);
				rows[i].addView(places[j++]);
				if (i != planIntermediate.size()) {
					input[i] = new EditText(context);
					input[i].setHint("Halt");
					input[i].setLayoutParams(par);
					input[i].setTextColor(Color.BLACK);
					input[i].setInputType(InputType.TYPE_CLASS_NUMBER);
					rows[i].addView(input[i]);
				}
				LayoutParams para = new LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT, 0, 1);
				rows[i].setLayoutParams(para);
				layout.addView(rows[i]);
			}
			ScrollView scroll = new ScrollView(context);
			scroll.setBackgroundColor(android.R.color.transparent);
			scroll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
			scroll.addView(layout);
			alert.setView(scroll);

			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// String value = input.getText().toString();
							// Do something with value!
							Toast.makeText(
									context,
									"Your Itenarary has been saved. Cabs Will Arrive At Pickup Locations At Given Times!",
									Toast.LENGTH_LONG).show();
							planIntermediate = new ArrayList<LatLng>();
						}
					});

			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Canceled.
							planIntermediate = new ArrayList<LatLng>();
						}
					});
			return "";
		}

		@Override
		protected void onPostExecute(String response) {
			busyDialog.dismiss();
			alert.show();
		}
	}

	private void zoomToMyLocation() {
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
				new LatLng(LocationServices.FusedLocationApi.getLastLocation(
						mGoogleApiClient).getLatitude(),
						LocationServices.FusedLocationApi.getLastLocation(
								mGoogleApiClient).getLongitude()), 15);
		map.animateCamera(cameraUpdate);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnRide:
			riding = true;
			sedanCab = true;
			exploring = false;
			planning = false;
			miniCab = false;
			booking = false;
			pooling = false;
			removePlaces();
			removeSedanCabs();
			removeMiniCabs();
			setSedanCabs();
			zoomToMyLocation();
			bookingLayout.setVisibility(View.GONE);
			categoryLayout.setVisibility(View.VISIBLE);
			break;
		case R.id.btnPlan:
			riding = false;
			exploring = false;
			planning = true;
			booking = false;
			pooling = false;
			removePlaces();
			zoomToMyLocation();
			removeMiniCabs();
			removeSedanCabs();
			Toast.makeText(context,
					"Long Press A Point on Map To Add It To Journey!",
					Toast.LENGTH_SHORT).show();
			categoryLayout.setVisibility(View.GONE);
			bookingLayout.setVisibility(View.GONE);
			break;
		case R.id.btnExplore:
			riding = false;
			exploring = true;
			planning = false;
			booking = false;
			pooling = false;
			removePlaces();
			setPlaces();
			removeMiniCabs();
			removeSedanCabs();
			zoomOut();
			bookingLayout.setVisibility(View.GONE);
			categoryLayout.setVisibility(View.GONE);
			Toast.makeText(
					context,
					"Explore Nearby Tourist Attractions. Click On A Place To Directly Book A Mini Cab To That Place!",
					Toast.LENGTH_SHORT).show();
			break;
		case R.id.btnSedan:
			miniCab = false;
			sedanCab = true;
			booking = true;
			pooling = false;
			removeMiniCabs();
			setSedanCabs();
			zoomToMyLocation();
			bookingLayout.setVisibility(View.VISIBLE);
			break;
		case R.id.btnMini:
			sedanCab = false;
			miniCab = true;
			booking = true;
			pooling = false;
			removeSedanCabs();
			setMiniCabs();
			zoomToMyLocation();
			bookingLayout.setVisibility(View.VISIBLE);
			break;
		case R.id.btnRideNow:
			riding = false;
			sedanCab = false;
			exploring = false;
			planning = false;
			miniCab = false;
			booking = false;
			pooling = false;
			booked = true;
			removePlaces();
			removeSedanCabs();
			removeMiniCabs();
			bookingLayout.setVisibility(View.GONE);
			categoryLayout.setVisibility(View.GONE);
			Toast.makeText(context,
					"A Cab Will Pick You In 10 Minutes. Pack Your Bags. :D",
					Toast.LENGTH_LONG).show();
			break;
		case R.id.btnPool:
			Toast.makeText(context, "Will Implement If I Get Hired. :D",
					Toast.LENGTH_LONG).show();
			break;
		}
	}

	private void zoomOut() {
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(map
				.getProjection()
				.fromScreenLocation(new Point(centerX, centerY)), 12);
		map.animateCamera(cameraUpdate);
	}

	private void removePlaces() {
		try {
			if (places.length != 0 && places != null) {
				for (Marker place : places) {
					place.remove();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void setPlaces() {
		places = new Marker[4];
		LatLng post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude + degreeChangePlace, post.longitude);
		places[0] = map.addMarker(new MarkerOptions().position(post));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude, post.longitude + degreeChangePlace);
		places[1] = map.addMarker(new MarkerOptions().position(post));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude - degreeChangePlace, post.longitude);
		places[2] = map.addMarker(new MarkerOptions().position(post));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude, post.longitude - degreeChangePlace);
		places[3] = map.addMarker(new MarkerOptions().position(post));

	}

	private void removeMiniCabs() {
		try {
			if (cabs.length != 0 && cabs != null) {
				for (Marker cab : cabs) {
					cab.remove();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void setMiniCabs() {
		cabs = new Marker[4];
		LatLng post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude + degreeChangeCab, post.longitude);
		cabs[0] = map.addMarker(new MarkerOptions().position(post).icon(
				BitmapDescriptorFactory.fromResource(R.drawable.mini)));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude, post.longitude + degreeChangeCab);
		cabs[1] = map.addMarker(new MarkerOptions().position(post).icon(
				BitmapDescriptorFactory.fromResource(R.drawable.mini)));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude - degreeChangeCab, post.longitude);
		cabs[2] = map.addMarker(new MarkerOptions().position(post).icon(
				BitmapDescriptorFactory.fromResource(R.drawable.mini)));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude, post.longitude - degreeChangeCab);
		cabs[3] = map.addMarker(new MarkerOptions().position(post).icon(
				BitmapDescriptorFactory.fromResource(R.drawable.mini)));

	}

	private void removeSedanCabs() {
		try {
			if (cabs.length != 0 && cabs != null) {
				for (Marker cab : cabs) {
					cab.remove();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void setSedanCabs() {
		cabs = new Marker[4];
		LatLng post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude + degreeChangeCab, post.longitude);
		cabs[0] = map.addMarker(new MarkerOptions().position(post).icon(
				BitmapDescriptorFactory.fromResource(R.drawable.sedan)));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude, post.longitude + degreeChangeCab);
		cabs[1] = map.addMarker(new MarkerOptions().position(post).icon(
				BitmapDescriptorFactory.fromResource(R.drawable.sedan)));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude - degreeChangeCab, post.longitude);
		cabs[2] = map.addMarker(new MarkerOptions().position(post).icon(
				BitmapDescriptorFactory.fromResource(R.drawable.sedan)));
		post = map.getProjection().fromScreenLocation(
				new Point(centerX, centerY));
		post = new LatLng(post.latitude, post.longitude - degreeChangeCab);
		cabs[3] = map.addMarker(new MarkerOptions().position(post).icon(
				BitmapDescriptorFactory.fromResource(R.drawable.sedan)));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onMyLocationButtonClick() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle connectionHint) {
		// TODO Auto-generated method stub
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, REQUEST, this);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mGoogleApiClient.disconnect();
	}

	@Override
	protected void onResume() {
		mGoogleApiClient.connect();
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// TODO Auto-generated method stub

	}

}
