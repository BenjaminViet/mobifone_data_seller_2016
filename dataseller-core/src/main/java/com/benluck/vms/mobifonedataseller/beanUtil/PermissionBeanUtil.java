package com.benluck.vms.mobifonedataseller.beanUtil;

import com.benluck.vms.mobifonedataseller.core.dto.PermissionDTO;
import com.benluck.vms.mobifonedataseller.domain.PermissionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: vietquocpham
 * Date: 10/29/16
 * Time: 12:00
 * To change this template use File | Settings | File Templates.
 */
public class PermissionBeanUtil {
    public static PermissionDTO entity2DTO(PermissionEntity entity){
        PermissionDTO dto = new PermissionDTO();
        dto.setPermissionId(entity.getPermissionId());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setPermissionGroupNumber(entity.getPermissionGroupNumber());
        dto.setPermissionGroupOrder(entity.getPermissionGroupOrder());
        dto.setOrderNo(entity.getOrderNo());
        return dto;
    }

    public static List<PermissionDTO> entityList2DTOList(List<PermissionEntity> entityList){
        List<PermissionDTO> dtoList = new ArrayList<PermissionDTO>();
        for (PermissionEntity entity : entityList){
            dtoList.add(PermissionBeanUtil.entity2DTO(entity));
        }
        return dtoList;
    }
}
