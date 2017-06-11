package com.mgn.bingenovelreader.activities

import android.animation.Animator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.animation.*
import com.bumptech.glide.Glide
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapters.StringRVAdapter
import com.mgn.bingenovelreader.network.NovelApi
import com.mgn.bingenovelreader.utils.SimpleAnimationListener
import com.mgn.bingenovelreader.utils.SuggestionsBuilder
import com.mgn.bingenovelreader.utils.addToSearchHistory
import com.mgn.bingenovelreader.utils.toast
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.content_search.*
import org.cryse.widget.persistentsearch.PersistentSearchView


class SearchActivity : AppCompatActivity() {

    val list = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setRecyclerView()
        setSearchView()

        loadingImageView.visibility = View.GONE
        Glide.with(this).load("https://media.giphy.com/media/ADyQEh474eu0o/giphy.gif").into(loadingImageView)
    }

    private fun setSearchView() {
        searchView.setHomeButtonListener { finish() }
        searchView.setSuggestionBuilder(SuggestionsBuilder())
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(searchTerm: String?) {
                searchTerm?.addToSearchHistory()
                searchNovels(searchTerm)
            }

            override fun onSearchExit() {}

            override fun onSearchCleared() {}

            override fun onSearchEditClosed() {
                searchViewBgTint
                        .animate()
                        .alpha(0.0f)
                        .setDuration(300)
                        .setListener(object : SimpleAnimationListener() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                searchViewBgTint.visibility = View.GONE
                            }
                        })
                        .start()
            }

            override fun onSearchTermChanged(searchTerm: String?) {}

            override fun onSearchEditBackPressed(): Boolean = false

            override fun onSearchEditOpened() {
                searchViewBgTint.visibility = View.VISIBLE
                searchViewBgTint
                        .animate()
                        .alpha(1.0f)
                        .setDuration(300)
                        .setListener(SimpleAnimationListener())
                        .start()
            }

        })

    }

    private fun setRecyclerView() {
        (1..20).mapTo(list) { "abc" + it }
        searchRecyclerView.setHasFixedSize(true)
        val llm = LinearLayoutManager(applicationContext)
        searchRecyclerView.layoutManager = llm

        val set = AnimationSet(true)

        var animation: Animation = AlphaAnimation(0.0f, 1.0f)
        animation.duration = 500
        set.addAnimation(animation)

        animation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        animation.setDuration(100)
        set.addAnimation(animation)

        val controller = LayoutAnimationController(set, 0.5f)

        val adapter = StringRVAdapter(list, {
            toast("$it Clicked")
        })
        searchRecyclerView.layoutAnimation = controller
        searchRecyclerView.adapter = adapter
    }

    fun searchNovels(searchTerm: String?) {
        //searchRecyclerView.visibility = View.INVISIBLE
        loadingImageView.visibility = View.VISIBLE

        Thread(Runnable {
            searchTerm?.let {
                list.clear()
                val newList = ArrayList(NovelApi().search(it)["novel-updates"]?.map { it.name })
                Handler(Looper.getMainLooper()).post {
                    loadingImageView.visibility = View.GONE
                    searchRecyclerView.visibility = View.VISIBLE
                    (searchRecyclerView.adapter as StringRVAdapter).updateData(newList)

                }
            }
        }).start()
    }


    override fun onBackPressed() {
        if (searchView.isEditing) {
            searchView.cancelEditing()
        } else {
            super.onBackPressed()
        }
    }

}
