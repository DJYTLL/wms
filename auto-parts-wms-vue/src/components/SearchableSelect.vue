<template>
  <div class="searchable-select" ref="containerRef">
    <!-- 显示框 -->
    <div 
      class="select-trigger" 
      :class="{ 'is-active': isOpen, 'is-disabled': disabled, 'sm': size === 'sm' }" 
      @click="toggleDropdown"
    >
      <span class="selected-text" :class="{ 'placeholder': !selectedLabel }">
        {{ selectedLabel || placeholder }}
      </span>
      <span class="arrow-icon">
        <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
      </span>
    </div>

    <!-- 下拉菜单 -->
    <div v-show="isOpen" class="dropdown-menu">
      <div class="search-wrapper">
        <input 
          ref="searchInputRef"
          v-model="searchQuery" 
          type="text" 
          class="search-input" 
          :placeholder="$t('action.search') + '...'"
          @click.stop
        />
      </div>
      <ul class="options-list">
        <li 
          v-for="option in filteredOptions" 
          :key="option.value" 
          class="option-item"
          :class="{ 'selected': option.value === modelValue }"
          @click="selectOption(option)"
        >
          {{ option.label }}
          <span v-if="option.code" class="code-tag">{{ option.code }}</span>
        </li>
        <li v-if="filteredOptions.length === 0" class="no-data">
          {{ $t('message.noItems') }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';

// 定义 Props
interface Option {
  value: string | number;
  label: string;
  code?: string; // 可选的辅助信息，如 SKU 或 编码
}

const props = defineProps<{
  modelValue?: string | number;
  options: Option[];
  placeholder?: string;
  disabled?: boolean;
  size?: 'default' | 'sm';
}>();

const emit = defineEmits(['update:modelValue', 'change']);

const isOpen = ref(false);
const searchQuery = ref('');
const containerRef = ref<HTMLElement | null>(null);
const searchInputRef = ref<HTMLInputElement | null>(null);

// 计算当前显示的 Label
const selectedLabel = computed(() => {
  const found = props.options.find(o => o.value === props.modelValue);
  return found ? found.label : '';
});

// 过滤选项
const filteredOptions = computed(() => {
  if (!searchQuery.value) return props.options;
  const query = searchQuery.value.toLowerCase();
  return props.options.filter(o => 
    o.label.toLowerCase().includes(query) || 
    (o.code && o.code.toLowerCase().includes(query))
  );
});

const toggleDropdown = () => {
  if (props.disabled) return;
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    searchQuery.value = ''; // 重置搜索
    setTimeout(() => {
      searchInputRef.value?.focus();
    }, 100);
  }
};

const selectOption = (option: Option) => {
  emit('update:modelValue', option.value);
  emit('change', option.value);
  isOpen.value = false;
};

// 点击外部关闭
const handleClickOutside = (event: MouseEvent) => {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    isOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.searchable-select {
  position: relative;
  width: 100%;
}

.select-trigger {
  border: 1px solid #d1d1d6;
  border-radius: 6px;
  padding: 8px 10px;
  background: white;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  transition: all 0.2s;
  min-height: 38px;
  box-sizing: border-box;
}

.select-trigger.sm {
  min-height: 32px;
  padding: 4px 8px;
  font-size: 13px;
}

.select-trigger:hover {
  border-color: #0071e3;
}

.select-trigger.is-active {
  border-color: #0071e3;
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.1);
}

.select-trigger.is-disabled {
  background-color: #f5f5f7;
  cursor: not-allowed;
  opacity: 0.7;
}

.selected-text {
  color: #1d1d1f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-grow: 1;
}

.selected-text.placeholder {
  color: #86868b;
}

.arrow-icon {
  color: #86868b;
  display: flex;
  align-items: center;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  margin-top: 4px;
  z-index: 1000;
  overflow: hidden;
  animation: fadeIn 0.1s ease;
}

.search-wrapper {
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.search-input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #0071e3;
}

.options-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 200px;
  overflow-y: auto;
}

.option-item {
  padding: 8px 12px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.option-item:hover {
  background-color: #f5f5f7;
}

.option-item.selected {
  background-color: #eef7ff;
  color: #0071e3;
  font-weight: 500;
}

.code-tag {
  font-size: 12px;
  color: #86868b;
  background: #f0f0f0;
  padding: 2px 4px;
  border-radius: 4px;
}

.no-data {
  padding: 12px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
