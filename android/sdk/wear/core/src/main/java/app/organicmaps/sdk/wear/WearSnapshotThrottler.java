package app.organicmaps.sdk.wear;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;

/**
 * Rate-limits Wear map snapshot publications.
 *
 * <p>Location updates and route changes arrive far more often than a watch can (or needs to)
 * consume them. The throttler forwards the first update immediately and coalesces the following ones
 * into a single trailing update, so the watch always converges to the latest state.
 */
public final class WearSnapshotThrottler
{
  public interface Callback
  {
    void onSnapshot();
  }

  private final long mMinIntervalMs;
  private final Callback mCallback;
  private long mLastPublishTime;
  private boolean mTrailingScheduled;

  public WearSnapshotThrottler(long minIntervalMs, @NonNull Callback callback)
  {
    mMinIntervalMs = minIntervalMs;
    mCallback = callback;
  }

  /** Requests a snapshot publication, applying the rate limit. Call from the main thread. */
  public synchronized void request()
  {
    final long now = SystemClock.uptimeMillis();
    final long elapsed = now - mLastPublishTime;
    if (mLastPublishTime == 0 || elapsed >= mMinIntervalMs)
    {
      mLastPublishTime = now;
      mCallback.onSnapshot();
    }
    else if (!mTrailingScheduled)
    {
      mTrailingScheduled = true;
      final long delay = mMinIntervalMs - elapsed;
      new Handler(Looper.getMainLooper()).postDelayed(this::fireTrailing, delay);
    }
  }

  private synchronized void fireTrailing()
  {
    mTrailingScheduled = false;
    mLastPublishTime = SystemClock.uptimeMillis();
    mCallback.onSnapshot();
  }
}
