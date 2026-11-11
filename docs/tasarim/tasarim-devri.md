# Choreo `:sample` + README tasarım devri

Onay: 2026-11-11 (Tugrul: "Onayla, devam"; düzeltmeler: Duration rozeti kaydırıldı, kurulum satırı `v0.1.0`).
Mockup: `/Users/tugrulakcadag/AndroidStudioProjects/Choreo/docs/tasarim/choreo-mockup-board.png` (kaynak: `choreo-mockup.html`, kareler A-F).
Karakter: sade profesyonel geliştirici aracı; tek ana renk (primary) + tek aksan (tertiary); hareket yalnız açıklayıcı. Açık + koyu tema. Çalışan uygulama olmadığından denetim yapılmadı; şartname mockup'tan yazıldı.

## Genel
- Yapı: 4 hedefli Navigation bar (80dp): Gallery, Sequences, Playground, Motion. Geniş pencerede (>=600dp) navigation rail'e dönüşür. Etiketler kısa tutulur, her zaman görünür.
- Top app bar (64dp, small): sol başlık (title-large), sağda 48dp "Pause all / Play all" ikon düğmesi (tüm ekranlarda; Motion hariç). Seçilince ikon pause_circle <-> play_circle, içerik açıklaması "Pause all animations" / "Play all animations".
- Renk rolleri: yüzey surface; kartlar surface-container; sahne surface-container-lowest + 16dp noktalı ızgara (outline-variant); şekiller primary ve tertiary; ikincil metin on-surface-variant; seçili öğe secondary/primary-container + on-primary-container. Koyu tema aynı roller (primary ~#B7C4FF, surface ~#121318). Hex değerleri örnektir, rol adı esastır; kontrast >=4.5:1 (metin), >=3:1 (ikon/büyük metin) tüm iki temada sağlanmalı.
- Tipografi: başlık title-large; kart başlığı label-large; açıklama body-medium; kod ve ms değerleri Roboto Mono 12sp (body-small boyutunda); chip/segment etiketi label-large.
- Şekil: kart 12dp, sahne 16dp, chip 8dp, segmented tam yuvarlak uç (20dp), kod bloğu 12dp.

## Tokens doğrulaması (m3read tokens, 2026-11-11)
Okunabilen ve doğrulananlar:
- Filter chip / assist chip: container height 32dp, shape 8dp (corner.small), outline 1dp. Dokunma alanı 48dp olmalı (görsel 32dp).
- Outlined segmented button: container height 40dp, outline 1dp, label label-large.
- Primary navigation tab: container height 48dp, active indicator 3dp, divider 1dp; ikon+etiketli varyant 64dp (biz yalnız etiketli kullanıyoruz).
- Slider: active track height 16dp, handle 4dp x 44dp (focus handle 2dp).
Okunamayan/doğrulanamayanlar: navigation bar, top app bar token sayfaları ve hareket (easing/duration) token sayfası okunamadı (nav bar yalnız guidelines metni okundu: 3-5 hedef, etiket zorunlu). Bunlar M3 genel değerleriyle yazıldı: nav bar 80dp, top app bar small 64dp; geliştirici Compose M3 varsayılanını kullanmalı, ölçüleri elle girmemeli.

## Ekranlar (öncelik sırasıyla)

### 1. Playground (Critical)
- Düzen: üstte sabit blok (sahne 170dp yüksek, transport); altında kayan parametre alanı; en altta kod bloğu (kayan alanın sonunda).
- Transport: restart (48dp ikon), play/pause (48dp filled), seek slider (track 16dp, handle 4x44dp), süre etiketi "0.70 / 1.2 s" (mono). Hepsi her zaman görünür; sabit blok kaymaz.
- Parametreler: Animation (filter chip satırı, yatay kayar, tek seçim: drawOn, eraseOff, moveTo, scaleTo, rotateTo, fadeTo, colorTo); Duration slider "1200 ms" (100-3000), Delay slider "0 ms" (0-2000); Easing exposed dropdown (Linear, FastOutSlowIn, EaseIn, EaseOut, EaseInOut, Spring yerine yalnız sürümde olanlar); Repeat segmented "1x | 3x | Infinite" (varsayılan 1x).
- Kod: seçime göre canlı üretilen satır(lar), sağ üstte 48dp "Copy code" düğmesi; kopyalanınca snackbar "Code copied".
- Durumlar: yükleme yok; hata: geçersiz parametre (örn. negatif süre) slider ile üretilemez, ama kod alanı hata durumunda error rolü ile "Invalid value" gösterir; Infinite seçiliyken sahnede etiket "Looping" ve pause vurgulanır (primary); reduced motion bkz. Motion; 1.5x yazıda sahne 130dp, transport sabit, parametre alanı kayar, kontroller min-height (sabit yükseklik yok); koyu tema: yüzey rolleri.
- Metin: "Playground", "Animation", "Duration", "Delay", "Easing", "Repeat", "Copy code".

### 2. Gallery (Critical)
- Filter chip: "Shapes" (varsayılan seçili) / "Animations". 2 sütunlu kart ızgarası, 12dp aralık, 16dp kenar. Şekiller: Circle, Rectangle, Rounded rect, Line, Polygon, Star, Path, SVG path-data. Animations sekmesi: moveTo, scaleTo, rotateTo, fadeTo, colorTo, drawOn, eraseOff.
- Kart: üstte 96dp canlı önizleme, altında ad (label-large) + tek satır kod (mono, tek satır, taşarsa kırp). Önizleme tek sefer oynar; sağ altta 48dp "Replay" düğmesi. Karta dokunma Playground'u o şekil/animasyonla açar.
- İçerik açıklaması: her önizlemeye "<ad> preview" (örn. "Star, drawing on").
- Durumlar: pause-all aktifken önizlemeler son karede durur; reduced motion açıkken son kare; boş durum yok; büyük yazıda kart yüksekliği içerikle büyür, ızgara 1 sütuna düşer (>=1.5x).

### 3. Sequences (Should, ama README GIF kaynağı)
- Primary tabs (48dp): Sequence, Parallel, Nested, Logo. Sahne 210dp; transport (Playground ile aynı); timeline kartı (satır başına bir animasyon, 28dp satır, bar 16dp, oynatma başlığı 2dp primary çizgi); kod bloğu, oynayan satır primary-container ile vurgulu.
- Logo sekmesi: logo drawOn (1 sn), sonra şekiller; tek sefer, döngü yok.
- Durumlar: reduced -> son kare, timeline başlığı sonda; uzun kod yatay kayar; 1.5x'te timeline etiketleri kısalmaz, satır yüksekliği büyür.
- Metin: "Sequences", "Sequence", "Parallel", "Nested", "Logo", "Timeline".

### 4. Motion (Should)
- Sistem durumu kartı (primary-container): ikon + "System: animations off" / "System: animations on"; açıklama "Animator duration scale is 0x. Choreo shows the final frame." (açıkken "Animations are enabled.").
- Politika radio listesi (56dp satır): "Follow system" (varsayılan, alt metin "Default"), "Always play", "Always show final frame".
- Önizleme çifti: "Normal" (oynar, kendi 48dp pause düğmesi) ve "Reduced" (son kare, "Final frame, no motion"). Tonal düğme "Open system settings".
- Durumlar: sistem ayarı canlı değişince kart güncellenir; koyu tema mockup D.

### 5. README görselleri (Should)
- Hero 1280x640 PNG, koyu: "Choreo", "Draw shapes. Choreograph motion. One timeline, pure Compose.", kurulum satırı `implementation("com.github.tugakca.choreo:choreo:v0.1.0")`, rozetler minSdk 23 / Apache 2.0 / no permissions; sağda logo drawOn kareleri + timeline şeridi.
- 4 GIF (plan mockup F): 1 Hero (Sequences > Logo, koyu, ~4 sn), 2 Sequence vs Parallel (~5 sn), 3 Playground seek+easing (~6 sn), 4 Reduced motion (~3 sn veya statik PNG). Emülatör 1080x1920, sistem çubukları kırpılmış, 15 fps, ~540px. GIF'ler tek sefer ya da kısa; README'ye hareket uyarısı (v0.2 maddesi) eklenir.
- Alt metin: her görsel için "alt" metni ("Choreo logo being drawn on", ...).

### 6. Logo (Nice-to-have, opsiyonel; Tugrul onayladı)
- Açık "C" yayı (primary, yuvarlak uç) + uçta tertiary nokta; wordmark "Choreo" Roboto Bold. Uygulama ikonu: primary zeminde beyaz yay + açık pembe nokta; koyu varyantta #121318 zemin. 16dp'de okunur kalmalı.

## Hareket (Material motion)
- Genel: kısa, açıklayıcı; Choreo'nun kendi demoları (şekil animasyonları) süre/easing'i kullanıcı seçer; arayüz hareketi M3 varsayılanı.
- Ekran geçişleri (nav bar hedefleri arası): fade through, ~300ms, emphasized (standard decelerate) easing; nav indicator M3 varsayılanı.
- Chip/segment/radio seçimi: short2-short3 (100-150ms), standard easing.
- Sahne içi durum değişimi (örn. kod vurgusu, timeline başlığı): doğrudan senkron, ek geçiş yok.
- Hareket olmaması gerekenler: yanıp sönen vurgular, otomatik başlayan sonsuz döngü, parallax, kart giriş zıplamaları, kaydırmada animasyon. Animator ölçeği 0 veya reduced politikası: arayüz geçişleri anlık, demolar son kare.
- Not: M3 hareket token sayfası okunamadı, süre/easing token adları genel M3 bilgisine dayanır; developer Compose M3 hareket varsayılanlarını kullanmalı.

## Tasarım kabul kriterleri (tester, emülatör)
1. Alt çubukta 4 etiketli hedef var: Gallery, Sequences, Playground, Motion; seçili hedefte indicator + ikon dolu.
2. Her ekranda (Motion hariç) top app bar'da "Pause all animations" düğmesi var, dokunma hedefi >=48dp; basınca tüm önizlemeler durur, ikon play'e döner.
3. Gallery'de 8 şekil kartı; her kartta tek satır kod ve >=48dp "Replay" düğmesi; önizlemeler döngü yapmaz.
4. Playground'da kaydırmaya rağmen sahne ve transport (restart, play/pause, seek, süre) ekranda kalır.
5. Playground'da Duration/Delay/Easing/Repeat değişince canlı kod güncellenir; Copy code snackbar "Code copied" gösterir.
6. Repeat varsayılanı 1x; Infinite seçilince sahnede "Looping" etiketi görünür ve pause erişilebilir.
7. Hiçbir ekranda saniyede 3'ten fazla yanıp sönen/değişen alan yok.
8. Sistem animasyon ölçeği 0 iken (adb ile) tüm demolar son kareyi gösterir, Motion kartı "System: animations off" der.
9. Motion'da "Follow system" varsayılan seçili; "Always play" seçilince ölçek 0 olsa da oynar.
10. Yazı boyutu 1.5x: metin kesilmez, transport ekranda kalır, tüm kontroller >=48dp, yatay kaydırma yalnız chip satırı ve kod bloğunda.
11. Açık ve koyu temada metin kontrastı >=4.5:1, ikon/kontrol >=3:1 (rastgele 5 öğe ölçülür).
12. TalkBack: şekil önizlemeleri ve düğmeler içerik açıklamasıyla okunur; seçili chip/radio "selected" bildirilir; odak sırası yukarıdan aşağı.
13. Geri hareketi uygulamadan çıkar ya da önceki hedefe döner; sistem çubukları arkasında içerik kenardan kenara (edge-to-edge) görünür.
14. Hero PNG ve GIF'ler 1280x640 / 540px genişlik, kurulum satırı `...choreo:v0.1.0`.

## Kapsam dışı
Play Store görseli, web sitesi, ayarlar/hesap ekranları, gradient/morph/dokunma demoları (Faz 2), v0.2 stagger/yoyo/dash/50 şekil stres demosu.

## Geliştirici notları
1. Şekil önizlemelerinin tek sefer oynaması ve "Pause all"in küresel durumu paylaşması gerekir.
2. Sistem animasyon ölçeği 0 durumu Motion kartında canlı okunmalı.
