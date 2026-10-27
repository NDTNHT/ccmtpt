<script setup lang="ts">
import Header from './components/Header.vue';
import CartModal from './components/CartModal.vue';
import AIChatbot from './components/AIChatbot.vue';
</script>

<template>
  <div class="min-h-[100dvh] flex flex-col bg-[#050505] text-[#f3f4f6] relative selection:bg-emerald-500/30 selection:text-emerald-300">
    
    <!-- Fixed Background Atmospheric Mesh Glow (GPU safe) -->
    <div class="fixed inset-0 pointer-events-none z-0 mesh-gradient-bg opacity-75"></div>

    <!-- Floating Island Navigation Header -->
    <Header />

    <!-- Main Dynamic Route View -->
    <main class="flex-1 relative z-10">
      <RouterView v-slot="{ Component }">
        <transition 
          name="fade" 
          mode="out-in"
        >
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>

    <!-- Global Cart Slide-over Drawer Modal -->
    <CartModal />

    <!-- Global Multi-Provider AI Assistant Copilot Modal -->
    <AIChatbot />

    <!-- Global Footer (Double-Bezel Agency Theme) -->
    <footer class="relative z-10 border-t border-white/10 py-12 px-4 sm:px-6 lg:px-8 mt-auto bg-black/40 backdrop-blur-md">
      <div class="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-6 text-xs text-zinc-400 font-mono">
        <div class="flex items-center gap-3">
          <div class="w-6 h-6 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
          </div>
          <span>&copy; 2026 AETHER SYSTEMS. All rights reserved.</span>
        </div>

        <div class="flex items-center gap-6">
          <span class="text-zinc-500">Clean Architecture v1.0 • Vue 3 &amp; Pinia</span>
          <a 
            href="https://github.com/NDTNHT/ccmtpt" 
            target="_blank" 
            rel="noopener noreferrer"
            class="text-zinc-300 hover:text-emerald-400 transition-colors flex items-center gap-1.5"
          >
            <span>GitHub Repository</span>
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
          </a>
        </div>
      </div>
    </footer>

  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s cubic-bezier(0.32, 0.72, 0, 1), transform 0.3s cubic-bezier(0.32, 0.72, 0, 1);
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
