package com.example.asincronizedsounds

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private val activePlayers = mutableListOf<MediaPlayer>()
    private val playerLock = Any()
    private lateinit var tvStatus: TextView
    private var coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentToast: Toast? = null
    
    private val audioResources = mapOf(
        1 to R.raw.audio1,
        2 to R.raw.audio2,
        3 to R.raw.audio3,
        4 to R.raw.audio4
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvStatus = findViewById(R.id.tvStatus)
        

        findViewById<Button>(R.id.btnPlayAudio1).setOnClickListener {
            playAudioAsync(1, "Audio 1")
        }
        findViewById<Button>(R.id.btnPlayAudio2).setOnClickListener {
            playAudioAsync(2, "Audio 2")
        }
        findViewById<Button>(R.id.btnPlayAudio3).setOnClickListener {
            playAudioAsync(3, "Audio 3")
        }
        findViewById<Button>(R.id.btnPlayAudio4).setOnClickListener {
            playAudioAsync(4, "Audio 4")
        }
        

        findViewById<Button>(R.id.btnStopAll).setOnClickListener {
            stopAllAudios()
        }
        
        addLog("Aplicación iniciada")
        updateStatus("Listo")
    }
    
    private fun playAudioAsync(audioId: Int, audioName: String) {
        coroutineScope.launch {
            addLog("Iniciando reproducción: $audioName")
            updateStatus("Reproduciendo: $audioName")
            

            val result = withContext(Dispatchers.Default) {
                playAudio(audioId)
            }
            
            if (result) {
                addLog("✓ Reproducción completada: $audioName")
            } else {
                addLog("✗ Error al reproducir: $audioName")
            }
        }
    }
    
    private fun playAudio(audioId: Int): Boolean {
        return try {
            val resourceId = audioResources[audioId] ?: return false
            val mp = MediaPlayer.create(this, resourceId)

            synchronized(playerLock) {
                activePlayers.add(mp)
            }

            mp.setOnCompletionListener {
                addLog("Sonido $audioId terminó")
                synchronized(playerLock) {
                    activePlayers.remove(it)
                }
                it.release()
            }
            
            mp.start()
            true
        } catch (e: Exception) {
            addLog("Error: ${e.message}")
            false
        }
    }
    
    private fun stopAllAudios() {
        addLog("Deteniendo todos los audios...")
        val playersToStop = synchronized(playerLock) {
            activePlayers.toList()
        }

        for (mp in playersToStop) {
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                addLog("Error al detener un audio en reproducción")
            }
        }

        synchronized(playerLock) {
            activePlayers.clear()
        }

        addLog("Todos los audios detenidos")
        updateStatus("Detenido")
    }
    
    private fun addLog(message: String) {
        runOnUiThread {
            currentToast?.cancel()
            currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            currentToast?.show()
        }
    }
    
    private fun updateStatus(status: String) {
        runOnUiThread {
            tvStatus.text = "Estado: $status"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAllAudios()
        coroutineScope.cancel()

        synchronized(playerLock) {
            for (mp in activePlayers) {
                mp.release()
            }
            activePlayers.clear()
        }
    }

    override fun onStop() {
        stopAllAudios()
        super.onStop()
    }
}