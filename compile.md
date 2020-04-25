## how to compile mupdf
### step 1:
mkdir mupdf_c
add files:
- build.gradle
- gradlew
- gradle/wrapper
    - gradle-wrapper.properties
    - gradle-wrapper.jar
- AndroidManifest.xml
- local.properties
- libmupdf

### step 2:
download mupdf git to libmupdf
->cd mupdf_c
->git clone git://git.ghostscript.com/mupdf.git libmupdf

### step 3:
compile libmupdf:
cd libmupdf
->git pull
->git submodule update
->make -C libmupdf generate
->cd ..
->./gradlew assembleRelease

### step4:
cp mupdf_c/build/outputs/aar/mupdf_c-1.16.0.aar  ../PDF/AMupdf/viewer/libs/mupdf-android-fitz-release.aar

### step 5:
get diff:
git diff ./ > archko.patch

### add custom function
before step3, you can modify /mupdf_c/libmupdf/platform/java/mupdf_native.c
in my options,i add Page_textAsHtml2,Page_textAsXHtml,Page_textAsText,Page_textAsTextOrHtml
and goto to step 5, generate the diff.
