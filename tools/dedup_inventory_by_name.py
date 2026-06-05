import json
from pathlib import Path

import pandas as pd


INPUT_PATH = Path(r"C:\Users\Administrator\AppData\Local\Temp\codex-xls-work\inventory_input.xlsx")
OUTPUT_DIR = Path(r"D:\project\outputs\inventory_dedup_20260602")
OUTPUT_PATH = OUTPUT_DIR / "库存明细浏览表_按产品名称去重.xlsx"
SUMMARY_PATH = OUTPUT_DIR / "summary.json"

NAME_COL = "产品名称"
TIME_COL = "最后一次入库时间"


def main() -> None:
    df = pd.read_excel(INPUT_PATH, sheet_name="库存明细浏览表")
    df["_original_row"] = range(len(df))
    df["_parsed_time"] = pd.to_datetime(df[TIME_COL], errors="coerce")

    deduped = (
        df.sort_values(
            by=[NAME_COL, "_parsed_time", "_original_row"],
            ascending=[True, False, True],
            na_position="last",
        )
        .drop_duplicates(subset=[NAME_COL], keep="first")
        .sort_values("_original_row")
        .drop(columns=["_original_row", "_parsed_time"])
    )

    duplicate_rows = int(df.duplicated(subset=[NAME_COL], keep=False).sum())
    duplicate_name_count = int(df.loc[df.duplicated(subset=[NAME_COL], keep=False), NAME_COL].nunique())

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    with pd.ExcelWriter(OUTPUT_PATH, engine="openpyxl") as writer:
        deduped.to_excel(writer, sheet_name="库存明细浏览表", index=False)

    summary = {
        "input_rows": int(len(df)),
        "output_rows": int(len(deduped)),
        "removed_rows": int(len(df) - len(deduped)),
        "duplicate_rows": duplicate_rows,
        "duplicate_name_count": duplicate_name_count,
        "name_column": NAME_COL,
        "time_column": TIME_COL,
    }
    SUMMARY_PATH.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
