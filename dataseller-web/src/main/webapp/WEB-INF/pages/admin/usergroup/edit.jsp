<%@ include file="/common/taglibs.jsp"%>
<%@ page language="java" pageEncoding="UTF-8" contentType="text/html;charset=utf-8" %>
<head>
    <title>
        <c:choose>
            <c:when test="${not empty item.pojo.userGroupId}">
                <fmt:message key="admin.edit_user_group.edit.heading_page"/>
            </c:when>
            <c:otherwise><fmt:message key="admin.edit_user_group.label.add_usergroup"/></c:otherwise>
        </c:choose>
    </title>
    <meta name="menu" content="<fmt:message key="admin.edit_user_group.edit.heading_page" />"/>
</head>

<c:set var="prefix" value="${vms:getPrefixUrl()}" />
<c:url var="backUrl" value="${prefix}/usergroup/list.html"/>
<c:url var="formUrl" value="${prefix}/usergroup/add.html"/>

<c:if test="${pojo.userGroupId != null}">
    <c:url var="formUrl" value="${prefix}/usergroup/edit.html"/>
</c:if>

<div class="page-title">
    <div class="title_left">
        <h3>
            <c:choose>
                <c:when test="${not empty item.pojo.userGroupId}">
                    <fmt:message key="admin.edit_user_group.edit.heading_page" />
                </c:when>
                <c:otherwise><fmt:message key="admin.edit_user_group.label.add_usergroup" /></c:otherwise>
            </c:choose>
        </h3>
    </div>
</div>
<div class="clearfix"></div>

<c:if test="${!empty messageResponse}">
    <div class="row">
        <div class="col-md-12 col-sm-12 col-xs-12">
            <div class="x_panel">
                <div class="x_content">
                    <div class="alert alert-${alertType} no-bottom">
                        <a class="close" data-dismiss="alert" href="#">&times;</a>
                        ${messageResponse}
                    </div>
                </div>
            </div>
        </div>
    </div>
</c:if>

<div class="row">
    <div class="col-md-12 col-sm-12 col-xs-12">
        <div class="x_panel">
            <div class="x_content">
                <form:form commandName="item" cssClass="form-horizontal form-label-left" id="formEdit" action="${formUrl}" method="post" validate="validate">
                    <div class="form-group">
                        <label class="control-label col-md-3 col-sm-3 col-xs-12" for="code">
                            <fmt:message key="usergroup.label.code" />
                        </label>
                        <div class="col-md-6 col-sm-6 col-xs-12">
                            <c:choose>
                                <c:when test="${not empty item.pojo.userGroupId && item.pojo.code eq Constants.USERGROUP_ADMIN}">
                                    <span class="form-control" disabled="true">${item.pojo.code}</span>
                                    <form:hidden path="pojo.code" />
                                </c:when>
                                <c:otherwise>
                                    <input id="code" type="text" name="pojo.code" <c:if test="${disabledEditCode}">readonly="readonly"</c:if> class="required nohtml form-control" value="${item.pojo.code}" />
                                    <form:errors for="code" path="pojo.code" cssClass="error-inline-validate"/>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="control-label col-md-3 col-sm-3 col-xs-12" for="description">
                            <fmt:message key="usergroup.label.description" />
                        </label>
                        <div class="col-md-6 col-sm-6 col-xs-12">
                            <form:input id="description" path="pojo.description" cssClass="required nohtml form-control"></form:input>
                            <form:errors for="description" path="pojo.description" cssClass="error-inline-validate"/>
                        </div>
                    </div>
                    
                    <c:choose>
                        <c:when test="${item.pojo.code eq Constants.USERGROUP_ADMIN}">
                            <div class="form-group">
                                <label class="control-label col-md-3 col-sm-3 col-xs-12" for="description">
                                    <fmt:message key="usergroup.permission" />
                                </label>
                                <div class="col-md-6 col-sm-6 col-xs-12">
                                    <div class="">
                                        <ul class="to_do">
                                            <c:forEach items="${permissionDTOList}" var="permission">
                                                <li>
                                                    <p>
                                                        <c:choose>
                                                            <c:when test="${permission.checked}">
                                                                <input name="checkList" type="checkbox" disabled="true" class="flat" checked="true" value="${permission.permissionId}"> ${permission.description}
                                                            </c:when>
                                                            <c:otherwise>
                                                                <input name="checkList" type="checkbox" disabled="true" class="flat" value="${permission.permissionId}"> ${permission.description}
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </p>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </div>
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="form-group">
                                <label class="control-label col-md-3 col-sm-3 col-xs-12" for="description">
                                    <fmt:message key="usergroup.permission" />
                                </label>
                                <div class="col-md-6 col-sm-6 col-xs-12">
                                    <div class="">
                                        <ul class="to_do">
                                            <c:forEach items="${permissionDTOList}" var="permission">
                                                <li>
                                                    <p>
                                                        <c:choose>
                                                            <c:when test="${permission.checked}">
                                                                <input name="checkList" type="checkbox" class="flat" checked="true" value="${permission.permissionId}"> ${permission.description}
                                                            </c:when>
                                                            <c:otherwise>
                                                                <input name="checkList" type="checkbox" class="flat" value="${permission.permissionId}"> ${permission.description}
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </p>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </div>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>

                    <div class="form-group last">
                        <div class="col-md-6 col-sm-6 col-xs-12 col-md-offset-3">
                            <a href="${backUrl}" class="btn btn-success"><i class="fa fa-times" aria-hidden="true"></i> <fmt:message key="label.huy" /></a>&nbsp;
                            <button id="btnSave" class="btn btn-primary"><i class="fa fa-floppy-o" aria-hidden="true"></i>
                                <c:choose>
                                    <c:when test="${not empty item.pojo.userGroupId}"><fmt:message key="label.update" /></c:when>
                                    <c:otherwise><fmt:message key="label.save" /></c:otherwise>
                                </c:choose>
                            </button>
                        </div>
                    </div>
                    <input type="hidden" name="crudaction" value="insert-update" />
                    <form:hidden path="pojo.userGroupId" />
                </form:form>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript">
    $(document).ready(function(){
        var $codeInput = $('#code');
        notAllowedEnterSpaceIn();
        $("#btnSave").click(function(){
            if($('#formEdit').valid()){
                $("#formEdit").submit();
            }
        });

        $("input[type='checkbox'][class='js-switch']").change(function(){
            $(this).closest('tr').find("input[type='hidden']")
        });


        function notAllowedEnterSpaceIn(){
            $codeInput.on('keypress', function(e){
                if ( e.which == 32 ) {
                    return false;
                }
            })
        }
    });

</script>