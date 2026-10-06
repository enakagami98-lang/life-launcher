"""uiautomator で画面上の文字を探してタップ/長押し/存在確認する小さな道具"""
import re, subprocess, sys, time
import xml.etree.ElementTree as ET

def adb(*a):
    return subprocess.run(["adb", *a], capture_output=True, text=True).stdout

def nodes():
    for _ in range(4):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("exec-out", "cat", "/sdcard/ui.xml")
        if "<hierarchy" in xml:
            return list(ET.fromstring(xml[xml.find("<hierarchy"):]).iter("node"))
        time.sleep(1)
    return []

def find(text, exact=True):
    ns = nodes()
    for n in ns:
        if text in (n.get("text"), n.get("content-desc")):
            return n
    if not exact:
        for n in ns:
            if text in n.get("text", "") or text in n.get("content-desc", ""):
                return n
    return None

def center(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2

cmd, text = sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else ""
if cmd == "texts":
    print(" | ".join(n.get("text") for n in nodes() if n.get("text")))
    sys.exit(0)
n = find(text, exact=(cmd == "has"))
if n is None:
    print(f"NOTFOUND: {text}")
    sys.exit(1)
x, y = center(n)
if cmd == "tap":
    adb("shell", "input", "tap", str(x), str(y))
elif cmd == "long":
    adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "1200")
print(f"FOUND: {text}")
