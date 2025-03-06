package io.cordys.crm.customer.service;

import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.customer.dto.request.PoolCustomerAssignRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerRequest;
import io.cordys.crm.customer.dto.response.PoolCustomerResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerPoolRelationService {

	public List<OptionDTO> getPoolOptions(String currentUser) {
		return null;
	}

	public List<PoolCustomerResponse> list(PoolCustomerRequest request, String currentUser) {
		return null;
	}

	public void delete(String id) {

	}

	public void assign(PoolCustomerAssignRequest request) {

	}

	public void pick(String id, String currentUser) {

	}
}
