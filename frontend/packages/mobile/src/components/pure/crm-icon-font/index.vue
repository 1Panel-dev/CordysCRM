<script setup lang="ts">
  import { computed } from 'vue';

  interface Props {
    name: string;
    color?: string;
    width?: string;
    height?: string;
    content?: number | string;
    dot?: boolean;
  }
  const props = withDefaults(defineProps<Props>(), {
    name: '',
    color: '#000',
    width: '1rem',
    height: '1rem',
    dot: false,
  });
  const iconName = computed(() => `#${props.name}`);
  const isGradient = computed(() => typeof props.color === 'string' && /gradient\(/i.test(props.color));

  const gradientId = `crm-icon-gradient-${props.name.replace(/[^a-zA-Z0-9_-]/g, '')}-${Math.random()
    .toString(36)
    .slice(2, 8)}`;

  function parseGradient(grad: string) {
    try {
      const inner = grad.replace(/^[^(]*\((.*)\)$/, '$1');
      const parts = inner.split(/,(?![^()]*\))/).map((part) => part.trim());
      let angle = 90;

      if (/deg$/.test(parts[0]) || /rad$/.test(parts[0]) || /turn$/.test(parts[0])) {
        const anglePart = parts.shift();

        if (anglePart) {
          angle = parseFloat(anglePart) || 90;
        }
      }

      const stops = parts.map((stop, index) => {
        const match = stop.match(/^(.*?)\s+([0-9.]+%?)$/);

        if (match) {
          return {
            color: match[1].trim(),
            offset: match[2].endsWith('%') ? match[2] : `${match[2]}%`,
          };
        }

        return {
          color: stop.trim(),
          offset: `${Math.round((index / Math.max(1, parts.length - 1)) * 100)}%`,
        };
      });

      const angleRad = (parseFloat(String(angle)) - 90) * (Math.PI / 180);
      const dx = Math.cos(angleRad);
      const dy = Math.sin(angleRad);

      return {
        stops,
        x1: Math.round((0.5 - dx * 0.5) * 1000) / 1000,
        y1: Math.round((0.5 - dy * 0.5) * 1000) / 1000,
        x2: Math.round((0.5 + dx * 0.5) * 1000) / 1000,
        y2: Math.round((0.5 + dy * 0.5) * 1000) / 1000,
      };
    } catch {
      return null;
    }
  }

  const parsedGradient = computed(() => {
    if (!isGradient.value || !props.color) {
      return null;
    }

    return parseGradient(props.color);
  });

  const gradientStops = computed(
    () =>
      parsedGradient.value?.stops ?? [
        { color: '#000', offset: '0%' },
        { color: '#000', offset: '100%' },
      ]
  );
  const gradientX1 = computed(() => parsedGradient.value?.x1 ?? 0);
  const gradientY1 = computed(() => parsedGradient.value?.y1 ?? 0);
  const gradientX2 = computed(() => parsedGradient.value?.x2 ?? 1);
  const gradientY2 = computed(() => parsedGradient.value?.y2 ?? 0);
</script>

<template>
  <van-badge :content="props.content" :dot="props.dot">
    <svg v-if="!isGradient" class="c-icon" aria-hidden="true">
      <use :xlink:href="iconName" :fill="color" />
    </svg>
    <svg v-else class="c-icon" aria-hidden="true">
      <defs>
        <linearGradient :id="gradientId" :x1="gradientX1" :y1="gradientY1" :x2="gradientX2" :y2="gradientY2">
          <stop
            v-for="stop in gradientStops"
            :key="`${stop.color}-${stop.offset}`"
            :offset="stop.offset"
            :stop-color="stop.color"
          />
        </linearGradient>
      </defs>
      <use :xlink:href="iconName" :fill="`url(#${gradientId})`" />
    </svg>
  </van-badge>
</template>

<style scoped lang="less">
  .c-icon {
    @apply relative;

    width: v-bind(width);
    height: v-bind(height);
    color: transparent; // 解决部分图标线条填充色问题
  }
</style>
