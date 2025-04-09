import { CascaderOption } from 'naive-ui';

import { useI18n } from '@lib/shared/hooks/useI18n';
import { mapTree } from '@lib/shared/method';

import { pathMap, PathMapItem } from '@/config/pathMap';

const { t } = useI18n();

export default function usePathMap() {
  const getModuleOptions = () => {
    return mapTree<CascaderOption>(pathMap, (e) => {
      return {
        value: e.key,
        label: t(e.locale),
        children: e.children,
      };
    });
  };

  // 通过 key 查找对应的 locale 路径
  const findLocalePath = (targetKey: string) => {
    function getLocalePathArr(trees: PathMapItem[], path: string[] = []): string[] {
      for (let i = 0; i < trees.length; i++) {
        const node = trees[i];
        const newPathArr = [...path, node.locale];

        if (node.key === targetKey) {
          return newPathArr;
        }

        if (targetKey.startsWith(node.key) && node.children) {
          const result = getLocalePathArr(node.children, newPathArr);
          if (result) {
            return result;
          }
        }
      }

      return [targetKey];
    }
    return getLocalePathArr(pathMap);
  };

  return {
    getModuleOptions,
    findLocalePath,
  };
}
