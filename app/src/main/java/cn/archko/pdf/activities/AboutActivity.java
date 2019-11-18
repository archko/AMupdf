package cn.archko.pdf.activities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.umeng.analytics.MobclickAgent;

import cn.archko.mupdf.R;
import cn.archko.pdf.utils.FileUtils;
import cn.archko.pdf.utils.LengthUtils;

/**
 * @author: archko 2018/12/16 :9:43
 */
public class AboutActivity extends AnalysticActivity {

    private static final Part[] PARTS = {
            // Start
            new Part(R.string.about_commmon_title, Format.HTML, "about_common.html"),
            //new Part(R.string.about_license_title, Format.HTML, "about_license.html"),
            new Part(R.string.about_3dparty_title, Format.HTML, "about_3rdparty.html"),
            new Part(R.string.about_changelog_title, Format.HTML, "about_changelog.html"),
            // End
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        String name = "AMuPDF";
        String version = "";
        try {
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageInfo.versionName;
            name = getResources().getString(packageInfo.applicationInfo.labelRes);
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String text = name + (LengthUtils.isNotEmpty(version) ? " v" + version : "");
        MaterialToolbar toolbar=findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        toolbar.setTitle(text);
        setSupportActionBar(toolbar);

        final ExpandableListView view = (ExpandableListView) findViewById(R.id.about_parts);
        view.setAdapter(new PartsAdapter());
        view.expandGroup(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd("about");
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onPageStart("about");
    }

    private static class Part {

        final int labelId;
        final Format format;
        final String fileName;
        CharSequence content;

        public Part(final int labelId, final Format format, final String fileName) {
            this.labelId = labelId;
            this.format = format;
            this.fileName = fileName;
        }

        public CharSequence getContent(final Context context) {
            if (TextUtils.isEmpty(content)) {
                try {
                    final String text = FileUtils.readAssetAsString(fileName);
                    content = format.format(text);
                } catch (final Exception e) {
                    e.printStackTrace();
                    content = "";
                }
            }
            return content;
        }
    }

    public class PartsAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return PARTS.length;
        }

        @Override
        public int getChildrenCount(final int groupPosition) {
            return 1;
        }

        @Override
        public Part getGroup(final int groupPosition) {
            return PARTS[groupPosition];
        }

        @Override
        public Part getChild(final int groupPosition, final int childPosition) {
            return PARTS[groupPosition];
        }

        @Override
        public long getGroupId(final int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(final int groupPosition, final int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView,
                                 final ViewGroup parent) {
            View container = null;
            TextView view = null;
            if (convertView == null) {
                container = LayoutInflater.from(AboutActivity.this).inflate(R.layout.about_part, parent, false);
            } else {
                container = convertView;
            }
            view = (TextView) container.findViewById(R.id.about_partText);
            view.setText(getGroup(groupPosition).labelId);
            return container;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
                                 final View convertView, final ViewGroup parent) {
            WebView view = null;
            if (!(convertView instanceof WebView)) {
                view = new WebView(AboutActivity.this);
            } else {
                view = ((WebView) convertView);
            }
            CharSequence content = getChild(groupPosition, childPosition).getContent(AboutActivity.this);
            view.loadData(content.toString(), "text/html", "UTF-8");
            //view.setBackgroundColor(Color.GRAY);
            return view;
        }

        @Override
        public boolean isChildSelectable(final int groupPosition, final int childPosition) {
            return false;
        }
    }

    private static enum Format {
        /**
         *
         */
        TEXT,

        /**
         *
         */
        HTML;

        /**
         *
         */
        /*WIKI {
            @Override
            public CharSequence format(final String text) {
                return Wiki.fromWiki(text);
            }
        };*/
        public CharSequence format(final String text) {
            return text;
        }
    }
}