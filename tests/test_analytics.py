import unittest
from src.analytics import summarize,target_rate,wilson_interval,rolling_backtest
class AnalyticsTests(unittest.TestCase):
 def test_summary(self):
  s=summarize([1,2,3,11,12]); self.assertEqual(s["count"],5); self.assertEqual(s["streak_high"],2)
 def test_target(self):
  r=target_rate([1,2,3,4],2); self.assertEqual(r["hits"],3); self.assertEqual(r["rate"],.75)
 def test_empty_interval(self):self.assertEqual(wilson_interval(0,0),(0.0,0.0))
 def test_backtest(self):
  r=rolling_backtest([1.2,2.2,3.5,1.1],2,2); self.assertEqual(r["test_n"],3); self.assertTrue(0<=r["rate"]<=1)
if __name__=="__main__":unittest.main()
