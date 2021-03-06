package com.benluck.vms.mobifonedataseller.webapp.controller;

import com.benluck.vms.mobifonedataseller.common.Constants;
import com.benluck.vms.mobifonedataseller.common.utils.DateUtil;
import com.benluck.vms.mobifonedataseller.core.business.*;
import com.benluck.vms.mobifonedataseller.core.dto.OrderDTO;
import com.benluck.vms.mobifonedataseller.core.dto.OrderDataCodeDTO;
import com.benluck.vms.mobifonedataseller.core.dto.PackageDataDTO;
import com.benluck.vms.mobifonedataseller.core.dto.UserDTO;
import com.benluck.vms.mobifonedataseller.editor.CustomCurrencyFormatEditor;
import com.benluck.vms.mobifonedataseller.editor.CustomDateEditor;
import com.benluck.vms.mobifonedataseller.security.util.SHA256Util;
import com.benluck.vms.mobifonedataseller.security.util.SecurityUtils;
import com.benluck.vms.mobifonedataseller.util.ExcelExtensionUtil;
import com.benluck.vms.mobifonedataseller.util.RedisUtil;
import com.benluck.vms.mobifonedataseller.util.RequestUtil;
import com.benluck.vms.mobifonedataseller.util.WebCommonUtil;
import com.benluck.vms.mobifonedataseller.utils.MobiFoneSecurityBase64Util;
import com.benluck.vms.mobifonedataseller.webapp.command.OrderCommand;
import com.benluck.vms.mobifonedataseller.webapp.dto.CellDataType;
import com.benluck.vms.mobifonedataseller.webapp.dto.CellValue;
import com.benluck.vms.mobifonedataseller.webapp.exception.ForBiddenException;
import com.benluck.vms.mobifonedataseller.webapp.task.TaskTakeCardCode;
import com.benluck.vms.mobifonedataseller.webapp.validator.OrderValidator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.lang.Boolean;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created by thaihoang on 10/31/2016.
 */

@Controller
public class OrderController extends ApplicationObjectSupport{

    private Logger logger = Logger.getLogger(OrderController.class);

    private static final Integer TOTAL_COLUMN_EXPORT = 9;

