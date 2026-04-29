package com.example.asincronizedsounds

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private val activePlayers = mutableListOf<MediaPlayer>()
    private val playerLock = Any()
    private val audioLog = mutableListOf<String>()
    private lateinit var logAdapter: ArrayAdapter<String>
    private lateinit var tvStatus: TextView
    private var coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
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
        val lvAudioLog: ListView = findViewById(R.id.lvAudioLog)
        
        // Configurar adaptador para el log
        logAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, audioLog)
        lvAudioLog.adapter = logAdapter
        
        // Configurar botones para reproducir audios
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
        
        // Botón para detener todos los audios
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
            
            // Ejecutar reproducción en hilo separado
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
                addLog("Sonido $audioId terminó naturalmente")
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
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message"
        
        runOnUiThread {
            audioLog.add(0, logEntry)
            if (audioLog.size > 50) {
                audioLog.removeAt(audioLog.size - 1)
            }
            logAdapter.notifyDataSetChanged()
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