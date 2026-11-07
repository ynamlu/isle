#!/usr/bin/env python3
"""Build the submission PDFs from Markdown with Chrome headless.

Usage:
  python3 scripts/build_docs_pdf.py

Requirements:
  - Python package `markdown` (pip install --user markdown)
  - Google Chrome at /Applications/Google Chrome.app/...
"""

from __future__ import annotations

import html
import re
import subprocess
from pathlib import Path

import markdown
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parent.parent
CHROME = Path("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")

DOCS = [
    {
        "src": "README.md",
        "pdf": "docs/pdf/README.pdf",
        "title": "心屿（Isle）",
        "subtitle": "项目 README",
        "version": "v2.3.3",
        "author": "沧州交通学院 · 计算机与信息技术学院 · 软件工程 2410 · 吴思彤",
        "header": "心屿 · README",
        "icon": "docs/心屿-新图标.png",
    },
    {
        "src": "docs/markdown/技术文档.md",
        "pdf": "docs/pdf/技术文档-心屿.pdf",
        "title": "心屿（Isle）",
        "subtitle": "技术文档",
        "version": "v2.3.3",
        "author": "沧州交通学院 · 计算机与信息技术学院 · 软件工程 2410 · 吴思彤",
        "header": "心屿 · 技术文档",
        "icon": "../心屿-新图标.png",
    },
    {
        "src": "docs/markdown/测试报告.md",
        "pdf": "docs/pdf/测试报告.pdf",
        "title": "心屿（Isle）",
        "subtitle": "测试报告",
        "version": "v2.3.3",
        "author": "沧州交通学院 · 计算机与信息技术学院 · 软件工程 2410 · 吴思彤",
        "header": "心屿 · 测试报告",
        "icon": "../心屿-新图标.png",
    },
    {
        "src": "docs/markdown/用户使用说明书.md",
        "pdf": "docs/pdf/用户使用说明书.pdf",
        "title": "心屿（Isle）",
        "subtitle": "用户使用说明书",
        "version": "v2.3.3",
        "author": "沧州交通学院 · 计算机与信息技术学院 · 软件工程 2410 · 吴思彤",
        "header": "心屿 · 用户使用说明书",
        "icon": "../心屿-新图标.png",
    },
    {
        "src": "docs/markdown/作品介绍.md",
        "pdf": "docs/pdf/作品介绍.pdf",
        "title": "心屿（Isle）",
        "subtitle": "作品介绍",
        "version": "v2.3.3",
        "author": "沧州交通学院 · 计算机与信息技术学院 · 软件工程 2410 · 吴思彤",
        "header": "心屿 · 作品介绍",
        "icon": "../心屿-新图标.png",
    },
    {
        "src": "docs/markdown/开发与AI协同全记录.md",
        "pdf": "docs/pdf/开发与AI协同全记录.pdf",
        "title": "心屿（Isle）",
        "subtitle": "开发与 AI 协同全记录",
        "version": "v2.3.3",
        "author": "沧州交通学院 · 计算机与信息技术学院 · 软件工程 2410 · 吴思彤",
        "header": "心屿 · 开发与 AI 协同全记录",
        "icon": "../心屿-新图标.png",
    },
]


def parse_frontmatter(text: str) -> tuple[dict[str, str], str]:
    if not text.startswith("---"):
        return {}, text
    parts = text.split("---", 2)
    if len(parts) < 3:
        return {}, text
    meta: dict[str, str] = {}
    for line in parts[1].splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            meta[key.strip()] = value.strip()
    return meta, parts[2].lstrip("\n")


def strip_first_h1(text: str) -> str:
    return re.sub(r"^\s*#\s+[^\n]+\n+", "", text, count=1)


def strip_before_first_h2(text: str) -> str:
    match = re.search(r"^##\s+", text, re.M)
    return text[match.start():] if match else text


def build_toc(md: markdown.Markdown) -> str:
    out = []
    for token in md.toc_tokens:
        level = token["level"]
        if level == 1:
            out.append(
                f'<li class="toc-h1"><a href="#{token["id"]}">{html.escape(token["name"])}</a></li>'
            )
            for child in token.get("children", []):
                if child["level"] == 2:
                    out.append(
                        f'<li class="toc-h2"><a href="#{child["id"]}">{html.escape(child["name"])}</a></li>'
                    )
        elif level == 2:
            out.append(
                f'<li class="toc-h2"><a href="#{token["id"]}">{html.escape(token["name"])}</a></li>'
            )
    return '<nav class="toc"><h2 class="toc-title">目录</h2><ul>' + "".join(out) + "</ul></nav>"


