package cn.archko.pdf.fragments

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.entity.FileBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * @author: archko 2020/11/16 :11:23
 */
class HistoryViewModel : ViewModel() {
    companion object {

        const val PAGE_SIZE = 21
    }

    private val _uiFileModel = MutableLiveData<Array<Any?>>()
    val uiFileModel: LiveData<Array<Any?>>
        get() = _uiFileModel

    fun loadFiles(curPage: Int, showExtension: Boolean) =
        viewModelScope.launch {
            val args = withContext(Dispatchers.IO) {
                var totalCount = 0

                val recent = RecentManager.instance
                totalCount = recent.progressCount
                val progresses = recent.readRecentFromDb(
                    PAGE_SIZE * (curPage),
                    PAGE_SIZE
                )

                val entryList = ArrayList<FileBean>()

                var entry: FileBean
                var file: File
                val path = Environment.getExternalStorageDirectory().path
                progresses?.map {
                    file = File(path + "/" + it.path)
                    entry = FileBean(FileBean.RECENT, file, showExtension)
                    entry.bookProgress = it
                    entryList.add(entry)
                }
                return@withContext arrayOf<Any?>(totalCount, entryList)
            }

            withContext(Dispatchers.Main) {
                _uiFileModel.value = args
            }
        }
}