package com.benluck.vms.mobifonedataseller.core.business.impl;

import com.benluck.vms.mobifonedataseller.beanUtil.OrderBeanUtil;
import com.benluck.vms.mobifonedataseller.beanUtil.PackageDataBeanUtil;
import com.benluck.vms.mobifonedataseller.common.Constants;
import com.benluck.vms.mobifonedataseller.common.utils.Config;
import com.benluck.vms.mobifonedataseller.core.business.OrderManagementLocalBean;
import com.benluck.vms.mobifonedataseller.core.dto.*;
import com.benluck.vms.mobifonedataseller.domain.*;
import com.benluck.vms.mobifonedataseller.session.OrderDataCodeLocalBean;
import com.benluck.vms.mobifonedataseller.session.OrderHistoryLocalBean;
import com.benluck.vms.mobifonedataseller.session.OrderLocalBean;
import com.benluck.vms.mobifonedataseller.session.PackageDataLocalBean;
import com.benluck.vms.mobifonedataseller.utils.MobiFoneSecurityBase64Util;

import javax.ejb.*;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: vietquocpham
 * Date: 11/2/16
 * Time: 07:12
 * To change this template use File | Settings | File Templates.
 */
@Stateless(name = "OrderManagementSessionEJB")
public class OrderManagementSessionBean implements OrderManagementLocalBean{

    @EJB
    private OrderLocalBean orderService;
    @EJB
    private OrderHistoryLocalBean orderHistoryService;
    @EJB
    private OrderDataCodeLocalBean orderDataCodeService;
    @EJB
    private PackageDataLocalBean packageDataService;

    public OrderManagementSessionBean() {
    }

    @Override
    public Object[] searchByProperties(Map<String, Object> properties, String sortExpression, String sortDirection, Integer offset, Integer limitItems, String whereClause) {
        Object[] resultObject = this.orderService.searchByProperties(properties, sortExpression, sortDirection, offset, limitItems, whereClause);
        List<OrderDTO> orderDTOList = OrderBeanUtil.entityList2DTOList((List<OrderEntity>)resultObject[1]);
        resultObject[1] = orderDTOList;
        return resultObject;
    }

    @Override
    public OrderDTO findById(Long orderId) throws ObjectNotFoundException {
        return OrderBeanUtil.entity2DTO(this.orderService.findById(orderId));
    }

    @Override
    public OrderDTO addItem(OrderDTO pojo) throws ObjectNotFoundException, DuplicateKeyException {

        OrderEntity entity = new OrderEntity();

        KHDNEntity khdnEntity = new KHDNEntity();
        khdnEntity.setKHDNId(pojo.getKhdn().getKHDNId());
        entity.setKhdn(khdnEntity);

        entity.setPackageData(this.packageDataService.findById(pojo.getPackageData().getPackageDataId()));

        UserEntity createdBy = new UserEntity();
        createdBy.setUserId(pojo.getCreatedBy().getUserId());
        entity.setCreatedBy(createdBy);

        entity.setQuantity(pojo.getQuantity());
        entity.setUnitPrice(pojo.getUnitPrice());
        entity.setIssuedDate(pojo.getIssuedDate());
        entity.setShippingDate(pojo.getShippingDate());
        entity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        entity.setOrderStatus(pojo.getOrderStatus());
        entity.setActiveStatus(Constants.ORDER_ACTIVE_STATUS_ALIVE);
        entity.setImportedOrder(pojo.getImportedOrder());
        if(pojo.getOrderStatus().equals(Constants.ORDER_STATUS_FINISH)){
            entity.setCardCodeProcessStatus(Constants.ORDER_CARD_CODE_PROCESSING_STATUS);
        }else{
            entity.setCardCodeProcessStatus(Constants.ORDER_CARD_CODE_NOT_START_STATUS);
        }
        entity = this.orderService.save(entity);

        createdOrderHistory(pojo, Constants.ORDER_HISTORY_OPERATOR_CREATED, entity);
        return OrderBeanUtil.entity2DTO(entity);
    }

