<script setup lang="ts">
import { ref, computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { Product, ProductVariant } from '../types/product';
import { useCartStore } from '../stores/cart';

const props = defineProps<{
  product: Product;
}>();

const cartStore = useCartStore();

// Selected variant reactive state (defaults to primary/first variant)
const selectedVariant = ref<ProductVariant>(props.product.variants[0]);

// Active preview image (supports hover thumbnail switching & variant auto-updating)
const activeImageUrl = ref<string>(selectedVariant.value.imageUrl || props.product.images[0]);

// EVENT HANDLER 1: @click variant selection
function handleSelectVariant(variant: ProductVariant) {
  selectedVariant.value = variant;
  activeImageUrl.value = variant.imageUrl;
}

// EVENT HANDLER 2: @mouseover preview image switcher
function handleHoverImage(imageUrl: string) {
  activeImageUrl.value = imageUrl;
}

// COMPUTED 1: Current unit price (base/sale price + variant price offset)
const currentUnitPrice = computed<number>(() => {
  const base = props.product.salePrice ?? props.product.basePrice;
  return base + selectedVariant.value.priceOffset;
});

// COMPUTED 2: Original price (before sale discount)
const originalPrice = computed<number>(() => {
  return props.product.basePrice + selectedVariant.value.priceOffset;
});

// COMPUTED 3: Trạng thái giảm giá (sale flag and discount percentage)
const isSale = computed<boolean>(() => {
  return !!props.product.salePrice && props.product.salePrice < props.product.basePrice;
});

const discountPercentage = computed<number>(() => {
  if (!isSale.value || !props.product.salePrice) return 0;
  return Math.round(((props.product.basePrice - props.product.salePrice) / props.product.basePrice) * 100);
});

// COMPUTED 4: Trạng thái tồn kho (inStock according to current variant)
const isInStock = computed<boolean>(() => {
  return selectedVariant.value.stockQuantity > 0;
});

const stockBadgeText = computed<string>(() => {
  if (selectedVariant.value.stockQuantity <= 0) {
    return 'Hết hàng (Sold Out)';
  }
  if (selectedVariant.value.stockQuantity <= 5) {
    return `Chỉ còn ${selectedVariant.value.stockQuantity} sản phẩm`;
  }
  return `Còn hàng (${selectedVariant.value.stockQuantity})`;
});

// Action: Add to Cart via Pinia
function handleAddToCart() {
  if (!isInStock.value) return;
  cartStore.addItem(props.product, selectedVariant.value, 1);
}
</script>

<template>
  <!-- DOUBLE-BEZEL OUTER SHELL (Doppelrand Hardware Aesthetic) -->
  <div class="group bezel-outer flex flex-col h-full bg-white/[0.02] border border-white/10 hover:border-emerald-500/40 transition-fluid duration-700">
    
    <!-- DOUBLE-BEZEL INNER CORE -->
    <div class="bezel-inner flex flex-col h-full bg-[#0c0c0e] p-6 relative overflow-hidden">
      
      <!-- Top Eyebrow Badges & Sale Status -->
      <div class="flex items-center justify-between gap-2 mb-4 z-10">
        <!-- Category Eyebrow Pill -->
        <span class="rounded-full px-3 py-1 text-[10px] uppercase tracking-[0.2em] font-medium bg-white/5 border border-white/10 text-zinc-400 font-mono">
          {{ product.category }}
        </span>

        <!-- Dynamic Sale & Stock Badges (v-if / v-else) -->
        <div class="flex items-center gap-1.5">
          <span 
            v-if="isSale" 
            class="rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider bg-amber-500/15 text-amber-400 border border-amber-500/30"
          >
            -{{ discountPercentage }}% OFF
          </span>

          <span 
            v-if="isInStock" 
            class="flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[10px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
            {{ stockBadgeText }}
          </span>
          <span 
            v-else 
            class="flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[10px] font-medium bg-rose-500/15 text-rose-400 border border-rose-500/30"
          >
            Hết hàng
          </span>
        </div>
      </div>

      <!-- Main Product Image Preview with Hover Transition -->
      <div class="relative w-full aspect-[4/3] rounded-2xl overflow-hidden bg-black/40 border border-white/5 mb-5 flex items-center justify-center group-hover:border-white/15 transition-fluid">
        <img 
          :src="activeImageUrl" 
          :alt="product.name"
          class="w-full h-full object-cover object-center transform transition-all duration-700 ease-[cubic-bezier(0.32,0.72,0,1)] group-hover:scale-105"
          loading="lazy"
        />

        <!-- Hover Image Switcher Thumbnails (v-for and @mouseover) -->
        <div class="absolute bottom-2.5 left-1/2 -translate-x-1/2 flex items-center gap-1.5 p-1 rounded-full bg-black/60 backdrop-blur-md border border-white/10 opacity-0 group-hover:opacity-100 transition-fluid duration-300">
          <button
            v-for="(img, idx) in product.images"
            :key="idx"
            @mouseover="handleHoverImage(img)"
            @click.prevent="handleHoverImage(img)"
            class="w-5 h-5 rounded-full overflow-hidden border transition-all duration-200"
            :class="activeImageUrl === img ? 'border-emerald-400 scale-110' : 'border-white/30 opacity-60 hover:opacity-100'"
            :aria-label="`Xem ảnh ${idx + 1}`"
          >
            <img :src="img" class="w-full h-full object-cover" />
          </button>
        </div>
      </div>

      <!-- Product Meta (Title, Tagline, Rating) -->
      <div class="flex-1 flex flex-col justify-between">
        <div>
          <!-- Title linked to dynamic route -->
          <RouterLink 
            :to="`/product/${product.id}`"
            class="block text-lg font-bold text-white group-hover:text-emerald-400 transition-colors font-display tracking-tight leading-snug"
          >
            {{ product.name }}
          </RouterLink>

          <!-- Tagline -->
          <p class="mt-1.5 text-xs text-zinc-400 line-clamp-2 leading-relaxed">
            {{ product.tagline }}
          </p>
        </div>

        <!-- Variant Color Swatches (v-for, @click event handling) -->
        <div class="mt-4 pt-4 border-t border-white/5">
          <div class="flex items-center justify-between mb-2">
            <span class="text-[11px] font-mono text-zinc-400">
              Color: <strong class="text-zinc-200">{{ selectedVariant.colorName }}</strong>
            </span>
            <span class="text-[10px] font-mono text-zinc-500">
              SKU: {{ selectedVariant.sku }}
            </span>
          </div>

          <div class="flex items-center gap-2">
            <button
              v-for="variant in product.variants"
              :key="variant.id"
              @click="handleSelectVariant(variant)"
              class="relative w-6 h-6 rounded-full border transition-all duration-300 flex items-center justify-center"
              :class="selectedVariant.id === variant.id ? 'border-emerald-400 ring-2 ring-emerald-500/20 scale-110' : 'border-white/20 hover:border-white/50'"
              :style="{ backgroundColor: variant.colorHex }"
              :title="`${variant.name} (${variant.stockQuantity} in stock)`"
              :aria-label="`Chọn biến thể ${variant.name}`"
            >
              <!-- Center active dot indicator -->
              <span 
                v-if="selectedVariant.id === variant.id" 
                class="w-1.5 h-1.5 rounded-full"
                :class="variant.colorHex === '#ffffff' || variant.colorHex === '#e4e4e7' ? 'bg-black' : 'bg-white'"
              ></span>
            </button>
          </div>
        </div>

        <!-- Pricing & Button-in-Button CTA (Chương 3 requirement) -->
        <div class="mt-5 pt-4 border-t border-white/5 flex items-center justify-between gap-4">
          <!-- Price Display -->
          <div class="flex flex-col">
            <div class="flex items-baseline gap-2">
              <span class="text-xl font-extrabold text-white font-mono tracking-tight">
                ${{ currentUnitPrice }}
              </span>
              <span 
                v-if="isSale" 
                class="text-xs text-zinc-500 line-through font-mono"
              >
                ${{ originalPrice }}
              </span>
            </div>
            <span class="text-[10px] text-zinc-500 font-mono">USD • Tax included</span>
          </div>

          <!-- BUTTON-IN-BUTTON NESTED ARCHITECTURE CTA -->
          <button 
            @click="handleAddToCart"
            :disabled="!isInStock"
            class="relative inline-flex items-center gap-3 rounded-full pl-5 pr-2 py-2 text-xs font-semibold transition-fluid group/btn"
            :class="isInStock 
              ? 'bg-emerald-500 text-black hover:bg-emerald-400 active:scale-[0.98] shadow-lg shadow-emerald-500/20' 
              : 'bg-zinc-800 text-zinc-500 cursor-not-allowed border border-white/5'"
          >
            <span>{{ isInStock ? 'Add to Cart' : 'Sold Out' }}</span>
            
            <!-- Nested Trailing Icon Wrapper -->
            <div 
              class="w-7 h-7 rounded-full flex items-center justify-center transition-transform duration-300 group-hover/btn:translate-x-0.5 group-hover/btn:-translate-y-0.5"
              :class="isInStock ? 'bg-black/15 text-black' : 'bg-zinc-700 text-zinc-500'"
            >
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
            </div>
          </button>
        </div>

      </div>

    </div>
  </div>
</template>
