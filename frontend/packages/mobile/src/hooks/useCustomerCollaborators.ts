import { computed, reactive, unref } from 'vue';

import type { CollaborationItem } from '@lib/shared/models/customer';

import { getCustomerCollaborationList } from '@/api/modules';

import type { MaybeRef } from 'vue';

const collaboratorCache = reactive<Record<string, CollaborationItem[]>>({});
const pendingRequests = new Map<string, Promise<CollaborationItem[]>>();

export function refreshCustomerCollaborators(customerId: string) {
  if (!customerId) return Promise.resolve([]);

  const pendingRequest = pendingRequests.get(customerId);
  if (pendingRequest) return pendingRequest;

  const request = getCustomerCollaborationList({ customerId })
    .then((collaborators) => {
      collaboratorCache[customerId] = collaborators;
      return collaborators;
    })
    .finally(() => {
      pendingRequests.delete(customerId);
    });

  pendingRequests.set(customerId, request);
  return request;
}

export default function useCustomerCollaborators(customerId: MaybeRef<string>) {
  const currentCustomerId = computed(() => unref(customerId));
  const collaborators = computed(() => collaboratorCache[currentCustomerId.value] || []);

  function refresh() {
    return refreshCustomerCollaborators(currentCustomerId.value);
  }

  function clear() {
    delete collaboratorCache[currentCustomerId.value];
  }

  return {
    collaborators,
    refresh,
    clear,
  };
}
