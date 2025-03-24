import {
  AddProductUrl,
  BatchDeleteProductUrl,
  BatchUpdateProductUrl,
  DeleteProductUrl,
  GetProductFormConfigUrl,
  GetProductListUrl,
  GetProductUrl,
  UpdateProductUrl,
} from '@lib/shared/api/requrls/product';
import type { CommonList, TableQueryParams } from '@lib/shared/models/common';
import type {
  BatchUpdateProductParams,
  ProductListItem,
  SaveProductParams,
  UpdateProductParams,
} from '@lib/shared/models/product';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import CDR from '@/api/http/index';

// 添加产品
export function addProduct(data: SaveProductParams) {
  return CDR.post({ url: AddProductUrl, data });
}

// 更新产品
export function updateProduct(data: UpdateProductParams) {
  return CDR.post({ url: UpdateProductUrl, data });
}

// 获取产品列表
export function getProductList(data: TableQueryParams) {
  return CDR.post<CommonList<ProductListItem>>({ url: GetProductListUrl, data });
}

// 获取产品表单配置
export function getProductFormConfig() {
  return CDR.get<FormDesignConfigDetailParams>({ url: GetProductFormConfigUrl });
}

// 获取产品详情
export function getProduct(id: string) {
  return CDR.get<ProductListItem>({ url: `${GetProductUrl}/${id}` });
}

// 删除产品
export function deleteProduct(id: string) {
  return CDR.get({ url: `${DeleteProductUrl}/${id}` });
}

// 批量删除产品
export function batchDeleteProduct(data: (string | number)[]) {
  return CDR.post({ url: BatchDeleteProductUrl, data });
}

// 批量更新产品
export function batchUpdateProduct(data: BatchUpdateProductParams) {
  return CDR.post({ url: BatchUpdateProductUrl, data });
}