    @Override
    public void updateItem(OrderDTO pojo, Boolean createdHistory) throws ObjectNotFoundException, DuplicateKeyException {
        OrderEntity dbItem = this.orderService.findById(pojo.getOrderId());

        if(!dbItem.getKhdn().getKHDNId().equals(pojo.getKhdn().getKHDNId())){
            KHDNEntity khdnEntity = new KHDNEntity();
            khdnEntity.setKHDNId(pojo.getKhdn().getKHDNId());
            dbItem.setKhdn(khdnEntity);
        }

        if(!dbItem.getPackageData().getPackageDataId().equals(pojo.getPackageData().getPackageDataId())){
            dbItem.setPackageData(this.packageDataService.findById(pojo.getPackageData().getPackageDataId()));
        }

        dbItem.setQuantity(pojo.getQuantity());
        dbItem.setUnitPrice(pojo.getUnitPrice());
        dbItem.setIssuedDate(pojo.getIssuedDate());
        dbItem.setShippingDate(pojo.getShippingDate());

        if (pojo.getOrderStatus() != null){
            dbItem.setOrderStatus(pojo.getOrderStatus());
        }else{
            pojo.setOrderStatus(dbItem.getOrderStatus());
        }

        dbItem.setLastModified(new Timestamp(System.currentTimeMillis()));
        dbItem.setCardCodeProcessStatus(pojo.getCardCodeProcessStatus());
        this.orderService.update(dbItem);

        if(createdHistory != null && createdHistory.booleanValue()){
            createdOrderHistory(pojo, Constants.ORDER_HISTORY_OPERATOR_UPDATED, null);
        }

        if(pojo.getCardCodeHashSet2Store() != null && pojo.getCardCodeHashSet2Store().size() > 0 && pojo.getCardCodeProcessStatus().equals(Constants.ORDER_CARD_CODE_COMPLETED_STATUS)){
            saveDataCodes4Order(dbItem, pojo.getCardCodeHashSet2Store());
        }
    }

    private void saveDataCodes4Order(OrderEntity orderEntity, HashSet<String> cardCodeHashSetList2Store) throws DuplicateKeyException{
        PackageDataDTO packageDataDTO = PackageDataBeanUtil.entity2DTO(orderEntity.getPackageData());

        Calendar current = Calendar.getInstance();
        Integer totalDataCode = this.orderDataCodeService.countTotal(current.get(Calendar.YEAR), packageDataDTO.getUnitPrice4CardCode()) + 1;

        Integer expiredDays = Integer.valueOf(Config.getInstance().getProperty("order_data_code_expired_2016"));
        if(current.get(Calendar.YEAR) >= 2017){
            expiredDays = Integer.valueOf(Config.getInstance().getProperty("order_data_code_expired_2017_or_later"));
        }
        Calendar expiredDate = Calendar.getInstance();
        expiredDate.add(Calendar.DAY_OF_YEAR, expiredDays);
        Timestamp expiredDate4CardCode = new Timestamp(expiredDate.getTimeInMillis());

        StringBuilder tmpEncodedCardCode = null;
        StringBuilder serial = null;
        StringBuilder tmpCardCode = null;
        Iterator<String> ito = cardCodeHashSetList2Store.iterator();

        while (ito.hasNext()){
            tmpEncodedCardCode = new StringBuilder(ito.next());
            tmpCardCode = new StringBuilder(MobiFoneSecurityBase64Util.decode(tmpEncodedCardCode.toString()));

            // Take same 5 characters in Card Code.
            serial = new StringBuilder(tmpCardCode.toString().substring(0, 5));

            // Generate full Serial.
            if(totalDataCode >= 0 && totalDataCode < 10){
                serial.append("000000");
            }else if(totalDataCode >= 10 && totalDataCode < 100){
                serial.append("00000");
            }else if(totalDataCode >= 100 && totalDataCode < 1000){
                serial.append("0000");
            }else if(totalDataCode >= 1000 && totalDataCode < 10000){
                serial.append("000");
            }else if(totalDataCode >= 10000 && totalDataCode < 100000){
                serial.append("00");
            }else if(totalDataCode >= 100000 && totalDataCode < 1000000){
                serial.append("0");
            }
            serial.append(totalDataCode.toString());

            OrderDataCodeEntity entity = new OrderDataCodeEntity();
            entity.setOrder(orderEntity);
            entity.setSerial(Long.valueOf(serial.toString()));
            entity.setDataCode(tmpEncodedCardCode.toString());
            entity.setExpiredDate(expiredDate4CardCode);
            this.orderDataCodeService.save(entity);
            totalDataCode++;
        }
    }

