# Analiza błędów i problemów w aplikacji bankowej

## 🔴 KRYTYCZNE PROBLEMY BEZPIECZEŃSTWA

### 1. SQL Injection w DAOImpl
**Lokalizacja:** [DAOImpl.java](src/main/java/db/dao/impl/DAOImpl.java#L27)

```java
String sql = "SELECT ... WHERE user_name = '"+userName+"'";
```

**Problem:** Bezpośrednie wstawianie parametrów do zapytania SQL bez PreparedStatement
**Skutek:** Atakujący może wstrzyknąć kod SQL, np. `userName = "admin' OR '1'='1"`
**Dotyczy również:**
- [findPasswordForUser](src/main/java/db/dao/impl/DAOImpl.java#L49) - linia 49
- [findAccountById](src/main/java/db/dao/impl/DAOImpl.java#L77) - linia 77
- [updateAccountState](src/main/java/db/dao/impl/DAOImpl.java#L97) - linia 97
- [setUserPassword](src/main/java/db/dao/impl/DAOImpl.java#L114) - linia 114
- [logOperation](src/main/java/db/dao/impl/DAOImpl.java#L166) - linia 166

**Rozwiązanie:** Użyć PreparedStatement

---

## 🔴 BŁĘDY LOGICZNE

### 2. Brak walidacji null w AccountManager.paymentIn
**Lokalizacja:** [AccountManager.java](src/main/java/biz/AccountManager.java#L27)

```java
Account account = dao.findAccountById(accountId);
Operation operation = new PaymentIn(user, ammount, description, account);
boolean success = account.income(ammount);  // NPE jeśli account == null
```

**Problem:** Nie sprawdza czy `account` nie jest null przed użyciem
**Skutek:** NullPointerException gdy konto nie istnieje

---

### 3. Brak atomowości transakcji w internalPayment
**Lokalizacja:** [AccountManager.java](src/main/java/biz/AccountManager.java#L55-L68)

```java
success = sourceAccount.outcome(ammount);
success = success && destAccount.income(ammount);
if (success) {
    success = dao.updateAccountState(sourceAccount);
    if (success) dao.updateAccountState(destAccount);
}
```

**Problem:** 
- Jeśli `updateAccountState(destAccount)` zawiedzie, pieniądze znikną
- Brak rollback-u po częściowej awarii
- Modyfikacja w pamięci, potem zapis do bazy - brak transakcji

**Skutek:** Możliwa utrata pieniędzy klienta

---

### 4. Nadpisywanie wyniku operacji
**Lokalizacja:** [AccountManager.java](src/main/java/biz/AccountManager.java#L42)

```java
boolean success = account.outcome(ammount);
success = dao.updateAccountState(account);  // Nadpisuje wynik outcome!
```

**Problem:** Jeśli `outcome` zwróci false, ale `updateAccountState` zwróci true, finalny wynik będzie błędny
**Skutek:** Niepoprawne logowanie statusu operacji

---

### 5. Niepoprawne logowanie w internalPayment
**Lokalizacja:** [AccountManager.java](src/main/java/biz/AccountManager.java#L67)

```java
success = sourceAccount.outcome(ammount);
success = success && destAccount.income(ammount);
// ...
history.logOperation(withdraw, success);
history.logOperation(payment, success);  // Ten sam status dla obu!
```

**Problem:** Obie operacje (wypłata i wpłata) dostają ten sam status, nawet jeśli tylko jedna się powiodła

---

### 6. Brak walidacji w Account.income
**Lokalizacja:** [Account.java](src/main/java/model/Account.java#L11)

```java
public boolean income(double ammount){
    if (ammount<0) return false;
    this.ammount+=ammount;
    return true;
}
```

**Problem:** Akceptuje `ammount = 0`, co jest bezużyteczną operacją
**Możliwe też:** przepełnienie double (bardzo duże liczby)

---

### 7. Błędna walidacja w Account.outcome
**Lokalizacja:** [Account.java](src/main/java/model/Account.java#L17)

```java
public boolean outcome(double ammount){
    if (this.ammount<ammount || ammount<0.01) return false;
    this.ammount-=ammount;
    return true;
}
```

**Problem:** 
- Jeśli `ammount = -100`, pierwszy warunek może być spełniony, ale drugi odrzuci
- Niejasna logika - czy `outcome(0)` powinno być dozwolone?
- Brak sprawdzenia czy ammount jest dodatnia PRZED porównaniem z saldem

---

### 8. Niezainicjalizowane bankHistory w InterestOperator
**Lokalizacja:** [InterestOperator.java](src/main/java/biz/InterestOperator.java#L19-L24)

```java
protected BankHistory bankHistory;  // null!

public InterestOperator (DAO dao, AccountManager am){
    this.dao=dao;
    accountManager = am;
    // bankHistory nie jest ustawione!
}

public void countInterestForAccount(Account account) throws SQLException {
    // ...
    bankHistory.logOperation(operation, success);  // NPE!
}
```

**Problem:** `bankHistory` nigdy nie jest inicjalizowane
**Skutek:** NullPointerException przy próbie logowania operacji odsetek

---

### 9. Brak walidacji userId w InterestOperator
**Lokalizacja:** [InterestOperator.java](src/main/java/biz/InterestOperator.java#L22)

```java
User user = dao.findUserByName("InterestOperator");
```

**Problem:** Nie sprawdza czy użytkownik "InterestOperator" istnieje
**Skutek:** NPE przy tworzeniu operacji jeśli użytkownik nie istnieje w bazie

---

### 10. Niebezpieczny współczynnik odsetek
**Lokalizacja:** [InterestOperator.java](src/main/java/biz/InterestOperator.java#L16)

```java
private double interestFactor = .2;  // 20% !!!
```

**Problem:** 
- 20% to ekstremalnie wysoka stopa procentowa (prawdopodobnie pomyłka)
- Czy to 20% rocznie? Miesięcznie? Dziennie? Brak dokumentacji
- Brak walidacji czy odsetki są sensowne

---

### 11. Nieobsłużone wyjątki RuntimeException
**Lokalizacja:** [BankHistory.java](src/main/java/biz/BankHistory.java#L30-L38)

```java
public void logPaymentIn(Account account, double ammount, boolean success) {
    throw new RuntimeException();  // Celowo niezaimplementowane?
}

public void logUnauthorizedOperation(Operation operation, boolean success) {
    throw new RuntimeException();  // Ale jest wywoływane w kodzie!
}
```

**Problem:** 
- Metody rzucają RuntimeException zamiast być zaimplementowane
- `logUnauthorizedOperation` jest wywoływana w [AccountManager.java](src/main/java/biz/AccountManager.java#L42)

---

## ⚠️ PROBLEMY Z BEZPIECZEŃSTWEM AUTORYZACJI

### 12. Słaba autoryzacja w AuthenticationManager.canInvokeOperation
**Lokalizacja:** [AuthenticationManager.java](src/main/java/biz/AuthenticationManager.java#L78)

```java
public boolean canInvokeOperation(Operation operation, User user) {
    if (user.getRole().getName().equals("Admin")) return true;
    if (operation.getType() == OperationType.PAYMENT_IN) return true;  // Każdy może wpłacać!
    if (operation.getType() == OperationType.WITHDRAW) {
        Withdraw op = (Withdraw) operation;
        return user.getId() == op.getUser().getId();  // Porównanie ID
    }
    return false;
}
```

**Problemy:**
- Każdy może wykonać wpłatę na dowolne konto (potencjalny exploit)
- Brak walidacji czy `user.getRole()` nie jest null
- Brak obsługi INTEREST - co jeśli ktoś spróbuje?
- Porównanie tylko ID użytkowników bez sprawdzenia właściciela konta

---

### 13. Brak walidacji role przed sprawdzeniem nazwy
**Lokalizacja:** [AuthenticationManager.java](src/main/java/biz/AuthenticationManager.java#L78)

```java
if (user.getRole().getName().equals("Admin")) return true;
```

**Problem:** NPE jeśli `user.getRole()` jest null

---

## ⚠️ PROBLEMY Z WALIDACJĄ DANYCH

### 14. Brak walidacji ujemnych kwot w AccountManager
**Lokalizacja:** [AccountManager.java](src/main/java/biz/AccountManager.java#L26-L48)

```java
public boolean paymentIn(User user, double ammount, ...) {
    // Brak sprawdzenia czy ammount > 0
}

public boolean paymentOut(User user, double ammount, ...) {
    // Brak sprawdzenia czy ammount > 0
}
```

**Problem:** Metody nie walidują kwot przed przekazaniem do Account
**Skutek:** Poleganie tylko na walidacji w Account (single point of failure)

---

### 15. Niebezpieczna konwersja w CurrencyExchange
**Lokalizacja:** [CurrencyExchange.java](src/main/java/exchange/CurrencyExchange.java#L30)

```java
public double exchange(String from, String to, double value) throws Exception {
    if (value < 0) throw new IllegalArgumentException("Value must be positive");
    double rate = rate(from,to);
    return rate*value;
}
```

**Problem:** 
- `value = 0` jest akceptowane (sensowne czy nie?)
- Brak walidacji overflow po mnożeniu
- Bardzo ogólny typ wyjątku `Exception`

---

### 16. Brak sprawdzenia null w rate()
**Lokalizacja:** [CurrencyExchange.java](src/main/java/exchange/CurrencyExchange.java#L16)

```java
private double rate(String from, String to) throws Exception{
    if (rates == null) throw new CurrencyExchangeIsNotInitialized();
    for (Rate rate : rates) {
        if (rate.getFrom().equals(from) && rate.getTo().equals(to)) { ... }
```

**Problem:** Nie sprawdza czy `from` lub `to` są null - NPE w equals

---

## 🟡 PROBLEMY Z PROJEKTOWANIEM

### 17. Statyczna metoda buildBank zwraca null
**Lokalizacja:** [AccountManager.java](src/main/java/biz/AccountManager.java#L71-L88)

```java
public static AccountManager buildBank() {
    try {
        // ...
        return aManager;
    } catch (SQLException e) {
        e.printStackTrace();
    } catch (ClassNotFoundException e) {
        e.printStackTrace();
    }
    return null;  // Zły wzorzec
}
```

**Problem:** 
- Zwraca null zamiast rzucić wyjątek
- Kod wywołujący musi pamiętać o sprawdzeniu null
- Łapie wyjątki i tylko drukuje stack trace

---

### 18. Logowanie loggedUser w AccountManager
**Lokalizacja:** [AccountManager.java](src/main/java/biz/AccountManager.java#L24)

```java
User loggedUser=null;

public boolean logIn(String userName, char[] password) {
    loggedUser = auth.logIn(userName, password);
    return loggedUser!=null;
}
```

**Problem:**
- Tylko jeden zalogowany użytkownik globalnie
- Nie działa dla aplikacji wielowątkowej/wielousers
- Nie jest thread-safe

---

### 19. Typo w nazwie metody
**Lokalizacja:** [CurrencyExchange.java](src/main/java/exchange/CurrencyExchange.java#L12)

```java
public void infitFromFile(String fileName) throws IOException {
```

**Problem:** Powinno być `initFromFile` zamiast `infitFromFile`

---

### 20. Literówka "ammount" zamiast "amount"
**Występuje wszędzie w kodzie:**
- `Account.ammount`
- `Payment.ammount`
- Wszystkie metody

**Problem:** Konsekwentna literówka w całym projekcie (refactoring byłby trudny)

---

## 📊 PODSUMOWANIE

| Kategoria | Liczba problemów |
|-----------|------------------|
| **Krytyczne bezpieczeństwo** | 6 |
| **Błędy logiczne** | 11 |
| **Problemy z autoryzacją** | 2 |
| **Problemy z walidacją** | 3 |
| **Problemy projektowe** | 3 |
| **RAZEM** | **25** |

## 🔧 PRIORYTETOWE NAPRAWY

1. **SQL Injection** - natychmiastowa naprawa (PreparedStatement)
2. **Transakcje w internalPayment** - dodać database transactions
3. **NullPointerException w InterestOperator** - inicjalizować bankHistory
4. **RuntimeException w BankHistory** - zaimplementować metody lub usunąć
5. **Walidacja null dla Account** - sprawdzać przed użyciem

## 🧪 SUGEROWANE TESTY

Utworzyć testy dla:
- SQL injection scenarios
- Null pointer cases  
- Transaction rollback
- Authorization edge cases
- Negative amounts
- Concurrent users
