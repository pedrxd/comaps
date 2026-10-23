package app.organicmaps.wear;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.wear.protocol.WearMapState;

/**
 * Read-only map view for the watch: draws the map snapshot received from the phone and overlays
 * the route polyline and the user's position.
 */
public final class WearMapView extends View
{
  private static final int ROUTE_COLOR = 0xFF2196F3;
  private static final int MY_POSITION_COLOR = 0xFFE91E63;
  private static final int MY_POSITION_STROKE_COLOR = Color.WHITE;

  @NonNull
  private final Paint mRoutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  @NonNull
  private final Paint mPositionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  @NonNull
  private final Paint mPositionStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  @NonNull
  private final float[] mProjected = new float[2];

  @Nullable
  private WearMapState mState;
  @Nullable
  private Bitmap mSnapshot;
  @Nullable
  private Rect mSnapshotSrc;
  @NonNull
  private final Rect mSnapshotDst = new Rect();

  public WearMapView(Context context)
  {
    super(context);
    init();
  }

  public WearMapView(Context context, @Nullable AttributeSet attrs)
  {
    super(context, attrs);
    init();
  }

  private void init()
  {
    mRoutePaint.setStyle(Paint.Style.STROKE);
    mRoutePaint.setStrokeCap(Paint.Cap.ROUND);
    mRoutePaint.setStrokeJoin(Paint.Join.ROUND);
    mRoutePaint.setColor(ROUTE_COLOR);
    mRoutePaint.setStrokeWidth(getResources().getDimension(R.dimen.route_stroke_width));

    mPositionPaint.setStyle(Paint.Style.FILL);
    mPositionPaint.setColor(MY_POSITION_COLOR);

    mPositionStrokePaint.setStyle(Paint.Style.STROKE);
    mPositionStrokePaint.setColor(MY_POSITION_STROKE_COLOR);
    mPositionStrokePaint.setStrokeWidth(getResources().getDimension(R.dimen.my_position_stroke_width));

    setBackgroundColor(Color.BLACK);
  }

  public void show(@Nullable WearMapState state)
  {
    mState = state;
    if (state != null && state.getSnapshot() != null)
    {
      final Bitmap bitmap = BitmapFactory.decodeByteArray(state.getSnapshot(), 0, state.getSnapshot().length);
      if (bitmap != null)
      {
        if (mSnapshot != null && !mSnapshot.isRecycled())
          mSnapshot.recycle();
        mSnapshot = bitmap;
        mSnapshotSrc = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
      }
    }
    invalidate();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh)
  {
    super.onSizeChanged(w, h, oldw, oldh);
    mSnapshotDst.set(0, 0, w, h);
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas)
  {
    super.onDraw(canvas);
    final WearMapState state = mState;
    if (state == null || !state.isNavigating())
      return;

    if (mSnapshot != null && mSnapshotSrc != null)
      canvas.drawBitmap(mSnapshot, mSnapshotSrc, mSnapshotDst, null);

    final int width = getWidth();
    final int height = getHeight();
    if (width <= 0 || height <= 0)
      return;

    final WearMapProjector projector =
        new WearMapProjector(width, height, state.getCenterLat(), state.getCenterLon(),
                             state.getZoom(), state.getBearing());

    final double[] route = state.getRoutePoints();
    if (route != null && route.length >= 4)
    {
      final Path path = new Path();
      projector.project(route[0], route[1], mProjected);
      path.moveTo(mProjected[0], mProjected[1]);
      for (int i = 2; i + 1 < route.length; i += 2)
      {
        projector.project(route[i], route[i + 1], mProjected);
        path.lineTo(mProjected[0], mProjected[1]);
      }
      canvas.drawPath(path, mRoutePaint);
    }

    if (state.hasMyPosition())
    {
      projector.project(state.getMyPositionLat(), state.getMyPositionLon(), mProjected);
      final float radius = getResources().getDimension(R.dimen.my_position_radius);
      canvas.drawCircle(mProjected[0], mProjected[1], radius, mPositionStrokePaint);
      canvas.drawCircle(mProjected[0], mProjected[1], radius * 0.7f, mPositionPaint);
    }
  }

  @Override
  protected void onDetachedFromWindow()
  {
    if (mSnapshot != null && !mSnapshot.isRecycled())
    {
      mSnapshot.recycle();
      mSnapshot = null;
    }
    super.onDetachedFromWindow();
  }
}
