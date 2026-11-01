import { createRouter, createWebHashHistory, RouterView } from 'vue-router';
import TaskOpsView from '../components/tasks/TaskOpsView.vue';
import AiCopilotView from '../components/ai/AiCopilotView.vue';
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [{ path: '/', component: RouterView, children: [
    { path: '', redirect: '/tasks' },
    { path: 'tasks', name: 'tasks', component: TaskOpsView },
    { path: 'tasks/:taskId', name: 'task-detail', component: TaskOpsView },
    { path: 'ai', name: 'ai', component: AiCopilotView }
  ] }, { path: '/:pathMatch(.*)*', redirect: '/tasks' }]
});
