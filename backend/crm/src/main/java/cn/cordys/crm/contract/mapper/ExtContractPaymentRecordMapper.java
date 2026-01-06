package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author song-cc-rock
 */
public interface ExtContractPaymentRecordMapper {

	/**
	 * 合同回款记录列表
	 * @param request 请求参数
	 * @param currentUser 当前用户
	 * @param currentOrg 当前组织
	 * @param deptDataPermission 数据权限
	 * @return 回款记录列表
	 */
	List<ContractPaymentRecordResponse> list(@Param("request")ContractPaymentRecordPageRequest request, @Param("currentUser") String currentUser,
											 @Param("currentOrg") String currentOrg, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);
}
