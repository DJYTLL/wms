import json
from pathlib import Path

import pandas as pd


INPUT_PATH = Path(r"C:\Users\Administrator\AppData\Local\Temp\codex-xls-work\inventory_input.xlsx")
OUTPUT_PATH = Path(r"D:\project\outputs\inventory_dedup_20260602\inventory_dedup_result.xlsx")

NAME_COL = "产品名称"
TIME_COL = "最后一次入库时间"
CODE_COL = "编码"
WAREHOUSE_COL = "仓库"


def main() -> None:
    src_df = pd.read_excel(INPUT_PATH)
    out_df = pd.read_excel(OUTPUT_PATH)
    src_df["_parsed_time"] = pd.to_datetime(src_df[TIME_COL], errors="coerce")

    samples = []
    checked_total = 0
    matched_total = 0

    duplicate_names = src_df[src_df.duplicated(subset=[NAME_COL], keep=False)][NAME_COL].dropna().unique()
    for name in duplicate_names:
        group = src_df[src_df[NAME_COL] == name].copy().sort_values(
            by=["_parsed_time"],
            ascending=[False],
            na_position="last",
        )
        kept = out_df[out_df[NAME_COL] == name]
        if kept.empty:
            continue
        expected = group.iloc[0]
        actual = kept.iloc[0]
        is_match = (
            str(expected[CODE_COL]) == str(actual[CODE_COL])
            and str(expected[WAREHOUSE_COL]) == str(actual[WAREHOUSE_COL])
            and str(expected[TIME_COL]) == str(actual[TIME_COL])
        )
        checked_total += 1
        if is_match:
            matched_total += 1
        if len(samples) < 5:
            samples.append(
                {
                    "name": name,
                    "candidates": group[[CODE_COL, WAREHOUSE_COL, TIME_COL]].head(5).to_dict(orient="records"),
                    "kept": actual[[CODE_COL, WAREHOUSE_COL, TIME_COL]].to_dict(),
                    "match_expected_latest": is_match,
                }
            )

    print(
        json.dumps(
            {
                "input_rows": int(len(src_df)),
                "output_rows": int(len(out_df)),
                "output_unique_names": int(out_df[NAME_COL].nunique()),
                "checked_duplicate_groups": checked_total,
                "matched_duplicate_groups": matched_total,
                "samples": samples,
            },
            ensure_ascii=False,
            indent=2,
            default=str,
        )
    )


if __name__ == "__main__":
    main()