def build_html(doc: dict[str, str]) -> str:
    src = ROOT / doc["src"]
    meta, body = parse_frontmatter(src.read_text(encoding="utf-8"))
    title = meta.get("title", doc["title"])
    subtitle = meta.get("subtitle", doc["subtitle"])
    version = meta.get("version", doc["version"])
    date = meta.get("date", "2026-11-06")
    author = meta.get("author", doc["author"])
    body = strip_before_first_h2(strip_first_h1(body))

    md = markdown.Markdown(
        extensions=["extra", "tables", "fenced_code", "sane_lists", "toc"],
        extension_configs={"toc": {"permalink": False}},
    )
    content = md.convert(body)
    toc = build_toc(md)

    icon_rel = doc["icon"]
    html_doc = f"""<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>{html.escape(title)} · {html.escape(subtitle)}</title>
<style>
  * {{ box-sizing: border-box; }}
  html, body {{ margin: 0; padding: 0; }}
  body {{
    font-family: -apple-system, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
    color: #23242a; font-size: 10.5pt; line-height: 1.72;
    -webkit-print-color-adjust: exact; print-color-adjust: exact;
  }}
  @page {{
    size: A4;
    margin: 17mm 15mm 17mm 15mm;
    @top-right {{ content: "{doc['header']}"; font-size: 8pt; color: #9aa0aa; }}
    @bottom-center {{ content: counter(page); font-size: 8.5pt; color: #8b919b; }}
  }}
  .cover {{
    height: 255mm; margin: 0; padding: 26mm 22mm;
    background: linear-gradient(145deg, #12112e 0%, #1d2f63 45%, #2a718f 75%, #48bfa8 100%);
    color: white; border-radius: 3mm; page-break-after: always;
    position: relative; overflow: hidden;
  }}
  .cover::after {{
    content: ""; position: absolute; width: 150mm; height: 150mm; right: -50mm; top: -40mm;
    border: .4mm solid rgba(255,255,255,.18); border-radius: 50%;
  }}
  .cover .badge {{
    display: inline-block; padding: 2.2mm 4.5mm; border: .3mm solid rgba(255,255,255,.3);
    border-radius: 99mm; background: rgba(255,255,255,.08);
    font-size: 9pt; letter-spacing: .18em; color: rgba(255,255,255,.88);
  }}
  .cover h1 {{
    margin: 22mm 0 5mm; font-size: 38pt; line-height: 1.05; letter-spacing: .12em;
    font-weight: 800; color: white; border: 0; padding: 0;
  }}
  .cover .subtitle {{ font-size: 17pt; letter-spacing: .08em; color: rgba(255,255,255,.92); }}
  .cover .tagline {{ margin-top: 8mm; font-size: 11pt; color: rgba(255,255,255,.7); line-height: 1.9; }}
  .cover .meta {{ position: absolute; left: 22mm; bottom: 26mm; font-size: 9pt; color: rgba(255,255,255,.62); line-height: 1.8; }}
  .cover img {{ width: 34mm; height: 34mm; border-radius: 8mm; margin-top: 14mm; }}
  .cover .accent {{ width: 28mm; height: 1.2mm; background: linear-gradient(90deg,#8FE3C0,#7ab8ff); border-radius: 99mm; margin: 9mm 0; }}
  .toc {{ page-break-after: always; padding-top: 8mm; }}
  .toc-title {{ border: 0; margin: 0 0 10mm; font-size: 20pt; color: #1d2f63; }}
  .toc ul {{ list-style: none; padding-left: 0; margin: 0; }}
  .toc li {{ margin: 2.2mm 0; font-size: 10.5pt; }}
  .toc li a {{ color: #23242a; text-decoration: none; }}
  .toc-h1 {{ margin-top: 4.5mm; font-weight: 700; color: #1d2f63; }}
  .toc-h2 {{ padding-left: 7mm; font-size: 9.8pt !important; color: #4d5666 !important; }}
  .content h1 {{
    page-break-before: always; margin: 8mm 0 5mm; padding-bottom: 3mm;
    font-size: 21pt; color: #1d2f63; border-bottom: .6mm solid #dfe6f2;
  }}
  h2 {{
    margin: 9mm 0 5mm; padding-top: 2mm;
    font-size: 18pt; line-height: 1.25; color: #1d2f63;
    border-bottom: .5mm solid #dfe6f2; padding-bottom: 2.5mm;
    page-break-after: avoid;
  }}
  .content > h2:first-child {{ page-break-before: always; margin-top: 0; }}
  h3 {{ margin: 7mm 0 2.5mm; font-size: 13pt; color: #2f6fb2; page-break-after: avoid; }}
  h4 {{ margin: 5mm 0 2mm; font-size: 11pt; color: #333; page-break-after: avoid; }}
  p {{ margin: 0 0 2.8mm; }}
  ul, ol {{ margin: 0 0 3mm 5mm; padding-left: 5mm; }}
  li {{ margin: 1.2mm 0; }}
  strong {{ color: #1d2f63; }}
  a {{ color: #2f6fb2; text-decoration: none; }}
  table {{
    width: 100%; border-collapse: collapse; margin: 3mm 0 5mm;
    font-size: 9pt; page-break-inside: auto;
  }}
  table th:first-child, table td:first-child {{ min-width: 14mm; }}
  }}
  tr {{ page-break-inside: avoid; page-break-after: auto; }}
  th, td {{ border: .25mm solid #d9e0ea; padding: 2mm 2.2mm; vertical-align: top; }}
  th {{ background: #eef3fa; color: #1d2f63; font-weight: 600; }}
  tbody tr:nth-child(even) td {{ background: #f8fafc; }}
  blockquote {{
    margin: 3mm 0; padding: 3mm 4mm; border-left: 1mm solid #7ab8ff;
    background: #f4f8ff; color: #4d5666; border-radius: 0 2mm 2mm 0;
  }}
  pre {{
    margin: 3mm 0; padding: 3.5mm; background: #f3f5f8; border: .25mm solid #e1e6ee;
    border-radius: 2mm; font-size: 8.5pt; line-height: 1.55; white-space: pre-wrap;
    page-break-inside: avoid;
  }}
  code {{ font-family: "SFMono-Regular", Menlo, Consolas, monospace; font-size: .92em; }}
  p code, li code {{ background: #eef1f5; padding: .4mm 1.2mm; border-radius: 1mm; }}
  img {{ max-width: 100%; max-height: 210mm; display: block; margin: 3mm auto; page-break-inside: avoid; border-radius: 2mm; }}
  hr {{ border: 0; border-top: .25mm solid #dfe6f2; margin: 6mm 0; }}
  .doc-end {{ display: none; }}
</style>
</head>
<body>
<section class="cover">
  <div class="badge">AI 创新应用挑战赛 · 参赛材料</div>
  <h1>{html.escape(title)}</h1>
  <div class="subtitle">{html.escape(subtitle)}</div>
  <div class="accent"></div>
  <div class="tagline">一座会记住你的心灵小岛<br/>面向大学生的 AI 情绪支持应用</div>
  <img src="{html.escape(icon_rel)}" alt="心屿图标"/>
  <div class="meta">{html.escape(author)}<br/>{html.escape(version)} · {html.escape(date)}</div>
</section>
{toc}
<div class="content">
{content}
</div>
</body>
</html>"""
    return html_doc


def main() -> None:
    if not CHROME.exists():
        raise SystemExit(f"Chrome not found: {CHROME}")

    for doc in DOCS:
        src = ROOT / doc["src"]
        pdf = ROOT / doc["pdf"]
        html_path = src.with_suffix(".pdf.html")
        html_path.write_text(build_html(doc), encoding="utf-8")
        pdf.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            [
                str(CHROME),
                "--headless",
                "--disable-gpu",
                "--hide-scrollbars",
                "--no-pdf-header-footer",
                "--virtual-time-budget=3000",
                f"--print-to-pdf={pdf}",
                f"file://{html_path}",
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        pages = len(PdfReader(str(pdf)).pages)
        print(f"{pdf.relative_to(ROOT)}: {pages} pages, {pdf.stat().st_size // 1024} KB")
        html_path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
