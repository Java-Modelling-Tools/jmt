# JSIM Test Files Diff Check
La seguente cartella contiene un controllo di conformità volto a verificare eventuali cambiamenti nel simulatore passando tra le diverse versioni di JMT, fino alla più recente al momento della redazione di questo documento, la versione `1.3.0`.

## `comparison_report.csv`
Il file CSV `comparison_report.csv` riporta i risultati della comparazione tra i dati ottenuti eseguendo lo stesso modello `.jsimg` nella versione `1.3.0` di JMT e in versioni precedenti.

Le colonne del file sono le seguenti:
1. **`Filename`** – indica il nome del file .jsimg a cui si riferiscono le misurazioni.
2. **`Measure Name`** – specifica il nome della misura.
3. **`Old Value`** – rappresenta il valore della misura nei risultati ottenuti runnando il modello in una versione precedente di JMT.
4. **`New Value`** – rappresenta il valore della misura nei risultati ottenuti runnando il modello nella versione `1.3.0` di JMT.
5. **`Change`** – indica la variazione tra New Value e Old Value.
6. **`% Change`** – indica la variazione percentuale rispetto al valore precedente.

## `/changed_files`
La cartella `/changed_files` contiene due sottocartelle:
- **`/previous`** – contiene i file `.jsimg` con i risultati ottenuti eseguendo il modello in una versione precedente di JMT.  
- **`/latest`** – contiene i file `.jsimg` con i risultati ottenuti eseguendo il modello nella versione `1.3.0` di JMT.  
