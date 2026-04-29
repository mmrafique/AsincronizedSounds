import wave
import struct
import math
import shutil
import os

os.chdir(r"c:\Users\User\Desktop\ESCALATOPIA\app\src\main\res\raw")

def create_audio_file(filename, frequency, duration, sample_rate=44100):
    """Crear archivo de audio WAV"""
    num_samples = duration * sample_rate
    frames = bytearray()
    
    for i in range(num_samples):
        sample = int(32767 * 0.3 * math.sin(2 * math.pi * frequency * i / sample_rate))
        frames.extend(struct.pack('<h', sample))
    
    with wave.open(filename, 'wb') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(bytes(frames))

print("Creando archivos de audio...")
create_audio_file('audio1.wav', 261.63, 2)   # Do
create_audio_file('audio2.wav', 329.63, 2)   # Mi
create_audio_file('audio3.wav', 392.00, 2)   # Sol
create_audio_file('audio4.wav', 523.25, 2)   # Do (octava alta)

# Renombrar a .mp3
for i in range(1, 5):
    try:
        shutil.move(f'audio{i}.wav', f'audio{i}.mp3')
        print(f"✓ audio{i}.mp3 creado")
    except Exception as e:
        print(f"✗ Error en audio{i}: {e}")

print("\nArchivos listos!")
