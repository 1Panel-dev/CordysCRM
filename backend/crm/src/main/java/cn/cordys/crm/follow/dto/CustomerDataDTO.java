package cn.cordys.crm.follow.dto;

import lombok.Data;

import java.util.List;

@Data
public class CustomerDataDTO {

    private boolean all = false;
    private boolean owner = false;
    private boolean self = false;

    private List<String> userIds;

}
