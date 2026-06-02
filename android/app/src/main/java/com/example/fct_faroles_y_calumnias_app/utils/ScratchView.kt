package com.example.fct_faroles_y_calumnias_app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ScratchView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var scratchBitmap: Bitmap? = null
    private var scratchCanvas: Canvas? = null
    private val scratchPaint = Paint()
    private val path = Path()
    private var revealed = false
    var onScratchedListener: (() -> Unit)? = null

    init {
        scratchPaint.apply {
            isAntiAlias = true
            strokeWidth = 60f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scratchBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        scratchCanvas = Canvas(scratchBitmap!!)
        dibujarCapaRasca(scratchCanvas!!, w, h)
    }

    private fun dibujarCapaRasca(canvas: Canvas, w: Int, h: Int) {
        // Fondo rojo oscuro
        val paintFondo = Paint()
        paintFondo.color = Color.parseColor("#8B0000")
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paintFondo)

        // Textura de puntos
        val paintPuntos = Paint()
        paintPuntos.color = Color.parseColor("#6B0000")
        paintPuntos.style = Paint.Style.FILL
        val espaciado = 30f
        val tamPunto = 6f
        var x = espaciado
        while (x < w) {
            var y = espaciado
            while (y < h) {
                canvas.drawRect(x, y, x + tamPunto, y + tamPunto, paintPuntos)
                y += espaciado
            }
            x += espaciado
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        scratchBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (revealed) return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                scratchCanvas?.drawPath(path, scratchPaint)
                invalidate()
                comprobarPorcentaje()
            }
            MotionEvent.ACTION_UP -> {
                path.reset()
            }
        }
        return true
    }

    private fun comprobarPorcentaje() {
        val bitmap = scratchBitmap ?: return
        val totalPixeles = bitmap.width * bitmap.height
        var pixelesTransparentes = 0

        val paso = 10
        var x = 0
        while (x < bitmap.width) {
            var y = 0
            while (y < bitmap.height) {
                if (Color.alpha(bitmap.getPixel(x, y)) == 0) {
                    pixelesTransparentes++
                }
                y += paso
            }
            x += paso
        }

        val porcentaje = (pixelesTransparentes.toFloat() / (totalPixeles / (paso * paso))) * 100

        if (porcentaje >= 65f && !revealed) {
            revealed = true
            revelarTodo()
        }
    }

    private fun revelarTodo() {
        scratchCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
        onScratchedListener?.invoke()
    }
}