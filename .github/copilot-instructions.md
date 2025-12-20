Verschaffe dir über #README.md und #IMPLEMENTATION.md einen Überblick über das Projekt.

Aktualisiere diese Dateien wenn nötig bei Code-Änderungen, sie sollten aber nicht zu groß werden.

Wenn du ein Missverständnis hast und dadurch ein Problem entsteht, dessen Bewältigung dich einige Anläufe kostete, dann dokumentiere die Lösung in #IMPLEMENTATION.md, damit andere davon profitieren können.

## Room Database Schema-Änderungen

**Problem:** Room-Crash beim App-Start mit "Room cannot verify the data integrity" nach Schema-Änderungen.

**Ursache:** Wenn man eine Migration bearbeitet, die bereits auf dem Test-Gerät ausgeführt wurde, stimmt der Schema-Hash nicht mehr mit der Versionsnummer überein.

**Lösung - IMMER befolgen:**

1. **Beim Hinzufügen neuer Datenbankfelder:**
   - **NIEMALS** eine bestehende Migration nachträglich ändern
   - **IMMER** die Version in `@Database(version = X)` erhöhen
   - **IMMER** eine neue Migration `MIGRATION_X_Y` erstellen
   
2. **Beispiel:**
   ```kotlin
   // Version erhöhen
   @Database(..., version = 42)
   
   // Neue Migration erstellen
   val MIGRATION_41_42 = object : Migration(41, 42) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE characters ADD COLUMN newField TEXT NOT NULL DEFAULT ''")
       }
   }
   
   // Migration registrieren
   .addMigrations(..., MIGRATION_40_41, MIGRATION_41_42)
   ```

3. **Beim Entwickeln (nur lokal):**
   - Alternative: App deinstallieren und neu installieren
   - Aber: Besser von Anfang an korrekte Migration erstellen

4. **Checkliste bei Schema-Änderungen:**
   - [ ] Datenmodell geändert (z.B. `Character.kt`)
   - [ ] Version in `ApplicatusDatabase.kt` erhöht
   - [ ] Neue Migration erstellt
   - [ ] Migration in `.addMigrations()` registriert
   - [ ] App kompiliert und startet ohne Crash