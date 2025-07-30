import type { TableDraggedParams } from '@lib/shared/models/common';

import { useAppStore } from '@/store';

import Sortable from 'sortablejs';

interface DragOptions {
  containerEl: HTMLElement | null;
  draggable: string;
  handle: string;
  data: any[];
  rowKey?: string;
  filter?: string;
  onDragEnd?: (params: TableDraggedParams) => void;
  dragMoveValidator?: (from: any, to: any) => boolean;
}

const sortable = ref<Sortable | null>(null);

export function initCommonSortable(options: DragOptions) {
  const appStore = useAppStore();

  const { containerEl, draggable, handle, data, filter, rowKey = 'id', onDragEnd, dragMoveValidator } = options;

  if (!containerEl) return;
  if (sortable.value) {
    sortable.value.destroy();
    sortable.value = null;
  }

  sortable.value = Sortable.create(containerEl, {
    ghostClass: 'sortable-ghost',
    draggable,
    handle,
    filter,
    setData(dataTransfer) {
      dataTransfer.setData('Text', '');
    },
    onMove(evt) {
      if (!dragMoveValidator) return true;
      const draggedEl = evt.dragged as HTMLElement;
      const relatedEl = evt.related as HTMLElement;
      const children = Array.from(evt.from.children).filter((el) => {
        if (!filter) return true;
        if (filter.startsWith('.')) return !el.classList.contains(filter.slice(1));
        if (filter.startsWith('#')) return el.id !== filter.slice(1);
        return true;
      });
      const fromRow = data[children.indexOf(draggedEl)];
      const toRow = data[children.indexOf(relatedEl)];
      return dragMoveValidator(fromRow, toRow);
    },
    onEnd(evt) {
      const { oldDraggableIndex, newDraggableIndex } = evt;
      if (oldDraggableIndex == null || newDraggableIndex == null || oldDraggableIndex === newDraggableIndex) return;

      const moveId = data[oldDraggableIndex]?.[rowKey];
      let targetId: string;
      let moveMode: 'AFTER' | 'BEFORE';

      if (newDraggableIndex >= data.length) {
        targetId = data[data.length - 1]?.[rowKey];
        moveMode = 'AFTER';
      } else if (newDraggableIndex === 0) {
        targetId = data[0]?.[rowKey];
        moveMode = 'BEFORE';
      } else if (oldDraggableIndex < newDraggableIndex) {
        targetId = data[newDraggableIndex]?.[rowKey];
        moveMode = 'AFTER';
      } else {
        targetId = data[newDraggableIndex]?.[rowKey];
        moveMode = 'BEFORE';
      }

      onDragEnd?.({
        moveId,
        targetId,
        moveMode,
        oldIndex: oldDraggableIndex,
        newIndex: newDraggableIndex,
        orgId: appStore.orgId,
      });
    },
  });
}

export default {};
