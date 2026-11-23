import pandas as pd
import plotly.express as px
import streamlit as st
from src.storage import add_round, get_rounds, delete_all
from src.analytics import summarize, target_rate, rolling_backtest, window_summaries

st.set_page_config(page_title="Crash Next Move Tracker", page_icon="✈️", layout="wide")
st.title("✈️ Crash Next Move Tracker")
st.caption("Historical analytics only — past rounds cannot reveal the next result.")
with st.sidebar:
    st.header("Record / import")
    value=st.number_input("Multiplier",min_value=0.01,value=1.0,step=0.01,format="%.2f")
    if st.button("Add round"):
        add_round(value); st.rerun()
    uploaded=st.file_uploader("CSV with multiplier column",type=["csv"])
    if uploaded is not None:
        df=pd.read_csv(uploaded)
        if "multiplier" not in df.columns: st.error("CSV must have multiplier column")
        else:
            clean=pd.to_numeric(df["multiplier"],errors="coerce").dropna()
            clean=clean[clean>0]
            if st.button("Import rows"):
                for x in clean: add_round(float(x))
                st.success(f"Imported {len(clean)}"); st.rerun()
    if st.button("Delete saved history"): delete_all(); st.rerun()
values=get_rounds()
if not values: st.info("Add multipliers or import CSV to begin."); st.stop()
s=summarize(values)
cols=st.columns(4)
cols[0].metric("Rounds",s["count"]); cols[1].metric("Median",f'{s["median"]:.2f}x')
cols[2].metric("Low streak (<2x)",s["streak_low"]); cols[3].metric("High streak (≥10x)",s["streak_high"])
recent=values[-50:]
df=pd.DataFrame({"Round":range(max(1,len(values)-len(recent)+1),len(values)+1),"Multiplier":recent})
st.subheader("Recent rounds")
st.plotly_chart(px.line(df,x="Round",y="Multiplier",markers=True),use_container_width=True)
st.dataframe(df.iloc[::-1],use_container_width=True,hide_index=True)
st.subheader("Rolling statistics")
stats=window_summaries(values); c=st.columns(4)
for col,w in zip(c,(10,25,50,100)):
    z=stats[w]; col.metric(f"Last {w} rounds",z["count"])
    if z["median"] is not None:
        col.write(f'Median: {z["median"]:.2f}x')
        col.write(f'Below 2x: {z["low_rate"]:.1%}')
        col.write(f'10x+: {z["high_rate"]:.1%}')
st.subheader("Historical target rate")
target=st.slider("Target multiplier",1.1,20.0,2.0,0.1)
r=target_rate(values,target)
st.metric(f"Historical rounds ≥ {target:.2f}x",f'{r["rate"]:.1%}',f'{r["hits"]}/{r["sample"]}')
st.caption(f'95% interval: {r["ci_low"]:.1%}–{r["ci_high"]:.1%}. Sample interval, not next-round odds.')
st.write("Rolling descriptive backtest:",rolling_backtest(values,target,100))
st.plotly_chart(px.histogram(pd.DataFrame({"Multiplier":values}),x="Multiplier",nbins=30),use_container_width=True)
st.warning("No next-round signal is verified. Streaks/frequencies are descriptive, not predictive.")
st.download_button("Export CSV",pd.DataFrame({"multiplier":values}).to_csv(index=False).encode(),"crash_history.csv","text/csv")
