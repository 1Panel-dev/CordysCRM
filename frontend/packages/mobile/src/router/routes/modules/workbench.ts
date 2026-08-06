import { WorkbenchRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const workbench: AppRouteRecordRaw = {
  path: '/workbench',
  name: WorkbenchRouteEnum.WORKBENCH,
  redirect: '/workbench/index',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: [],
  },
  children: [
    {
      path: 'index',
      name: WorkbenchRouteEnum.WORKBENCH_INDEX,
      component: () => import('@/views/workbench/index.vue'),
      meta: {
        locale: 'menu.workbench',
        permissions: [],
        isCache: true,
        depth: 1,
      },
    },
    {
      path: 'duplicateCheck',
      name: WorkbenchRouteEnum.WORKBENCH_DUPLICATE_CHECK,
      component: () => import('@/views/workbench/duplicateCheck/index.vue'),
      meta: {
        locale: 'menu.duplicateCheck',
        permissions: [
          'CUSTOMER_MANAGEMENT:READ',
          'CUSTOMER_MANAGEMENT_POOL:READ',
          'CLUE_MANAGEMENT:READ',
          'CLUE_MANAGEMENT_POOL:READ',
        ],
        depth: 2,
        isCache: true,
      },
    },
    {
      path: 'agent',
      name: WorkbenchRouteEnum.WORKBENCH_AGENT,
      component: () => import('@/views/workbench/agent/index.vue'),
      meta: {
        locale: '',
        permissions: [],
        depth: 2,
        isCache: true,
      },
    },
    {
      path: 'ai-chat',
      name: WorkbenchRouteEnum.WORKBENCH_AI_CHAT,
      component: () => import('@/views/workbench/ai-chat/index.vue'),
      meta: {
        locale: '',
        permissions: [],
        depth: 2,
      },
    },
    {
      path: 'task',
      name: WorkbenchRouteEnum.WORKBENCH_TASK,
      component: () => import('@/views/workbench/task/index.vue'),
      meta: {
        locale: '',
        permissions: [],
        depth: 1,
        isCache: true,
      },
    },
    {
      path: 'approval',
      name: WorkbenchRouteEnum.WORKBENCH_APPROVAL,
      component: () => import('@/views/workbench/approval/index.vue'),
      meta: {
        locale: '',
        permissions: [],
        depth: 1,
        isCache: true,
      },
    },
  ],
};

export default workbench;
