import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddProductPriceUrl,
  AddProductUrl,
  BatchDeleteProductUrl,
  BatchUpdateProductUrl,
  DeleteProductPriceUrl,
  DeleteProductUrl,
  DownloadProductTemplateUrl,
  DragSortProductUrl,
  GetProductFormConfigUrl,
  GetProductListUrl,
  GetProductOptionsUrl,
  GetProductPriceFormConfigUrl,
  GetProductPriceListUrl,
  GetProductPriceUrl,
  GetProductUrl,
  ImportProductUrl,
  PreCheckProductImportUrl,
  UpdateProductPriceUrl,
  UpdateProductUrl,
} from '@lib/shared/api/requrls/product';
import type { CommonList, TableDraggedParams, TableQueryParams } from '@lib/shared/models/common';
import { BatchUpdatePoolAccountParams } from '@lib/shared/models/customer';
import type {
  AddPriceParams,
  ProductListItem,
  SaveProductParams,
  UpdatePriceParams,
  UpdateProductParams,
} from '@lib/shared/models/product';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';
import { ValidateInfo } from '@lib/shared/models/system/org';

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
  function batchUpdateProduct(data: BatchUpdatePoolAccountParams) {
    return CDR.post({ url: BatchUpdateProductUrl, data });
  }
  // 拖拽排序产品
  function dragSortProduct(data: TableDraggedParams) {
    return CDR.post({ url: DragSortProductUrl, data });
  }

  function preCheckImportProduct(file: File) {
    return CDR.uploadFile<{ data: ValidateInfo }>({ url: PreCheckProductImportUrl }, { fileList: [file] }, 'file');
  }

  function downloadProductTemplate() {
    return CDR.get(
      {
        url: DownloadProductTemplateUrl,
        responseType: 'blob',
      },
      { isTransformResponse: false, isReturnNativeResponse: true }
    );
  }

  function importProduct(file: File) {
    return CDR.uploadFile({ url: ImportProductUrl }, { fileList: [file] }, 'file');
  }

  // 获取意向产品选项
  function getProductOptions() {
    return CDR.get<{ id: string; name: string }[]>({ url: GetProductOptionsUrl });
  }

  // 更新价格表
  function updateProductPrice(data: UpdatePriceParams) {
    return CDR.post({ url: UpdateProductPriceUrl, data });
  }

  // 获取价格表列表
  function getProductPriceList(data: TableQueryParams) {
    return CDR.post({ url: GetProductPriceListUrl, data });
  }

  // 添加价格表
  function addProductPrice(data: AddPriceParams) {
    return CDR.post({ url: AddProductPriceUrl, data });
  }

  // 获取价格表详情
  function getProductPrice(id: string) {
    return CDR.get<ProductListItem>({ url: `${GetProductPriceUrl}/${id}` });
  }

  // 删除价格表
  function deleteProductPrice(id: string) {
    return CDR.get({ url: `${DeleteProductPriceUrl}/${id}` });
  }

  // 获取价格表单配置
  function getProductPriceFormConfig() {
    return CDR.get<FormDesignConfigDetailParams>({ url: GetProductPriceFormConfigUrl });
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
    preCheckImportProduct,
    downloadProductTemplate,
    importProduct,
    getProductOptions,
    updateProductPrice,
    getProductPriceList,
    addProductPrice,
    getProductPrice,
    deleteProductPrice,
    getProductPriceFormConfig,
  };
}
