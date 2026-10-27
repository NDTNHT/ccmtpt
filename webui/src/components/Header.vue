<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useCartStore } from '../stores/cart';
import { useAiStore } from '../stores/ai';

const route = useRoute();
const cartStore = useCartStore();
const aiStore = useAiStore();

// storeToRefs preserves reactivity when destructuring state/getters from Pinia!
const { totalItemCount } = storeToRefs(cartStore);

// Mobile navigation menu toggle state
const isMobileMenuOpen = ref<boolean>(false);

function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value;
}

function closeMobileMenu() {
  isMobileMenuOpen.value = false;
}
</script>

<template>
  <header class="fixed top-0 left-0 right-0 z-40 px-4 pt-4 sm:pt-6 pointer-events-none">
    <div class="max-w-6xl mx-auto flex items-center justify-between glass-island rounded-full px-5 py-3 pointer-events-auto transition-fluid">
      
      <!-- Brand Logo -->
      <RouterLink 
        to="/" 
        class="flex items-center gap-2.5 group"
        @click="closeMobileMenu"
      >
        <div class="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center transition-fluid group-hover:scale-105 group-hover:border-emerald-500/60">
          <div class="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_12px_#10b981] animate-pulse"></div>
        </div>
        <div class="flex flex-col">
          <span class="text-sm font-bold tracking-tight text-white flex items-center gap-1.5 font-display">
            AETHER <span class="text-xs px-1.5 py-0.5 rounded bg-emerald-500/15 text-emerald-400 font-mono font-medium">SPA</span>
          </span>
          <span class="text-[10px] text-zinc-400 tracking-wider uppercase font-mono">Clean Arch • Vue 3</span>
        </div>
      </RouterLink>

      <!-- Desktop Navigation Links -->
      <nav class="hidden md:flex items-center gap-1 bg-white/5 border border-white/5 rounded-full p-1 text-xs font-medium">
        <RouterLink 
          to="/" 
          class="px-4 py-2 rounded-full transition-fluid"
          :class="route.path === '/' ? 'bg-white/10 text-white shadow-sm font-semibold' : 'text-zinc-400 hover:text-white hover:bg-white/5'"
        >
          Catalog
        </RouterLink>

        <RouterLink 
          to="/tasks" 
          class="px-4 py-2 rounded-full transition-fluid"
          :class="route.path === '/tasks' ? 'bg-white/10 text-white shadow-sm font-semibold' : 'text-zinc-400 hover:text-white hover:bg-white/5'"
        >
          Clean Architecture Tasks
        </RouterLink>

        <RouterLink 
          to="/architecture" 
          class="px-4 py-2 rounded-full transition-fluid"
          :class="route.path === '/architecture' ? 'bg-white/10 text-white shadow-sm font-semibold' : 'text-zinc-400 hover:text-white hover:bg-white/5'"
        >
          Báo Cáo Kiến Trúc (Chương 3)
        </RouterLink>
      </nav>

      <!-- Right Interactive Controls -->
      <div class="flex items-center gap-2">
        <!-- AI Assistant Launch Button -->
        <button 
          @click="aiStore.openAssistant()"
          class="flex items-center gap-2 px-3.5 py-2 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 hover:bg-emerald-500/20 hover:border-emerald-500/40 transition-fluid group active:scale-[0.98]"
          title="Open AI Task Assistant"
        >
          <svg class="w-4 h-4 transition-transform group-hover:rotate-12" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.75">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" />
          </svg>
          <span class="hidden sm:inline">AI Copilot</span>
        </button>

        <!-- Cart Drawer Trigger Button with Dynamic Counter Badge -->
        <button 
          @click="cartStore.toggleModal()"
          class="relative flex items-center justify-center w-10 h-10 rounded-full bg-white/5 border border-white/10 hover:bg-white/10 text-white transition-fluid active:scale-95 group"
          aria-label="View Cart"
        >
          <svg class="w-4 h-4 transition-transform group-hover:scale-110" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.75">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 10-7.5 0v4.5m11.356-1.993l1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 01-1.12-1.243l1.264-12A1.125 1.125 0 015.513 7.5h12.974c.576 0 1.059.435 1.119 1.007zM8.625 10.5a.375.375 0 11-.75 0 .375.375 0 01.75 0zm7.5 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
          </svg>
          <!-- Dynamic Reactive Badge -->
          <span 
            v-if="totalItemCount > 0"
            class="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-emerald-500 text-black text-[11px] font-bold flex items-center justify-center shadow-lg shadow-emerald-500/30 animate-pulse font-mono"
          >
            {{ totalItemCount }}
          </span>
        </button>

        <!-- Mobile Hamburger Button with Morphing Animation -->
        <button 
          @click="toggleMobileMenu"
          class="md:hidden flex flex-col justify-center items-center w-10 h-10 rounded-full bg-white/5 border border-white/10 text-white transition-fluid"
          aria-label="Toggle navigation"
        >
          <span 
            class="w-4 h-0.5 bg-white rounded-full transition-all duration-300"
            :class="isMobileMenuOpen ? 'rotate-45 translate-y-1' : '-translate-y-0.5'"
          ></span>
          <span 
            class="w-4 h-0.5 bg-white rounded-full transition-all duration-300"
            :class="isMobileMenuOpen ? '-rotate-45 -translate-y-0' : 'translate-y-0.5'"
          ></span>
        </button>
      </div>
    </div>

    <!-- Mobile Drawer Overlay Menu -->
    <div 
      v-if="isMobileMenuOpen"
      class="md:hidden mt-3 max-w-sm mx-auto glass-island rounded-3xl p-5 pointer-events-auto border border-white/10 shadow-2xl transition-fluid"
    >
      <nav class="flex flex-col gap-2 text-sm">
        <RouterLink 
          to="/" 
          @click="closeMobileMenu"
          class="px-4 py-2.5 rounded-xl transition-fluid"
          :class="route.path === '/' ? 'bg-emerald-500/20 text-emerald-400 font-semibold' : 'text-zinc-300 hover:bg-white/5'"
        >
          Catalog Sản Phẩm
        </RouterLink>

        <RouterLink 
          to="/tasks" 
          @click="closeMobileMenu"
          class="px-4 py-2.5 rounded-xl transition-fluid"
          :class="route.path === '/tasks' ? 'bg-emerald-500/20 text-emerald-400 font-semibold' : 'text-zinc-300 hover:bg-white/5'"
        >
          Clean Architecture Tasks
        </RouterLink>

        <RouterLink 
          to="/architecture" 
          @click="closeMobileMenu"
          class="px-4 py-2.5 rounded-xl transition-fluid"
          :class="route.path === '/architecture' ? 'bg-emerald-500/20 text-emerald-400 font-semibold' : 'text-zinc-300 hover:bg-white/5'"
        >
          Báo Cáo Kiến Trúc (Chương 3)
        </RouterLink>
      </nav>
    </div>
  </header>
</template>
