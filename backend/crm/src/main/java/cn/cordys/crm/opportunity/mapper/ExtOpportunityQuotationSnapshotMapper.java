package cn.cordys.crm.opportunity.mapper;

import cn.cordys.crm.opportunity.domain.OpportunityQuotationSnapshot;
import org.apache.ibatis.annotations.Param;

public interface ExtOpportunityQuotationSnapshotMapper {

    void update(@Param("snapshot") OpportunityQuotationSnapshot snapshot);
}