import { pinyin } from 'pinyin-pro';

const isUnorderedMatch = (source: string, query: string) => {
  if (!query) return true;
  const counts = new Map<string, number>();
  for (const ch of source) {
    counts.set(ch, (counts.get(ch) || 0) + 1);
  }
  for (const ch of query) {
    const left = counts.get(ch) || 0;
    if (left <= 0) return false;
    counts.set(ch, left - 1);
  }
  return true;
};

const toPinyinTokens = (value: string) => {
  try {
    const result: unknown = pinyin(value, { toneType: 'none', type: 'array' });
    if (Array.isArray(result)) return result.map(item => String(item));
    return String(result).split(/\s+/).filter(Boolean);
  } catch {
    return [];
  }
};

export const buildFuzzySearchSources = (...values: Array<string | number | null | undefined>) => {
  const text = values
    .filter(value => value !== null && value !== undefined)
    .map(value => String(value))
    .join('')
    .toLowerCase();
  const tokens = toPinyinTokens(text);
  const fullPinyin = tokens.join('').toLowerCase();
  const initials = tokens.map(token => token[0] || '').join('').toLowerCase();
  return [text, fullPinyin, initials].filter(Boolean);
};

export const fuzzyIncludes = (sources: string[], keyword: string) => {
  const query = keyword.trim().toLowerCase();
  if (!query) return true;
  return sources.some(source => source.includes(query) || isUnorderedMatch(source, query));
};

export const filterByFuzzyKeyword = <T>(
  items: T[],
  keyword: string,
  getSearchValues: (item: T) => Array<string | number | null | undefined>
) => {
  if (!keyword.trim()) return items;
  return items.filter(item => fuzzyIncludes(buildFuzzySearchSources(...getSearchValues(item)), keyword));
};
