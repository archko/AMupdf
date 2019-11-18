package cn.archko.pdf.activities;

import androidx.appcompat.app.AppCompatActivity;

import com.umeng.analytics.MobclickAgent;

/**
 * @author: archko 2018/12/16 :9:43
 */
public class AnalysticActivity extends AppCompatActivity {

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this); // 基础指标统计，不能遗漏
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this); // 基础指标统计，不能遗漏
    }
}
