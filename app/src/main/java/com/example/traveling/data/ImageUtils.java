package com.example.traveling.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Utility class for converting images to/from Base64 for Firestore storage.
 * Images are compressed to stay under Firestore's 1 MB document limit.
 */
public final class ImageUtils {

    private static final int MAX_DIMENSION = 800;   // px
    private static final int JPEG_QUALITY = 60;      // 0-100

    private ImageUtils() {}

    /** Encode a Bitmap to a Base64 string (compressed JPEG) */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        Bitmap scaled = scaleBitmap(bitmap);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    /** Decode a Base64 string back to a Bitmap */
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /** Load a Bitmap from a content URI and encode to Base64 */
    public static String uriToBase64(Context context, Uri uri) {
        if (uri == null) return null;
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            return bitmapToBase64(bitmap);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap scaleBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return bitmap;
        float ratio = Math.min((float) MAX_DIMENSION / w, (float) MAX_DIMENSION / h);
        return Bitmap.createScaledBitmap(bitmap,
                Math.round(w * ratio), Math.round(h * ratio), true);
    }
}
