package com.example.musik.Homepage

import android.Manifest
import android.app.AlertDialog
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.Menu
import android.view.View
import android.view.View.GONE
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.SearchView.VISIBLE
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musik.Mutilpurpose.Constant
import com.example.musik.Mutilpurpose.Multipurpose
import com.example.musik.R
import com.example.musik.Song.Song
import com.example.musik.Song.SongAdapter
import com.example.musik.databinding.ActivityHomeBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import java.util.*


class HomeActivity : AppCompatActivity() {

    private lateinit var songAdapter: SongAdapter
    private var songList: ArrayList<Song> = ArrayList()
    private lateinit var homeBinding: ActivityHomeBinding
    private lateinit var exoPlayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*setContentView(R.layout.activity_home)*/
        homeBinding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        setupComponent()


        /*We check required permission. If everything's OK, we fetch all songs*/
        val flag = checkPermission()
        if( flag ) fetch()


        /*set up event*/
        setupEvent()
        setupEventForDefaultMusicPlayer()
    }


    /**
     * @since 06-03-2023
     * on resume
     */
    override fun onResume() {
        super.onResume()
        Multipurpose.setStatusBarColor(this, window)
    }

    /**
     * @since 06-03-2023
     * on destroy
     */
    override fun onDestroy() {
        super.onDestroy()
        if(exoPlayer.isPlaying) { exoPlayer.stop() }
        exoPlayer.release()
    }

    /**
     * @since 06-03-2023
     * inflate specific menu in this activity
     * */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top, menu)

        val menuItem = menu.findItem(R.id.buttonSearch)
        val searchView = menuItem.actionView as SearchView


        searchView.queryHint = getString(R.string.enter_keyword)
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(keyword: String): Boolean {
                filterWithKeyword(keyword.lowercase())
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }/*end onCreateOptionsMenu()*/

    /**
     * @since 03-03-2023
     * set up component
     */
    private fun setupComponent()
    {
        setSupportActionBar(homeBinding.toolbar)
        supportActionBar!!.setTitle(R.string.app_name)

        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.shuffleModeEnabled = true/*by default, shuffle mode is enabled*/
    }



    /*The request code used in ActivityCompat.requestPermissions()
     and returned in the Activity's onRequestPermissionsResult()*/
    private var permissionCode = 1
    private val permissionsRequired = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE

    )
    private fun checkPermission(): Boolean
    {
        val permissionsNeeded = mutableListOf<String>()

        /*add permission that is not granted into permissionNeeded Array*/
        for(element in permissionsRequired)
        {
            val flag: Int = ContextCompat.checkSelfPermission(this, element)
            if( flag != PackageManager.PERMISSION_GRANTED ){
                permissionsNeeded.add(element)
            }
        }

        /*if permissions needed Array is not empty*/
        if(permissionsNeeded.isNotEmpty()){
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), permissionCode )
            return false
        }

        return true
    }/*end checkPermission*/


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val resultStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if(resultStorage == PackageManager.PERMISSION_GRANTED)
        {
            fetch()
        }
        if( resultStorage == PackageManager.PERMISSION_DENIED ) {
            val message = getString(R.string.guide_storage)
            requestPermission(message, Manifest.permission.READ_EXTERNAL_STORAGE )
        }

    }/*end onRequestPermissionsResult*/

    /**
     * open dialog and tell users why Musik need Storage permission
     * - Case 1: if storage permissions is granted, fetch all songs from local storage
     * - Case 2: if storage permissions is not granted, open the application's settings to users give permission
     */
    private val requestPermissionLauncher = registerForActivityResult( ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
            if(isGranted){
                fetch()
            }
            else
            {
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                with(intent) {
                    data = Uri.fromParts("package", packageName, null)
                    addCategory(CATEGORY_DEFAULT)
                    addFlags(FLAG_ACTIVITY_NEW_TASK)
                    addFlags(FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }

                startActivity(intent)
            }
        }

    /**
     * @since 03-03-2023
     * request permission
     */
    private fun requestPermission(message: String, permission: String)
    {
        val dialogInterfacePositive = DialogInterface.OnClickListener{
                _, _ ->
                requestPermissionLauncher.launch(permission)
        }

        val dialogInterfaceNegative = DialogInterface.OnClickListener{
                dialog, _ ->
            Toast.makeText(this, getString(R.string.musik_needs_storage_permission), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            finish()
        }


        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", dialogInterfacePositive)
            .setNegativeButton("No, thank",dialogInterfaceNegative )
            .create()
            .show()
    }

    /**
     * @author Phong-Kaster
     * @since 06-03-2023
     * find and load all songs in local storage to the application
     */
    private fun fetch(){
        /*Step 1: define list of songs*/
        val songs = arrayListOf<Song>()
        val mediaStorageUri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
        else
        {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        /*Step 2: define projection*/
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION
        )

        /*Step 3: define order*/
        val sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER

        val cursor = contentResolver.query(mediaStorageUri, projection, null, null, sortOrder)
        val idColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val albumCoverColumn =  cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext())
        {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val albumCover = cursor.getLong(albumCoverColumn)
            val album = cursor.getString(albumColumn)
            val artist = cursor.getString(artistColumn)
            val size = cursor.getLong(sizeColumn) / 1000000// convert from byte to megabyte
            val duration = cursor.getLong(durationColumn)

            val uriSong = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)// = song uri
            val uriAlbumCover = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumCover)// = album cover
            val element = Song(uriAlbumCover, name, artist, album, uriSong, duration)// = create a song instance
            if( size <= 0)// if size <= 0 MB then ignore this song
            {
                continue
            }
            songs.add(element)
        }
        cursor.close()

        /*Step 4: show*/
        setupRecyclerView(songs)


    }/*end fun fetch*/

    /**
     * @since 06-03-2023
     * setup Recycler View songs
     */
    private fun setupRecyclerView(list: ArrayList<Song>)
    {
        if( list.size == 0)
        {
            Toast.makeText(this, "Empty !", Toast.LENGTH_SHORT).show()
            return
        }

        // update songList
        songList.clear()
        songList.addAll(list)


        // update recycler view
        val layoutManger = LinearLayoutManager(this)
        homeBinding.recyclerView.layoutManager = layoutManger


        songAdapter = SongAdapter(this, songList, exoPlayer)
        homeBinding.recyclerView.adapter = songAdapter
    }/*end showSongs*/


    /**
     * @since 07-03-2023
     * set up event onClick
     */
    private fun setupEvent()
    {
        /*===Step 1: This object will update name, artist & album cover from song that users click on*/
        val listener = object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                /*we have to wait ExoPlayer read entire file to get accuracy duration*/
                if(playbackState == ExoPlayer.STATE_READY)
                {
                    val current = Multipurpose.getReadableTimestamp(exoPlayer.currentPosition.toInt())
                    val duration = Multipurpose.getReadableTimestamp(exoPlayer.duration.toInt())

                    homeBinding.defaultMediaControl.progressStart.text = current
                    homeBinding.defaultMediaControl.progressEnd.text = duration
                    homeBinding.defaultMediaControl.seekBar.progress = exoPlayer.currentPosition.toInt()
                    homeBinding.defaultMediaControl.seekBar.max = exoPlayer.duration.toInt()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                //get name, artist & album cover
                val name = mediaItem!!.mediaMetadata.title
                val artist = mediaItem.mediaMetadata.artist
                val albumCover = mediaItem.mediaMetadata.artworkUri



                // for COMPACT MEDIA CONTROL,  update name, artist, album cover & play/pause button's icon
                homeBinding.compactMediaControlName.text = name
                homeBinding.compactMediaControlArtist.text = artist
                homeBinding.compactMediaControlAlbumCover.setImageURI(albumCover)
                homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_pause_v2)


                // for DEFAULT MEDIA CONTROL, update name, artist, album cover & play/pause button's icon
                homeBinding.defaultMediaControl.name.text = name
                homeBinding.defaultMediaControl.artist.text = artist
                homeBinding.defaultMediaControl.albumCover.setImageURI(albumCover)
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_pause)

                //for DEFAULT MEDIA CONTROL(dmc), update progress & seek bar
                dmcUpdateProgress()
                dmcSetUpEvent()
            }
        }
        exoPlayer.addListener(listener)/*and finally we add the above listener to exo player*/


        /*===Step 2: button play/ pause on Compact media Control*/
        homeBinding.compactMediaControlPlayPause.setOnClickListener {
            /*Step 2 - Case 1: exoplayer is playing music*/
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_play_v2)
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_play)
            }
            /*Step 2 - Case 2: exoplayer is not playing music */
            else {
                exoPlayer.play()
                homeBinding.compactMediaControlPlayPause.setImageResource(R.drawable.ic_pause_v2)
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }/*end Step 2*/


        /*===Step 3: button skip next & previous*/
        homeBinding.compactMediaControlSkipPrevious.setOnClickListener {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPrevious()
                exoPlayer.play()
            }
        }
        homeBinding.compactMediaControlSkipNext.setOnClickListener {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNext()
                exoPlayer.play()
            }
        }/*end Step 3*/
    }

    /**
     * @since 07-03-2023
     * Default Media Control stands for D.M.C
     * Compact Media Control stands for C.M.C
     *
     * this function establishes event clickOn for the layout included at the bottom of this activity
     * this layout shows default media control instead of compact media control
     *
     * all clickOn events which  are declared in this function, is written in "activity_music_player"
     */
    private fun setupEventForDefaultMusicPlayer(){
        /*====================SHOW D.M.C - clickOn C.M.C ====================*/
        homeBinding.compactMediaControl.setOnClickListener{

            /*If the app is opened but none of songs is selected,
            * users click on compact media control to shuffle list of songs and play music*/
            if( !exoPlayer.isPlaying && !exoPlayer.hasNextMediaItem())
            {
                val items = songAdapter.getMediaItems(songList)
                exoPlayer.setMediaItems(items)
                exoPlayer.prepare()
                exoPlayer.play()
            }

            /*Slide default media control up from the bottom of screen*/
            Multipurpose.slideUp(homeBinding.defaultMediaControl.layout)
            homeBinding.appBarLayout.visibility = GONE
            homeBinding.defaultMediaControl.layout.visibility = VISIBLE
            homeBinding.defaultMediaControl.layout.isClickable = true
        }/*end SHOW D.M.C - clickOn C.M.C  */

        /*====================BUTTON CLOSE - HIDE D.M.C TEMPORARILY ====================*/
        homeBinding.defaultMediaControl.buttonClose.setOnClickListener{
            Multipurpose.slideDown(homeBinding.defaultMediaControl.layout)
            homeBinding.appBarLayout.visibility = VISIBLE
            homeBinding.defaultMediaControl.layout.visibility = GONE
            homeBinding.defaultMediaControl.layout.isClickable = false
        }/*end BUTTON CLOSE*/
       /* button more*/
    }


    /**
     * @since 06-03-2023
     * filter songs with keyword
     * any song has name or artist matching with keyword then is showed !
     */
    private fun filterWithKeyword(keyword: String){
        val songListFiltered = arrayListOf<Song>()

        if(songList.size == 0) return

        for( element in songList)
        {
            val name = element.name.lowercase(Locale.ROOT)
            val artist = element.artist.lowercase(Locale.ROOT)
            if( name.contains(keyword) || artist.contains(keyword) ) {songListFiltered.add(element)}
        }
        songAdapter.reload(songListFiltered)
    }


    /**
     * @since 08-03-2023
     * on Back Pressed
     */
    override fun onBackPressed() {
        if( homeBinding.defaultMediaControl.layout.visibility == VISIBLE)
        {
            Multipurpose.slideDown(homeBinding.defaultMediaControl.layout)
            homeBinding.appBarLayout.visibility = VISIBLE
            homeBinding.defaultMediaControl.layout.visibility = GONE
            homeBinding.defaultMediaControl.layout.isClickable = false
        }
        else
        {
            super.onBackPressed()
        }
    }

    /**
     * @since 08-03-2023
     * dmc stands for Default Media Control
     * update progress & seekbar
     * update indicator's position of seek bar every 1 second
     */
    private fun dmcUpdateProgress()
    {
        val mainLooper = Looper.getMainLooper()
        val runnable = Runnable{
            if( exoPlayer.isPlaying)
            {
                val current = Multipurpose.getReadableTimestamp(exoPlayer.currentPosition.toInt())
                homeBinding.defaultMediaControl.progressStart.text = current
                homeBinding.defaultMediaControl.seekBar.progress = exoPlayer.currentPosition.toInt()
            }
            dmcUpdateProgress()
        }
        Handler(mainLooper).postDelayed(runnable, 1000)
    }

    /**
     * @since 08-03-2023
     * dmc stands for Default Media Control
     * set up event for default media control
     */
    private fun dmcSetUpEvent()
    {
        /*======================= BUTTON PLAY/PAUSE =======================*/
        homeBinding.defaultMediaControl.buttonPlayPause.setOnClickListener{
            /*Step 2 - Case 1: exoplayer is playing music*/
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_play)
            }
            /*Step 2 - Case 2: exoplayer is not playing music */
            else
            {
                exoPlayer.play()
                homeBinding.defaultMediaControl.buttonPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }/*end BUTTON PLAY/PAUSE*/

        /*BUTTON SKIP PREVIOUS AND SKIP NEXT*/
        homeBinding.defaultMediaControl.buttonSkipPrevious.setOnClickListener {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPrevious()
                exoPlayer.play()
            }
        }
        homeBinding.defaultMediaControl.buttonSkipNext.setOnClickListener {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNext()
                exoPlayer.play()
            }
        }/*end BUTTON SKIP PREVIOUS AND SKIP NEXT*/


        /*======================= BUTTON SHUFFLE =======================*/
        homeBinding.defaultMediaControl.buttonShuffle.setOnClickListener {
            if(exoPlayer.shuffleModeEnabled)// shuffle off
            {
                exoPlayer.shuffleModeEnabled = false
                homeBinding.defaultMediaControl.buttonShuffle.setImageResource(R.drawable.ic_shuffle_off)
                Toast.makeText(this, "Shuffle off", Toast.LENGTH_SHORT).show()
            }
            else// shuffle on
            {
                exoPlayer.shuffleModeEnabled = true
                homeBinding.defaultMediaControl.buttonShuffle.setImageResource(R.drawable.ic_shuffle_on)
                Toast.makeText(this, "Shuffle on", Toast.LENGTH_SHORT).show()
            }
        }/*end BUTTON SHUFFLE*/

        /*======================= BUTTON REPEAT =======================*/
        var repeatMode = Constant.REPEAT_MODE_OFF
        homeBinding.defaultMediaControl.buttonRepeat.setOnClickListener {
            if( repeatMode == Constant.REPEAT_MODE_ALL)// repeat on
            {
                repeatMode = Constant.REPEAT_MODE_ONE
                exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                homeBinding.defaultMediaControl.buttonRepeat.setImageResource(R.drawable.ic_repeat_mode_all)
                Toast.makeText(this, "Repeat mode all", Toast.LENGTH_SHORT).show()
            }
            else if( repeatMode == Constant.REPEAT_MODE_ONE)// repeat only one
            {
                repeatMode = Constant.REPEAT_MODE_OFF
                exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                homeBinding.defaultMediaControl.buttonRepeat.setImageResource(R.drawable.ic_repeat_mode_one)
                Toast.makeText(this, "Repeat mode one", Toast.LENGTH_SHORT).show()
            }
            else if( repeatMode == Constant.REPEAT_MODE_OFF )// repeat off
            {
                repeatMode = Constant.REPEAT_MODE_ALL
                exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                homeBinding.defaultMediaControl.buttonRepeat.setImageResource(R.drawable.ic_repeat_mode_off)
                Toast.makeText(this, "Repeat mode off", Toast.LENGTH_SHORT).show()
            }
        }/*end BUTTON REPEAT*/

        /* ======================= SEEK BAR =======================*/
        var progressPosition = 0// this variable stores the current position of seek bar
        val seekBarListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                progressPosition = seekBar.progress// update progress position
            }
            /*update current progress position after users leave their off seek bar*/
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if( exoPlayer.playbackState == ExoPlayer.STATE_READY)
                {
                    exoPlayer.seekTo(progressPosition.toLong())
                    homeBinding.defaultMediaControl.progressStart.text = Multipurpose.getReadableTimestamp(progressPosition)
                    seekBar.progress = progressPosition
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }
        }/*end seekBarListener*/
        homeBinding.defaultMediaControl.seekBar.setOnSeekBarChangeListener(seekBarListener)
        /*end SEEK BAR*/
    }

}