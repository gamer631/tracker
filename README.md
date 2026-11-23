# Crash Next Move Tracker

A Python/Streamlit dashboard for tracking crash multipliers, analyzing recent rounds, estimating historical target-hit rates, testing rules with rolling backtests, and exporting data.

**Analytics only:** this app does not predict hidden future outcomes. Crash rounds may be random or independent; historical patterns do not establish a reliable edge.

## Features
- Add rounds manually or import CSV (`multiplier` column required)
- Persistent SQLite storage
- Recent history and streak metrics
- Rolling windows (10 / 25 / 50 / 100)
- Historical target-hit rates with Wilson confidence intervals
- Rolling backtest (descriptive, no future-data leakage)
- Charts and CSV export

## Quick start
Python 3.10+ recommended.

```bash
python -m venv .venv
# Windows PowerShell:
.venv\\Scripts\\Activate.ps1
# macOS/Linux:
source .venv/bin/activate
pip install -r requirements.txt
streamlit run app.py
```

## CSV import
A CSV with a `multiplier` column:

```csv
multiplier
1.14
2.35
1.01
6.72
```

Historical hit rates and confidence intervals describe the saved sample only—not the next round. Don't stake money based on these outputs or chase losses.
