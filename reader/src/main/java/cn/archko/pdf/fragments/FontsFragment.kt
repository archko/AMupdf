package cn.archko.pdf.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.App
import cn.archko.pdf.R
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.common.FontHelper
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Utils
import com.google.android.material.appbar.MaterialToolbar
import com.umeng.analytics.MobclickAgent
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.FileFilter

/**
 * 字体列表
 * @author: archko 2019/9/29 :15:58
 */
open class FontsFragment : DialogFragment() {

    lateinit var recyclerView: RecyclerView
    lateinit var adapter: BaseRecyclerAdapter<FontBean>
    var mStyleHelper: StyleHelper? = null
    var mDataListener: DataListener? = null
    var selectedFontName: String? = null

    public fun setStyleHelper(styleHelper: StyleHelper?) {
        this.mStyleHelper = styleHelper
    }

    public fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = android.R.style.Theme_Holo_Dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            themeId = android.R.style.Theme_Material_Dialog;
        }
        setStyle(DialogFragment.STYLE_NO_FRAME, themeId)
        isCancelable = false

        val sp: SharedPreferences = App.getInstance().getSharedPreferences(FontHelper.FONT_SP_FILE, Context.MODE_PRIVATE)
        selectedFontName = sp.getString(FontHelper.FONT_KEY_NAME, FontHelper.SYSTEM_FONT)
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart(TAG);
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd(TAG);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.item_font, container, false)
        view.findViewById<View>(R.id.layout_search).visibility = View.GONE
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener(View.OnClickListener { dismiss() })

        toolbar?.setTitle(R.string.dialog_title_font)
        toolbar?.setSubtitle(R.string.no_fonts)

        recyclerView = view.findViewById(R.id.files)
        recyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = object : BaseRecyclerAdapter<FontBean>(activity) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
                val view = mInflater.inflate(cn.archko.pdf.R.layout.item_outline, parent, false)
                return FontHolder(view)
            }
        }
        recyclerView.adapter = adapter

        loadFonts();
    }

    private fun loadFonts() {
        doAsync {
            val fontDir = FileUtils.getStorageDir(FontHelper.FONT_DIR)
            val list = ArrayList<FontBean>()

            list.add(FontBean(FontHelper.DEFAULT, FontHelper.SYSTEM_FONT, null))
            list.add(FontBean(FontHelper.SANS_SERIF, FontHelper.SYSTEM_FONT_SAN, null))
            list.add(FontBean(FontHelper.SERIF, FontHelper.SYSTEM_FONT_SERIF, null))
            list.add(FontBean(FontHelper.MONOSPACE, FontHelper.SYSTEM_FONT_MONO, null))
            fontDir.listFiles(FileFilter { file ->
                if (file.isDirectory)
                    return@FileFilter false
                val fname = file.name.toLowerCase()

                if (fname.endsWith(".ttf", true))
                    return@FileFilter true
                if (fname.endsWith(".ttc", true))
                    return@FileFilter true
                false
            })?.map {
                list.add(FontBean(FontHelper.CUSTOM, it.name, it))
            }

            uiThread {
                if (list.size > 0) {
                    adapter.data = list
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@FontsFragment.activity, R.string.no_fonts, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    inner class FontHolder(itemView: View?) : BaseViewHolder<FontBean>(itemView) {

        var title: TextView = itemView!!.findViewById(cn.archko.pdf.R.id.title)

        init {
            itemView!!.minimumHeight = Utils.dipToPixel(48f)
        }

        override fun onBind(data: FontBean?, position: Int) {
            title.setText(data?.fontName + ":测试字体 test font")
            if (data?.fontType == FontHelper.CUSTOM) {
                if (null != data.file) {
                    val typeface = mStyleHelper?.fontHelper?.createFontByPath(data.file?.absolutePath!!)
                    title.setTypeface(typeface)
                }
            } else {
                when (data?.fontType) {
                    FontHelper.DEFAULT -> title.setTypeface(Typeface.DEFAULT)
                    FontHelper.SANS_SERIF -> title.setTypeface(Typeface.SANS_SERIF)
                    FontHelper.SERIF -> title.setTypeface(Typeface.SERIF)
                    FontHelper.MONOSPACE -> title.setTypeface(Typeface.MONOSPACE)
                }
            }

            if (selectedFontName.equals(data?.fontName)) {
                itemView.setBackgroundResource(R.color.button_pressed)
            } else {
                itemView.setBackgroundResource(R.color.transparent)
            }
            itemView.setOnClickListener {
                mStyleHelper?.fontHelper?.saveFont(data!!)
                this@FontsFragment.dismiss()
                mDataListener?.onSuccess(data)
            }
        }
    }

    companion object {

        val TAG = "FontsFragment"

        fun showFontsDialog(activity: FragmentActivity?, styleHelper: StyleHelper?, dataListener: DataListener?) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("font_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            // Create and show the dialog.
            val fragment = FontsFragment()
            val bundle = Bundle()
            fragment.arguments = bundle

            fragment.setListener(dataListener)
            fragment.setStyleHelper(styleHelper)
            fragment.show(ft!!, "font_dialog")
        }
    }
}
