package com.mgn.bingenovelreader.activity

import android.support.v7.app.AppCompatActivity

class SearchActivity : AppCompatActivity() {
//class SearchActivity : AppCompatActivity(), GenericAdapter.Listener<Novel> {

//    lateinit var adapter: GenericAdapter<Novel>
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_search)
//
//        setRecyclerView()
//        setSearchView()
//
//        setLoadingView(R.drawable.loading_search, null)
//        enableLoadingView(false, null)
//        //searchNovels("Realms")
//    }
//
//    private fun setRecyclerView() {
//        adapter = GenericAdapter(items = ArrayList<Novel>(), layoutResId = R.layout.listitem_novel_search, listener = this)
//        recyclerView.setDefaults(adapter)
//    }
//
//    private fun setSearchView() {
//        searchView.setHomeButtonListener { finish() }
//        searchView.setSuggestionBuilder(SuggestionsBuilder())
//        searchView.setSearchListener(object : PersistentSearchView.SearchListener {
//
//            override fun onSearch(searchTerm: String?) {
//                if (!Util.checkNetwork(applicationContext)) {
//                    toast("No Active Internet! (⋋▂⋌)")
//                } else {
//                    searchTerm?.addToSearchHistory()
//                    searchNovels(searchTerm)
//                }
//            }
//
//            override fun onSearchEditOpened() {
//                searchViewBgTint.visibility = View.VISIBLE
//                searchViewBgTint
//                        .animate()
//                        .alpha(1.0f)
//                        .setDuration(300)
//                        .setListener(SimpleAnimationListener())
//                        .start()
//            }
//
//            override fun onSearchEditClosed() {
//                searchViewBgTint
//                        .animate()
//                        .alpha(0.0f)
//                        .setDuration(300)
//                        .setListener(object : SimpleAnimationListener() {
//                            override fun onAnimationEnd(animation: Animator) {
//                                super.onAnimationEnd(animation)
//                                searchViewBgTint.visibility = View.GONE
//                            }
//                        })
//                        .start()
//            }
//
//            override fun onSearchExit() {}
//            override fun onSearchCleared() {}
//            override fun onSearchTermChanged(searchTerm: String?) {}
//            override fun onSearchEditBackPressed(): Boolean = false
//        })
//
//    }
//
//    private fun searchNovels(searchTerm: String?) {
//        recyclerView.visibility = View.INVISIBLE
//        enableLoadingView(true, recyclerView)
//
//        Thread(Runnable {
//            searchTerm?.let {
//                val newList = ArrayList(NovelApi().search(it)[Constants.NovelSites.NOVEL_UPDATES])
//                newList.addAll(ArrayList(NovelApi().search(it)[Constants.NovelSites.ROYAL_ROAD]))
//                Handler(Looper.getMainLooper()).post {
//                    enableLoadingView(false, recyclerView)
//                    adapter.updateData(newList)
//                    if (adapter.items.isEmpty())
//                        toast("Found nothing for the search - $it. Try Again!")
//                }
//            }
//        }).start()
//    }
//
//    //region Adapter Listener Methods - onItemClick(), viewBinder()
//
//    override fun onItemClick(item: Novel) {
//        //Do Nothing
//        addToDownloads(item)
//    }
//
//    override fun bind(item: Novel, itemView: View) {
//        if (item.imageFilePath == null) {
//            Glide.with(this).asBitmap().load(item.imageUrl).into(object : SimpleTarget<Bitmap>() {
//                override fun onResourceReady(bitmap: Bitmap?, transition: Transition<in Bitmap>?) {
//                    itemView.novelImageView.setImageBitmap(bitmap)
//                    Thread(Runnable {
//                        val file = File(filesDir, IMAGES_DIR_NAME + "/" + Uri.parse(item.imageUrl).getFileName())
//                        val os = FileOutputStream(file)
//                        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, os)
//                        item.imageFilePath = file.path
//                    }).start()
//                }
//            })
//        } else {
//            Glide.with(this).load(File(item.imageFilePath)).into(itemView.novelImageView)
//        }
//
//        itemView.novelTitleTextView.text = item.name
//        itemView.novelDescriptionTextView.text = item.shortDescription
//
//        var genresText = item.genres?.joinToString { it }
//        if (StringUtil.isBlank(genresText)) genresText = "N/A"
//        itemView.novelGenreTextView.text = genresText
//
//        if (item.rating != null) {
//            try {
//                itemView.novelRatingBar.rating = item.rating!!.toFloat()
//            } catch (e: Exception) {
//                Log.w("Library Activity", "Rating: " + item.rating, e)
//            }
//            val ratingText = "(" + item.rating + ")"
//            itemView.novelRatingTextView.text = ratingText
//        }
//
//        if (dbHelper.getNovel(item.name!!) != null) {
//            itemView.downloadButton.setImageResource(R.drawable.ic_playlist_add_check_black_vector)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                itemView.downloadButton.imageTintList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(ContextCompat.getColor(this, R.color.LimeGreen)))
//            }
//        } else {
//            itemView.downloadButton.setImageResource(R.drawable.ic_file_download_black_vector)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                itemView.downloadButton.imageTintList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(ContextCompat.getColor(this, R.color.SlateGray)))
//            }
//        }
//
////        itemView.downloadButton.setOnClickListener {
////           addToDownloads(item)
////        }
//    }
//
//    //endregion
//
//    private fun addToDownloads(item: Novel) {
//        if (dbHelper.getNovel(item.name!!) == null) {
//            val novelId = dbHelper.insertNovel(item)
//            dbHelper.createDownloadQueue(novelId)
//            startDownloadService(novelId)
//            adapter.updateItem(item)
//        }
//    }
//
//    private fun startDownloadService(novelId: Long) {
//        val serviceIntent = Intent(this, DownloadService::class.java)
//        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
//        startService(serviceIntent)
//    }
//
//    override fun onBackPressed() {
//        if (searchView.isEditing) {
//            searchView.cancelEditing()
//        } else {
//            super.onBackPressed()
//        }
//    }

}
