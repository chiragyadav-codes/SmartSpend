# SmartSpend 💰

SmartSpend is an Android app that automatically tracks your income and expenses by reading transaction notifications from your banking and UPI apps — no manual data entry required. It categorizes spending, generates monthly reports with charts, and offers simple rule-based investment suggestions based on your savings.

## ✨ Features

- **🔐 Authentication** — Secure email/password login via Firebase Auth
- **✍️ Manual Transactions** — Add, edit, and delete income/expense entries
- **🔔 Automatic Detection** — Reads transaction notifications using Android's NotificationListenerService
- **🧠 Smart Parsing** — Extracts amount, merchant, and transaction type from notification text using regex
- **🏷️ Auto-Categorization** — Maps merchants to categories (Food, Transport, Shopping, Bills, etc.)
- **📊 Monthly Reports** — Income/expense/savings totals with category-wise breakdown, navigable by month
- **📈 Visual Charts** — Pie chart (expense by category) and bar chart (income vs expense)
- **📤 CSV Export** — Download and share any month's transaction data
- **💡 Investment Suggestions** — Rule-based tips based on your monthly savings rate

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Android XML Views |
| Backend / Database | Firebase (Authentication + Firestore) |
| Charts | [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) |
| Architecture | Activity-based, package-per-feature |

## 📱 Screenshots

| Home | Add Transaction | Monthly Summary |
|---|---|---|
| _add screenshot_ | _add screenshot_ | _add screenshot_ |

## 🏗️ How It Works

1. **NotificationListenerService** listens for incoming notifications from banking/UPI apps (with user-granted permission)
2. A **regex-based parser** extracts the amount, merchant name, and transaction type (debit/credit) from the notification text
3. The merchant name is matched against a **keyword-based category map** (e.g., "Swiggy" → Food)
4. The categorized transaction is saved to **Firestore**, tagged with `source: "auto"` to distinguish it from manually added entries
5. **Reports, charts, and suggestions** are all generated from this same transaction data, queried and grouped by month/category

## 🚀 Setup

1. Clone the repo:
```bash
   git clone https://github.com/chiragyadav-codes/SmartSpend.git
```
2. Open in Android Studio
3. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
4. Enable **Authentication** (Email/Password) and **Firestore Database**
5. Download your own `google-services.json` and place it in `app/`
6. Set up Firestore Security Rules (see `firestore.rules` reference below)
7. Build and run on a physical device (NotificationListenerService requires granting Notification Access in system settings)

## 🔒 Privacy & Permissions

SmartSpend uses Android's **Notification Access** permission to read notification *text only* — it does not log into, connect to, or access your banking apps or accounts in any way. All data is stored in your own Firebase project, scoped to your authenticated user via Firestore Security Rules.

## 📄 Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /transactions/{transactionId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
    }
  }
}
```

## 🗺️ Roadmap

- [ ] Support for real bank/UPI notification formats beyond the demo parser
- [ ] Budget limits with alerts
- [ ] Dark mode
- [ ] Recurring transaction detection

## 👤 Author

**Chirag Yadav**
[GitHub](https://github.com/chiragyadav-codes) · [LinkedIn](https://linkedin.com/in/chirag-yadav-73a578423)

## 📝 License

This project is for educational/portfolio purposes.
