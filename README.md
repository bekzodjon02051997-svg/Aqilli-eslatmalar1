# Aqlli Eslatma — Android MVP

O'zbekcha matn/ovoz kiritish, vazifalarni saqlash va Android notification eslatmalari uchun boshlang'ich loyiha.

## Hozirgi MVP funksiyalari
- O'zbekcha ovoz kiritishni Android qurilmadagi speech recognition xizmati orqali chaqirish (`uz-UZ`; natija qurilma/xizmatga bog'liq).
- Matndan bir nechta vazifani ajratish (yangi qator, nuqtali vergul yoki gap chegaralari).
- Sana/vaqt tanlash va sodda "bugun", "ertaga", "indin", "soat 10", "10:30" talqinlari.
- Lokal saqlash.
- Android notification va ikkita alohida bajarish bosqichi.
- Takroriy eslatma va telefon qayta yoqilganda qayta rejalashtirish.
- GitHub Actions orqali APK build.

## Muhim cheklovlar
Bu — ishlashni boshlash uchun MVP, to'liq AI mahsuloti emas. U hali o'zbekcha nutqni AI yordamida ishonchli tozalash, barcha o'zbekcha sonlarni tushunish, murakkab sanalarni to'liq aniqlash, takrorlanuvchi kalendar qoidalari, Room migratsiyalari va keng avtomatlashtirilgan testlarni amalga oshirmaydi. Speech recognition sifati Android qurilmadagi xizmatga bog'liq. Android force-stop, ruxsatlar va batareya cheklovlari bildirishnomalarga ta'sir qilishi mumkin.

## GitHub'ga yuklash
1. GitHub'da `AqlliEslatma` nomli bo'sh repository yarating.
2. ZIP faylni kompyuteringizda oching.
3. Ichidagi fayl va papkalarni repository'ga yuklang (papkaning o'zini ichma-ich takroran yuklamang).
4. `app/build.gradle.kts`, `settings.gradle.kts` va `.github/workflows/build-apk.yml` repository'da borligini tekshiring.
5. `Actions` bo'limida `Build Android APK` workflow bajarilishini kuting.
6. Muvaffaqiyatli tugagach, workflow run sahifasidagi `Artifacts` qismidan `AqlliEslatma-debug-apk` faylini yuklab oling.

## Lokal build
Android Studio va Android SDK o'rnatilgan bo'lsa:
```bash
gradle assembleDebug
```
APK odatda `app/build/outputs/apk/debug/app-debug.apk` yo'lida yaratiladi.

## Bepul ishlatish
Mahalliy vazifalar va notificationlar uchun pullik AI API kerak emas. GitHub Actions va onlayn speech xizmatlarining foydalanish limitlari alohida bo'lishi mumkin. APK'ni o'rnatgach, notification ruxsatini yoqing.
