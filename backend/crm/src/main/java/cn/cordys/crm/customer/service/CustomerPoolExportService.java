package cn.cordys.crm.customer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerPoolExportService extends CustomerExportService {
}
