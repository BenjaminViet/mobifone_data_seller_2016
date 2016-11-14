package com.benluck.vms.mobifonedataseller.webapp.command;

import com.benluck.vms.mobifonedataseller.common.Constants;
import com.benluck.vms.mobifonedataseller.core.dto.OrderDTO;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: vietquocpham
 * Date: 11/2/16
 * Time: 07:14
 * To change this template use File | Settings | File Templates.
 */
public class OrderCommand extends AbstractCommand<OrderDTO>{
    public OrderCommand(){
        this.pojo = new OrderDTO();
    }

    public Date issuedDate;
    private Date shippingDate;
    private Integer exportOptionType = Constants.ADMIN_EXPORT_4_KHDN;

    public Date getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(Date issuedDate) {
        this.issuedDate = issuedDate;
    }

    public Date getShippingDate() {
        return shippingDate;
    }

    public void setShippingDate(Date shippingDate) {
        this.shippingDate = shippingDate;
    }

    public Integer getExportOptionType() {
        return exportOptionType;
    }

    public void setExportOptionType(Integer exportOptionType) {
        this.exportOptionType = exportOptionType;
    }
}
