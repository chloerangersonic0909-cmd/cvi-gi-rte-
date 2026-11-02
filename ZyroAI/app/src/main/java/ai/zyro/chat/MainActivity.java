package ai.zyro.chat;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(10, 13, 24);
    private final int panel = Color.rgb(21, 27, 45);
    private final int cyan = Color.rgb(70, 210, 255);
    private LinearLayout chat;
    private EditText prompt, apiKey, endpoint, model;
    private ScrollView scroll;
    private SharedPreferences prefs;
    private boolean busy = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("zyro_settings", MODE_PRIVATE);
        buildUi();
        if (prefs.getString("api_key", "").trim().isEmpty() || prefs.getString("endpoint", "").trim().isEmpty() || prefs.getString("model", "").trim().isEmpty()) {
            new android.os.Handler(getMainLooper()).post(this::showRequiredSettings);
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawableCompat shape(int color, int radius) {
        return new GradientDrawableCompat(color, dp(radius));
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16), dp(16), dp(16), dp(10));
        root.setBackgroundColor(bg);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL); header.setOrientation(LinearLayout.HORIZONTAL);
        TextView logo = text("Z", 26, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER); logo.setBackground(shape(cyan, 14));
        logo.setTextColor(bg);
        header.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        titles.addView(text("Zyro AI", 22, Color.WHITE, true));
        titles.addView(text("Ethical Hacking • Cybersecurity • Code", 12, cyan, false));
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        Button settings = button("⚙");
        header.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(44)));
        root.addView(header);

        TextView intro = text("ผู้ช่วยเขียนโค้ดและความปลอดภัยไซเบอร์ สำหรับงานที่ได้รับอนุญาต", 14, Color.LTGRAY, false);
        intro.setPadding(0, dp(14), 0, dp(10)); root.addView(intro);

        scroll = new ScrollView(this);
        chat = new LinearLayout(this); chat.setOrientation(LinearLayout.VERTICAL); chat.setPadding(dp(2), dp(4), dp(2), dp(12));
        scroll.addView(chat);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addMessage("Zyro AI • Cyber Lab", "พร้อมช่วยเขียนโค้ด วิเคราะห์ช่องโหว่ ป้องกันระบบ และฝึกในแล็บที่ได้รับอนุญาต ตั้งค่า API key ที่ปุ่ม ⚙️ เพื่อเริ่มต้น", false);

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL); composer.setGravity(Gravity.BOTTOM);
        prompt = new EditText(this); prompt.setTextColor(Color.WHITE); prompt.setHintTextColor(Color.GRAY);
        prompt.setHint("พิมพ์ข้อความที่นี่…"); prompt.setTextSize(15); prompt.setPadding(dp(12), dp(8), dp(12), dp(8));
        prompt.setBackground(shape(panel, 16)); prompt.setMinHeight(dp(50));
        composer.addView(prompt, new LinearLayout.LayoutParams(0, -2, 1));
        Button send = button("ส่ง ➤");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(76), dp(50)); sp.setMargins(dp(8), 0, 0, 0);
        composer.addView(send, sp); root.addView(composer);
        send.setOnClickListener(v -> sendMessage());
        prompt.setOnEditorActionListener((v, action, event) -> { sendMessage(); return true; });

        settings.setOnClickListener(v -> showSettings(false));
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setTextSize(14); b.setBackground(shape(Color.rgb(32, 105, 145), 14));
        return b;
    }
    private void addMessage(String who, String body, boolean user) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10)); card.setBackground(shape(user ? Color.rgb(24, 58, 83) : panel, 14));
        TextView label = text(who, 12, user ? cyan : Color.rgb(170, 190, 210), true);
        TextView content = text(body, 15, Color.WHITE, false); content.setPadding(0, dp(5), 0, 0);
        card.addView(label); card.addView(content);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(5), 0, dp(5)); chat.addView(card, p);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }
    private void sendMessage() {
        if (busy) return;
        String question = prompt.getText().toString().trim();
        if (question.isEmpty()) return;
        String key = prefs.getString("api_key", "").trim();
        if (key.isEmpty() || prefs.getString("endpoint", "").trim().isEmpty() || prefs.getString("model", "").trim().isEmpty()) { showRequiredSettings(); return; }
        prompt.setText(""); addMessage("คุณ", question, true);
        busy = true; addMessage("Zyro AI", "กำลังคิด…", false);
        new Thread(() -> {
            String answer;
            try { answer = requestAI(question, key); }
            catch (Exception e) { answer = "เชื่อมต่อไม่สำเร็จ: " + e.getMessage(); }
            final String result = answer;
            runOnUiThread(() -> {
                busy = false;
                int last = chat.getChildCount() - 1;
                if (last >= 0) chat.removeViewAt(last);
                addMessage("Zyro AI", result, false);
            });
        }).start();
    }
    private String requestAI(String question, String key) throws Exception {
        String urlText = prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions").trim();
        String modelName = prefs.getString("model", "gpt-4o-mini").trim();
        HttpURLConnection c = (HttpURLConnection)new URL(urlText).openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(60000);
        c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + key);
        JSONObject body = new JSONObject(); body.put("model", modelName);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", "You are Zyro AI Cyber Lab, a specialist assistant for software engineering, ethical hacking, and defensive cybersecurity. Reply in the user's language, preferably Thai when the user writes Thai. Help with programming, debugging, secure code review, threat modeling, vulnerability explanations, security testing in systems the user owns or is explicitly authorized to test, CTF challenges, and isolated lab exercises. Give clear, practical code examples for benign and defensive use. For security testing, keep advice scoped to authorized targets and explain safe validation and remediation. Do not help steal credentials or data, deploy malware, evade detection, maintain unauthorized access, or compromise real third-party systems. If a request is ambiguous and could target a real system, ask for authorization/scope or provide a safe lab or defensive alternative. Be technically precise and do not claim code was tested unless it was."));
        messages.put(new JSONObject().put("role", "user").put("content", question));
        body.put("messages", messages);
        try (OutputStream out = c.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) response.append(line);
        }
        c.disconnect();
        JSONObject json = new JSONObject(response.toString());
        if (code < 200 || code >= 300) {
            String message = json.optJSONObject("error") != null ? json.getJSONObject("error").optString("message", "HTTP " + code) : "HTTP " + code;
            throw new Exception(message);
        }
        return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "ไม่มีข้อความตอบกลับ");
    }
    private void showSettings(boolean required) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(8), dp(4), dp(8), dp(4));
        TextView help = text("รองรับผู้ให้บริการที่ใช้ OpenAI-compatible Chat Completions API เท่านั้น", 12, cyan, false);
        help.setPadding(0, 0, 0, dp(6)); form.addView(help);
        apiKey = field("API Key", prefs.getString("api_key", ""), true); form.addView(apiKey);
        endpoint = field("API Endpoint URL", prefs.getString("endpoint", ""), false); form.addView(endpoint);
        model = field("Model name", prefs.getString("model", ""), false); form.addView(model);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle(required ? "ตั้งค่า Zyro AI ก่อนเริ่มใช้งาน" : "ตั้งค่า Zyro AI")
            .setMessage("ใส่ API Key, Endpoint URL และชื่อโมเดลจากผู้ให้บริการที่คุณเลือก ค่าจะถูกบันทึกไว้ในเครื่องนี้")
            .setView(form)
            .setPositiveButton(required ? "บันทึกและเริ่มใช้งาน" : "บันทึก", null)
            .setNegativeButton("ยกเลิก", null)
            .create();
        dialog.setCancelable(!required);
        dialog.setCanceledOnTouchOutside(!required);
        dialog.setOnShowListener(d -> {
            if (required) dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String key = apiKey.getText().toString().trim();
                String url = endpoint.getText().toString().trim();
                String modelName = model.getText().toString().trim();
                if (key.isEmpty() || url.isEmpty() || modelName.isEmpty()) {
                    Toast.makeText(this, "กรุณากรอกทั้ง 3 ช่องให้ครบ", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!(url.startsWith("https://") || url.startsWith("http://"))) {
                    Toast.makeText(this, "Endpoint ต้องเป็น URL ที่ขึ้นต้นด้วย https:// หรือ http://", Toast.LENGTH_LONG).show();
                    return;
                }
                prefs.edit().putString("api_key", key).putString("endpoint", url).putString("model", modelName).apply();
                Toast.makeText(this, "บันทึกการตั้งค่าแล้ว", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
        dialog.show();
    }
    private void showRequiredSettings() {
        showSettings(true);
    }
    private EditText field(String hint, String value, boolean secret) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setText(value); e.setHint(hint);
        e.setHintTextColor(Color.GRAY); e.setTextColor(Color.WHITE); e.setTextSize(13);
        e.setPadding(dp(10), dp(7), dp(10), dp(7)); e.setBackground(shape(panel, 10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(5), 0, dp(5)); e.setLayoutParams(p);
        if (secret) e.setInputType(129);
        return e;
    }
    private static class GradientDrawableCompat extends android.graphics.drawable.GradientDrawable {
        GradientDrawableCompat(int color, float radius) { setColor(color); setCornerRadius(radius); }
    }
    private class AlertDialogCompat {
        private final Context context; private final View view; private final Runnable save;
        AlertDialogCompat(Context c, View v, Runnable r) { context=c; view=v; save=r; }
        void show() {
            new android.app.AlertDialog.Builder(context).setTitle("ตั้งค่า Zyro AI")
                .setMessage("ใช้ API key จากผู้ให้บริการที่คุณเลือก ผู้ให้บริการอาจมีค่าใช้จ่ายและนโยบายการใช้งาน")
                .setView(view).setPositiveButton("บันทึก", (d,w) -> save.run())
                .setNegativeButton("ยกเลิก", null).show();
        }
    }
}