    @Autowired
    private OrderManagementLocalBean orderService;
    @Autowired
    private PackageDataManagementLocalBean packageDataService;
    @Autowired
    private KHDNManagementLocalBean KHDNService;
    @Autowired
    private OrderDataCodeManagementLocalBean orderDataCodeService;
    @Autowired
    private CodeHistoryManagementLocalBean codeHistoryService;
    @Autowired
    private OrderValidator validator;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor("dd/MM/yyyy"));
        binder.registerCustomEditor(Double.class, new CustomCurrencyFormatEditor());
        binder.registerCustomEditor(Integer.class, new CustomCurrencyFormatEditor());
    }

    @RequestMapping(value = {"/admin/order/list.html", "/user/order/list.html", "/khdn/order/list.html"} )
    public ModelAndView list(@ModelAttribute(Constants.FORM_MODEL_KEY)OrderCommand command,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             RedirectAttributes redirectAttributes){

        if(!SecurityUtils.userHasAuthority(Constants.ORDER_MANAGER)){
            logger.warn("User: " + SecurityUtils.getPrincipal().getDisplayName() + ", userId: " + SecurityUtils.getLoginUserId() + " is trying to access non-authorized page: " + "/order/list.html page. ACCESS DENIED FOR BIDDEN!");
            throw new ForBiddenException();
        }

        ModelAndView mav = new ModelAndView("/admin/order/list");
        String action = command.getCrudaction();

        if (StringUtils.isNotBlank(action)){
            if(action.equals("delete")){
                if(command.getPojo().getOrderId() != null){
                    try{
                        if(!SecurityUtils.userHasAuthority(Constants.ORDER_STATUS_MANAGER)){
                            redirectAttributes.addFlashAttribute(Constants.ALERT_TYPE, "danger");
                            redirectAttributes.addFlashAttribute(Constants.MESSAGE_RESPONSE_MODEL_KEY, this.getMessageSourceAccessor().getMessage("order.only_vms_user_can_delete_order"));
                        }else{
                            this.orderService.deleteItem(command.getPojo().getOrderId(), SecurityUtils.getLoginUserId());
                            redirectAttributes.addFlashAttribute(Constants.ALERT_TYPE, "success");
                            redirectAttributes.addFlashAttribute(Constants.MESSAGE_RESPONSE_MODEL_KEY, this.getMessageSourceAccessor().getMessage("database.delete_item_successfully", new Object[]{this.getMessageSourceAccessor().getMessage("admin.donhang.label.order")}));
                        }

                        return new ModelAndView(new StringBuilder("redirect:").append(WebCommonUtil.getPrefixUrl()).append("/order/list.html").toString());
                    }catch (Exception e){
                        mav.addObject(Constants.ALERT_TYPE, "info");
                        redirectAttributes.addFlashAttribute(Constants.MESSAGE_RESPONSE_MODEL_KEY, this.getMessageSourceAccessor().getMessage("database.delete_item_exception", new Object[]{this.getMessageSourceAccessor().getMessage("admin.donhang.label.order")}));
                    }
                }
            }else if (action.equals(Constants.ACTION_SEARCH)){
                executeSearch(mav, command, request);
            }else if(action.equals(Constants.ACTION_EXPORT)){
                try{
                    exportOrder2Excel(command, request, response);
                }catch (Exception e){
                    e.printStackTrace();
                    mav.addObject(Constants.ALERT_TYPE, "danger");
                    mav.addObject(Constants.MESSAGE_RESPONSE_MODEL_KEY, this.getMessageSourceAccessor().getMessage("order.export_failed"));
                    logger.error(e.getMessage());
                }
            }
        }

        preferenceData(mav, command);
        return mav;
    }

    private void exportOrder2Excel(OrderCommand command, HttpServletRequest request, HttpServletResponse response) throws Exception{
        SimpleDateFormat df = new SimpleDateFormat("dd-M-yyyy");
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        String exportDate = df.format(currentTimestamp);

        List<OrderDataCodeDTO> dtoList = this.orderDataCodeService.fetchByOrderId(command.getPojo().getOrderId());

        if(dtoList.size() == 0){
            logger.error("Error happened when fetching Data Code by OrderId: " + command.getPojo().getOrderId());
            throw new Exception("Error happened when fetching Data Code by OrderId: " + command.getPojo().getOrderId());
        }

        String outputFileName = "/files/temp/export/don_hang_" + exportDate + ".xlsx";
        FileOutputStream fileOut = new FileOutputStream(request.getSession().getServletContext().getRealPath(outputFileName));
        XSSFWorkbook workbook = new XSSFWorkbook(OPCPackage.open(request.getSession().getServletContext().getRealPath("/files/temp/export/don_hang.xlsx")));
        XSSFCreationHelper createHelper = workbook.getCreationHelper();
        XSSFSheet sheet = workbook.getSheetAt(0);
        int startRow = 5;

        XSSFFont normalFont = workbook.createFont();

        XSSFCellStyle stringCellFormat = workbook.createCellStyle();
        stringCellFormat.setFont(normalFont);

        XSSFCellStyle integerCellFormat = workbook.createCellStyle();
        integerCellFormat.setFont(normalFont);
        integerCellFormat.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle doubleCellFormat = workbook.createCellStyle();
        doubleCellFormat.setFont(normalFont);
        doubleCellFormat.setDataFormat(createHelper.createDataFormat().getFormat("#,###"));
        doubleCellFormat.setAlignment(HorizontalAlignment.CENTER);

        boolean adminExport4MOBIFONE = false;

        if((SecurityUtils.userHasAuthority(Constants.USERGROUP_ADMIN) || SecurityUtils.userHasAuthority(Constants.USERGROUP_VMS_USER)) && command.getExportOptionType().equals(Constants.ADMIN_EXPORT_4_MOBIFONE)){
            adminExport4MOBIFONE = true;
        }

        if(dtoList.size() > 0){
            int indexRow = 0;
            for(OrderDataCodeDTO dto : dtoList){
                XSSFRow row = sheet.createRow(startRow + indexRow);
                CellValue[] resValue = addCellValues(dto, indexRow, adminExport4MOBIFONE);
                ExcelExtensionUtil.addRow(row, resValue, stringCellFormat, integerCellFormat, doubleCellFormat);
                indexRow++;
            }
            workbook.write(fileOut);
            fileOut.close();
            response.sendRedirect(request.getSession().getServletContext().getContextPath() + outputFileName);
        }
    }
    private CellValue[] addCellValues(OrderDataCodeDTO dto, int indexRow, boolean adminExport4MOBIFONE){
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        CellValue[] resValue = new CellValue[TOTAL_COLUMN_EXPORT];
        int columnIndex = 0;
        resValue[columnIndex++] = new CellValue(CellDataType.INT, indexRow + 1);
        resValue[columnIndex++] = new CellValue(CellDataType.STRING, dto.getSerial().toString());

        if(adminExport4MOBIFONE){
            resValue[columnIndex++] = new CellValue(CellDataType.STRING, SHA256Util.hash(MobiFoneSecurityBase64Util.decode(dto.getDataCode().toString())));
        }else{
            resValue[columnIndex++] = new CellValue(CellDataType.STRING, dto.getDataCode().toString());
        }
        resValue[columnIndex++] = new CellValue(CellDataType.DOUBLE, Double.valueOf(dto.getOrder().getPackageData().getValue()));
        resValue[columnIndex++] = new CellValue(CellDataType.STRING, dto.getOrder().getPackageData().getVolume());
        resValue[columnIndex++] = new CellValue(CellDataType.STRING, dto.getOrder().getPackageData().getDuration());
        resValue[columnIndex++] = new CellValue(CellDataType.STRING, dto.getOrder().getPackageData().getName());
        resValue[columnIndex++] = new CellValue(CellDataType.STRING, dto.getOrder().getKhdn().getMst());
        resValue[columnIndex++] = new CellValue(CellDataType.STRING, df.format(dto.getExpiredDate()));
        return resValue;
    }

    private void executeSearch(ModelAndView mav, OrderCommand command, HttpServletRequest request){
        OrderDTO pojo = command.getPojo();

        RequestUtil.initSearchBean(request, command);
        Map<String, Object> properties = new HashMap<String, Object>();

        if(pojo.getKhdn() != null && pojo.getKhdn().getKHDNId() != null){
            properties.put("khdn.KHDNId", pojo.getKhdn().getKHDNId());
        }
        if(pojo.getPackageData() != null && pojo.getPackageData().getPackageDataId() != null){
            properties.put("packageData.packageDataId", pojo.getPackageData().getPackageDataId());
        }

        if(!SecurityUtils.userHasAuthority(Constants.USERGROUP_ADMIN) && !SecurityUtils.userHasAuthority(Constants.USERGROUP_VMS_USER)){
            properties.put("khdn.stb_vas", SecurityUtils.getPrincipal().getIsdn() != null ? SecurityUtils.getPrincipal().getIsdn() : "-1");
        }

        StringBuilder whereClause = new StringBuilder("A.activeStatus = " + Constants.ORDER_ACTIVE_STATUS_ALIVE);

        command.setSortExpression(" createdDate ");
        command.setSortDirection(Constants.SORT_DESC);

        Object[] resultObject = this.orderService.searchByProperties(properties, command.getSortExpression(), command.getSortDirection(), command.getFirstItem(), command.getReportMaxPageItems(), whereClause.toString());
        command.setTotalItems(Integer.valueOf(resultObject[0].toString()));
        command.setListResult((List<OrderDTO>)resultObject[1]);
        command.setMaxPageItems(command.getReportMaxPageItems());
        mav.addObject(Constants.LIST_MODEL_KEY, command);
    }

    private void preferenceData(ModelAndView mav, OrderCommand command){
        if(RedisUtil.pingRedisServer()){
            mav.addObject("packageDataList", packageDataService.findAll());
            mav.addObject("KHDNList", KHDNService.findAll());
            mav.addObject("packageDataIdListHasGeneratedCardCode", this.packageDataService.findPackageDataIdListHasGeneratedCardCode(Calendar.getInstance().get(Calendar.YEAR)));

            mav.addObject("hasImportedUsedCardCode", RedisUtil.getRedisValueByKey(Constants.IMPORTED_CARD_CODE_REDIS_KEY_AND_HASKEY, Constants.IMPORTED_CARD_CODE_REDIS_KEY_AND_HASKEY));

            if(command.getPojo() != null && command.getPojo().getOrderId() != null && command.getPojo().getKhdn() != null && StringUtils.isNotBlank(command.getPojo().getKhdn().getStb_vas())){
                mav.addObject("totalRemainingPaidPackageValue", this.codeHistoryService.calculateTotalPaidPackageValue(command.getPojo().getKhdn().getStb_vas(), command.getPojo().getOrderId()));
            }else{
                mav.addObject("totalRemainingPaidPackageValue", 0D);
            }
        }else{
            mav.addObject(Constants.ALERT_TYPE, "danger");
            mav.addObject(Constants.MESSAGE_RESPONSE_MODEL_KEY, this.getMessageSourceAccessor().getMessage("redis.msg.server_dead"));
        }
    }

    @RequestMapping(value = {"/admin/order/add.html", "/user/order/add.html", "/khdn/order/add.html",
                            "/admin/order/edit.html", "/user/order/edit.html", "/khdn/order/edit.html"})
    public ModelAndView updateOrCreateOrder(@ModelAttribute(Constants.FORM_MODEL_KEY)OrderCommand command,
                                            BindingResult bindingResult,
                                            RedirectAttributes redirectAttributes){

        if(!SecurityUtils.userHasAuthority(Constants.ORDER_MANAGER)){
            logger.warn("User: " + SecurityUtils.getPrincipal().getDisplayName() + ", userId: " + SecurityUtils.getLoginUserId() + " is trying to access non-authorized page: " + "/order/add.html or /order/edit.html page. ACCESS DENIED FOR BIDDEN!");
            throw new ForBiddenException();
        }

        ModelAndView mav = new ModelAndView("/admin/order/edit");

        if (!RedisUtil.pingRedisServer()){
            return new ModelAndView(new StringBuilder("redirect:").append(WebCommonUtil.getPrefixUrl()).append("/order/list.html").toString());
        }

        Boolean hasImportedUsedCardCode = (Boolean)RedisUtil.getRedisValueByKey(Constants.IMPORTED_CARD_CODE_REDIS_KEY_AND_HASKEY, Constants.IMPORTED_CARD_CODE_REDIS_KEY_AND_HASKEY);
        if(hasImportedUsedCardCode == null || !hasImportedUsedCardCode.booleanValue()){
            logger.warn("Please import Used Card Code list before using this feature.");
            return new ModelAndView(new StringBuilder("redirect:").append(WebCommonUtil.getPrefixUrl()).append("/order/list.html").toString());
        }

        String crudaction = command.getCrudaction();

        OrderDTO pojo = command.getPojo();
        try{
            if (StringUtils.isNotBlank(crudaction)){
                if(crudaction.equals("insert-update")){
                    validator.validate(command, bindingResult);
                    convertDate2Timestamp(command);
                    if (!bindingResult.hasErrors()){
                        UserDTO updatedBy = new UserDTO();
                        updatedBy.setUserId(SecurityUtils.getLoginUserId());
                        pojo.setCreatedBy(updatedBy);

                        PackageDataDTO packageDataDTO = this.packageDataService.findById(pojo.getPackageData().getPackageDataId());
                        if(packageDataDTO.getUnitPrice4CardCode().length() > 2){
                            mav.addObject(Constants.ALERT_TYPE, "info");
                            mav.addObject(Constants.MESSAGE_RESPONSE_MODEL_KEY, this.getMessageSourceAccessor().getMessage("order.only_support_unit_price_2_digit"));
                        }else{

                            if (pojo.getOrderId() == null ){
                                pojo.setOrderStatus(Constants.ORDER_STATUS_WAITING);
                                pojo = this.orderService.addItem(pojo);

                                redirectAttributes.addFlashAttribute(Constants.ALERT_TYPE, "success");
                                redirectAttributes.addFlashAttribute("messageResponse", this.getMessageSourceAccessor().getMessage("database.add_item_successfully", new Object[]{this.getMessageSourceAccessor().getMessage("admin.donhang.label.order")}));
                            } else {
                                this.orderService.updateItem(pojo, true);

                                redirectAttributes.addFlashAttribute(Constants.ALERT_TYPE, "success");
                                redirectAttributes.addFlashAttribute("messageResponse", this.getMessageSourceAccessor().getMessage("database.update_item_successfully", new Object[]{this.getMessageSourceAccessor().getMessage("admin.donhang.label.order")}));
                            }

                            return new ModelAndView(new StringBuilder("redirect:").append(WebCommonUtil.getPrefixUrl()).append("/order/list.html").toString());
                        }
                    }
                }else if (crudaction.equals("finish-order")){
                    if (!SecurityUtils.userHasAuthority(Constants.ORDER_STATUS_MANAGER)){
                        logger.warn("User: " + SecurityUtils.getPrincipal().getDisplayName() + ", userId: " + SecurityUtils.getLoginUserId() + " is trying to finish order but no ORDER_STATUS_MANAGER. ACCESS DENIED FOR BIDDEN!");
                        throw new ForBiddenException();
                    }else{
                        validator.validate(command, bindingResult);
                        convertDate2Timestamp(command);
                        if (!bindingResult.hasErrors()){
                            UserDTO updatedBy = new UserDTO();
                            updatedBy.setUserId(SecurityUtils.getLoginUserId());
                            pojo.setCreatedBy(updatedBy);

                            PackageDataDTO packageDataDTO = this.packageDataService.findById(pojo.getPackageData().getPackageDataId());
                            if(packageDataDTO.getUnitPrice4CardCode().length() > 2){
                                mav.addObject(Constants.ALERT_TYPE, "info");
                                mav.addObject(Constants.MESSAGE_RESPONSE_MODEL_KEY, this.getMessageSourceAccessor().getMessage("order.only_support_unit_price_2_digit"));
                            }else{
                                pojo.setOrderStatus(Constants.ORDER_STATUS_FINISH);
                                if (pojo.getOrderId() == null ){
                                    pojo.setCardCodeProcessStatus(Constants.ORDER_CARD_CODE_PROCESSING_STATUS);
                                    pojo = this.orderService.addItem(pojo);

                                    startTaskTakingCardCode(pojo.getOrderId(), packageDataDTO.getUnitPrice4CardCode());

                                    redirectAttributes.addFlashAttribute(Constants.ALERT_TYPE, "success");
                                    redirectAttributes.addFlashAttribute("messageResponse", this.getMessageSourceAccessor().getMessage("database.add_item_successfully", new Object[]{this.getMessageSourceAccessor().getMessage("admin.donhang.label.order")}));
                                } else {
                                    pojo.setCardCodeProcessStatus(Constants.ORDER_CARD_CODE_PROCESSING_STATUS);
                                    this.orderService.updateItem(pojo, true);

                                    startTaskTakingCardCode(pojo.getOrderId(), packageDataDTO.getUnitPrice4CardCode());
                                    redirectAttributes.addFlashAttribute(Constants.ALERT_TYPE, "success");
                                    redirectAttributes.addFlashAttribute("messageResponse", this.getMessageSourceAccessor().getMessage("database.update_item_successfully", new Object[]{this.getMessageSourceAccessor().getMessage("admin.donhang.label.order")}));
                                }

                                return new ModelAndView(new StringBuilder("redirect:").append(WebCommonUtil.getPrefixUrl()).append("/order/list.html").toString());
                            }
                        }
                    }
                }
            }else if(pojo.getOrderId() != null){
                OrderDTO originOrderDTO = this.orderService.findById(command.getPojo().getOrderId());

                if(originOrderDTO.getOrderStatus().equals(Constants.ORDER_STATUS_FINISH)){
                    redirectAttributes.addFlashAttribute(Constants.ALERT_TYPE, "danger");
                    redirectAttributes.addFlashAttribute(Constants.MESSAGE_RESPONSE_MODEL_KEY, this.getMessageSourceAccessor().getMessage("order.not_allow_update_finish_order"));
                    return new ModelAndView(new StringBuilder("redirect:").append(WebCommonUtil.getPrefixUrl()).append("/order/list.html").toString());
                }
                command.setPojo(originOrderDTO);
            }
        }catch (Exception e){
            logger.error("Error when add or update Order. \nDetails: " + e.getMessage());
            mav.addObject(Constants.ALERT_TYPE, "danger");
            mav.addObject("messageResponse", this.getMessageSourceAccessor().getMessage("database.exception.duplicated_id"));
        }

        preferenceData(mav, command);
        return mav;
    }

    private void startTaskTakingCardCode(Long orderId, String unitPriceCode){
        TaskTakeCardCode taskTakeCardCode = new TaskTakeCardCode(SecurityUtils.getLoginUserId(), orderId, unitPriceCode);
        Timer timer = new Timer(true);
        timer.schedule(taskTakeCardCode, 0);
    }

    /**
     * Copy Date value and format to Timestamp.
     * @param command
     */
    private void convertDate2Timestamp(OrderCommand command){
        if(command.getIssuedDate() != null){
            command.getPojo().setIssuedDate(DateUtil.dateToTimestamp(command.getIssuedDate(), Constants.VI_DATE_FORMAT));
        }
        if(command.getShippingDate() != null){
            command.getPojo().setShippingDate(DateUtil.dateToTimestamp(command.getShippingDate(), Constants.VI_DATE_FORMAT));
        }
    }

    @RequestMapping(value = "/ajax/order/getListByKHDNid.html")
    public @ResponseBody Map getListOrderByKHDNid(@RequestParam(value = "khdnId", required = false) Long khdnId){
        Map<String, Object> mapRes = new HashMap<String, Object>();

        try{
            if (khdnId != null){
                mapRes.put("orderList", this.orderService.findListByKHDNIdInWaitingStatus(khdnId));
                mapRes.put("res", true);
            }else{
                mapRes.put("orderList", this.orderService.findAllInWaitingStatus());
                mapRes.put("res", true);
            }
        }catch (Exception e){
            mapRes.put("res", false);
        }

        return mapRes;
    }

    @RequestMapping(value = "/ajax/order/getListByKHDNidHasPayment.html")
    public @ResponseBody Map getListOrderByKHDNidAndPaid(@RequestParam(value = "khdnId", required = false) Long khdnId){
        Map<String, Object> mapRes = new HashMap<String, Object>();

        try{
            if (khdnId != null){
                mapRes.put("orderList", this.orderService.findListByKHDNIdHasPayment(khdnId));
                mapRes.put("res", true);
            }else{
                mapRes.put("orderList", this.orderService.findAllHasCreatedPayment());
                mapRes.put("res", true);
            }
        }catch (Exception e){
            mapRes.put("res", false);
        }

        return mapRes;
    }
}
