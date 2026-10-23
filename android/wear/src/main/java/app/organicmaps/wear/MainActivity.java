package app.organicmaps.wear;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import app.organicmaps.wear.protocol.WearMapData;
import app.organicmaps.wear.protocol.WearMapState;
import app.organicmaps.wear.protocol.WearMapStateCodec;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Watch entry point. Read-only: when a route is assigned on the phone it mirrors the phone map and
 * route; when no route is assigned it stays on a black screen with instructions to create one.
 */
public final class MainActivity extends Activity implements DataClient.OnDataChangedListener
{
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final Uri MAP_STATE_URI = mapStateUri("*");

  private FrameLayout mRoot;
  private WearMapView mMapView;
  private LinearLayout mIdleLayout;
  private TextView mIdleMessage;
  private DataClient mDataClient;
  private CapabilityClient mCapabilityClient;
  private boolean mResumed;
  private int mLifecycleGeneration;
  private int mRefreshGeneration;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    mRoot = new FrameLayout(this);
    mRoot.setBackgroundColor(Color.BLACK);

    mMapView = new WearMapView(this);
    mMapView.setVisibility(View.GONE);
    mRoot.addView(mMapView, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

    mIdleLayout = new LinearLayout(this);
    mIdleLayout.setGravity(Gravity.CENTER);
    mIdleLayout.setOrientation(LinearLayout.VERTICAL);
    final int padding = getResources().getDimensionPixelSize(R.dimen.screen_padding);
    mIdleLayout.setPadding(padding, padding, padding, padding);

    final TextView title = new TextView(this);
    title.setGravity(Gravity.CENTER);
    title.setText(R.string.app_name);
    title.setTextColor(Color.WHITE);
    title.setTextSize(18);

    mIdleMessage = new TextView(this);
    mIdleMessage.setGravity(Gravity.CENTER);
    mIdleMessage.setText(R.string.wear_no_route_message);
    mIdleMessage.setTextColor(Color.WHITE);
    mIdleMessage.setTextSize(14);

    mIdleLayout.addView(title);
    mIdleLayout.addView(mIdleMessage);
    mRoot.addView(mIdleLayout, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

    setContentView(mRoot);
    renderIdle();

    mDataClient = Wearable.getDataClient(this);
    mCapabilityClient = Wearable.getCapabilityClient(this);
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    // Listen only while visible to avoid background wakeups. Register before reading the persistent
    // item so a state change cannot be missed between the initial read and listener registration.
    mResumed = true;
    final int generation = ++mLifecycleGeneration;
    mDataClient.addListener(this, MAP_STATE_URI, DataClient.FILTER_LITERAL)
        .addOnSuccessListener(unused -> {
          if (isResumed(generation))
            refreshMapState();
          else if (!mResumed)
            mDataClient.removeListener(this);
        })
        .addOnFailureListener(e -> {
          Log.w(TAG, "Failed to listen for map state", e);
          if (isResumed(generation))
          {
            ++mRefreshGeneration;
            renderIdle();
          }
        });
  }

  @Override
  protected void onPause()
  {
    mResumed = false;
    ++mLifecycleGeneration;
    ++mRefreshGeneration;
    mDataClient.removeListener(this)
        .addOnFailureListener(e -> Log.w(TAG, "Failed to stop map state listener", e));
    super.onPause();
  }

  @Override
  public void onDataChanged(DataEventBuffer dataEvents)
  {
    if (mResumed && WearStateSource.requiresRefresh(dataEvents))
      refreshMapState();
  }

  private void refreshMapState()
  {
    final int generation = ++mRefreshGeneration;
    mCapabilityClient.getCapability(WearMapData.CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE)
        .addOnSuccessListener(capability -> {
          if (!isCurrent(generation))
            return;

          final Node companion = WearStateSource.selectCompanion(capability.getNodes());
          if (companion == null)
          {
            renderIdle();
            return;
          }
          readMapState(companion, generation);
        })
        .addOnFailureListener(e -> {
          Log.w(TAG, "Failed to find the companion phone", e);
          if (isCurrent(generation))
            renderIdle();
        });
  }

  private void readMapState(@NonNull Node companion, int generation)
  {
    final Uri uri = mapStateUri(companion.getId());
    mDataClient.getDataItems(uri, DataClient.FILTER_LITERAL)
        .addOnSuccessListener(dataItems -> renderFromDataItems(dataItems, generation))
        .addOnFailureListener(e -> {
          Log.w(TAG, "Failed to read map state", e);
          if (isCurrent(generation))
            renderIdle();
        });
  }

  private void renderFromDataItems(@NonNull DataItemBuffer dataItems, int generation)
  {
    WearMapState state = null;
    try
    {
      // A node owns at most one DataItem at a path. Treat any unexpected result as no current state.
      if (dataItems.getCount() == 1)
        state = WearMapStateCodec.decode(dataItems.get(0).getData());
    }
    finally
    {
      dataItems.release();
    }

    if (isCurrent(generation))
      render(state);
  }

  private boolean isCurrent(int generation)
  {
    return mResumed && generation == mRefreshGeneration;
  }

  private boolean isResumed(int generation)
  {
    return mResumed && generation == mLifecycleGeneration;
  }

  private void renderIdle()
  {
    render(null);
  }

  private void render(WearMapState state)
  {
    final boolean navigating = state != null && state.isNavigating();
    if (navigating)
    {
      mIdleLayout.setVisibility(View.GONE);
      mMapView.setVisibility(View.VISIBLE);
      mMapView.show(state);
    }
    else
    {
      mMapView.setVisibility(View.GONE);
      mIdleLayout.setVisibility(View.VISIBLE);
    }
  }

  @NonNull
  private static Uri mapStateUri(@NonNull String authority)
  {
    return new Uri.Builder()
        .scheme(PutDataRequest.WEAR_URI_SCHEME)
        .authority(authority)
        .path(WearMapData.PATH_MAP_STATE)
        .build();
  }
}
