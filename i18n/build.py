"""i18n/ の訳文から app/src/main/res/values*/strings.xml を作る。

使い方: python i18n/build.py
- base.py の S が「キー: (日本語, 英語)」。英語がほかの言語の訳の元で、訳が無い言語では英語が出る
- tr_xx.py の T が「キー: 訳文」。足りないキーは英語のまま（ビルドは通る）
- 書式の %1$s %2$d などはどの言語でも同じ数・同じ番号で残すこと
"""
import importlib.util
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent
RES = ROOT.parent / "app" / "src" / "main" / "res"

# ファイル名 → Android のフォルダ名
LANGS = {
    "zh_CN": "values-zh-rCN", "zh_TW": "values-zh-rTW", "ko": "values-ko",
    "es": "values-es", "fr": "values-fr", "de": "values-de", "pt": "values-pt",
    "it": "values-it", "ru": "values-ru", "in": "values-in", "th": "values-th",
    "vi": "values-vi", "hi": "values-hi", "ar": "values-ar", "ur": "values-ur", "tr": "values-tr",
    "bn": "values-bn", "mr": "values-mr", "te": "values-te", "ta": "values-ta",
}


LOCALE_TAG = {"zh_CN": "zh-CN", "zh_TW": "zh-TW", "ko": "ko", "es": "es", "fr": "fr", "de": "de", "pt": "pt",
              "it": "it", "ru": "ru", "in": "id", "th": "th", "vi": "vi", "hi": "hi", "ar": "ar", "ur": "ur",
              "tr": "tr", "bn": "bn", "mr": "mr", "te": "te", "ta": "ta"}


def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / f"{name}.py")
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


def esc(s):
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = s.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if s[:1] in ("@", "?"):
        s = "\\" + s
    return s


def fmt_args(s):
    return sorted(re.findall(r"%\d\$,?[sdf]", s))


def write(folder, entries):
    d = RES / folder
    d.mkdir(parents=True, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- i18n/build.py で自動生成。直接書き換えない -->", "<resources>"]
    for k, v in entries.items():
        lines.append(f'    <string name="{k}">{esc(v)}</string>')
    lines.append("</resources>")
    (d / "strings.xml").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    base = dict(load("base").S)
    # チュートリアルの文字は tutorial.py に言語ごとの並びで入っている
    tut = load("tutorial")
    for i, k in enumerate(tut.KEYS):
        base[k] = (tut.L["ja"][i], tut.L["en"][i])
    ok = True
    write("values", {"app_name": "Loosen", **{k: v[1] for k, v in base.items()}})
    write("values-ja", {k: v[0] for k, v in base.items()})
    for k, (ja, en) in base.items():
        if fmt_args(ja) != fmt_args(en):
            print(f"[ja] 書式が英語と違う: {k}"); ok = False
    for code, folder in LANGS.items():
        if not (ROOT / f"tr_{code}.py").exists():
            continue
        t = dict(load(f"tr_{code}").T)
        if code in tut.L:
            assert len(tut.L[code]) == len(tut.KEYS), f"tutorial.py の {code} の数が合わない"
            t.update(dict(zip(tut.KEYS, tut.L[code])))
        out = {}
        for k, (_, en) in base.items():
            v = t.get(k)
            if v is None:
                continue
            if fmt_args(v) != fmt_args(en):
                print(f"[{code}] 書式が英語と違うので英語にする: {k}"); ok = False
                continue
            out[k] = v
        extra = set(t) - set(base)
        if extra:
            print(f"[{code}] 使われていないキー: {sorted(extra)[:5]}")
        missing = len(base) - len(out)
        print(f"{code}: {len(out)}/{len(base)}" + (f"（{missing}件は英語）" if missing else ""))
        write(folder, out)
    # Android 13 以降の「アプリの言語」一覧（スマホの設定に合わせるのが基本）
    tags = ["en", "ja"] + [LOCALE_TAG[c] for c in LANGS if (ROOT / f"tr_{c}.py").exists()]
    x = RES / "xml"; x.mkdir(exist_ok=True)
    xml = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- i18n/build.py で自動生成 -->",
           '<locale-config xmlns:android="http://schemas.android.com/apk/res/android">']
    xml += [f'    <locale android:name="{t}" />' for t in tags]
    xml.append("</locale-config>")
    (x / "locales_config.xml").write_text("\n".join(xml) + "\n", encoding="utf-8")
    print(f"キー数 {len(base)}")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
