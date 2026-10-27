<script setup lang="ts">
import { ref, computed } from 'vue';
import type { Product } from '../types/product';
import ProductCard from './ProductCard.vue';

const props = defineProps<{
  products: Product[];
}>();

// Filter State (Reactive Composition API)
const searchQuery = ref<string>('');
const selectedCategory = ref<string>('ALL');
const onlyInStock = ref<boolean>(false);

const categories = ['ALL', 'Keyboards', 'Workstations', 'Haptics', 'Audio'];

// COMPUTED: Filtered product list based on user selections
const filteredProducts = computed<Product[]>(() => {
  return props.products.filter(product => {
    // 1. Text search match
    const matchesSearch = 
      product.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      product.tagline.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      product.description.toLowerCase().includes(searchQuery.value.toLowerCase());

    // 2. Category match
    const matchesCategory = 
      selectedCategory.value === 'ALL' || 
      product.category === selectedCategory.value;

    // 3. In-stock filter
    const matchesStock = !onlyInStock.value || product.inStock;

    return matchesSearch && matchesCategory && matchesStock;
  });
});

function resetFilters() {
  searchQuery.value = '';
  selectedCategory.value = 'ALL';
  onlyInStock.value = false;
}
</script>

<template>
  <section class="w-full">
    
    <!-- Filter Bar & Search Controls -->
    <div class="mb-10 p-4 sm:p-5 rounded-3xl bg-white/[0.03] border border-white/10 backdrop-blur-xl">
      <div class="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-4">
        
        <!-- Category Filter Pills (v-for and @click) -->
        <div class="flex items-center gap-1.5 overflow-x-auto pb-2 lg:pb-0 scrollbar-none">
          <button
            v-for="cat in categories"
            :key="cat"
            @click="selectedCategory = cat"
            class="px-4 py-2 rounded-full text-xs font-semibold whitespace-nowrap transition-fluid"
            :class="selectedCategory === cat 
              ? 'bg-emerald-500 text-black shadow-lg shadow-emerald-500/25' 
              : 'bg-white/5 text-zinc-400 hover:text-white hover:bg-white/10'"
          >
            {{ cat === 'ALL' ? 'Tất cả' : cat }}
          </button>
        </div>

        <!-- Search Input & In-Stock Switch -->
        <div class="flex flex-wrap sm:flex-nowrap items-center gap-3">
          <!-- Search Field -->
          <div class="relative flex-1 sm:w-64">
            <svg class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
            </svg>
            <input 
              v-model="searchQuery"
              type="text"
              placeholder="Tìm kiếm thiết bị..."
              class="w-full pl-10 pr-4 py-2 rounded-full bg-black/40 border border-white/10 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-emerald-500/60 focus:ring-1 focus:ring-emerald-500/30 transition-fluid"
            />
          </div>

          <!-- In-Stock Toggle -->
          <label class="flex items-center gap-2 px-3 py-2 rounded-full bg-white/5 border border-white/10 text-xs text-zinc-300 cursor-pointer select-none hover:bg-white/10 transition-fluid">
            <input 
              v-model="onlyInStock" 
              type="checkbox" 
              class="w-3.5 h-3.5 rounded accent-emerald-500"
            />
            <span class="text-[11px] font-medium whitespace-nowrap">Chỉ còn hàng</span>
          </label>
        </div>

      </div>
    </div>

    <!-- Asymmetrical Bento Grid of Products (v-for) -->
    <div 
      v-if="filteredProducts.length > 0"
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 sm:gap-8 items-stretch"
    >
      <div 
        v-for="(product, idx) in filteredProducts" 
        :key="product.id"
        class="transition-all duration-700 ease-[cubic-bezier(0.32,0.72,0,1)]"
        :class="idx === 0 || idx === 3 ? 'lg:col-span-7' : 'lg:col-span-5'"
      >
        <ProductCard :product="product" />
      </div>
    </div>

    <!-- Empty State -->
    <div 
      v-else 
      class="py-20 text-center bezel-outer max-w-md mx-auto"
    >
      <div class="bezel-inner p-8 flex flex-col items-center">
        <div class="w-12 h-12 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-zinc-500 mb-4">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m5.231 13.481L15 17.25m-4.5-15H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
          </svg>
        </div>
        <h3 class="text-base font-bold text-white mb-1">Không tìm thấy sản phẩm</h3>
        <p class="text-xs text-zinc-400 mb-6">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm của bạn.</p>
        <button 
          @click="resetFilters"
          class="px-5 py-2 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold hover:bg-emerald-500/20 transition-fluid"
        >
          Xóa tất cả bộ lọc
        </button>
      </div>
    </div>

  </section>
</template>
