package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.FamilyConfig;

/**
 * 让龙哥在电视上改 NAS / 电脑管家地址，不用重新打包。
 */
public class FamilyEditDialog extends BaseDialog {
    private final Context context;
    private final Runnable onSaved;
    private final EditText inputNas;
    private final EditText inputPc;

    public FamilyEditDialog(@NonNull Context context, Runnable onSaved) {
        super(context);
        this.context = context;
        this.onSaved = onSaved;
        setContentView(R.layout.dialog_family_edit);
        setCanceledOnTouchOutside(false);

        inputNas = findViewById(R.id.inputNas);
        inputPc = findViewById(R.id.inputPc);
        inputNas.setText(FamilyConfig.get(FamilyConfig.SOURCE_NAS).url);
        inputPc.setText(FamilyConfig.get(FamilyConfig.SOURCE_PC).url);

        findViewById(R.id.btnEditCancel).setOnClickListener(v -> dismiss());
        findViewById(R.id.btnEditSave).setOnClickListener(v -> save());
    }

    private void save() {
        String nas = normalize(inputNas.getText().toString());
        String pc = normalize(inputPc.getText().toString());
        FamilyConfig.setNas(nas);
        FamilyConfig.setPc(pc);
        Toast.makeText(context, "地址已保存", Toast.LENGTH_SHORT).show();
        if (onSaved != null) onSaved.run();
        dismiss();
    }

    private static String normalize(String value) {
        String url = value == null ? "" : value.trim();
        if (TextUtils.isEmpty(url)) return "";
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url;
    }
}
