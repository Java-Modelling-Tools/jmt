# Regression Testing Analysis Report / Rapporto di Analisi dei Test di Regressione

## [English] Regression Testing Analysis Report

### Overview
This archive contains the results of the automated regression testing performed on the JMT (Java Modelling Tools) simulation engine. The analysis compares performance metrics between **previous** versions and the **1.3.0** version to detect numerical instabilities or regressions.

The results are exported into two CSV files:

### 1. Detailed Report (`comparison_detailed.csv`)
This file contains the granular, row-by-row comparison of every performance metric extracted from the simulation files.

* **Filename**: The name of the JMT model file (`.jsimg`).
* **Measure Name**: The specific performance measure being evaluated (e.g., *System Throughput*, *Queue Length*).
* **Old Value**: The result from the previous versions.
* **New Value**: The result from the 1.3.0 version.
* **SMAPE (%)**: The **Symmetric Mean Absolute Percentage Error**. It quantifies the difference between the two values as a percentage relative to their average.
```math
SMAPE = \frac{|new\_value - old\_value|}{\frac{new\_value + old\_value}{2}} * 100
```
* **Is Stable (< [threshold]%)**: A boolean check indicating if the deviation is within the acceptable tolerance.
  * `TRUE`: The difference is negligible (less than [threshold]%).
  * `FALSE`: The difference is significant, indicating a potential regression.

### 2. Summary Report (`comparison_summary.csv`)
This file provides a high-level overview of the health of each model file in the test suite.

* **Filename**: The name of the JMT model file.
* **Overall Status**:
  * `Unchanged`: All metrics are numerically identical.
  * `Changed (Stable)`: Differences exist, but all are within the [threshold]% tolerance.
  * `Unstable`: At least one metric exceeds the [threshold]% error threshold.
  * `Metrics Added/Removed`: The solver produced a different set of metrics than before (structural change).
* **Total Metrics**: Total number of indices compared for this file.
* **Max Deviation**: The highest SMAPE percentage observed in this file.
* **Unstable Metrics Count**: The number of metrics that failed the stability check.

---

## [Italiano] Rapporto di Analisi dei Test di Regressione

### Panoramica
Questo archivio contiene i risultati dei test di regressione automatizzati eseguiti sul motore di simulazione JMT (Java Modelling Tools). L'analisi confronta le metriche di prestazione tra le versioni **precedenti** e la versione **1.3.0** per rilevare instabilità numeriche o regressioni.

I risultati sono esportati in due file CSV:

### 1. Report Dettagliato (`comparison_detailed.csv`)
Questo file contiene il confronto granulare, riga per riga, di ogni metrica di prestazione estratta dai file di simulazione.

* **Filename**: Il nome del file del modello JMT (`.jsimg`).
* **Measure Name**: La specifica misura di prestazione valutata (es. *System Throughput*, *Queue Length*).
* **Old Value**: Il risultato ottenuto dalle versioni precedente.
* **New Value**: Il risultato ottenuto dalla versione 1.3.0.
* **SMAPE (%)**: L'errore percentuale medio assoluto simmetrico (**Symmetric Mean Absolute Percentage Error**). Quantifica la differenza tra i due valori come percentuale relativa alla loro media.
```math
SMAPE = \frac{|new\_value - old\_value|}{\frac{new\_value + old\_value}{2}} * 100
```
* **Is Stable (< [soglia]%)**: Controllo booleano che indica se la deviazione rientra nella tolleranza accettabile.
  * `TRUE`: La differenza è trascurabile (inferiore al [soglia]%).
  * `FALSE`: La differenza è significativa, indicando una potenziale regressione.

### 2. Report di Riepilogo (`comparison_summary.csv`)
Questo file fornisce una panoramica ad alto livello dello stato di ciascun file modello nella suite di test.

* **Filename**: Il nome del file del modello JMT.
* **Overall Status**:
  * `Unchanged`: Tutte le metriche sono numericamente identiche.
  * `Changed (Stable)`: Esistono differenze, ma tutte rientrano nella tolleranza del [soglia]%.
  * `Unstable`: Almeno una metrica supera la soglia di errore del [soglia]%.
  * `Metrics Added/Removed`: Il risolutore ha prodotto un insieme di metriche diverso rispetto a prima (cambiamento strutturale).
* **Total Metrics**: Numero totale di indici confrontati per questo file.
* **Max Deviation**: La percentuale SMAPE più alta osservata in questo file.
* **Unstable Metrics Count**: Il numero di metriche che non hanno superato il controllo di stabilità.