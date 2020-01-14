package io.github.gmathi.novellibrary.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import io.github.gmathi.novellibrary.util.ContextLocaleWrapper
import io.github.gmathi.novellibrary.util.ContextLocaleWrapper.Companion.getLanguage


open class BaseFragment : Fragment() {

    override fun getContext(): Context? {
        val context = super.getContext()
        return if (context == null) context else ContextLocaleWrapper.wrapContextWithLocale(context, getLanguage())
    }

    /*fun setLocale(lang: String) {
        val myLocale = Locale(lang);
        val res = getResources();
        val dm = res.getDisplayMetrics();
        val conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        val refresh = Intent(this, AndroidLocalize.class);
        finish();
        startActivity(refresh);
    }*/
}
