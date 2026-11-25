# S-Towny (v1.2.0)

Paper icin 3 katmanli (Kasaba -> Krallik -> Kraliyet) arazi plugini. Komutlar `/st` ile baslar.
Gercek Towny'nin tam klonu degildir (savas, ittifak, plot satisi yok).

Ozellikler: Vault ekonomi (Essentials dahil), katlanarak artan claim maliyeti, claim koruma (blok + sandik),
arazi girisinde ekranda kasaba adi, claim efekti, /st pvp, /st town breakblock|placeblock <oyuncu>, /st land,
vergi/bakim, davet, yardimci, Dynmap, scoreboard (sadece secili dunyada), TR/RU/EN.

Dunya kisitlamasi: config.yml -> world-restriction.enabled: true ve worlds listesine dunya adini yaz.
Scoreboard notu: baska bir scoreboard plugini varsa birbirini ezebilir; update-interval-seconds: 1 yap.
Derleme: mvn clean package veya GitHub Actions.

## v1.2.0
- Ekranin ustunde SURENLI boss bar (kasaba: "Kasaba (Krallik) - (PVP/Guvenli) - [Ev/Misafir]", doga: "Doga"); buyuk baslik kaldirildi.
- Scoreboard bastan yazildi (titreme yok, Towny dunyasi disinda hic dokunmaz). Baska scoreboard plugini varsa
  Towny dunyasini onda hariç tut ya da scoreboard.enabled: false yapip %stowny_*% placeholder'larini kullan.
- /st delete (onay ister: /st delete confirm), /st border (Dynmap'siz sinir gosterimi, 3 dk).
- PlaceholderAPI: %stowny_town% %stowny_kingdom% %stowny_royal% %stowny_balance% %stowny_claims% %stowny_pvp% %stowny_here%
