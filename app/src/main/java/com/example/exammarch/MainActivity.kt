package com.example.exammarch

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.SeekBar
import com.example.exammarch.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var mp: MediaPlayer
    private lateinit var referenceMediaState: DatabaseReference
    private lateinit var referenceSeekbarPosition: DatabaseReference
    private var totalTime: Int = 0
    private var mediaPlayerState: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        referenceMediaState = Firebase.database.getReference("media_player_state")
        referenceSeekbarPosition = Firebase.database.getReference("media_player_position")


        /** Media player REALTIME Start or Stop*/
        referenceMediaState.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mediaPlayerState = snapshot.getValue<Boolean>() == true
                if (mediaPlayerState) {
                    mp.start()
                    binding.playBtn.setBackgroundResource(R.drawable.ic_baseline_pause_24)
                } else {
                    if (mp.isPlaying) {
                        mp.pause()
                        binding.playBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        /** Media player REALTIME position(progress) change listener*/
        referenceSeekbarPosition.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue<Int>()
                if (value != null) {
                    val position = (value * mp.duration / 100)
                    mp.seekTo(position)
                }

            }

            override fun onCancelled(error: DatabaseError) {}
        })


        mp = MediaPlayer.create(this, defaultRingtoneUri)
        mp.isLooping = true
        mp.setVolume(0.5f, 0.5f)
        totalTime = mp.duration


        //VolumeBar
        binding.volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volumeNum = progress / 100.0f
                    mp.setVolume(volumeNum, volumeNum)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        //Position Bar
        binding.positionBar.max = totalTime
        binding.positionBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mp.seekTo(progress)
                    referenceSeekbarPosition.setValue(progress*100/mp.duration)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        //Thread
        Thread(Runnable {
            while (mp != null) {
                try {
                    val msg = Message()
                    msg.what = mp.currentPosition
                    handler.sendMessage(msg)
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                }
            }
        }).start()


     mp.setOnCompletionListener {
         it.seekTo(0)
         it.pause()
         binding.playBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
         referenceMediaState.setValue(false)
     }
    }
    var handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            val currentPositon = msg.what

            //UpdatePosition
            binding.positionBar.progress = currentPositon

            //Update Labels
            val elapsedTime = createTimeLabel(currentPositon)
            binding.elapsedTimeLabel.text = elapsedTime

            val remainingTime = createTimeLabel(totalTime - currentPositon)
            binding.RemainningTimeLabel.text = "-$remainingTime"
        }
    }

    fun createTimeLabel(time: Int): String {
        var timeLabel = ""
        val min = time / 1000 / 60
        val sec = time / 1000 % 60

        timeLabel = "$min:"
        if (sec < 10) timeLabel += "0"
        timeLabel += sec

        return timeLabel
    }

    fun playBtnClick(view: View) {
        if (mp.isPlaying) {
            //Stop
            mp.pause()
            mediaPlayerState = false
            binding.playBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
        } else {
            //Start
            mp.start()
            mediaPlayerState = true
            binding.playBtn.setBackgroundResource(R.drawable.ic_baseline_pause_24)
        }
        referenceMediaState.setValue(mediaPlayerState)
    }

}