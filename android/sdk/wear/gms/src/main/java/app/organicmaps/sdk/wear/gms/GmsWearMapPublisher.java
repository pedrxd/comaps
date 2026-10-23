package app.organicmaps.sdk.wear.gms;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import app.organicmaps.sdk.wear.WearMapPublisher;
import app.organicmaps.wear.protocol.WearMapData;
import app.organicmaps.wear.protocol.WearMapState;
import app.organicmaps.wear.protocol.WearMapStateCodec;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Publishes map/route state to a paired Wear OS device through the Google Wear Data Layer.
 *
 * <p>Lives in the google-only {@code :sdk:wear:gms} module and is registered into
 * {@link app.organicmaps.sdk.wear.WearBridge} at startup by {@link WearBridgeInitProvider}.
 */
final class GmsWearMapPublisher implements WearMapPublisher
{
  private static final String TAG = GmsWearMapPublisher.class.getSimpleName();

  @NonNull
  private final Context mContext;

  GmsWearMapPublisher(@NonNull Context context)
  {
    mContext = context.getApplicationContext();
  }

  @Override
  public void publish(@NonNull WearMapState state)
  {
    final byte[] payload = WearMapStateCodec.encode(state);
    final PutDataRequest request = PutDataRequest.create(WearMapData.PATH_MAP_STATE);
    request.setData(payload);
    request.setUrgent();
    Wearable.getDataClient(mContext).putDataItem(request)
        .addOnFailureListener(e -> Log.w(TAG, "Failed to publish Wear map state", e));
  }
}
