package com.benluck.vms.mobifonedataseller.webapp.RestModel;

import com.benluck.vms.mobifonedataseller.core.dto.OrderDTO;
import com.benluck.vms.mobifonedataseller.core.dto.OrderDataCodeDTO;
import com.benluck.vms.mobifonedataseller.utils.MobiFoneSecurityBase64Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: vietquocpham
 * Date: 12/4/16
 * Time: 19:22
 * To change this template use File | Settings | File Templates.
 */
public class OrderRestModelBeanUtil {
    public static OrderRestModel dto2RestModel(OrderDTO orderDTO, List<OrderDataCodeDTO> orderDataCodeDTOList){
        OrderRestModel model = new OrderRestModel();
        model.setPackageDataName(orderDTO.getPackageData().getName());
        model.setKhdName(orderDTO.getKhdn().getName());
        model.setQuantity(orderDTO.getQuantity());
        model.setUnitPrice(orderDTO.getUnitPrice());
        model.setIssuedDate(orderDTO.getIssuedDate());
        model.setShippingDate(orderDTO.getShippingDate());
        List<String> cardCodeList = new ArrayList<String>();
        for (OrderDataCodeDTO orderDataCodeDTO : orderDataCodeDTOList){
            cardCodeList.add(MobiFoneSecurityBase64Util.decode(orderDataCodeDTO.getDataCode()));
        }
        model.setCardCodeList(cardCodeList);
        return model;
    }
}