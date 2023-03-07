package com.example.musik.Song

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musik.Playerpage.PlayerActivity
import com.example.musik.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.material.card.MaterialCardView

/**
 * @author Phong-Kaster
 * @since 03-03-2023
 * recycler view adapter for the "Song" class
 */
class SongAdapter constructor(private val context: Context, var list: ArrayList<Song>,var exoPlayer: ExoPlayer) :
    RecyclerView.Adapter<SongAdapter.ViewHolder>() {

    /* Create new views (invoked by the layout manager)*/
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        /*Create a new view, which defines the UI of the list item*/
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_view_element_song, viewGroup, false)

        return ViewHolder(view)
    }

    /*Return the size of your dataset (invoked by the layout manager)*/
    override fun getItemCount() = list.size

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView
        val albumCover: ImageView
        val artist: TextView
        val album: TextView
        val layout: MaterialCardView

        init {
             /*Define click listener for the ViewHolder's View*/
            name = view.findViewById(R.id.name)
            albumCover = view.findViewById(R.id.albumCover)
            album = view.findViewById(R.id.album)
            artist = view.findViewById(R.id.artist)
            layout = view.findViewById(R.id.cardView)
        }
    }



    /* Replace the contents of a view (invoked by the layout manager)*/
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val song = list[position]

        /* Get element from your dataset at this position and replace the contents of the view with that element*/
        viewHolder.name.text = song.name
        viewHolder.artist.text = song.artist
        viewHolder.album.text = song.album
        viewHolder.albumCover.setImageURI(song.albumCover)
        if( viewHolder.albumCover.drawable == null)
        {
            viewHolder.albumCover.setImageResource(R.drawable.img_song)
        }

        /*onClick event - play song when clicked on*/
        viewHolder.layout.setOnClickListener{
            if( exoPlayer.isPlaying )
            {
                exoPlayer.pause()
                exoPlayer.seekTo(position, 0)
            }
            else
            {
                val items = getMediaItems()
                exoPlayer.setMediaItems(items, position, 0)
            }
            exoPlayer.prepare()
            exoPlayer.play()
            val intent = Intent(context, PlayerActivity::class.java)
            context.startActivity(intent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun reload(songs: ArrayList<Song>){
        this.list = songs
        notifyDataSetChanged()
    }

    /**
     * @since 06-03-2023
     * get all songs that their format is Media Item
     * @return ArrayList<MediaItem>
     */
    private fun getMediaItems(): ArrayList<MediaItem>
    {
        val dataSet = arrayListOf<MediaItem>()
        for(song in list)
        {
            val elementMetaData = MediaMetadata.Builder()
                .setTitle(song.name)
                .setArtist(song.artist)
                .setArtworkUri(song.albumCover)
                .build()

            val element = MediaItem.Builder()
                .setUri(song.uri)
                .setMediaMetadata(elementMetaData)
                .build()
            dataSet.add(element)
        }
        return dataSet
    }
}
