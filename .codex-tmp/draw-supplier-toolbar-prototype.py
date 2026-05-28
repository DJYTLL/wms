from PIL import Image, ImageDraw, ImageFont

W, H = 1660, 820
OUT = r"D:\project\.codex-tmp\supplier-toolbar-layout-prototype.png"
FONT = r"C:\Windows\Fonts\simhei.ttf"

img = Image.new("RGB", (W, H), "#f6f8fb")
draw = ImageDraw.Draw(img)

font_title = ImageFont.truetype(FONT, 24)
font = ImageFont.truetype(FONT, 14)
font_small = ImageFont.truetype(FONT, 13)
font_bold = ImageFont.truetype(FONT, 14)


def rect(x1, y1, x2, y2, fill, outline=None, radius=0, width=1):
    if radius:
        draw.rounded_rectangle((x1, y1, x2, y2), radius=radius, fill=fill, outline=outline, width=width)
    else:
        draw.rectangle((x1, y1, x2, y2), fill=fill, outline=outline, width=width)


def text(x, y, value, fill="#1f2937", fnt=font):
    draw.text((x, y), value, font=fnt, fill=fill)


def field(x, y, w, label, select=False):
    rect(x, y, x + w, y + 32, "#ffffff", "#d8e0ec", radius=4)
    text(x + 12, y + 8, label, "#8a97aa", font)
    if select:
        draw.line((x + w - 20, y + 13, x + w - 15, y + 18, x + w - 10, y + 13), fill="#a6b1c2", width=1)


def button(x, y, w, label, fill="#ffffff", border="#d8e0ec", color="#3f4a5f"):
    rect(x, y, x + w, y + 32, fill, border, radius=5)
    bbox = draw.textbbox((0, 0), label, font=font_bold)
    text(x + (w - (bbox[2] - bbox[0])) / 2, y + 8, label, color, font_bold)


# Page title
text(30, 26, "供应商管理", "#1f2937", font_title)

# Toolbar card
card_x, card_y, card_w, card_h = 30, 68, 1600, 66
rect(card_x, card_y, card_x + card_w, card_y + card_h, "#ffffff", "#e4e9f2", radius=8)

# Single-line-first toolbar: controls wrap only when width is insufficient.
x0, y0 = card_x + 18, card_y + 16
gap = 12
cursor = x0
for label in ["名称", "编码", "简称", "联系人", "电话"]:
    field(cursor, y0, 140, label)
    cursor += 140 + gap
for label in ["供应商类型", "往来类别", "状态：全部"]:
    field(cursor, y0, 150, label, True)
    cursor += 150 + gap

search_x = card_x + card_w - 18 - 292
rect(search_x, y0, search_x + 32, y0 + 32, "#ffffff", "#d8e0ec", radius=5)
cx, cy = search_x + 16, y0 + 16
draw.arc((cx - 8, cy - 8, cx + 8, cy + 8), 35, 310, fill="#657186", width=2)
draw.line((cx + 7, cy - 8, cx + 11, cy - 8), fill="#657186", width=2)
draw.line((cx + 11, cy - 8, cx + 11, cy - 4), fill="#657186", width=2)
button(search_x + 42, y0, 62, "搜索", "#409eff", "#409eff", "#ffffff")
button(search_x + 124, y0, 88, "重置默认")
button(search_x + 232, y0, 60, "新增", "#ecf5ff", "#cfe5ff", "#1c6fc9")

# Hint line showing toolbar content boundary
draw.line((card_x + 18, card_y + card_h - 1, card_x + card_w - 18, card_y + card_h - 1), fill="#eef2f7")

# Table card
table_x, table_y, table_w = 30, 148, 1600
rect(table_x, table_y, table_x + table_w, 752, "#ffffff", "#e4e9f2", radius=8)
text(table_x + table_w - 70, table_y + 12, "列设置", "#667085", font_small)
draw.line((table_x, table_y + 38, table_x + table_w, table_y + 38), fill="#e4e9f2")

headers = [
    ("序号", 70), ("编码", 120), ("名称", 160), ("供应商类型", 140), ("区域", 120),
    ("联系人", 120), ("电话", 130), ("手机", 130), ("微信客服", 140), ("采购员", 120),
    ("往来类别", 140), ("往来主体", 140), ("操作", 160)
]
x = table_x
y = table_y + 38
rect(table_x, y, table_x + table_w, y + 44, "#fbfcfe")
for label, col_w in headers:
    draw.line((x, y, x, 752), fill="#edf1f6")
    text(x + 12, y + 14, label, "#4b5565", font_bold)
    x += col_w
draw.line((table_x + table_w, y, table_x + table_w, 752), fill="#edf1f6")

for r in range(6):
    row_y = y + 44 + r * 50
    fill = "#ffffff" if r % 2 == 0 else "#fafafa"
    rect(table_x, row_y, table_x + table_w, row_y + 50, fill)
    draw.line((table_x, row_y + 50, table_x + table_w, row_y + 50), fill="#edf1f6")
    values = [
        str(r + 1), f"SUP-{r + 1:03d}", f"供应商{r + 1}", "-", "", f"联系人{r + 1}",
        f"021-7000000{r + 1}", f"1370000000{r + 1}", "", "", "供应商", "-" if r > 1 else "往来主体", "编辑  删除"
    ]
    x = table_x
    for (label, col_w), value in zip(headers, values):
        color = "#1d7afc" if label == "操作" else "#1f2937"
        if label == "操作":
            text(x + 12, row_y + 16, "编辑", "#1d7afc", font_small)
            text(x + 56, row_y + 16, "删除", "#ff4d4f", font_small)
        else:
            text(x + 12, row_y + 16, value, color, font)
        x += col_w

# Pagination mock
text(1160, 724, "共 9 条", "#4b5565", font_small)
button(1215, 710, 128, "10条/页")
button(1400, 710, 36, "1", "#409eff", "#409eff", "#ffffff")
text(1495, 724, "前往", "#4b5565", font_small)
button(1530, 710, 56, "1")
text(1595, 724, "页", "#4b5565", font_small)

img.save(OUT)
print(OUT)
