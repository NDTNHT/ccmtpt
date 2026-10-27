<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { MOCK_PRODUCTS } from '../data/mockProducts';
import type { Product, ProductVariant } from '../types/product';
import { useCartStore } from '../stores/cart';

const route = useRoute();
const cartStore = useCartStore();

const productId = route.params.id as string;
const product = computed<Product | undefined>(() => {
  return MOCK_PRODUCTS.find(p => p.id === productId);
});

// Selected variant reactive state
const selectedVariant = ref<ProductVariant>(
  product.value?.variants[0] || {
    id: 'default',
    name: 'Default',
    colorName: 'Standard',
    colorHex: '#000000',
    sku: 'DEF-01',
    stockQuantity: 10,
    priceOffset: 0,
    imageUrl: ''
  }
);

// Active preview image
const activeImage = ref<string>(
  selectedVariant.value.imageUrl || product.value?.images[0] || ''
);

// Quantity selector
const quantity = ref<number>(1);

function selectVariant(v: ProductVariant) {
  selectedVariant.value = v;
  activeImage.value = v.imageUrl;
  // Guard quantity if higher than stock
  if (quantity.value > v.stockQuantity) {
    quantity.value = Math.max(1, v.stockQuantity);
  }
}

// COMPUTED: current calculated price
const currentPrice = computed<number>(() => {
  if (!product.value) return 0;
  const base = product.value.salePrice ?? product.value.basePrice;
  return base + selectedVariant.value.priceOffset;
});

const isInStock = computed<boolean>(() => {
  return selectedVariant.value.stockQuantity > 0;
});

function handleAddToCart() {
  if (!product.value || !isInStock.value) return;
  cartStore.addItem(product.value, selectedVariant.value, quantity.value);
}
</script>

