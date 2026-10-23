import type { TableQueryParams } from '@lib/shared/models/common';
export interface ChangeRecordQueryParams extends TableQueryParams {
  sourceId: string;
}
