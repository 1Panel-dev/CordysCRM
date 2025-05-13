import { useRouter } from 'vue-router';

import type { FollowDetailItem } from '@lib/shared/models/customer';

import { PlanEnumType, RecordEnumType } from '@/config/follow';

import { CommonRouteEnum } from '@/enums/routeEnum';

export default function useFollowApi(followProps: {
  type: 'followRecord' | 'followPlan';
  formKey: PlanEnumType | RecordEnumType;
  sourceId: string;
  initialSourceName?: string;
  readonly?: boolean;
}) {
  const router = useRouter();

  function transformField(item: FollowDetailItem, optionMap?: Record<string, any>) {
    const methodKey = followProps.type === 'followPlan' ? 'method' : 'followMethod';
    let followMethod;
    if (optionMap) {
      followMethod =
        optionMap[methodKey]?.find((e: any) => e.id === item[methodKey as keyof FollowDetailItem])?.name || '-';
    }
    return {
      ...item,
      [methodKey]: followMethod,
    };
  }

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: followProps.formKey,
        id: followProps.sourceId,
        initialSourceName: followProps.initialSourceName,
      },
    });
  }

  function handleEdit(item: FollowDetailItem) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: followProps.formKey,
        id: item.id,
        needInitDetail: 'Y',
      },
    });
  }

  function goDetail(item: FollowDetailItem) {
    router.push({
      name: CommonRouteEnum.FOLLOW_DETAIL,
      query: {
        formKey: followProps.formKey,
        id: item.id,
        needInitDetail: 'Y',
        readonly: String(followProps.readonly),
      },
    });
  }

  return {
    transformField,
    goCreate,
    handleEdit,
    goDetail,
  };
}
