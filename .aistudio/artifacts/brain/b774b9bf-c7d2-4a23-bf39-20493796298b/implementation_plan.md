# خطة إعداد سير عمل GitHub Actions لبناء وحفظ Debug APK

إعداد سير عمل أوتوماتيكي متكامل عبر GitHub Actions داخل المسار `.github/workflows/build.yml` يقوم بتجميع تطبيق "وافي" وحفظ ملف `app-debug.apk` كـ Artifact قابل للتنزيل مباشرة.

### User Review & Critical Decisions

> [!IMPORTANT]
> تم تأكيد خيارات سير العمل بناءً على إجاباتك:
> - **مسار التجميع**: بناء ملف الـ APK مباشرة عبر الأمر `./gradlew assembleDebug` لتسريع زمن عمل الـ CI وتوفير الموارد.
> - **مدة الاحتفاظ بملف الـ APK**: الاحتفاظ بالـ Artifact لمدة 7 أيام (`retention-days: 7`).
> - **شروط التشغيل (Triggers)**: يتم التشغيل تلقائياً عند الدفع (`push`) لفرعي `main` و `develop` بالإضافة إلى التشغيل اليدوي (`workflow_dispatch`).

---

### 1. Overview & Core Concept

- **الهدف**: تمكين فريق التطوير من الحصول على نسخة تجريبية جاهزة للتثبيت فوراً (`app-debug.apk`) عند دفع أي تحديث للكود على مستودع GitHub دون الحاجة لبناء التطبيق محلياً.
- **التوافق التام مع المشروع**:
  - ضبط إصدار Java إلى **JDK 17** (Temurin) ليتوافق بدقة مع Gradle 9.3.1 و Android Gradle Plugin 9.1.1 و Kotlin 2.2.10.
  - إعداد ملف التوقيع `debug.keystore` تلقائياً من `debug.keystore.base64` أو إنشائه إن لم يكن متوفراً لتفادي أي خطأ توقيع في الـ Runner.
  - تهيئة ملف `.env` من `.env.example` لمنع توقف Secrets Gradle Plugin.
  - استخدام `actions/upload-artifact@v4` لرفع الـ APK وحفظه لمدة 7 أيام باسم واضح وسهل التنزيل.

---

### 2. CI/CD Architecture & Pipeline Flow

```
┌────────────────────────────────────────────────────────┐
│               GitHub Actions Runner                    │
│                 (ubuntu-latest)                        │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│ 1. Checkout Repository (actions/checkout@v4)           │
├────────────────────────────────────────────────────────┤
│ 2. Setup JDK 17 (actions/setup-java@v4 - Temurin)      │
├────────────────────────────────────────────────────────┤
│ 3. Setup Gradle Caching (gradle/actions/setup-gradle)  │
├────────────────────────────────────────────────────────┤
│ 4. Prepare Environment & Keystore:                     │
│    - cp .env.example .env                              │
│    - Decode debug.keystore.base64 to debug.keystore    │
├────────────────────────────────────────────────────────┤
│ 5. Grant execute permissions (chmod +x gradlew)        │
├────────────────────────────────────────────────────────┤
│ 6. Build Debug APK (./gradlew assembleDebug)           │
├────────────────────────────────────────────────────────┤
│ 7. Upload Artifact (actions/upload-artifact@v4)        │
│    - Path: app/build/outputs/apk/debug/app-debug.apk   │
│    - Retention: 7 days                                 │
└────────────────────────────────────────────────────────┘
```

---

### 3. Key Technical Decisions & Trade-Offs

- **استخدام JDK 17**:
  - *السبب*: يتطلب Gradle 9.3.1 و AGP 9.1.1 بيئة تشغيل Java 17 كحد أدنى قياسي ومستقر لأجهزة Ubuntu Runners في GitHub.
- **استخراج الـ Keystore الآمن**:
  - *السبب*: ملف `debug.keystore` موجود في `.gitignore`، بينما `debug.keystore.base64` متوفر في المشروع، وسيقوم سير العمل بفك تشفيره تلقائياً إلى جذر المشروع لتجاوز أي خطأ في خطوة التوقيع `signingConfigs.debugConfig`.
- **الاحتفاظ بالـ Artifact لمدة 7 أيام**:
  - *السبب*: التزاماً بطلبك لتوفير مساحة التخزين في حساب GitHub مع إتاحة مدة كافية للمطورين لتجربة وتنزيل النسخة.
