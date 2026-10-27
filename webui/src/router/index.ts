import { createRouter, createWebHistory } from 'vue-router';
import ProductCatalogView from '../views/ProductCatalogView.vue';
import ProductDetailView from '../views/ProductDetailView.vue';
import TaskDashboardView from '../views/TaskDashboardView.vue';
import ArchitectureGuideView from '../views/ArchitectureGuideView.vue';

const routes = [
  {
    path: '/',
    name: 'catalog',
    component: ProductCatalogView,
    meta: { title: 'Aether Hardware Catalog' }
  },
  {
    path: '/product/:id',
    name: 'product-detail',
    component: ProductDetailView,
    meta: { title: 'Product Specification' }
  },
  {
    path: '/tasks',
    name: 'tasks',
    component: TaskDashboardView,
    meta: { title: 'Clean Architecture Tasks & AI Assistant' }
  },
  {
    path: '/architecture',
    name: 'architecture',
    component: ArchitectureGuideView,
    meta: { title: 'Báo Cáo Kiến Trúc & Cài Đặt Hệ Thống' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0, behavior: 'smooth' };
  }
});
