package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.FamilyConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

/**
 * 家庭版首页的“一键换源”弹窗。
 * 老人只需要按确定：自动选择会依次尝试云端、NAS、电脑管家。
 */
public class FamilySourceDialog extends BaseDialog {
    private final Context context;
    private final TextView rowAuto;
    private final TextView rowGithub;
    private final TextView rowNas;
    private final TextView rowPc;
    private final TextView status;

    public FamilySourceDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        setContentView(R.layout.dialog_family_source);
        setCanceledOnTouchOutside(false);

        rowAuto = findViewById(R.id.rowAuto);
        rowGithub = findViewById(R.id.rowGithub);
        rowNas = findViewById(R.id.rowNas);
        rowPc = findViewById(R.id.rowPc);
        status = findViewById(R.id.sourceStatus);

        rowAuto.setOnClickListener(v -> autoPick());
        rowGithub.setOnClickListener(v -> choose(FamilyConfig.SOURCE_GITHUB));
        rowNas.setOnClickListener(v -> choose(FamilyConfig.SOURCE_NAS));
        rowPc.setOnClickListener(v -> choose(FamilyConfig.SOURCE_PC));

        View.OnLongClickListener edit = v -> {
            openEditor();
            return true;
        };
        rowGithub.setOnLongClickListener(edit);
        rowNas.setOnLongClickListener(edit);
        rowPc.setOnLongClickListener(edit);

        findViewById(R.id.rowEdit).setOnClickListener(v -> openEditor());
        findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        refreshRows();
    }

    private void refreshRows() {
        bind(rowGithub, FamilyConfig.get(FamilyConfig.SOURCE_GITHUB));
        bind(rowNas, FamilyConfig.get(FamilyConfig.SOURCE_NAS));
        bind(rowPc, FamilyConfig.get(FamilyConfig.SOURCE_PC));

        String current = Hawk.get(HawkConfig.API_URL, "");
        if (current == null) current = "";
        status.setText(current.isEmpty() ? "当前还没有配置地址" : "当前：" + shortUrl(current));
    }

    private void bind(TextView view, FamilyConfig.Source source) {
        String url = source.url;
        String second = url.isEmpty() ? "未设置，点此或长按修改" : shortUrl(url);
        String text = source.label + "\n" + second;
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        int start = source.label.length() + 1;
        sb.setSpan(new RelativeSizeSpan(0.72f), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(Color.parseColor("#99FFFFFF")), start, text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(sb);
    }

    private void choose(int key) {
        FamilyConfig.Source source = FamilyConfig.get(key);
        if (source.url.isEmpty()) {
            status.setText(source.label + " 还没有填地址，请先修改地址");
            openEditor();
            return;
        }
        apply(source);
    }

    private void autoPick() {
        status.setText("正在检测可用地址…");
        rowAuto.setEnabled(false);
        new Thread(() -> {
            FamilyConfig.Source picked = FamilyConfig.autoPick();
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    rowAuto.setEnabled(true);
                    if (!isShowing()) return;
                    if (picked == null) {
                        status.setText("三个地址都不通：请检查网络，或电脑管家是否开机");
                    } else {
                        apply(picked);
                    }
                });
            }
        }, "family-source-probe").start();
    }

    private void apply(FamilyConfig.Source source) {
        dismiss();
        Toast.makeText(context, "正在切换：" + source.label, Toast.LENGTH_SHORT).show();
        FamilyConfig.apply(context, source.url);
    }

    private void openEditor() {
        FamilyEditDialog dialog = new FamilyEditDialog(context, this::refreshRows);
        dialog.show();
    }

    private static String shortUrl(String url) {
        String value = url == null ? "" : url.trim();
        if (value.length() <= 72) return value;
        return value.substring(0, 44) + "..." + value.substring(value.length() - 24);
    }
}
