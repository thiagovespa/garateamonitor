package br.com.thiagovespa.android.garateamonitor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by thiago on 10/24/17.
 */

public class Utils {


    /**
     * Add a marker text
     * @param context context
     * @param map map to add the marker
     * @param location location of the marker
     * @param text text to be added
     * @param padding padding of the text
     * @param fontSize Size of the font
     * @param color color of the text
     * @return marker
     */
    public static Marker addText(final Context context, final GoogleMap map,
                          final LatLng location, final String text, final int padding,
                          final int fontSize, int color) {
        Marker marker = null;

        if (context == null || map == null || location == null || text == null
                || fontSize <= 0) {
            return marker;
        }

        final TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(fontSize);

        final Paint paintText = textView.getPaint();

        final Rect boundsText = new Rect();
        paintText.getTextBounds(text, 0, textView.length(), boundsText);
        paintText.setTextAlign(Paint.Align.CENTER);

        final Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        final Bitmap bmpText = Bitmap.createBitmap(boundsText.width() + 2
                * padding, boundsText.height() + 2 * padding, conf);

        final Canvas canvasText = new Canvas(bmpText);
        paintText.setColor(color);

        canvasText.drawText(text, canvasText.getWidth() / 2,
                canvasText.getHeight() - padding - boundsText.bottom, paintText);

        final MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.fromBitmap(bmpText))
                .anchor(0.5f, 1);

        marker = map.addMarker(markerOptions);

        return marker;
    }

    public static int dpFromPx(final Context context, final int px) {
        return (int) ((float) px / context.getResources().getDisplayMetrics().density);
    }

    public static int pxFromDp(final Context context, final int dp) {
        return (int) ((float) dp * context.getResources().getDisplayMetrics().density);
    }
}
