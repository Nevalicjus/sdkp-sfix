## SDKP Submision Fixer

QOL fixy
  - dodaje `.project`, jeśli nie istnieje
  - dodaje `.classpath`, jeśli nie istnieje
  - jeśli odpowiedni folder nie istneieje, tworzy go i przenosi tam wszystkie pliki
  - usuwa `.DS_Store`, `._.DS_Store`, `__MACOSX`
  - do plików `src/*.java` dodaje javadoc author, jeśli nie istnieje

`SDKPSubmissionFixer.s_project` i `SDKPSubmissionFixer.s_classpath` można łatwo zmienić na dane konfiguracją Stringi

`SDKPSubmissionFixer.SubmissionFile` jest wewnętrzną klasą konfiguracyjną od "informacji nt. oddającego i zadania",
która prawdopodobnie już istnieje i trzeba ją na nią zamienić

Wszysktie booleanowe właściwości `SDKPSubmissionFixer` powinny też być dyktowane przez wcześniej stworzoną klasę konfiguracyjną