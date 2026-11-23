"""SQLite persistence for recorded crash multipliers."""
from pathlib import Path
from datetime import datetime,timezone
import sqlite3
DB_PATH=Path("crash_tracker.sqlite3")
def connect(db_path:Path=DB_PATH):
    con=sqlite3.connect(db_path)
    con.execute("CREATE TABLE IF NOT EXISTS rounds (id INTEGER PRIMARY KEY AUTOINCREMENT, multiplier REAL NOT NULL CHECK(multiplier>0), created_at TEXT NOT NULL)")
    con.commit(); return con
def add_round(multiplier:float,db_path:Path=DB_PATH):
    v=float(multiplier)
    if not v>0:raise ValueError("Multiplier must be positive")
    with connect(db_path) as con:con.execute("INSERT INTO rounds(multiplier,created_at) VALUES (?,?)",(v,datetime.now(timezone.utc).isoformat()))
def get_rounds(db_path:Path=DB_PATH)->list[float]:
    with connect(db_path) as con:rows=con.execute("SELECT multiplier FROM rounds ORDER BY id").fetchall()
    return [float(r[0]) for r in rows]
def delete_all(db_path:Path=DB_PATH):
    with connect(db_path) as con:con.execute("DELETE FROM rounds")
