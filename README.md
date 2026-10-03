# 🛒 MarketWise (OikoNomos) - Android Smart Family Expense Tracker

Εφαρμογή έξυπνης διαχείρισης και κατανομής οικογενειακών εξόδων με φωνητική καταχώρηση στα Ελληνικά, σάρωση αποδείξεων (Receipt OCR), ανάλυση ορίων προϋπολογισμού και **διπλή αρχιτεκτονική βάσης δεδομένων** (Τοπική SQLite/Room + Cloud Firebase Firestore).

---

## 🏗️ 1. Αρχιτεκτονική Βάσης Δεδομένων (Dual Database Architecture)

Για να διασφαλίζεται **100% ασφάλεια και καμία απώλεια δεδομένων (Zero Data Loss)** κατά τις ενημερώσεις ή την αλλαγή συσκευής από τους χρήστες:

### Α. Τοπική Βάση (Local Storage - Room Database)
- **Τεχνολογία**: Android Jetpack Room (SQLite).
- **Πίνακες**:
  - `expenses`: Καταχωρημένα έξοδα, κατηγορία, ημερομηνία, κατανομή μελών.
  - `family_members`: Μέλη οικογένειας, ρόλοι, χρώματα avatar, φωνητικές λέξεις-κλειδιά.
  - `expense_categories`: 15 κατηγορίες με μηνιαία όρια προϋπολογισμού και εικονίδια.
  - `recurring_expenses`: Πάγιες εντολές & συνδρομές (ημέρα μήνα, αυτόματη εκτέλεση).
  - `savings_goals`: Οικογενειακοί στόχοι αποταμίευσης με μπάρες προόδου.
- **Ασφάλεια Migrations**: Χρησιμοποιούνται τα επίσημα `MIGRATION_1_2` και `MIGRATION_2_3` με απενεργοποιημένο το destructive migration (`fallbackToDestructiveMigration(false)`), ώστε κανένα μελλοντικό update της εφαρμογής να μην μπορεί να διαγράψει τα δεδομένα των χρηστών.

### Β. Cloud Βάση ανά Χρήστη & Οικογένεια (Firebase Firestore)
- **Προσωπική Βάση ανά Χρήστη (`/users/{userId}`)**:
  - Κάθε χρήστης που συνδέεται (Google Sign-In ή Email/Password) αποκτά αυτόματα τη δική του ανεξάρτητη, κρυπτογραφημένη συλλογή στο Cloud:
    - `/users/{userId}/expenses/{expenseId}`
  - Τα έξοδα συγχρονίζονται αυτόματα σε πραγματικό χρόνο. Εάν ο χρήστης αλλάξει κινητό ή διαγράψει την εφαρμογή, με την είσοδό του όλα τα δεδομένα επανέρχονται άμεσα.
- **Κοινή Οικογενειακή Βάση (`/households/{householdId}`)**:
  - Για οικογένειες που θέλουν να μοιράζονται το ίδιο εξοδολόγιο σε πολλαπλά κινητά.
  - Σύνδεση με μοναδικό κωδικό πρόσκλησης (π.χ. `OIKO-4821`).
  - Πλήρης συγχρονισμός σε πραγματικό χρόνο ανάμεσα σε όλα τα μέλη.
- **Κανόνες Ασφαλείας (Firestore Security Rules)**:
  - Εφαρμόζεται αυστηρός έλεγχος πρόσβασης (`request.auth.uid == userId`).
  - Κανένας χρήστης δεν μπορεί να διαβάσει ή να επεξεργαστεί δεδομένα άλλου χρήστη.

---

## 🚀 2. Σύνδεση με το GitHub (Πλήρης Έλεγχος Κώδικα)

Το έργο είναι ήδη αρχικοποιημένο με `git` (κλαδί `main`).

### Τρόπος 1: Μέσω του Google AI Studio UI (Άμεσο)
1. Στο επάνω δεξιά μέρος του παραθύρου του AI Studio, πατήστε το μενού **Settings** (⚙️) ή το κουμπί **Export / GitHub**.
2. Επιλέξτε **Push to GitHub**.
3. Συνδέστε τον λογαριασμό σας στο GitHub και επιλέξτε το όνομα του repository (π.χ. `kynigosmarios/marketwise`).

### Τρόπος 2: Μέσω Git CLI
Αν κατεβάσετε το ZIP του project στον υπολογιστή σας:
```bash
# 1. Δημιουργήστε ένα νέο κενό repository στο github.com
# 2. Συνδέστε το τοπικό project με το GitHub:
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/marketwise.git

# 3. Ανεβάστε τον κώδικα στο main κλαδί:
git branch -M main
git push -u origin main
```

---

## 📱 3. Δημοσίευση στο Google Play Store (Release Guide)

Όταν είστε έτοιμοι να δημοσιεύσετε την εφαρμογή στο Google Play:

1. **Δημιουργία Release Keystore**:
   - Δημιουργήστε ένα ασφαλές κλειδί υπογραφής (`release.keystore`).
   - Φυλάξτε το αρχείο κλειδιού και τους κωδικούς σε ασφαλές μέρος.

2. **Προσθήκη SHA-1 & SHA-256 στο Firebase Console**:
   - Μεταβείτε στο [Firebase Console](https://console.firebase.google.com/).
   - Στις ρυθμίσεις του Project (`com.aistudio.marketwise.vkmzla`), προσθέστε τα αποτυπώματα SHA-1 και SHA-256 του Release κλειδιού σας, ώστε να λειτουργεί απρόσκοπτα το Google Sign-In στην τελική έκδοση.

3. **Δημιουργία Android App Bundle (.aab)**:
   ```bash
   gradle bundleRelease
   ```
   Το παραγόμενο αρχείο βρίσκεται στο:
   `app/build/outputs/bundle/release/app-release.aab`

4. **Ανέβασμα στο Google Play Console**:
   - Συνδεθείτε στο [Google Play Console](https://play.google.com/console).
   - Δημιουργήστε νέα εφαρμογή «MarketWise - Έξοδα Οικογένειας».
   - Ανεβάστε το αρχείο `.aab` στην καρτέλα **Production** (ή Closed Testing για δοκιμές).

---

## 🔒 4. Εγγύηση Ακεραιότητας Δεδομένων (Data Resilience)

- **Offline-First**: Η εφαρμογή λειτουργεί πλήρως ακόμα και χωρίς ίντερνετ (μέσω SQLite/Room).
- **Auto-Sync on Connect**: Μόλις συνδεθεί στο ίντερνετ, συγχρονίζει αθόρυβα με το Firebase Firestore.
- **Permanent Backups**: Όλα τα έξοδα αποθηκεύονται τόσο τοπικά όσο και στο Cloud του χρήστη, εξαλείφοντας τον κίνδυνο απώλειας.
