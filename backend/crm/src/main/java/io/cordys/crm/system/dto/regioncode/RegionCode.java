package io.cordys.crm.system.dto.regioncode;

import lombok.Data;

import java.util.List;

@Data
public class RegionCode {

    private String code;
    private String name;
    private List<RegionCode> children;

}