<template>
  <div class="pt-28 pb-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
    
    <!-- Back to Catalog Navigation -->
    <div class="mb-8">
      <RouterLink 
        to="/"
        class="inline-flex items-center gap-2 text-xs font-mono text-zinc-400 hover:text-emerald-400 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
        </svg>
        <span>Quay lại Catalog (Vue Router Dynamic Route)</span>
      </RouterLink>
    </div>

    <!-- Product Details Content -->
    <div v-if="product" class="grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
      
      <!-- Left Column: Gallery (Double-Bezel) -->
      <div class="lg:col-span-7">
        <div class="bezel-outer p-2 bg-white/[0.02] border border-white/10">
          <div class="bezel-inner p-6 bg-[#0c0c0e]">
            
            <!-- Main Display Image -->
            <div class="w-full aspect-[4/3] rounded-2xl overflow-hidden bg-black/60 border border-white/5 mb-4">
              <img 
                :src="activeImage" 
                :alt="product.name"
                class="w-full h-full object-cover object-center transition-all duration-500"
              />
            </div>

            <!-- Thumbnails Selector (@mouseover & @click) -->
            <div class="grid grid-cols-4 gap-3">
              <button 
                v-for="(img, idx) in product.images"
                :key="idx"
                @mouseover="activeImage = img"
                @click="activeImage = img"
                class="aspect-square rounded-xl overflow-hidden border transition-fluid"
                :class="activeImage === img ? 'border-emerald-500 scale-105' : 'border-white/10 opacity-60 hover:opacity-100'"
              >
                <img :src="img" class="w-full h-full object-cover" />
              </button>
            </div>

          </div>
        </div>

        <!-- Technical Specification Matrix Table -->
        <div class="mt-8 bezel-outer p-2">
          <div class="bezel-inner p-6 bg-[#0c0c0e]">
            <h3 class="text-sm font-bold text-white uppercase tracking-wider font-mono mb-4 flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
              Thông Số Kỹ Thuật (Hardware Specifications)
            </h3>

            <div class="divide-y divide-white/5 text-xs font-mono">
              <div 
                v-for="(val, key) in product.specs" 
                :key="key"
                class="py-2.5 flex items-center justify-between"
              >
                <span class="text-zinc-400">{{ key }}</span>
                <span class="text-white font-semibold">{{ val }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: Product Meta, Variant Selection & Purchase Controls -->
      <div class="lg:col-span-5 flex flex-col space-y-6">
        
        <!-- Category Pill & Title -->
        <div>
          <span class="rounded-full px-3 py-1 text-[10px] uppercase tracking-[0.2em] font-medium bg-white/5 border border-white/10 text-zinc-400 font-mono">
            {{ product.category }}
          </span>
          <h1 class="text-2xl sm:text-3xl font-extrabold text-white mt-3 font-display tracking-tight leading-tight">
            {{ product.name }}
          </h1>
          <p class="text-sm text-zinc-400 mt-2 leading-relaxed">
            {{ product.description }}
          </p>
        </div>

        <!-- Rating & Reviews -->
        <div class="flex items-center gap-3 text-xs">
          <div class="flex items-center text-amber-400">
            ★ ★ ★ ★ ★
          </div>
          <span class="text-white font-mono font-bold">{{ product.rating }}</span>
          <span class="text-zinc-500 font-mono">({{ product.reviewCount }} đánh giá từ chuyên gia)</span>
        </div>

        <!-- Price Display -->
        <div class="p-5 rounded-2xl bg-white/[0.02] border border-white/10 flex items-baseline justify-between">
          <div class="flex items-baseline gap-3">
            <span class="text-3xl font-extrabold text-white font-mono tracking-tight">
              ${{ currentPrice }}
            </span>
            <span v-if="product.salePrice" class="text-sm text-zinc-500 line-through font-mono">
              ${{ product.basePrice + selectedVariant.priceOffset }}
            </span>
          </div>
          <span class="text-xs text-emerald-400 font-mono">
            {{ isInStock ? `Còn hàng (${selectedVariant.stockQuantity})` : 'Hết hàng' }}
          </span>
        </div>

        <!-- Variant Selector (@click) -->
        <div>
          <label class="block text-xs font-mono text-zinc-400 mb-2">
            Chọn biến thể màu sắc: <strong class="text-white">{{ selectedVariant.colorName }}</strong>
          </label>
          <div class="flex items-center gap-3">
            <button 
              v-for="v in product.variants"
              :key="v.id"
              @click="selectVariant(v)"
              class="flex items-center gap-2.5 px-3 py-2 rounded-xl border transition-fluid"
              :class="selectedVariant.id === v.id ? 'bg-emerald-500/10 border-emerald-500 text-white' : 'bg-white/5 border-white/10 text-zinc-400 hover:text-white'"
            >
              <span 
                class="w-3.5 h-3.5 rounded-full border border-white/20"
                :style="{ backgroundColor: v.colorHex }"
              ></span>
              <span class="text-xs font-medium">{{ v.name }}</span>
            </button>
          </div>
        </div>

        <!-- Quantity Picker & Add to Cart -->
        <div class="space-y-4 pt-4 border-t border-white/10">
          <div class="flex items-center gap-4">
            <!-- Counter -->
            <div class="flex items-center bg-white/5 border border-white/10 rounded-full px-3 py-2">
              <button 
                @click="quantity = Math.max(1, quantity - 1)"
                class="w-6 h-6 rounded-full flex items-center justify-center text-zinc-400 hover:text-white transition-colors"
              >
                -
              </button>
              <span class="px-3 text-sm font-mono font-bold text-white">
                {{ quantity }}
              </span>
              <button 
                @click="quantity = Math.min(selectedVariant.stockQuantity, quantity + 1)"
                :disabled="quantity >= selectedVariant.stockQuantity"
                class="w-6 h-6 rounded-full flex items-center justify-center text-zinc-400 hover:text-white disabled:opacity-30 transition-colors"
              >
                +
              </button>
            </div>

            <!-- Button-in-Button CTA -->
            <button 
              @click="handleAddToCart"
              :disabled="!isInStock"
              class="flex-1 relative inline-flex items-center justify-between rounded-full pl-6 pr-2 py-3 text-xs font-bold transition-fluid group"
              :class="isInStock ? 'bg-emerald-500 text-black hover:bg-emerald-400 shadow-lg shadow-emerald-500/25 active:scale-[0.98]' : 'bg-zinc-800 text-zinc-500 cursor-not-allowed'"
            >
              <span>{{ isInStock ? 'Thêm Vào Giỏ Hàng' : 'Tạm Thời Hết Hàng' }}</span>
              
              <div class="w-8 h-8 rounded-full bg-black/15 flex items-center justify-center text-black transition-transform duration-300 group-hover:translate-x-1">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
              </div>
            </button>
          </div>
        </div>

        <!-- Feature List -->
        <div class="p-5 rounded-2xl bg-white/[0.01] border border-white/5 space-y-2">
          <span class="text-xs font-mono uppercase tracking-wider text-zinc-400 font-semibold">
            Đặc điểm thiết kế
          </span>
          <ul class="space-y-1.5 text-xs text-zinc-300 pt-1">
            <li v-for="(feat, idx) in product.features" :key="idx" class="flex items-center gap-2">
              <span class="text-emerald-400 font-bold">✓</span>
              <span>{{ feat }}</span>
            </li>
          </ul>
        </div>

      </div>

    </div>

    <!-- Product Not Found Fallback -->
    <div v-else class="text-center py-24">
      <h2 class="text-lg font-bold text-white mb-2">Không tìm thấy sản phẩm!</h2>
      <p class="text-xs text-zinc-400 mb-6">Mã định danh sản phẩm {{ productId }} không tồn tại trong kho.</p>
      <RouterLink 
        to="/" 
        class="px-5 py-2.5 rounded-full bg-emerald-500 text-black font-semibold text-xs inline-block"
      >
        Quay lại Catalog
      </RouterLink>
    </div>

  </div>
</template>
