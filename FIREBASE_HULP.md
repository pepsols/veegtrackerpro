# 🚀 Firebase & Cloud Dashboard Hulp

Dit document helpt je bij het instellen en beheren van de **Cloud Meldkamer** en de live verbinding met de Veegtracker Pro app via Firebase.

---

## 1. Firebase CLI Installeren
Zorg dat je de Firebase-gereedschappen op je PC hebt:
1. Open je terminal (PowerShell of CMD).
2. Voer uit: `npm install -g firebase-tools`
3. Log in op je Google account: `firebase login`

---

## 2. De "Machine in de Cloud" Starten (Hosting)
Om je HTML dashboard op internet te zetten zodat je overal taken kunt maken:

1. **Initialisatie:**
   `firebase init hosting`
   * Selecteer je Firebase project.
   * Map voor bestanden: `app/src/main/assets/web`
   * Configureer als single-page app: `Yes`
2. **Online Zetten:**
   `firebase deploy --only hosting`
   * Je krijgt nu een URL (bijv. `https://project-id.web.app`) die je op elke PC kunt openen.

---

## 3. Live Taken Maken & Aanpassen
In je `index.html` dashboard (nu online):
* **Taken:** Klik op "Nieuwe Taak Maken". Dit wordt direct naar de Firestore cloud gestuurd.
* **Live Sync:** De app luistert naar deze wijzigingen. Als jij een taak toevoegt, ziet de chauffeur dit direct op zijn scherm.
* **Onkruidlocaties:** Deze worden vanuit de app naar de cloud gestuurd en verschijnen live op je PC dashboard.

---

## 4. Test Versies Versturen (App Distribution)
Wil je een nieuwe versie van de app naar testers (of je eigen telefoon) sturen zonder de Play Store?

1. **Build & Upload:**
   `./gradlew assembleRelease appDistributionUploadRelease`
2. **Testers Beheren:**
   Ga naar de [Firebase Console](https://console.firebase.google.com/) -> App Distribution om mensen uit te nodigen.

---

## 5. Firebase AI Terminal (Data Beheer)
Je kunt de Firebase terminal gebruiken om live je data te inspecteren:

* **Firestore Bekijken:**
  `firebase firestore:data:get /routes`
* **Real-time Monitoring:**
  Gebruik de Firebase Console om live te zien hoe de GPS-punten binnenkomen in de `routes` en `pois` collecties.

---

## ⚠️ Belangrijk: Configuraties
Zorg dat je de volgende codes hebt ingevuld in `app/src/main/assets/web/index.html`:
```javascript
const firebaseConfig = {
    apiKey: "JOUW_API_KEY",
    authDomain: "JOUW_PROJECT.firebaseapp.com",
    projectId: "JOUW_PROJECT_ID",
    // ... de rest van je Firebase console
};
```

*Je kunt deze codes vinden in de Firebase Console bij Projectinstellingen -> Algemeen -> Jouw apps.*
