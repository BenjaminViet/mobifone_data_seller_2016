package com.benluck.vms.mobifonedataseller.session.impl;

import com.benluck.vms.mobifonedataseller.domain.OrderEntity;
import com.benluck.vms.mobifonedataseller.session.OrderLocalBean;

import javax.ejb.Stateless;

/**
 * Created with IntelliJ IDEA.
 * User: vietquocpham
 * Date: 11/2/16
 * Time: 07:13
 * To change this template use File | Settings | File Templates.
 */
@Stateless(name = "OrderSessionEJB")
public class OrderSessionBean extends AbstractSessionBean<OrderEntity, Long> implements OrderLocalBean{
    public OrderSessionBean() {
    }
}