    private void createdOrderHistory(OrderDTO pojo, Integer operator, OrderEntity dbItem) throws DuplicateKeyException{
        OrderHistoryEntity entity = new OrderHistoryEntity();

        if(dbItem != null){
            entity.setOrder(dbItem);
        }else{
            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setOrderId(pojo.getOrderId());
            entity.setOrder(orderEntity);
        }

        KHDNEntity khdnEntity = new KHDNEntity();
        khdnEntity.setKHDNId(pojo.getKhdn().getKHDNId());
        entity.setKhdn(khdnEntity);

        PackageDataEntity packageDataEntity = new PackageDataEntity();
        packageDataEntity.setPackageDataId(pojo.getPackageData().getPackageDataId());
        entity.setPackageData(packageDataEntity);

        UserEntity createdBy = new UserEntity();
        createdBy.setUserId(pojo.getCreatedBy().getUserId());
        entity.setCreatedBy(createdBy);

        entity.setOperator(operator);
        entity.setQuantity(pojo.getQuantity());
        entity.setUnitPrice(pojo.getUnitPrice());
        entity.setIssuedDate(pojo.getIssuedDate());
        entity.setShippingDate(pojo.getShippingDate());
        entity.setOrderStatus(pojo.getOrderStatus());
        entity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        this.orderHistoryService.save(entity);
    }

    @Override
    public void deleteItem(Long orderId, Long modifiedByUserId) throws ObjectNotFoundException, DuplicateKeyException, RemoveException {
        OrderEntity dbItem = this.orderService.findById(orderId);
        dbItem.setActiveStatus(Constants.ORDER_ACTIVE_STATUS_DIE);

        OrderDTO pojo = new OrderDTO();
        pojo.setOrderId(dbItem.getOrderId());

        KHDNDTO khdndto = new KHDNDTO();
        khdndto.setKHDNId(dbItem.getKhdn().getKHDNId());
        pojo.setKhdn(khdndto);

        PackageDataDTO packageDataDTO = new PackageDataDTO();
        packageDataDTO.setPackageDataId(dbItem.getPackageData().getPackageDataId());
        pojo.setPackageData(packageDataDTO);

        UserDTO modifiedBy = new UserDTO();
        modifiedBy.setUserId(modifiedByUserId);
        pojo.setCreatedBy(modifiedBy);

        pojo.setQuantity(dbItem.getQuantity());
        pojo.setUnitPrice(dbItem.getUnitPrice());
        pojo.setIssuedDate(dbItem.getIssuedDate());
        pojo.setShippingDate(dbItem.getShippingDate());
        pojo.setOrderStatus(dbItem.getOrderStatus());

        createdOrderHistory(pojo, Constants.ORDER_HISTORY_OPERATOR_DELETED, null);
    }

    @Override
    public OrderDTO createOldOrder(OrderDTO pojo) throws ObjectNotFoundException, DuplicateKeyException {
        OrderEntity entity = new OrderEntity();

        KHDNEntity khdnEntity = new KHDNEntity();
        khdnEntity.setKHDNId(pojo.getKhdn().getKHDNId());
        entity.setKhdn(khdnEntity);

        entity.setPackageData(this.packageDataService.findById(pojo.getPackageData().getPackageDataId()));

        UserEntity createdBy = new UserEntity();
        createdBy.setUserId(pojo.getCreatedBy().getUserId());
        entity.setCreatedBy(createdBy);

        entity.setQuantity(pojo.getQuantity());
        entity.setUnitPrice(pojo.getUnitPrice());
        entity.setIssuedDate(pojo.getIssuedDate());
        entity.setShippingDate(pojo.getShippingDate());
        entity.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        entity.setOrderStatus(pojo.getOrderStatus());
        entity.setActiveStatus(Constants.ORDER_ACTIVE_STATUS_ALIVE);
        entity.setImportedOrder(Constants.IS_IMPORTED_ORDER);
        entity.setCardCodeProcessStatus(Constants.ORDER_CARD_CODE_PROCESSING_STATUS);
        entity = this.orderService.save(entity);

        createdOrderHistory(pojo, Constants.ORDER_HISTORY_OPERATOR_CREATED, entity);
        return OrderBeanUtil.entity2DTO(entity);
    }

