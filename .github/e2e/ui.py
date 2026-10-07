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

def visible_center(text, exact=True):
    """画面外なら上にスクロールしてから中心座標を返す"""
    for _ in range(10):
        n = find(text, exact)
        if n is None:
            adb("shell", "input", "swipe", "540", "1700", "540", "1000", "400"); time.sleep(1)
            continue
        x, y = center(n)
        if 140 <= y <= 2150:
            return x, y
        if y > 2150:
            adb("shell", "input", "swipe", "540", "1700", "540", "1000", "400")
        else:
            adb("shell", "input", "swipe", "540", "900", "540", "1600", "400")
        time.sleep(1)
    return None

cmd = sys.argv[1]
text = sys.argv[2] if len(sys.argv) > 2 else ""
if cmd == "texts":
    print(" | ".join(n.get("text") for n in nodes() if n.get("text")))
elif cmd == "tapclass":
    idx = int(sys.argv[3]) if len(sys.argv) > 3 else 0
    ns = [n for n in nodes() if n.get("class") == text]
    if len(ns) <= idx:
        print(f"NOTFOUND: {text}"); sys.exit(1)
    x, y = center(ns[idx]); adb("shell", "input", "tap", str(x), str(y)); print("FOUND")
elif cmd == "tapnear":
    # text と同じ高さにある class の部品（スイッチなど）をタップ
    cls = sys.argv[3]
    pos = visible_center(text)
    if pos is None:
        print(f"NOTFOUND: {text}"); sys.exit(1)
    ns = [n for n in nodes() if n.get("class") == cls]
    if not ns:
        print(f"NOTFOUND: {cls}"); sys.exit(1)
    pos = center(find(text))
    best = min(ns, key=lambda n: abs(center(n)[1] - pos[1]))
    x, y = center(best); adb("shell", "input", "tap", str(x), str(y)); print("FOUND")
elif cmd in ("has", "hasp"):
    n = find(text, exact=(cmd == "has"))
    print(("FOUND: " if n is not None else "NOTFOUND: ") + text)
    sys.exit(0 if n is not None else 1)
elif cmd in ("tap", "long", "tapp"):
    pos = visible_center(text, exact=(cmd != "tapp"))
    if pos is None:
        print(f"NOTFOUND: {text}"); sys.exit(1)
    x, y = pos
    if cmd == "long":
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "1200")
    else:
        adb("shell", "input", "tap", str(x), str(y))
    print(f"FOUND: {text}")
