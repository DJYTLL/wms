<template>
  <el-select
    v-model="innerValue"
    :placeholder="placeholder"
    filterable
    :filter-method="filterProduct"
    clearable
    v-bind="$attrs"
    @change="emitChange"
  >
    <el-option v-for="item in filteredOptions" :key="item.id" :label="item.name" :value="item.id" />
  </el-select>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { pinyin } from 'pinyin-pro';

interface OptionItem {
  id: number;
  name: string;
  code?: string;
}

const props = withDefaults(
  defineProps<{
    modelValue: number | null;
    options: OptionItem[];
    placeholder?: string;
  }>(),
  {
    placeholder: ''
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | null): void;
  (e: 'change', value: number | null): void;
}>();

const keyword = ref('');

const innerValue = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
});

const filterProduct = (query: string) => {
  keyword.value = query || '';
};

const isSubsequence = (source: string, query: string) => {
  if (!query) return true;
  let i = 0;
  for (const ch of source) {
    if (ch === query[i]) i += 1;
    if (i >= query.length) return true;
  }
  return false;
};

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
    const result: any = pinyin(value, { toneType: 'none', type: 'array' });
    if (Array.isArray(result)) return result;
    return String(result).split(/\s+/).filter(Boolean);
  } catch {
    return [];
  }
};

const buildSearchSources = (name: string, code?: string) => {
  const base = `${name || ''}${code || ''}`.toLowerCase();
  const tokens = toPinyinTokens(name || '');
  const fullPinyin = tokens.join('').toLowerCase();
  const initials = tokens.map(token => token[0] || '').join('').toLowerCase();
  return [base, fullPinyin, initials].filter(Boolean);
};

const calcMatchScore = (source: string, query: string) => {
  if (!query) return 0;
  if (source === query) return 1000;
  if (source.startsWith(query)) return 900;
  if (source.includes(query)) return 700;
  if (isSubsequence(source, query)) return 500;
  if (isUnorderedMatch(source, query)) return 300;
  return 0;
};

const filteredOptions = computed(() => {
  if (!keyword.value) return props.options;
  const key = keyword.value.toLowerCase();
  return props.options
    .map(item => {
      const sources = buildSearchSources(item.name || '', item.code);
      const score = Math.max(...sources.map(source => calcMatchScore(source, key)));
      return { item, score };
    })
    .filter(entry => entry.score > 0)
    .sort((a, b) => b.score - a.score)
    .map(entry => entry.item);
});

const emitChange = (value: number | null) => {
  emit('change', value);
};
</script>
