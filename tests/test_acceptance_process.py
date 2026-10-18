"""验收取消必须清理测试子进程；不启动数据库或浏览器。"""
import importlib.util
import os
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import time
import unittest

ROOT=Path(__file__).resolve().parent.parent
spec=importlib.util.spec_from_file_location('acceptance',ROOT/'scripts/acceptance.py')
acceptance=importlib.util.module_from_spec(spec)
spec.loader.exec_module(acceptance)

@unittest.skipUnless(sys.platform.startswith('linux'),'进程归属检查使用 Linux /proc')
class AcceptanceProcessTests(unittest.TestCase):
    def assertStopped(self,pid):
        deadline=time.monotonic()+5
        while time.monotonic()<deadline:
            file=Path('/proc')/str(pid)/'stat'
            if not file.exists() or file.read_text().split()[2]=='Z':return
            time.sleep(.05)
        self.fail('测试子进程仍存活')

    def test_normal_parent_exit_cleans_remaining_child(self):
        with tempfile.TemporaryDirectory() as td:
            record=Path(td)/'pid'
            script="import subprocess,sys,pathlib; p=subprocess.Popen([sys.executable,'-c','import time;time.sleep(60)']);pathlib.Path(sys.argv[1]).write_text(str(p.pid))"
            self.assertEqual(acceptance.run_tests([sys.executable,'-c',script,str(record)],ROOT,os.environ.copy()),0)
            self.assertStopped(int(record.read_text()))

    def test_interrupt_returns_failure_and_cleans_child_group(self):
        with tempfile.TemporaryDirectory() as td:
            record=Path(td)/'pid';wrapper=Path(td)/'wrapper.py'
            child="import subprocess,sys,pathlib,time; p=subprocess.Popen([sys.executable,'-c','import time;time.sleep(60)']);pathlib.Path(sys.argv[1]).write_text(str(p.pid));time.sleep(60)"
            wrapper.write_text("import importlib.util,signal,sys,os\n"+
                f"spec=importlib.util.spec_from_file_location('acceptance',{str(ROOT/'scripts/acceptance.py')!r})\nm=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)\n"+
                "signal.signal(signal.SIGTERM,m.stop_signal)\ntry: \n"+
                f" m.run_tests([sys.executable,'-c',{child!r},{str(record)!r}],{str(ROOT)!r},os.environ.copy())\n"+
                "except KeyboardInterrupt:sys.exit(130)\n")
            process=subprocess.Popen([sys.executable,str(wrapper)])
            try:
                deadline=time.monotonic()+5
                while not record.exists() and time.monotonic()<deadline:time.sleep(.05)
                self.assertTrue(record.exists());process.send_signal(signal.SIGTERM)
                self.assertEqual(process.wait(timeout=15),130);self.assertStopped(int(record.read_text()))
            finally:
                if process.poll() is None:process.kill();process.wait()

if __name__=='__main__':unittest.main()
