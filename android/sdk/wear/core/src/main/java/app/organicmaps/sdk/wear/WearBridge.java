package app.organicmaps.sdk.wear;

import androidx.annotation.NonNull;
import app.organicmaps.wear.protocol.WearMapState;

/**
 * Routes map/route state to a paired Wear OS device.
 *
 * <p>The Google variant bundles {@code :sdk:wear:gms}, whose manifest-merged {@code ContentProvider}
 * registers the real publisher at startup. On F-Droid/Huawei/Web nothing registers and the bridge
 * falls back to a no-op, so callers in common code never reference Play services types.
 */
public final class WearBridge
{
  @NonNull
  private static final WearMapPublisher NO_OP = state -> {};

  @NonNull
  private static volatile WearMapPublisher sPublisher = NO_OP;

  /** Called once at process startup from the Google Wear module. */
  public static void register(@NonNull WearMapPublisher publisher)
  {
    sPublisher = publisher;
  }

  /** Maps a navigation-active flag to a published state; suitable as a routing state listener. */
  public static void publishNavigating(boolean navigating)
  {
    if (!navigating)
      sPublisher.publish(WearMapState.idle());
  }

  /** Publishes a full navigation snapshot (viewport, position, route and map image). */
  public static void publishNavigation(@NonNull WearMapState state)
  {
    sPublisher.publish(state);
  }

  private WearBridge() {}
}
