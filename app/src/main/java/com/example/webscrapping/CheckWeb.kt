package com.example.webscrapping

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.lang.Exception
import org.jsoup.Jsoup

class CheckWeb(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    /**
     * Método principal que realiza la tarea en segundo plano.
     *
     * <p>Obtiene la URL y la palabra a buscar desde `SharedPreferences`,
     * se conecta a la página web usando `Jsoup`, y verifica si la palabra
     * está presente en el contenido. Si encuentra la palabra, llama a
     * `sendNotification` para enviar una notificación.</p>
     *
     * @return Un objeto [Result] que indica si el trabajo fue exitoso o no.
     */
    override fun doWork(): Result {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("WebCheckerPrefs", Context.MODE_PRIVATE)

            val semaforo = sharedPreferences.getString("semaforo", null) ?: return Result.failure()

            if (isStopped || semaforo.equals("R")) {
                return Result.failure()
            }

            // Obtener la URL y la palabra desde las preferencias compartidas
            val url = sharedPreferences.getString("url", null) ?: return Result.failure()
            val word = sharedPreferences.getString("word", null) ?: return Result.failure()

            if (isStopped || semaforo.equals("R")) {
                return Result.failure()
            }

            // Conectar a la página web
            val doc = Jsoup.connect(url).get()

            // Dividir el contenido en párrafos
            val paragraphs = doc.select("p")  // Selecciona todos los párrafos

            // Buscar la palabra en los párrafos
            var foundAt = ""
            for (i in paragraphs.indices) {
                val paragraph = paragraphs[i].text()
                val words = paragraph.split(" ")  // Dividir el párrafo en palabras

                for (j in words.indices) {
                    if (words[j].contains(word, ignoreCase = true)) {
                        // Si la palabra se encuentra, obtener las dos palabras antes y después
                        val before = if (j > 0) words[j - 1] else ""
                        val after = if (j < words.size - 1) words[j + 1] else ""
                        foundAt = "Palabra encontrada en párrafo ${i + 1}: $before ${words[j]} $after..."
                        break
                    }
                }

                if (foundAt.isNotEmpty()) {
                    sendNotification(foundAt, url)  // Pasamos tanto el párrafo como la URL
                    break
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }

        return Result.success()
    }


    /**
     * Envía una notificación al usuario si se encuentra la palabra en la página web.
     *
     * <p>La notificación se muestra con el título "¡Se encontró la palabra!"
     * y un mensaje que indica que la palabra fue encontrada.</p>
     */
    @SuppressLint("NotificationPermission")
    private fun sendNotification(foundAt: String, url: String) {
        val context = applicationContext

        // Intent para abrir la URL en el navegador
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear la notificación con el párrafo encontrado y la URL
        val notification = NotificationCompat.Builder(context, "guestlist_channel")
            .setContentTitle("¡Se encontró la palabra!")
            .setContentText("$foundAt\nToca para abrir la página.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)  // Al tocar la notificación, se abre la URL
            .setAutoCancel(true)  // La notificación se cierra automáticamente cuando se toca
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}
