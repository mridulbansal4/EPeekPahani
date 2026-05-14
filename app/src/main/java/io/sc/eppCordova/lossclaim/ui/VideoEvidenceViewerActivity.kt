package io.sc.eppCordova.lossclaim.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.ActivityVideoViewerBinding
import io.sc.eppCordova.lossclaim.domain.model.EvidenceVideo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoEvidenceViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoViewerBinding
    private var player: ExoPlayer? = null
    private var videos: List<EvidenceVideo> = emptyList()
    private var villageGat = "Unknown"
    private var disasterTypeStr = "Unknown"

    companion object {
        private const val EXTRA_VIDEOS = "extra_videos"
        private const val EXTRA_VILLAGE_GAT = "extra_village_gat"
        private const val EXTRA_DISASTER = "extra_disaster"

        fun start(context: Context, videosJson: String, villageGat: String, disasterType: String) {
            val intent = Intent(context, VideoEvidenceViewerActivity::class.java).apply {
                putExtra(EXTRA_VIDEOS, videosJson)
                putExtra(EXTRA_VILLAGE_GAT, villageGat)
                putExtra(EXTRA_DISASTER, disasterType)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videosJson = intent.getStringExtra(EXTRA_VIDEOS) ?: "[]"
        villageGat = intent.getStringExtra(EXTRA_VILLAGE_GAT) ?: "Unknown"
        disasterTypeStr = intent.getStringExtra(EXTRA_DISASTER) ?: "Unknown"

        val type = object : TypeToken<List<EvidenceVideo>>() {}.type
        videos = Gson().fromJson(videosJson, type)

        binding.btnClose.setOnClickListener { finish() }
        
        setupPlayer()
        setupRecyclerView()

        if (videos.isNotEmpty()) {
            playVideo(videos[0])
        } else {
            binding.tvOverlayTimestamp.text = "No Video Evidence"
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
    }

    private fun setupRecyclerView() {
        binding.rvVideos.layoutManager = LinearLayoutManager(this)
        binding.rvVideos.adapter = VideoEvidenceAdapter(videos) { video ->
            playVideo(video)
        }
    }

    private fun playVideo(video: EvidenceVideo) {
        val file = File(video.videoPath)
        if (file.exists()) {
            val mediaItem = MediaItem.fromUri(video.videoPath)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            
            val sdf = SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault())
            val dateStr = sdf.format(Date(video.timestamp))
            binding.tvOverlayTimestamp.text = dateStr
            binding.tvOverlayLocation.text = villageGat
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    inner class VideoEvidenceAdapter(
        private val list: List<EvidenceVideo>,
        private val onClick: (EvidenceVideo) -> Unit
    ) : RecyclerView.Adapter<VideoEvidenceAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
            val tvVideoTitle: TextView = view.findViewById(R.id.tvVideoTitle)
            val tvVideoTimestamp: TextView = view.findViewById(R.id.tvVideoTimestamp)
            val tvVideoDuration: TextView = view.findViewById(R.id.tvVideoDuration)
            val tvGpsVerified: TextView = view.findViewById(R.id.tvGpsVerified)

            fun bind(video: EvidenceVideo, position: Int) {
                tvVideoTitle.text = "Inspection Video ${position + 1}"
                
                val sdf = SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault())
                tvVideoTimestamp.text = sdf.format(Date(video.timestamp))
                tvVideoDuration.text = "Duration: ${video.durationSeconds}s"

                if (video.gpsVerified) {
                    tvGpsVerified.text = "GPS Verified"
                } else {
                    tvGpsVerified.text = "No GPS"
                }

                // Load thumbnail using Glide
                Glide.with(itemView.context)
                    .load(File(video.videoPath))
                    .into(ivThumbnail)

                itemView.setOnClickListener { onClick(video) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_evidence, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position], position)
        }

        override fun getItemCount(): Int = list.size
    }
}