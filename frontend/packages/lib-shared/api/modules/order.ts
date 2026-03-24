import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddOrderUrl,
  AddOrderViewUrl,
  DeleteOrderUrl,
  UpdateOrderStageUrl,
  DeleteOrderViewUrl,
  DragOrderViewUrl,
  EnableOrderViewUrl,
  FixedOrderViewUrl,
  GetOrderDetailUrl,
  OrderPageUrl,
  OrderDetailSnapshotUrl,
  OrderFormConfigUrl,
  OrderFormConfigSnapshotUrl,
  OrderInContractPageUrl,
  GetOrderTabUrl,
  GetOrderViewDetailUrl,
  GetOrderViewListUrl,
  UpdateOrderUrl,
  UpdateOrderViewUrl,
  UpdateOrderStatusUrl,
  UpdateOrderStatusRollbackUrl,
  SortOrderStatusUrl,
  AddOrderStatusUrl,
  GetOrderStatusConfigUrl,
  DeleteOrderStatusUrl,
  DownloadOrderUrl,
  OrderStatisticUrl,
} from '@lib/shared/api/requrls/order';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';
import type { CommonList, TableDraggedParams } from '@lib/shared/models/common';
import type { CustomerTabHidden } from '@lib/shared/models/customer';
import type { OrderItem, UpdateOrderParams } from '@lib/shared/models/order';
import type { TableQueryParams } from '@lib/shared/models/common';

import type { ViewItem, ViewParams } from '@lib/shared/models/view';
import {
  StageBaseParams,
  OpportunityStageConfig,
  UpdateOpportunityStageRollbackParams,
  UpdateStageBaseParams,
} from '@lib/shared/models/opportunity';

export default function useOrderApi(CDR: CordysAxios) {
  // 列表
  function getOrderList(data: TableQueryParams) {
    return CDR.post<CommonList<OrderItem>>({ url: OrderPageUrl, data });
  }

  // 合同下的列表
  function getOrderInContractList(data: TableQueryParams) {
    return CDR.post<CommonList<OrderItem>>({ url: OrderInContractPageUrl, data });
  }

  // 订单详情
  function getOrderDetail(id: string) {
    return CDR.get<OrderItem>({ url: `${GetOrderDetailUrl}/${id}` });
  }

  // 详情快照
  function getOrderDetailSnapshot(id: string) {
    return CDR.get<OrderItem>({ url: `${OrderDetailSnapshotUrl}/${id}` });
  }

  // 新增订单
  function addOrder(data: UpdateOrderParams) {
    return CDR.post({ url: AddOrderUrl, data });
  }

  // 更新订单
  function updateOrder(data: UpdateOrderParams) {
    return CDR.post({ url: UpdateOrderUrl, data });
  }

  // 删除订单
  function deleteOrder(id: string) {
    return CDR.get({ url: `${DeleteOrderUrl}/${id}` });
  }

  // 获取表单配置
  function getOrderFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({
      url: OrderFormConfigUrl,
    });
  }

  // 获取表单配置快照
  function getOrderFormSnapshotConfig(id?: string) {
    return CDR.get<FormDesignConfigDetailParams>({
      url: `${OrderFormConfigSnapshotUrl}/${id}`,
    });
  }

  function downloadOrder(id: string) {
    return CDR.get({ url: `${DownloadOrderUrl}/${id}` });
  }

  // 获取订单tab显隐配置
  function getOrderTab() {
    return CDR.get<CustomerTabHidden>({ url: GetOrderTabUrl });
  }

  // 视图管理
  function addOrderView(data: ViewParams) {
    return CDR.post({ url: AddOrderViewUrl, data });
  }

  function updateOrderView(data: ViewParams) {
    return CDR.post({ url: UpdateOrderViewUrl, data });
  }

  function getOrderViewList() {
    return CDR.get<ViewItem[]>({ url: GetOrderViewListUrl });
  }

  function getOrderViewDetail(id: string) {
    return CDR.get({ url: `${GetOrderViewDetailUrl}/${id}` });
  }

  function fixedOrderView(id: string) {
    return CDR.get({ url: `${FixedOrderViewUrl}/${id}` });
  }

  function enableOrderView(id: string) {
    return CDR.get({ url: `${EnableOrderViewUrl}/${id}` });
  }

  function deleteOrderView(id: string) {
    return CDR.get({ url: `${DeleteOrderViewUrl}/${id}` });
  }

  function dragOrderView(data: TableDraggedParams) {
    return CDR.post({ url: DragOrderViewUrl, data });
  }

  // 更新订单状态配置
  function updateOrderStatus(data: UpdateStageBaseParams) {
    return CDR.post({ url: UpdateOrderStatusUrl, data });
  }

  // 订单状态回退配置
  function updateOrderStatusRollback(data: UpdateOpportunityStageRollbackParams) {
    return CDR.post({ url: UpdateOrderStatusRollbackUrl, data });
  }

  // 订单状态排序
  function sortOrderStatus(data: string[]) {
    return CDR.post({ url: SortOrderStatusUrl, data });
  }

  // 添加订单状态
  function addOrderStatus(data: StageBaseParams) {
    return CDR.post({ url: AddOrderStatusUrl, data });
  }

  // 获取订单状态配置
  function getOrderStatusConfig() {
    return CDR.get<OpportunityStageConfig>({ url: GetOrderStatusConfigUrl }, { ignoreCancelToken: true });
  }

  // 删除订单状态
  function deleteOrderStatus(id: string) {
    return CDR.get({ url: `${DeleteOrderStatusUrl}/${id}` });
  }

  // 更新阶段
  function updateOrderStage(data: { id: string; stage: string }) {
    return CDR.post({ url: UpdateOrderStageUrl, data });
  }

  // 订单统计
  function getOrderStatistic(data: TableQueryParams) {
    return CDR.post({ url: OrderStatisticUrl, data }, { ignoreCancelToken: true });
  }

  return {
    getOrderFormConfig,
    getOrderFormSnapshotConfig,
    addOrder,
    getOrderDetail,
    getOrderDetailSnapshot,
    updateOrder,
    deleteOrder,
    getOrderList,
    getOrderInContractList,
    getOrderTab,
    addOrderView,
    updateOrderView,
    getOrderViewList,
    getOrderViewDetail,
    fixedOrderView,
    enableOrderView,
    deleteOrderView,
    dragOrderView,
    updateOrderStatus,
    updateOrderStatusRollback,
    sortOrderStatus,
    addOrderStatus,
    getOrderStatusConfig,
    deleteOrderStatus,
    updateOrderStage,
    downloadOrder,
    getOrderStatistic,
  };
}
