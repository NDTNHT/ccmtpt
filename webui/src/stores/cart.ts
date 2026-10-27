import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Product, ProductVariant, CartItem } from '../types/product';

export const useCartStore = defineStore('cart', () => {
  // 1. STATE (Reactivity backed by JavaScript Proxy)
  const items = ref<CartItem[]>([]);
  const isModalOpen = ref<boolean>(false);
  const FREE_SHIPPING_THRESHOLD = 500;

  // 2. GETTERS / COMPUTED PROPERTIES
  /**
   * Total count of all item units in the cart.
   * Auto-cached by Vue 3 computed dependency tracking.
   */
  const totalItemCount = computed<number>(() => {
    return items.value.reduce((acc, item) => acc + item.quantity, 0);
  });

  /**
   * Total monetary amount of the cart (USD).
   */
  const totalPrice = computed<number>(() => {
    return items.value.reduce((acc, item) => acc + (item.unitPrice * item.quantity), 0);
  });

  /**
   * Evaluates if cart qualifies for free express freight ($500+).
   */
  const isEligibleForFreeShipping = computed<boolean>(() => {
    return totalPrice.value >= FREE_SHIPPING_THRESHOLD;
  });

  /**
   * Remaining amount needed to qualify for free shipping.
   */
  const freeShippingProgress = computed<number>(() => {
    if (totalPrice.value >= FREE_SHIPPING_THRESHOLD) return 100;
    return Math.min(100, Math.round((totalPrice.value / FREE_SHIPPING_THRESHOLD) * 100));
  });

  const remainingForFreeShipping = computed<number>(() => {
    return Math.max(0, FREE_SHIPPING_THRESHOLD - totalPrice.value);
  });

  // 3. ACTIONS
  /**
   * Adds a product variant to the cart. If variant exists, increments quantity.
   */
  function addItem(product: Product, variant: ProductVariant, quantity: number = 1) {
    const cartItemId = `${product.id}-${variant.id}`;
    const effectivePrice = (product.salePrice ?? product.basePrice) + variant.priceOffset;

    const existingIndex = items.value.findIndex(item => item.id === cartItemId);
    if (existingIndex > -1) {
      const currentItem = items.value[existingIndex];
      // Guard against stock overflow
      const newQty = Math.min(currentItem.quantity + quantity, variant.stockQuantity);
      items.value[existingIndex] = {
        ...currentItem,
        quantity: newQty
      };
    } else {
      items.value.push({
        id: cartItemId,
        product,
        selectedVariant: variant,
        quantity: Math.min(quantity, Math.max(1, variant.stockQuantity)),
        unitPrice: effectivePrice
      });
    }

    // Auto open drawer feedback
    isModalOpen.value = true;
  }

  /**
   * Updates quantity of a specific cart item. Removes item if quantity reaches 0.
   */
  function updateQuantity(itemId: string, newQuantity: number) {
    if (newQuantity <= 0) {
      removeItem(itemId);
      return;
    }
    const target = items.value.find(item => item.id === itemId);
    if (target) {
      target.quantity = Math.min(newQuantity, target.selectedVariant.stockQuantity);
    }
  }

  /**
   * Removes an item from the cart.
   */
  function removeItem(itemId: string) {
    items.value = items.value.filter(item => item.id !== itemId);
  }

  /**
   * Empties all items.
   */
  function clearCart() {
    items.value = [];
  }

  /**
   * Modal drawer toggles.
   */
  function toggleModal() {
    isModalOpen.value = !isModalOpen.value;
  }

  function openModal() {
    isModalOpen.value = true;
  }

  function closeModal() {
    isModalOpen.value = false;
  }

  return {
    // State
    items,
    isModalOpen,
    FREE_SHIPPING_THRESHOLD,
    // Getters
    totalItemCount,
    totalPrice,
    isEligibleForFreeShipping,
    freeShippingProgress,
    remainingForFreeShipping,
    // Actions
    addItem,
    updateQuantity,
    removeItem,
    clearCart,
    toggleModal,
    openModal,
    closeModal
  };
});
