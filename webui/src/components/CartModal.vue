<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { useCartStore } from '../stores/cart';

const cartStore = useCartStore();

// storeToRefs enables destructuring reactive state and getters without losing Proxy tracking
const { 
  items, 
  isModalOpen, 
  totalPrice, 
  totalItemCount, 
  isEligibleForFreeShipping, 
  freeShippingProgress, 
  remainingForFreeShipping,
  FREE_SHIPPING_THRESHOLD 
} = storeToRefs(cartStore);

function handleCheckout() {
  alert(`Đang chuyển hướng thanh toán cho đơn hàng ${totalItemCount.value} sản phẩm trị giá $${totalPrice.value} USD!`);
}
</script>

<template>
  <!-- Backdrop Blur Overlay -->
  <Teleport to="body">
    <div 
      v-if="isModalOpen"
      class="fixed inset-0 z-50 flex justify-end bg-black/70 backdrop-blur-md transition-all duration-500"
      @click.self="cartStore.closeModal()"
    >
      <!-- Slide-over Drawer Panel -->
      <div 
        class="w-full max-w-lg h-full bg-[#0a0a0c] border-l border-white/10 shadow-2xl flex flex-col transform transition-all duration-500 ease-[cubic-bezier(0.32,0.72,0,1)]"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cart-heading"
      >
        
        <!-- Header -->
        <div class="px-6 py-5 border-b border-white/10 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 10-7.5 0v4.5m11.356-1.993l1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 01-1.12-1.243l1.264-12A1.125 1.125 0 015.513 7.5h12.974c.576 0 1.059.435 1.119 1.007zM8.625 10.5a.375.375 0 11-.75 0 .375.375 0 01.75 0zm7.5 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
              </svg>
            </div>
            <div>
              <h2 id="cart-heading" class="text-base font-bold text-white font-display">
                Giỏ Hàng Của Bạn
              </h2>
              <span class="text-xs text-zinc-400 font-mono">
                {{ totalItemCount }} sản phẩm đã chọn
              </span>
            </div>
          </div>

          <button 
            @click="cartStore.closeModal()"
            class="w-9 h-9 rounded-full bg-white/5 border border-white/10 hover:bg-white/10 flex items-center justify-center text-zinc-400 hover:text-white transition-fluid"
            aria-label="Đóng giỏ hàng"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Free Shipping Incentive Progress Bar -->
        <div class="px-6 py-4 bg-white/[0.02] border-b border-white/5">
          <div class="flex items-center justify-between text-xs mb-2">
            <span v-if="isEligibleForFreeShipping" class="text-emerald-400 font-medium flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
              Đủ điều kiện Miễn Phí Vận Chuyển Hỏa Tốc!
            </span>
            <span v-else class="text-zinc-400">
              Mua thêm <strong class="text-white font-mono">${{ remainingForFreeShipping }}</strong> để được miễn phí ship
            </span>
            <span class="font-mono text-zinc-500 text-[11px]">{{ freeShippingProgress }}%</span>
          </div>

          <!-- Progress track -->
          <div class="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
            <div 
              class="h-full bg-gradient-to-r from-emerald-500 to-teal-400 transition-all duration-500 ease-out"
              :style="{ width: `${freeShippingProgress}%` }"
            ></div>
          </div>
        </div>

        <!-- Cart Items List (v-if / v-else) -->
        <div class="flex-1 overflow-y-auto px-6 py-4 space-y-4">
          
          <!-- When items exist (v-for) -->
          <div 
            v-if="items.length > 0"
            class="space-y-3"
          >
            <div 
              v-for="item in items" 
              :key="item.id"
              class="bezel-outer p-1.5 bg-white/[0.01] border border-white/5 hover:border-white/15 transition-fluid"
            >
              <div class="bezel-inner p-3 bg-[#0d0d10] flex items-center gap-3">
                <!-- Thumbnail -->
                <div class="w-16 h-16 rounded-xl overflow-hidden bg-black/60 border border-white/10 flex-shrink-0">
                  <img 
                    :src="item.selectedVariant.imageUrl || item.product.images[0]" 
                    :alt="item.product.name"
                    class="w-full h-full object-cover"
                  />
                </div>

                <!-- Info -->
                <div class="flex-1 min-w-0">
                  <h4 class="text-xs font-bold text-white truncate font-display">
                    {{ item.product.name }}
                  </h4>
                  <div class="flex items-center gap-2 mt-1">
                    <span 
                      class="w-2.5 h-2.5 rounded-full border border-white/20"
                      :style="{ backgroundColor: item.selectedVariant.colorHex }"
                    ></span>
                    <span class="text-[11px] text-zinc-400 font-mono">
                      {{ item.selectedVariant.colorName }}
                    </span>
                  </div>
                  <div class="text-xs font-bold font-mono text-emerald-400 mt-1">
                    ${{ item.unitPrice }} <span class="text-[10px] text-zinc-500 font-normal">/ unit</span>
                  </div>
                </div>

                <!-- Quantity Controls (@click event handling) -->
                <div class="flex flex-col items-end gap-2">
                  <div class="flex items-center bg-white/5 border border-white/10 rounded-full px-2 py-1">
                    <button 
                      @click="cartStore.updateQuantity(item.id, item.quantity - 1)"
                      class="w-5 h-5 rounded-full flex items-center justify-center text-zinc-400 hover:text-white transition-colors"
                      aria-label="Giảm số lượng"
                    >
                      -
                    </button>
                    <span class="px-2 text-xs font-mono font-semibold text-white">
                      {{ item.quantity }}
                    </span>
                    <button 
                      @click="cartStore.updateQuantity(item.id, item.quantity + 1)"
                      :disabled="item.quantity >= item.selectedVariant.stockQuantity"
                      class="w-5 h-5 rounded-full flex items-center justify-center text-zinc-400 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                      aria-label="Tăng số lượng"
                    >
                      +
                    </button>
                  </div>

                  <!-- Remove item button -->
                  <button 
                    @click="cartStore.removeItem(item.id)"
                    class="text-[10px] text-zinc-500 hover:text-rose-400 transition-colors font-mono"
                  >
                    Xóa
                  </button>
                </div>

              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div 
            v-else 
            class="h-full flex flex-col items-center justify-center text-center py-12"
          >
            <div class="w-16 h-16 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-zinc-600 mb-4">
              <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
              </svg>
            </div>
            <h3 class="text-sm font-bold text-white mb-1">Giỏ hàng đang trống</h3>
            <p class="text-xs text-zinc-400 max-w-xs mb-6">
              Khám phá các thiết bị công nghệ cao và bàn phím công thái học trong danh mục của chúng tôi.
            </p>
            <button 
              @click="cartStore.closeModal()"
              class="px-5 py-2.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold hover:bg-emerald-500/20 transition-fluid"
            >
              Xem danh mục sản phẩm
            </button>
          </div>

        </div>

        <!-- Footer Breakdown & Checkout CTA (Chương 3 requirement) -->
        <div 
          v-if="items.length > 0"
          class="p-6 border-t border-white/10 bg-[#09090b]"
        >
          <!-- Pricing Summary Table -->
          <div class="space-y-2 mb-4 text-xs font-mono">
            <div class="flex items-center justify-between text-zinc-400">
              <span>Tạm tính (Subtotal)</span>
              <span class="text-white">${{ totalPrice }}</span>
            </div>
            <div class="flex items-center justify-between text-zinc-400">
              <span>Vận chuyển (Express Freight)</span>
              <span :class="isEligibleForFreeShipping ? 'text-emerald-400 font-bold' : 'text-white'">
                {{ isEligibleForFreeShipping ? 'MIỄN PHÍ' : '$25' }}
              </span>
            </div>
            <div class="flex items-center justify-between pt-2 border-t border-white/5 text-sm font-bold">
              <span class="text-white">Tổng cộng (Total)</span>
              <span class="text-emerald-400 text-lg font-mono">
                ${{ isEligibleForFreeShipping ? totalPrice : totalPrice + 25 }}
              </span>
            </div>
          </div>

          <!-- Button-in-Button Checkout CTA -->
          <div class="flex items-center gap-3">
            <button 
              @click="cartStore.clearCart()"
              class="px-4 py-3 rounded-full text-xs font-medium text-zinc-400 hover:text-white bg-white/5 border border-white/10 hover:bg-white/10 transition-fluid"
            >
              Xóa hết
            </button>

            <button 
              @click="handleCheckout"
              class="flex-1 relative inline-flex items-center justify-between rounded-full pl-6 pr-2 py-2 text-xs font-bold bg-emerald-500 text-black hover:bg-emerald-400 active:scale-[0.98] transition-fluid group shadow-lg shadow-emerald-500/25"
            >
              <span>Tiến Hành Thanh Toán</span>
              
              <div class="w-8 h-8 rounded-full bg-black/15 flex items-center justify-center text-black transition-transform duration-300 group-hover:translate-x-1">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
                </svg>
              </div>
            </button>
          </div>
        </div>

      </div>
    </div>
  </Teleport>
</template>
