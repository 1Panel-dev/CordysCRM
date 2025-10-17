import useOpenNewPage from '@/hooks/useOpenNewPage';

import { ClueRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

export default function useOpenDetailPage() {
  const { openNewPage } = useOpenNewPage();
  function openNewPageClue(row: any) {
    openNewPage(ClueRouteEnum.CLUE_MANAGEMENT, {
      id: row.clueId,
      transitionType: undefined,
      name: row.clueName,
    });
  }
  function openNewPageCluePool(row: any) {
    openNewPage(ClueRouteEnum.CLUE_MANAGEMENT_POOL, {
      id: row.clueId,
      name: row.clueName,
      poolId: row.poolId,
    });
  }

  function openNewPageOpportunity(row: any) {
    openNewPage(OpportunityRouteEnum.OPPORTUNITY, {
      id: row.opportunityId,
      opportunityName: row.opportunityName,
    });
  }

  function openNewPageCustomer(row: any) {
    openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
      id: row.customerId,
    });
  }

  function openNewPageCustomerSea(row: any) {
    openNewPage(CustomerRouteEnum.CUSTOMER_OPEN_SEA, {
      id: row.customerId,
      poolId: row.poolId,
    });
  }

  function goDetail(item: any) {
    if (item.opportunityId) {
      openNewPageOpportunity(item);
    } else if (item.resourceType === 'CLUE') {
      if (item.poolId) {
        openNewPageCluePool(item);
      } else {
        openNewPageClue(item);
      }
    } else if (item.resourceType === 'CUSTOMER') {
      if (item.poolId) {
        openNewPageCustomerSea(item);
      } else {
        openNewPageCustomer(item);
      }
    }
  }

  return {
    goDetail,
  };
}