    @Override
    public void updateOldOrder(Long orderId, List<UsedCardCodeDTO> usedCardCodeList, Integer orderStatus) throws ObjectNotFoundException, DuplicateKeyException {
        OrderEntity dbItem = this.orderService.findById(orderId);
        dbItem.setOrderStatus(orderStatus);
        if(usedCardCodeList != null && usedCardCodeList.size() > 0){
            dbItem.setCardCodeProcessStatus(Constants.ORDER_CARD_CODE_COMPLETED_STATUS);
        }else{
            dbItem.setCardCodeProcessStatus(Constants.ORDER_CARD_CODE_FAILED_STATUS);
        }
        dbItem = this.orderService.update(dbItem);

        saveDataCodes4Order(dbItem, usedCardCodeList);
    }

    private void saveDataCodes4Order(OrderEntity orderEntity, List<UsedCardCodeDTO> cardCodeDTOList) throws DuplicateKeyException{
        PackageDataDTO packageDataDTO = PackageDataBeanUtil.entity2DTO(orderEntity.getPackageData());

        Calendar current = Calendar.getInstance();
        Integer totalDataCode = this.orderDataCodeService.countTotal(current.get(Calendar.YEAR), packageDataDTO.getUnitPrice4CardCode()) + 1;

        Integer expiredDays = Integer.valueOf(Config.getInstance().getProperty("order_data_code_expired_2016"));
        if(current.get(Calendar.YEAR) >= 2017){
            expiredDays = Integer.valueOf(Config.getInstance().getProperty("order_data_code_expired_2017_or_later"));
        }
        Calendar expiredDate = Calendar.getInstance();
        expiredDate.add(Calendar.DAY_OF_YEAR, expiredDays);
        Timestamp expiredDate4CardCode = new Timestamp(expiredDate.getTimeInMillis());

        StringBuilder serial = null;
        StringBuilder tmpCardCode = null;

        for (UsedCardCodeDTO usedCardCodeDTO : cardCodeDTOList){
            tmpCardCode = new StringBuilder(usedCardCodeDTO.getCardCode());

            // Take same 5 characters in Card Code.
            serial = new StringBuilder(tmpCardCode.toString().substring(0, 5));

            // Generate full Serial.
            if(totalDataCode >= 0 && totalDataCode < 10){
                serial.append("000000");
            }else if(totalDataCode >= 10 && totalDataCode < 100){
                serial.append("00000");
            }else if(totalDataCode >= 100 && totalDataCode < 1000){
                serial.append("0000");
            }else if(totalDataCode >= 1000 && totalDataCode < 10000){
                serial.append("000");
            }else if(totalDataCode >= 10000 && totalDataCode < 100000){
                serial.append("00");
            }else if(totalDataCode >= 100000 && totalDataCode < 1000000){
                serial.append("0");
            }
            serial.append(totalDataCode.toString());

            OrderDataCodeEntity entity = new OrderDataCodeEntity();
            entity.setOrder(orderEntity);
            entity.setSerial(Long.valueOf(serial.toString()));
            entity.setDataCode(MobiFoneSecurityBase64Util.encode(tmpCardCode.toString()));
            entity.setExpiredDate(expiredDate4CardCode);
            this.orderDataCodeService.save(entity);
            totalDataCode++;
        }
    }

    @Override
    public List<OrderDTO> fetchAllOrderList4KHDNByShopCode(String shopCode) {
        return OrderBeanUtil.entityList2DTOList(this.orderService.fetchAllOrderList4KHDNByShopCode(shopCode));
    }

    @Override
    public OrderDTO findByIdAndShopCode(Long orderId, String shopCode) {
        return OrderBeanUtil.entity2DTO(this.orderService.findByIdAndShopCode(orderId, shopCode));
    }

    @Override
    public List<OrderDTO> findListByKHDNIdInWaitingStatus(Long khdnId) {
        return OrderBeanUtil.entityList2DTOList(this.orderService.findListByKHDNIdInWaitingStatus(khdnId));
    }

    @Override
    public List<OrderDTO> findAllInWaitingStatus() {
        return OrderBeanUtil.entityList2DTOList(this.orderService.findAllInWaitingStatus());
    }

    @Override
    public List<OrderDTO> findAll() {
        return OrderBeanUtil.entityList2DTOList(this.orderService.findAll());
    }

    @Override
    public List<OrderDTO> findAllHasCreatedPayment() {
        return OrderBeanUtil.entityList2DTOList(this.orderService.findAllHasCreatedPayment());
    }

    @Override
    public List<OrderDTO> findListByKHDNIdHasPayment(Long khdnId) {
        return OrderBeanUtil.entityList2DTOList(this.orderService.findListByKHDNIdHasPayment(khdnId));
    }
}
