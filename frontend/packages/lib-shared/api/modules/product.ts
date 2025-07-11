import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddProductUrl,
  BatchDeleteProductUrl,
  BatchUpdateProductUrl,
  DeleteProductUrl,
  DragSortProductUrl,
  GetProductFormConfigUrl,
  GetProductListUrl,
  GetProductUrl,
  UpdateProductUrl,
} from '@lib/shared/api/requrls/product';
import type { CommonList, TableDraggedParams, TableQueryParams } from '@lib/shared/models/common';
import type {
  BatchUpdateProductParams,
  ProductListItem,
  SaveProductParams,
  UpdateProductParams,
} from '@lib/shared/models/product';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

export default function useProductApi(CDR: CordysAxios) {
  // 添加产品
  function addProduct(data: SaveProductParams) {
    return CDR.post({ url: AddProductUrl, data });
  }

  // 更新产品
  function updateProduct(data: UpdateProductParams) {
    return CDR.post({ url: UpdateProductUrl, data });
  }

  // 获取产品列表
  function getProductList(data: TableQueryParams) {
    return CDR.post<CommonList<ProductListItem>>({ url: GetProductListUrl, data });
  }

  // 获取产品表单配置
  function getProductFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetProductFormConfigUrl });
  }

  // 获取产品详情
  function getProduct(id: string) {
    return CDR.get<ProductListItem>({ url: `${GetProductUrl}/${id}` });
  }

  // 删除产品
  function deleteProduct(id: string) {
    return CDR.get({ url: `${DeleteProductUrl}/${id}` });
  }

  // 批量删除产品
  function batchDeleteProduct(data: (string | number)[]) {
    return CDR.post({ url: BatchDeleteProductUrl, data });
  }

  // 批量更新产品
  function batchUpdateProduct(data: BatchUpdateProductParams) {
    return CDR.post({ url: BatchUpdateProductUrl, data });
  }
  // 拖拽排序产品
  function dragSortProduct(data: TableDraggedParams) {
    return CDR.post({ url: DragSortProductUrl, data });
  }

  return {
    addProduct,
    updateProduct,
    getProductList,
    getProductFormConfig,
    getProduct,
    deleteProduct,
    batchDeleteProduct,
    batchUpdateProduct,
    dragSortProduct,
  };
}
