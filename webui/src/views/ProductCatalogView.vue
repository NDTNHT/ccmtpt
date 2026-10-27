<script setup lang="ts">
import { ref } from 'vue';
import { MOCK_PRODUCTS } from '../data/mockProducts';
import ProductList from '../components/ProductList.vue';

const products = ref(MOCK_PRODUCTS);
</script>

<template>
  <div class="pt-28 pb-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
    
    <!-- Hero Section (Awwwards-Tier Spatial Rhythm & Eyebrow Tag) -->
    <section class="py-12 sm:py-20 flex flex-col items-center text-center relative">
      
      <!-- Eyebrow Tag -->
      <div class="inline-flex items-center gap-2 rounded-full px-3.5 py-1.5 text-[11px] uppercase tracking-[0.2em] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/25 mb-6 shadow-sm">
        <span class="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
        <span>Aether Hardware Series 2026</span>
      </div>

      <!-- Main Headline with Variable Grotesk Stacking -->
      <h1 class="text-4xl sm:text-6xl lg:text-7xl font-extrabold text-white tracking-tight leading-[1.08] max-w-4xl font-display">
        Bàn phím công thái học &amp; Thiết bị ngoại vi <span class="bg-gradient-to-r from-emerald-400 via-teal-300 to-indigo-400 bg-clip-text text-transparent">Chính xác cao.</span>
      </h1>

      <!-- Subtitle with generous line-height -->
      <p class="mt-6 text-sm sm:text-base text-zinc-400 max-w-2xl leading-relaxed">
        Thiết kế phần cứng unibody nhôm CNC với công nghệ phản hồi xúc giác điện từ và vi xử lý kép. Tích hợp trực tiếp với kiến trúc Clean Architecture &amp; AI Assistant.
      </p>

      <!-- Key Specs Pill Metrics -->
      <div class="mt-10 flex flex-wrap items-center justify-center gap-3 sm:gap-6 text-xs text-zinc-400 font-mono">
        <div class="flex items-center gap-2 px-4 py-2 rounded-full bg-white/[0.03] border border-white/10">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
          <span>1000Hz Polling Rate</span>
        </div>
        <div class="flex items-center gap-2 px-4 py-2 rounded-full bg-white/[0.03] border border-white/10">
          <span class="w-1.5 h-1.5 rounded-full bg-indigo-400"></span>
          <span>Vue 3 Composition Proxy</span>
        </div>
        <div class="flex items-center gap-2 px-4 py-2 rounded-full bg-white/[0.03] border border-white/10">
          <span class="w-1.5 h-1.5 rounded-full bg-amber-400"></span>
          <span>Pinia Central Store</span>
        </div>
      </div>

    </section>

    <!-- Product Bento Grid Section -->
    <ProductList :products="products" />

  </div>
</template>
