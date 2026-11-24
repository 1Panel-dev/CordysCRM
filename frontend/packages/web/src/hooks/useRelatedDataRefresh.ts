type RelatedDataListener = (formId: string, resourceId: string) => void;

const listeners = new Set<RelatedDataListener>();

export function notifyRelatedDataChanged(formId: string, resourceId: string) {
  listeners.forEach((listener) => listener(formId, resourceId));
}

// 关联 Tab 新增、编辑、删除、导入等成功后，统一上报数据变更
export function useRelatedTableChanges(
  props: {
    detailTabPageFormId?: string;
    detailTabResourceId?: string;
    parentFormId?: string;
    parentResourceId?: string;
  },
  signals: (() => unknown)[]
) {
  const notify = () => {
    const formId = props.parentFormId ?? props.detailTabPageFormId;
    const resourceId = props.parentResourceId ?? props.detailTabResourceId;
    if (formId && resourceId) {
      notifyRelatedDataChanged(formId, resourceId);
    }
  };
  watch(signals, notify);
  return notify;
}

export function useRelatedDataListener(listener: RelatedDataListener) {
  listeners.add(listener);
  onScopeDispose(() => listeners.delete(listener));
}

export function useRelatedDetailRefresh(options: {
  formId: () => string;
  resourceId: () => string;
  active: () => boolean;
  refresh: () => unknown;
}) {
  const dirty = ref(false);
  useRelatedDataListener((formId, resourceId) => {
    if (formId === options.formId() && resourceId === options.resourceId()) dirty.value = true;
  });
  watch([options.formId, options.resourceId], () => {
    dirty.value = false;
  });
  watch([options.active, dirty], ([active, changed]) => {
    if (active && changed) {
      dirty.value = false;
      options.refresh();
    }
  });
}
