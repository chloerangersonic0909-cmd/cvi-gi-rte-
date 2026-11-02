# Zyro AI Android

แอปแชต AI สำหรับ Android พร้อมหน้าตาโทนเข้มและตัวช่วยเขียนโค้ด

## สร้าง APK ผ่าน GitHub Actions
1. เปิดแท็บ **Actions** ใน repository
2. เลือก workflow **Build Zyro AI APK**
3. กด **Run workflow**
4. เมื่อทำงานสำเร็จ เปิด workflow run แล้วดาวน์โหลด artifact ชื่อ `Zyro-AI-debug-apk`
5. แตก ZIP เพื่อรับไฟล์ `app-debug.apk`

workflow จะทำงานอัตโนมัติเมื่อมีการแก้ไขไฟล์ในโฟลเดอร์ `ZyroAI/` หรือ workflow นี้

## ใช้งาน
- ติดตั้ง APK บนอุปกรณ์ Android
- กด ⚙️ แล้วใส่ API key, endpoint และ model ของผู้ให้บริการที่รองรับ OpenAI-compatible Chat Completions
- ค่าเริ่มต้นใช้ endpoint ของ OpenAI และ model `gpt-4o-mini`
- API key จะถูกเก็บใน SharedPreferences ของแอปบนเครื่องนี้ อย่าแชร์ APK ที่ฝัง secret และอย่าใช้ API key ที่ไม่ใช่ของคุณ

แอปนี้ไม่ได้รวม API key หรือบริการ AI แบบฟรีไม่จำกัด ผู้ให้บริการอาจมีค่าใช้จ่าย โควตา และนโยบายการใช้งานของตนเอง
