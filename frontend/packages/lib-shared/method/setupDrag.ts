interface DragPosition {
  startX: number;
  startY: number;
  startLeft: number;
  startTop: number;
}

export function setupDrag(dragElement: HTMLElement | null) {
  if (!dragElement) return;
  let isDragging = false;
  let dragPosition: DragPosition;

  const moveHandler = (clientX: number, clientY: number) => {
    dragElement.style.cursor = 'grabbing';

    // 移动超过5px才认为是拖动
    if (!isDragging && (Math.abs(clientX - dragPosition.startX) > 5 || Math.abs(clientY - dragPosition.startY) > 5)) {
      isDragging = true;
    }

    if (isDragging) {
      // 计算新位置
      const newLeft = dragPosition.startLeft + clientX - dragPosition.startX;
      const newTop = dragPosition.startTop + clientY - dragPosition.startY;

      // 边界检查(可选) - 确保元素不会移出视口
      const maxX = window.innerWidth - dragElement.offsetWidth;
      const maxY = window.innerHeight - dragElement.offsetHeight;

      // 设置新位置(限制在边界内)
      dragElement.style.left = `${Math.max(0, Math.min(newLeft, maxX))}px`;
      dragElement.style.top = `${Math.max(0, Math.min(newTop, maxY))}px`;

      dragElement.style.right = 'auto';
      dragElement.style.bottom = 'auto';
    }
  };

  // 鼠标/触摸移动处理
  const move = (e: MouseEvent | TouchEvent) => {
    e.preventDefault();
    if (e instanceof MouseEvent) {
      moveHandler(e.clientX, e.clientY);
    } else if (e.touches?.[0]) {
      moveHandler(e.touches[0].clientX, e.touches[0].clientY);
    }
  };

  // 停止拖动
  const stopDrag = () => {
    document.removeEventListener('mousemove', move);
    document.removeEventListener('touchmove', move);
    document.removeEventListener('mouseup', stopDrag);
    document.removeEventListener('touchend', stopDrag);
    dragElement.style.cursor = 'pointer';

    // 如果是拖拽操作(不是点击)，则阻止接下来的点击事件
    if (isDragging) {
      const clickHandler = (e: Event) => {
        e.stopImmediatePropagation();
        e.preventDefault();
        dragElement.removeEventListener('click', clickHandler);
      };

      dragElement.addEventListener('click', clickHandler, true);

      // 300ms后移除点击拦截
      setTimeout(() => {
        dragElement.removeEventListener('click', clickHandler, true);
      }, 300);
    }
  };

  // 开始拖动 - 鼠标事件
  const startMouseDrag = (e: MouseEvent) => {
    isDragging = false;
    const style = window.getComputedStyle(dragElement);

    dragPosition = {
      startX: e.clientX,
      startY: e.clientY,
      startLeft: parseInt(style.left, 10) || 0,
      startTop: parseInt(style.top, 10) || 0,
    };
    // 添加移动和松开事件
    document.addEventListener('mousemove', move);
    document.addEventListener('mouseup', stopDrag);
  };

  // 开始拖动 - 触摸事件
  const startTouchDrag = (e: TouchEvent) => {
    isDragging = false;
    if (e.touches[0]) {
      const style = window.getComputedStyle(dragElement);

      dragPosition = {
        startX: e.touches[0].clientX,
        startY: e.touches[0].clientY,
        startLeft: parseInt(style.left, 10) || 0,
        startTop: parseInt(style.top, 10) || 0,
      };

      document.addEventListener('touchmove', move, { passive: false });
      document.addEventListener('touchend', stopDrag);
    }
  };

  // 添加事件监听
  dragElement.addEventListener('mousedown', startMouseDrag);
  dragElement.addEventListener('touchstart', startTouchDrag, { passive: false });
}

export default {};
