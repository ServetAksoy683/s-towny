# S-Towny (v1.1.0)

Paper icin 3 katmanli (Kasaba -> Krallik -> Kraliyet) arazi plugini. Komutlar `/st` ile baslar.
Gercek Towny'nin tam klonu degildir (savas, ittifak, plot satisi yok).

Ozellikler: Vault ekonomi (Essentials dahil), katlanarak artan claim maliyeti, claim koruma (blok + sandik),
arazi girisinde ekranda kasaba adi, claim efekti, /st pvp, /st town breakblock|placeblock <oyuncu>, /st land,
vergi/bakim, davet, yardimci, Dynmap, scoreboard (sadece secili dunyada), TR/RU/EN.

Dunya kisitlamasi: config.yml -> world-restriction.enabled: true ve worlds listesine dunya adini yaz.
Scoreboard notu: baska bir scoreboard plugini varsa birbirini ezebilir; update-interval-seconds: 1 yap.
Derleme: mvn clean package veya GitHub Actions.
