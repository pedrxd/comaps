package app.organicmaps.sdk.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import app.organicmaps.wear.protocol.WearMapMode;
import app.organicmaps.wear.protocol.WearMapState;
import org.junit.Test;

public class WearBridgeTest
{
  @Test
  public void publishesIdleWhenNavigationStops()
  {
    final RecordingPublisher publisher = new RecordingPublisher();
    WearBridge.register(publisher);

    WearBridge.publishNavigating(false);

    assertEquals(WearMapMode.IDLE, publisher.mLastState.getMode());
  }

  @Test
  public void doesNotPublishWhenNavigationStarts()
  {
    final RecordingPublisher publisher = new RecordingPublisher();
    WearBridge.register(publisher);

    WearBridge.publishNavigating(true);

    assertNull(publisher.mLastState);
  }

  @Test
  public void publishesNavigationSnapshot()
  {
    final RecordingPublisher publisher = new RecordingPublisher();
    WearBridge.register(publisher);

    final WearMapState state = WearMapState.navigation(55.75, 37.61, 15, 90f, true, 55.751, 37.611, null, null);
    WearBridge.publishNavigation(state);

    assertSame(state, publisher.mLastState);
  }

  @Test
  public void registrationReplacesPublisher()
  {
    final RecordingPublisher replaced = new RecordingPublisher();
    final RecordingPublisher current = new RecordingPublisher();
    WearBridge.register(replaced);
    WearBridge.register(current);

    WearBridge.publishNavigating(false);

    assertNull(replaced.mLastState);
    assertEquals(WearMapMode.IDLE, current.mLastState.getMode());
  }

  private static final class RecordingPublisher implements WearMapPublisher
  {
    private WearMapState mLastState;

    @Override
    public void publish(WearMapState state)
    {
      mLastState = state;
    }
  }
}
