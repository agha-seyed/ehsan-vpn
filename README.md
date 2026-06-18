<div align="center">
<img width="800" alt="Wirangaran Logo" src="wirangaran-logo.jpg" />
</div>

<div align="center">
  <a href="#english">English</a> | <a href="#italiano">Italiano</a> | <a href="#فارسی">فارسی</a>
</div>

<br/>

<h1 id="english">🇬🇧 Ehsan VPN (Wirangaran)</h1>

Ehsan VPN is a powerful, secure, and highly optimized Virtual Private Network (VPN) client for Android, proudly developed by the **Wirangaran** programming group.

## Overview
The **Ehsan VPN** application is built upon the robust **Xray Core** and supports the newest and most secure anti-censorship protocols, including **VLESS (Reality)**, **Trojan**, and **ShadowSocks**. Recently, the project underwent a comprehensive refactoring and security enhancement phase to make it truly production-ready.

Key features of this application include:
- **Modern & Fast UI/UX:** Designed using Jetpack Compose with advanced graphic elements, AMOLED Dark mode, and smooth animations.
- **Full Support for Modern Protocols:** Stable connections utilizing the VLESS Reality protocol and Stealth TLS through an optimized Xray Core integration.
- **Speed & Ping Test:** Ability to check server ping and accurately monitor traffic consumption as well as real download/upload speeds using OkHttp.
- **Smart Split Tunneling & Persistence:** Option to select which applications route through the VPN tunnel. All settings, including Split Tunneling, Language, and Theme preferences, are persistently saved using a custom `SharedPreferencesHelper`.
- **Android 13/14+ Stability & Core Reliability:** Optimized for running foreground services without causing force-close issues. Fully complies with Android 13+ Notification Permission requirements. Features a highly stable, reflection-free Xray Core binding with intelligent background loop termination to prevent memory leaks.
- **Production-Ready Security:** Protected by **ProGuard/R8** for code obfuscation and minification, with specific exceptions for V2ray libraries. It enforces encrypted communication via a custom `network_security_config.xml`.
- **Advanced Configuration Support:** Native support for parsing and injecting `alpn` parameters directly into VLESS Reality configurations.
- **Modular Architecture:** A clean, decoupled codebase where the massive UI components are smartly extracted into dedicated composable files (`VpnComposables.kt`), significantly improving maintainability and build speeds.

## Run Locally
**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open the project in Android Studio.
2. Wait for all Gradle dependencies to download.
3. Check the SDK and build settings in `local.properties` or `build.gradle.kts` to match your environment.
4. Run the application on an Emulator or a real device. (A physical device connected to the internet is preferred for real connection testing).

---
> 🌟 *Proudly developed by the Wirangaran Group* 🌟

<br/><hr/><br/>

<h1 id="italiano">🇮🇹 Ehsan VPN (Wirangaran)</h1>

Ehsan VPN è un client di rete privata virtuale (VPN) potente, sicuro e altamente ottimizzato per Android, sviluppato con orgoglio dal gruppo di programmazione **Wirangaran**.

## Panoramica (Overview)
L'applicazione **Ehsan VPN** è basata sul robusto **Xray Core** e supporta i protocolli anti-censura più recenti e sicuri, inclusi **VLESS (Reality)**, **Trojan** e **ShadowSocks**. Recentemente, il progetto ha subito una fase completa di refactoring e miglioramento della sicurezza per renderlo pronto per la produzione.

Le caratteristiche principali di questa applicazione includono:
- **UI/UX Moderna e Veloce:** Progettata utilizzando Jetpack Compose con elementi grafici avanzati, modalità AMOLED Dark e animazioni fluide.
- **Pieno Supporto per Protocolli Moderni:** Connessioni stabili utilizzando il protocollo VLESS Reality e Stealth TLS attraverso un'integrazione ottimizzata di Xray Core.
- **Test di Velocità e Ping:** Possibilità di controllare il ping del server e monitorare accuratamente il consumo di traffico e le reali velocità di download/upload.
- **Split Tunneling Intelligente e Persistenza:** Opzione per selezionare quali applicazioni passano attraverso il tunnel VPN. Tutte le impostazioni (lingua, tema, app selezionate) vengono salvate in modo permanente tramite un `SharedPreferencesHelper` personalizzato.
- **Stabilità su Android 13/14+ e Affidabilità del Core:** Ottimizzata per l'esecuzione dei servizi in background (Foreground Service). Rispetta pienamente i requisiti per i permessi di notifica su Android 13+. Dispone di un'integrazione del Core Xray altamente stabile, priva di Reflection, con terminazione intelligente dei loop in background per prevenire perdite di memoria.
- **Sicurezza Pronta per la Produzione:** Protetto da **ProGuard/R8** per l'offuscamento del codice, con regole specifiche per le librerie V2ray. Applica rigorosamente la crittografia di rete tramite un `network_security_config.xml`.
- **Supporto Configurazione Avanzata:** Supporto nativo per l'estrazione e l'inserimento dei parametri `alpn` nelle configurazioni VLESS Reality.
- **Architettura Modulare:** Codice pulito e disaccoppiato in cui i grandi componenti UI sono estratti in file componibili dedicati, migliorando significativamente la manutenibilità.

