package io.github.gmathi.novellibrary.fragment

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import io.github.gmathi.novellibrary.R

/**
 * Created by a6001823 on 11/13/17.
 */
class CenteredTextFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val text = arguments.getString(EXTRA_TEXT)
        val textView = view.findViewById<TextView>(R.id.text)
        textView.text = text
        textView.setOnClickListener { v -> Toast.makeText(v.context, text, Toast.LENGTH_SHORT).show() }
    }

    companion object {

        private val EXTRA_TEXT = "text"

        fun createFor(text: String): CenteredTextFragment {
            val fragment = CenteredTextFragment()
            val args = Bundle()
            args.putString(EXTRA_TEXT, text)
            fragment.arguments = args
            return fragment
        }
    }
}
