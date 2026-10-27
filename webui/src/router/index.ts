import { createRouter, createWebHistory } from 'vue-router';
import TaskDashboardView from '../views/TaskDashboardView.vue';
import ProductCatalogView from '../views/ProductCatalogView.vue';
import ProductDetailView from '../views/ProductDetailView.vue';
import ArchitectureGuideView from '../views/ArchitectureGuideView.vue';

const routes = [
  {
    path: '/',
    name: 'tasks',
    component: TaskDashboardView,
    meta: { title: 'Task Management System • Clean Architecture' }
  },
  {
    path: '/shop',
    name: 'shop',
    component: ProductCatalogView,
    meta: { title: 'Demo Báo Cáo Chương 3 • Giỏ Hàng & Sản Phẩm' }
  },
  {
    path: '/product/:id',
    name: 'product-detail',
    component: ProductDetailView,
    meta: { title: 'Chi Tiết Sản Phẩm • Dynamic Route' }
  },
  {
    path: '/architecture',
    name: 'architecture',
    component: ArchitectureGuideView,
    meta: { title: 'Báo Cáo Kiến Trúc Hệ Thống (Chương 3)' }
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
