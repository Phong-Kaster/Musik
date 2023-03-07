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
import android.provider.MediaStore
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.Menu
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musik.Mutilpurpose.Multipurpose
import com.example.musik.R
import com.example.musik.Song.Song
import com.example.musik.Song.SongAdapter
import com.example.musik.databinding.ActivityHomeBinding
import com.google.android.exoplayer2.ExoPlayer
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
            MediaStore.Audio.Media.SIZE)

        /*Step 3: define order*/
        val sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER

        val cursor = contentResolver.query(mediaStorageUri, projection, null, null, sortOrder)
        val idColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val albumCoverColumn =  cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

        while (cursor.moveToNext())
        {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val albumCover = cursor.getLong(albumCoverColumn)
            val album = cursor.getString(albumColumn)
            val artist = cursor.getString(artistColumn)
            val size = cursor.getLong(sizeColumn) / 1000000// convert from byte to megabyte


            val uriSong = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)// = song uri
            val uriAlbumCover = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumCover)// = album cover
            val element = Song(uriAlbumCover, name, artist, album, uriSong)// = create a song instance
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
}