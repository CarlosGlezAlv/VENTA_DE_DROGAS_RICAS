package com.example.venta_de_drogas_ricas;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfManager {

    private static PdfConfig cargarConfiguracion(Context context) {
        try {
            InputStream is = context.getAssets().open("pdf.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            return new Gson().fromJson(json, PdfConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void exportarVentaAPdf(Context context, VentasManager.VentaRecord venta) {
        PdfConfig config = cargarConfiguracion(context);
        String nombreTienda = "Mi Tienda";
        int colorEnfasis = Color.BLACK;
        float textSizeNormal = 12f;
        float textSizeTitle = 18f;

        if (config != null) {
            if (config.negocio != null && config.negocio.nombre_tienda != null) {
                nombreTienda = config.negocio.nombre_tienda;
            }
            if (config.apariencia != null) {
                try {
                    colorEnfasis = Color.parseColor(config.apariencia.color_enfasis);
                } catch (Exception e) {
                    // ignorar y dejar negro
                }
                if ("pequeno".equalsIgnoreCase(config.apariencia.tamano_texto)) {
                    textSizeNormal = 10f;
                    textSizeTitle = 16f;
                } else if ("grande".equalsIgnoreCase(config.apariencia.tamano_texto)) {
                    textSizeNormal = 14f;
                    textSizeTitle = 22f;
                }
            }
        }

        PdfDocument document = new PdfDocument();
        // Un ticket estándar suele ser largo. Usaremos un ancho de 300 puntos y largo variable.
        int pageWidth = 300;
        int pageHeight = 600 + (venta.productos != null ? venta.productos.size() * 30 : 0);
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(textSizeTitle);
        titlePaint.setColor(colorEnfasis);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        paint.setTextSize(textSizeNormal);
        paint.setColor(Color.BLACK);

        int y = 40;
        
        // Cabecera
        canvas.drawText(nombreTienda, pageWidth / 2, y, titlePaint);
        y += 30;

        paint.setTextAlign(Paint.Align.LEFT);
        
        // Info de Venta
        canvas.drawText("Folio: " + (venta.folio != null ? venta.folio : "N/A"), 20, y, paint);
        y += 20;
        canvas.drawText("Fecha: " + venta.fecha, 20, y, paint);
        y += 30;

        // Separador
        Paint linePaint = new Paint();
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(1f);
        canvas.drawLine(20, y, pageWidth - 20, y, linePaint);
        y += 20;

        // Productos
        titlePaint.setTextSize(textSizeNormal);
        titlePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("CANT", 20, y, titlePaint);
        canvas.drawText("PRODUCTO", 70, y, titlePaint);
        
        titlePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("SUBTOTAL", pageWidth - 20, y, titlePaint);
        y += 20;

        paint.setTextAlign(Paint.Align.LEFT);
        MonedaManager monedaManager = MonedaManager.getInstance(context);
        String moneda = venta.moneda_visual != null ? venta.moneda_visual : "MXN";

        if (venta.productos != null) {
            for (VentasManager.ProductoVendido p : venta.productos) {
                canvas.drawText(String.valueOf((int)p.cantidad), 20, y, paint);
                // Si el nombre es muy largo, recortarlo
                String nombreCto = p.nombre;
                if (nombreCto.length() > 15) {
                    nombreCto = nombreCto.substring(0, 15) + "...";
                }
                canvas.drawText(nombreCto, 70, y, paint);
                
                String subFmt = monedaManager.formatear(java.math.BigDecimal.valueOf(p.subtotal), moneda);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(subFmt, pageWidth - 20, y, paint);
                paint.setTextAlign(Paint.Align.LEFT);
                y += 20;
            }
        }

        y += 10;
        canvas.drawLine(20, y, pageWidth - 20, y, linePaint);
        y += 20;

        // Total
        titlePaint.setTextSize(textSizeTitle);
        titlePaint.setTextAlign(Paint.Align.RIGHT);
        String totalFmt = monedaManager.formatear(java.math.BigDecimal.valueOf(venta.total_visual), moneda);
        canvas.drawText("TOTAL: " + totalFmt, pageWidth - 20, y, titlePaint);

        document.finishPage(page);

        // Guardar archivo
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Recibo_" + (venta.folio != null ? venta.folio : timestamp) + ".pdf";

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                    if (os != null) {
                        document.writeTo(os);
                        Toast.makeText(context, "PDF guardado en Descargas: " + fileName, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Fallback
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(path, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    document.writeTo(fos);
                    Toast.makeText(context, "PDF guardado en Descargas: " + fileName, Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al guardar el PDF", Toast.LENGTH_SHORT).show();
        }

        document.close();
    }
}
