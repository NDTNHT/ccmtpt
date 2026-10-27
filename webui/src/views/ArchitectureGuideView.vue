<script setup lang="ts">
import { ref, computed } from 'vue';

// Interactive demonstration of Composition API Reactivity
const demoCount = ref<number>(2);
const demoUnitPrice = ref<number>(389);
const demoDiscount = ref<number>(10); // 10%

// Computed property dynamically tracking dependencies
const demoTotal = computed<number>(() => {
  const gross = demoCount.value * demoUnitPrice.value;
  return gross * (1 - demoDiscount.value / 100);
});
</script>

<template>
  <div class="pt-28 pb-24 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto space-y-12">
    
    <!-- Title Banner -->
    <div class="border-b border-white/10 pb-8">
      <div class="inline-flex items-center gap-2 rounded-full px-3 py-1 text-[10px] uppercase tracking-[0.2em] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 mb-3 font-mono">
        <span>Báo Cáo Kỹ Thuật • Chương 3</span>
      </div>
      <h1 class="text-3xl sm:text-5xl font-extrabold text-white tracking-tight font-display">
        Kiến Trúc Vue 3 SPA, Reactivity &amp; Cài Đặt Hệ Thống
      </h1>
      <p class="text-sm text-zinc-400 mt-3 leading-relaxed max-w-3xl">
        Tài liệu kiến trúc và hướng dẫn cài đặt hệ thống chi tiết phục vụ chấm điểm đồ án (2.5 điểm), bao gồm nguyên lý JavaScript Proxy, Composition API, Pinia State Management, Vue Router và tự động hóa CI/CD với GitHub Actions.
      </p>
    </div>

    <!-- 1. TỔNG QUAN KIẾN TRÚC SPA & ƯU ĐIỂM VUE 3 -->
    <section class="bezel-outer p-2">
      <div class="bezel-inner p-6 sm:p-8 bg-[#0c0c0e] space-y-6">
        <div class="flex items-center gap-3">
          <span class="w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-400 font-mono font-bold flex items-center justify-center border border-emerald-500/20 text-xs">
            01
          </span>
          <h2 class="text-xl font-bold text-white font-display">
            Tổng Quan Kiến Trúc SPA &amp; Nguyên Lý Reactive Với JavaScript Proxy
          </h2>
        </div>

        <div class="space-y-4 text-xs sm:text-sm text-zinc-300 leading-relaxed font-sans">
          <p>
            <strong>Kiến trúc SPA (Single Page Application):</strong> Ứng dụng tải duy nhất một tài liệu HTML (`index.html`) ban đầu. Khi người dùng điều hướng qua lại giữa các màn hình, JavaScript chặn các hành động tải trang mặc định, tự động cập nhật DOM thông qua Virtual DOM mà không cần reload trình duyệt. Điều này mang lại trải nghiệm mượt mà, loại bỏ hiện tượng nhấp nháy trắng màn hình.
          </p>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 my-4 font-mono text-xs">
            <div class="p-4 rounded-xl bg-white/[0.02] border border-white/10">
              <span class="text-amber-400 font-bold block mb-1">Vue 2: Object.defineProperty</span>
              <p class="text-zinc-400 font-sans">
                Chỉ can thiệp getter/setter trên các thuộc tính đã biết trước tại thời điểm khởi tạo. Không thể phát hiện việc thêm/xóa key mới trong Object hoặc thay đổi phần tử mảng qua chỉ số (phải dùng `Vue.set`).
              </p>
            </div>
            <div class="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30">
              <span class="text-emerald-400 font-bold block mb-1">Vue 3: ES6 JavaScript Proxy</span>
              <p class="text-zinc-300 font-sans">
                Bao bọc trực tiếp toàn bộ Target Object. Tự động bẫy (trap) tất cả các thao tác: `get`, `set`, `deleteProperty`, `has`, duyệt qua `Reflect API`. Tự động theo dõi các thuộc tính lồng nhau theo nhu cầu (Lazy Observation), tiết kiệm bộ nhớ tới 50% so với Vue 2.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 2. CÚ PHÁP COMPOSITION API -->
    <section class="bezel-outer p-2">
      <div class="bezel-inner p-6 sm:p-8 bg-[#0c0c0e] space-y-6">
        <div class="flex items-center gap-3">
          <span class="w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-400 font-mono font-bold flex items-center justify-center border border-emerald-500/20 text-xs">
            02
          </span>
          <h2 class="text-xl font-bold text-white font-display">
            Cú Pháp Composition API (&lt;script setup&gt;, ref, reactive, computed, watch)
          </h2>
        </div>

        <div class="space-y-4 text-xs sm:text-sm text-zinc-300 leading-relaxed font-sans">
          <ul class="space-y-2.5">
            <li>
              <code class="text-emerald-400 font-mono">&lt;script setup&gt;:</code> Cú pháp compile-time sugar giúp code ngắn gọn, mọi biến, hàm, component được import đều tự động expose ra template mà không cần khai báo `return` hay `components`.
            </li>
            <li>
              <code class="text-emerald-400 font-mono">ref():</code> Khởi tạo giá trị phản ứng cho kiểu dữ liệu nguyên thủy (primitive) hoặc đối tượng. Truy cập trong script qua `.value`.
            </li>
            <li>
              <code class="text-emerald-400 font-mono">reactive():</code> Khởi tạo reactive state trực tiếp trên Object/Array dựa trên Proxy (không cần `.value` trong script).
            </li>
            <li>
              <code class="text-emerald-400 font-mono">computed():</code> Tính toán giá trị phái sinh có khả năng tự động ghi nhớ (cache) kết quả và chỉ tính lại khi các dependency thay đổi.
            </li>
            <li>
              <code class="text-emerald-400 font-mono">watch():</code> Theo dõi sự thay đổi của một hoặc nhiều nguồn phản ứng để kích hoạt side-effects (gọi API, lưu localStorage, logging).
            </li>
          </ul>

          <!-- Interactive Demo Widget -->
          <div class="mt-6 p-5 rounded-2xl bg-black/60 border border-white/10">
            <span class="text-xs font-mono uppercase tracking-wider text-emerald-400 font-bold block mb-3">
              Live Demo: Proxy Reactivity &amp; Computed Caching
            </span>
            <div class="flex flex-wrap items-center gap-4 text-xs font-mono">
              <label class="flex items-center gap-2">
                <span>Số lượng (ref):</span>
                <input 
                  v-model.number="demoCount" 
                  type="number" 
                  min="1" 
                  max="99"
                  class="w-16 px-2 py-1 bg-white/5 border border-white/10 rounded text-center text-white"
                />
              </label>

              <label class="flex items-center gap-2">
                <span>Đơn giá ($):</span>
                <input 
                  v-model.number="demoUnitPrice" 
                  type="number" 
                  step="10"
                  class="w-20 px-2 py-1 bg-white/5 border border-white/10 rounded text-center text-white"
                />
              </label>

              <label class="flex items-center gap-2">
                <span>Giảm giá (%):</span>
                <input 
                  v-model.number="demoDiscount" 
                  type="number" 
                  min="0" 
                  max="100"
                  class="w-16 px-2 py-1 bg-white/5 border border-white/10 rounded text-center text-white"
                />
              </label>

              <div class="ml-auto px-4 py-2 rounded-xl bg-emerald-500/20 text-emerald-300 font-bold text-sm">
                Computed Total: ${{ demoTotal.toFixed(2) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 3. QUẢN LÝ ĐIỀU HƯỚNG VỚI VUE ROUTER 4 -->
    <section class="bezel-outer p-2">
      <div class="bezel-inner p-6 sm:p-8 bg-[#0c0c0e] space-y-6">
        <div class="flex items-center gap-3">
          <span class="w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-400 font-mono font-bold flex items-center justify-center border border-emerald-500/20 text-xs">
            03
          </span>
          <h2 class="text-xl font-bold text-white font-display">
            Quản Lý Điều Hướng Với Vue Router 4 (Nested &amp; Dynamic Routing)
          </h2>
        </div>

        <div class="space-y-4 text-xs sm:text-sm text-zinc-300 leading-relaxed font-sans">
          <p>
            Dự án triển khai định tuyến hiện đại sử dụng <code>createRouter</code> và <code>createWebHistory</code>:
          </p>
          <ul class="space-y-2 list-disc list-inside text-zinc-400">
            <li>
              <code class="text-white font-mono">/</code>: Route gốc render catalog sản phẩm <code>ProductCatalogView.vue</code>.
            </li>
            <li>
              <code class="text-white font-mono">/product/:id</code>: Dynamic Route nhận tham số <code>:id</code> truyền vào qua URL, được trích xuất an toàn trong component bằng <code>useRoute().params.id</code>.
            </li>
            <li>
              <code class="text-white font-mono">/tasks</code>: Route quản lý tác vụ liên kết backend Spring Boot Clean Architecture.
            </li>
            <li>
              <code class="text-white font-mono">/architecture</code>: Báo cáo kỹ thuật chi tiết.
            </li>
          </ul>
        </div>
      </div>
    </section>

    <!-- 4. QUẢN LÝ TRẠNG THÁI TOÀN CỤC VỚI PINIA -->
    <section class="bezel-outer p-2">
      <div class="bezel-inner p-6 sm:p-8 bg-[#0c0c0e] space-y-6">
        <div class="flex items-center gap-3">
          <span class="w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-400 font-mono font-bold flex items-center justify-center border border-emerald-500/20 text-xs">
            04
          </span>
          <h2 class="text-xl font-bold text-white font-display">
            Quản Lý Trạng Thái Toàn Cục Với Pinia (Store, State, Getters, Actions)
          </h2>
        </div>

        <div class="space-y-4 text-xs sm:text-sm text-zinc-300 leading-relaxed font-sans">
          <p>
            Pinia được thiết kế chuyên biệt cho Vue 3 với TypeScript type inference tự nhiên và kiến trúc Setup Store:
          </p>
          <div class="p-4 rounded-xl bg-black/60 border border-white/5 font-mono text-xs text-zinc-300 space-y-2">
            <div><span class="text-emerald-400 font-bold">1. State:</span> <code>items = ref&lt;CartItem[]&gt;([])</code>, <code>isModalOpen = ref&lt;boolean&gt;(false)</code></div>
            <div><span class="text-emerald-400 font-bold">2. Getters:</span> <code>totalPrice = computed(...)</code>, <code>totalItemCount = computed(...)</code>, <code>isEligibleForFreeShipping = computed(...)</code></div>
            <div><span class="text-emerald-400 font-bold">3. Actions:</span> <code>addItem()</code>, <code>updateQuantity()</code>, <code>removeItem()</code>, <code>clearCart()</code></div>
            <div><span class="text-purple-400 font-bold">4. Best Practice:</span> Sử dụng <code>storeToRefs(cartStore)</code> khi destructuring để không phá vỡ liên kết Proxy!</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 5. CHƯƠNG 3: THIẾT KẾ & CÀI ĐẶT HỆ THỐNG (2.5 ĐIỂM) -->
    <section class="bezel-outer p-2">
      <div class="bezel-inner p-6 sm:p-8 bg-[#0c0c0e] space-y-6">
        <div class="flex items-center gap-3">
          <span class="w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-400 font-mono font-bold flex items-center justify-center border border-emerald-500/20 text-xs">
            05
          </span>
          <h2 class="text-xl font-bold text-white font-display">
            Chương 3: Cây Component &amp; Xử Lý Logic Giao Diện (2.5 Điểm)
          </h2>
        </div>

        <div class="space-y-4 text-xs sm:text-sm text-zinc-300 leading-relaxed font-sans">
          <h3 class="text-sm font-bold text-white font-display">Tổ Chức Cây Component</h3>
          
          <!-- Component Hierarchy ASCII Tree -->
          <div class="p-4 rounded-xl bg-black/70 border border-white/10 font-mono text-xs text-emerald-400">
            <pre>
App.vue (Root Layout &amp; Atmospheric Mesh)
 ├── Header.vue (Floating Island Glass, Brand Logo, NavLinks, Cart Counter, AICopilot Trigger)
 ├── RouterView (Dynamic View Container)
 │    ├── ProductCatalogView.vue
 │    │    └── ProductList.vue (Asymmetric Bento Grid, Category Filter Pills, Search)
 │    │         └── ProductCard.vue (Doppelrand Bezel, Image Switcher, Swatches, Button-in-Button CTA)
 │    ├── ProductDetailView.vue (Gallery, Specs Matrix Table, Variant Selector)
 │    ├── TaskDashboardView.vue (Clean Architecture Task Entities &amp; Spring Boot API)
 │    └── ArchitectureGuideView.vue (Báo cáo kiến trúc hệ thống)
 ├── CartModal.vue (Slide-over Teleport Drawer, Free Shipping Gauge, Quantity Controls)
 └── AIChatbot.vue (Multi-Provider AI Copilot: Breakdown, Priority, Summary, Custom OpenAI/Claude Format)
            </pre>
          </div>

          <h3 class="text-sm font-bold text-white font-display pt-2">Xử Lý Logic Giao Diện Đáp Ứng Yêu Cầu Đồ Án</h3>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs font-mono">
            <div class="p-3 rounded-xl bg-white/[0.02] border border-white/5">
              <strong class="text-white block mb-1">v-for Directive:</strong>
              <span class="text-zinc-400">Duyệt danh sách sản phẩm, các biến thể màu sắc (variants), danh sách ảnh thumbnail và checklist subtasks.</span>
            </div>
            <div class="p-3 rounded-xl bg-white/[0.02] border border-white/5">
              <strong class="text-white block mb-1">v-if / v-else Directives:</strong>
              <span class="text-zinc-400">Hiển thị linh hoạt nhãn giảm giá (sale), huy hiệu còn hàng/hết hàng (inStock), giỏ hàng rỗng và trạng thái tải.</span>
            </div>
            <div class="p-3 rounded-xl bg-white/[0.02] border border-white/5">
              <strong class="text-white block mb-1">Event Handling (@click, @mouseover):</strong>
              <span class="text-zinc-400">@mouseover trên thumbnails đổi ảnh preview tức thì; @click trên color swatches cập nhật màu và tồn kho.</span>
            </div>
            <div class="p-3 rounded-xl bg-white/[0.02] border border-white/5">
              <strong class="text-white block mb-1">Computed Properties:</strong>
              <span class="text-zinc-400">Tính toán totalPrice, totalItemCount, discountPercentage, isInStock và thanh tiến trình miễn phí ship.</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 6. CI/CD TỰ ĐỘNG HÓA VỚI GITHUB ACTIONS -->
    <section class="bezel-outer p-2">
      <div class="bezel-inner p-6 sm:p-8 bg-[#0c0c0e] space-y-6">
        <div class="flex items-center gap-3">
          <span class="w-8 h-8 rounded-full bg-emerald-500/10 text-emerald-400 font-mono font-bold flex items-center justify-center border border-emerald-500/20 text-xs">
            06
          </span>
          <h2 class="text-xl font-bold text-white font-display">
            Quy Trình Tự Động Hóa CI/CD Bằng GitHub Actions
          </h2>
        </div>

        <div class="space-y-4 text-xs sm:text-sm text-zinc-300 leading-relaxed font-sans">
          <p>
            Quy trình CI/CD được thiết lập tại <code>.github/workflows/deploy.yml</code> tự động chạy trên mọi nhánh khi có commit hoặc Pull Request:
          </p>
          <div class="p-4 rounded-xl bg-black/60 border border-white/5 font-mono text-xs text-zinc-300 space-y-2">
            <div><span class="text-emerald-400">1. Backend Pipeline:</span> Checkout -> Setup Java 17 Temurin -> Run <code>mvn clean test</code> (Kiểm thử 153/153 Clean Architecture tests).</div>
            <div><span class="text-emerald-400">2. Frontend Pipeline:</span> Setup Node.js 20+ -> Run <code>npm ci</code> -> Run <code>npm run build</code> (Vite compile &amp; type check).</div>
            <div><span class="text-emerald-400">3. Release Artifacts:</span> Đóng gói SPA tĩnh vào thư mục distribution sẵn sàng triển khai lên Nginx, Vercel hoặc Docker Container.</div>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>