## Esecuzione in Locale (Run Locally)
**Prerequisiti:** [Android Studio](https://developer.android.com/studio)

1. Apri il progetto in Android Studio.
2. Attendi il download di tutte le dipendenze Gradle.
3. Controlla le impostazioni dell'SDK e della build in `local.properties` o `build.gradle.kts` per adattarle al tuo ambiente.
4. Esegui l'applicazione su un emulatore o un dispositivo reale. (È preferibile un dispositivo fisico connesso a Internet per testare la connessione reale).

---
> 🌟 *Sviluppato con orgoglio dal Gruppo Wirangaran* 🌟

<br/><hr/><br/>

<h1 id="فارسی">🇮🇷 Ehsan VPN (ویرانگران)</h1>

این اپلیکیشن یک کلاینت قدرتمند، امن و بهینه‌سازی‌شده برای اتصال به شبکه‌های خصوصی مجازی (VPN) در سیستم‌عامل اندروید است که با افتخار توسط **گروه برنامه‌نویسی ویرانگران (Wirangaran)** توسعه داده شده است.

## معرفی برنامه (Overview)

اپلیکیشن **Ehsan VPN** بر پایه هسته قدرتمند **Xray Core** نوشته شده و از جدیدترین و امن‌ترین پروتکل‌های عبور از فیلترینگ از جمله **VLESS (Reality)**، **Trojan** و **ShadowSocks** پشتیبانی می‌کند. در آخرین به‌روزرسانی‌ها، این پروژه یک فاز کاملِ رفع باگ و ارتقای امنیت را پشت سر گذاشته و اکنون کاملاً آماده انتشار در مقیاس وسیع (Production Ready) می‌باشد.

برخی از امکانات کلیدی این برنامه عبارتند از:
- **رابط کاربری مدرن و سریع (UI/UX):** طراحی شده با استفاده از Jetpack Compose با المان‌های گرافیکی پیشرفته، حالت تاریک (AMOLED Dark Mode) و انیمیشن‌های روان.
- **پشتیبانی کامل از پروتکل‌های نوین:** اتصال پایدار با استفاده از پروتکل VLESS Reality و Stealth TLS از طریق یکپارچه‌سازی بی‌نقص با Xray Core.
- **تست پینگ و سرعت واقعی:** قابلیت بررسی پینگ سرورها، پایش دقیق مصرف ترافیک و انجام تست سرعت دانلود کاملاً واقعی (توسط `OkHttp`).
- **تونل‌زنی هوشمند دائمی (Split Tunneling):** امکان انتخاب اپلیکیشن‌هایی که می‌خواهید از تونل VPN عبور نکنند. تمامی تنظیمات برنامه شامل زبان، تم و لیست اپلیکیشن‌های استثنا شده به صورت دائم از طریق لایه `SharedPreferences` ذخیره می‌شوند.
- **پایداری در اندروید ۱۳/۱۴ و پایداری هسته (Core Reliability):** بهینه‌سازی شده برای اجرای Foreground Service بدون فورس‌کلوز. هماهنگی کامل با مجوز نوتیفیکیشن اندروید ۱۳. همچنین اتصال به هسته Xray به صورت کاملاً بومی و ایمن (بدون استفاده از متدهای شکننده Reflection) بازنویسی شده است تا از نشت حافظه (Memory Leak) در زمان قطعی اتصال جلوگیری شود.
- **امنیت در سطح تجاری و مقاوم در برابر کرش:** کدها با تکنولوژی **ProGuard/R8** رمزگذاری (Obfuscate) شده‌اند و استثناهای لازم برای کتابخانه‌های Xray اضافه شده تا برنامه در نسخه نهایی هرگز کرش نکند. ترافیک ناامن و Cleartext نیز مسدود شده است.
- **پشتیبانی از تنظیمات پیشرفته کانفیگ:** قابلیت استخراج هوشمند پارامتر `alpn` از لینک‌های دریافتی و تزریق بومی آن به فایل کانفیگ JSON برای اتصالات VLESS Reality.
- **معماری ماژولار و تمیز:** انتقال از کدهای یکپارچه و سنگین به کدهای ساختاریافته. تمامی المان‌های رابط کاربری به صورت هوشمندانه در فایل‌های اختصاصی (`VpnComposables.kt`) دسته‌بندی شده‌اند تا علاوه بر سرعت بیلد بسیار بالا، توسعه‌های آینده به راحتی انجام شوند و دکمه‌های اتصال به سیستم ضد‌لمس مکرر (Debounce) مجهز شده‌اند.

## توسعه و راه اندازی (Run Locally)

**پیش‌نیازها:** [Android Studio](https://developer.android.com/studio)

۱. پروژه را در Android Studio باز کنید.
۲. منتظر بمانید تا تمامی وابستگی‌های گریدل (Gradle dependencies) دانلود شوند.
۳. در فایل `local.properties` یا `build.gradle.kts`، تنظیمات مربوط به SDK و بیلد را مطابق با محیط خود بررسی کنید.
۴. برنامه را بر روی شبیه‌ساز (Emulator) یا دستگاه واقعی خود اجرا کنید. (ترجیحاً برای تست اتصال واقعی، از دستگاه فیزیکی متصل به اینترنت استفاده شود).

---
> 🌟 *توسعه‌یافته با افتخار توسط گروه ویرانگران* 🌟
