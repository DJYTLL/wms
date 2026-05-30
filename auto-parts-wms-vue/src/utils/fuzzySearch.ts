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

const normalizeDigits = (value: string) => value.replace(/\D+/g, '');

const isNumericLikeQuery = (value: string) => {
  const trimmed = value.trim();
  return !!trimmed && /^[\d\s()+\-\\/]+$/.test(trimmed) && /\d/.test(trimmed);
};

type SearchQuery = {
  value: string;
  allowUnordered: boolean;
};

const hasCjk = (value: string) => /[\u4e00-\u9fff]/.test(value);

const toSearchQuery = (value: string): SearchQuery => ({
  value,
  allowUnordered: hasCjk(value),
});

const splitCompactAlphaNumericQuery = (value: string) => {
  if (!/^[a-z0-9]+$/.test(value) || !/[a-z]/.test(value) || !/\d/.test(value)) {
    return [value];
  }
  return value.match(/[a-z]+|\d+/g) || [value];
};

const toSearchQueries = (keyword: string) => {
  const query = keyword.trim().toLowerCase();
  if (!query) return [];
  if (isNumericLikeQuery(query)) return [toSearchQuery(query)];
  const parts = query
    .split(/\s+/)
    .flatMap(part => splitCompactAlphaNumericQuery(part))
    .filter(Boolean);
  const uniqueParts = Array.from(new Set(parts));
  return uniqueParts.map(toSearchQuery);
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

const fuzzyIncludesQuery = (sources: string[], query: SearchQuery) => {
  if (isNumericLikeQuery(query.value)) {
    const normalizedQuery = normalizeDigits(query.value);
    if (!normalizedQuery) return true;
    return sources.some((source) => normalizeDigits(source).includes(normalizedQuery));
  }
  return sources.some(
    source => source.includes(query.value) || (query.allowUnordered && isUnorderedMatch(source, query.value))
  );
};

export const fuzzyIncludes = (sources: string[], keyword: string) => {
  const queries = toSearchQueries(keyword);
  if (!queries.length) return true;
  return queries.some(query => fuzzyIncludesQuery(sources, query));
};

export const countFuzzyKeywordMatches = (sources: string[], keyword: string) => {
  const queries = toSearchQueries(keyword);
  if (!queries.length) return 0;
  return queries.reduce((count, query) => count + (fuzzyIncludesQuery(sources, query) ? 1 : 0), 0);
};

export const filterByFuzzyKeyword = <T>(
  items: T[],
  keyword: string,
  getSearchValues: (item: T) => Array<string | number | null | undefined>
) => {
  if (!keyword.trim()) return items;
  return items
    .map((item, index) => ({
      item,
      index,
      score: countFuzzyKeywordMatches(buildFuzzySearchSources(...getSearchValues(item)), keyword),
    }))
    .filter(result => result.score > 0)
    .sort((left, right) => right.score - left.score || left.index - right.index)
    .map(result => result.item);
};
