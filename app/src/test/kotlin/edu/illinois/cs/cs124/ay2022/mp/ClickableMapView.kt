package edu.illinois.cs.cs124.ay2022.mp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.os.SystemClock
import android.view.GestureDetector
import android.view.GestureDetector.OnDoubleTapListener
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/*
 * Helper class for osmdroid MapView testing on later checkpoints.
 * Prying into the internals of this class is necessary to be able to deliver click events during
 * testing.
 * You should not need to modify it.
 *
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class ClickableMapView(private val mapView: MapView) {
    private var doubleTapListener: OnDoubleTapListener? = null
    private var gestureListener: GestureDetector.OnGestureListener? = null

    init {
        /*
         * The code below is generally considered to be NOT A GOOD IDEA.
         * We are using a feature of Java called reflection to allow us to access private fields on the
         * MapView class.
         * This is the only way (at least that I could figure out) how to deliver click events to the
         * MapView during testing.
         *
         * Because these fields are not part of the public API of the MapView class, the
         * code below could stop working at any moment: for example, if these fields were ever
         * renamed.
         * However, to allow the MapView component to be able to be tested, this was currently
         * necessary.
         *
         * The next right thing to do here would be to contact the maintainers of osmdroid and ask
         * that these fields be opened, or that new interfaces be provided to facilitate testing.
         * And hey, someone did that: https://github.com/osmdroid/osmdroid/issues/1859.
         */
        try {
            val mGestureDetector = MapView::class.java.getDeclaredField("mGestureDetector")
            mGestureDetector.isAccessible = true
            val detector = mGestureDetector[mapView] as GestureDetector
            val mDoubleTapListener = GestureDetector::class.java.getDeclaredField("mDoubleTapListener")
            mDoubleTapListener.isAccessible = true
            doubleTapListener = mDoubleTapListener[detector] as OnDoubleTapListener
            val mListener = GestureDetector::class.java.getDeclaredField("mListener")
            mListener.isAccessible = true
            gestureListener = mListener[detector] as GestureDetector.OnGestureListener
            check(!(doubleTapListener == null || gestureListener == null))
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
        update()
    }

    // Update the MapView
    fun update() {
        // Robolectric does not replicate certain calls to layout components.
        // Failing to call the draw method will result in markers that are not positioned, which makes testing
        // impossible
        mapView.draw(
            Canvas(
                Bitmap.createBitmap(mapView.width, mapView.height, Bitmap.Config.ARGB_8888)
            )
        )
    }

    // Deliver a click event to the map at a specified x, y pixel coordinates value
    fun click(point: Point): Boolean {
        // Create and deliver the event
        val e = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            point.x.toFloat(),
            point.y.toFloat(),
            0
        )
        return doubleTapListener!!.onSingleTapConfirmed(e)
    }

    // Deliver a long press event to the map at a specified latitude and longitude
    fun longPress(geoPoint: GeoPoint?) {
        // Convert the latitude and longitude to a x, y pixel coordinates value
        val point = Point()
        mapView.projection.toPixels(geoPoint, point)

        // Create and deliver the event
        val e = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            point.x.toFloat(),
            point.y.toFloat(),
            0
        )
        gestureListener!!.onLongPress(e)
    }
}
