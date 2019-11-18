### This is a pdf reader base on mupdf.

cd to AMupdf dir:
git clone https://gitee.com/archko/viewer.git

the viewer is modified for textreflow with image.
the git patch is archko.patch

### how to use the reader module:
create a class AMuPDFRecyclerViewActivity : MuPDFRecyclerViewActivity()

regist activity:
```
<activity
    android:name="xx.AMuPDFRecyclerViewActivity"
    android:configChanges="orientation|keyboardHidden|screenSize"
    android:screenOrientation="sensor">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />

        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data android:mimeType="application/pdf" />
        <data android:mimeType="application/vnd.ms-xpsdocument" />
        <data android:mimeType="application/oxps" />
        <data android:mimeType="application/x-cbz" />
        <data android:mimeType="application/epub+zip" />
        <data android:mimeType="text/xml" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />

        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data android:pathPattern=".*\\.pdf" />
        <data android:pathPattern=".*\\.xps" />
        <data android:pathPattern=".*\\.oxps" />
        <data android:pathPattern=".*\\.cbz" />
        <data android:pathPattern=".*\\.epub" />
        <data android:pathPattern=".*\\.fb2" />
    </intent-filter>
</activity>
```

open AMuPDFRecyclerViewActivity with a intent():
``` 
val intent = Intent()
intent.setDataAndType(Uri.fromFile(f), "application/pdf")
intent.setClass(activity!!, AMuPDFRecyclerViewActivity::class.java)
intent.action = "android.intent.action.VIEW"
activity?.startActivity(intent)
```      

### released apks:
- [https://www.coolapk.com/apk/233108](https://www.coolapk.com/apk/233108)
- [https://play.google.com/store/apps/details?id=cn.archko.mupdf](https://play.google.com/store/apps/details?id=cn.archko.mupdf)
