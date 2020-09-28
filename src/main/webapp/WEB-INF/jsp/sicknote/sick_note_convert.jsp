<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="asset" uri = "/WEB-INF/asset.tld"%>

<!DOCTYPE html>
<html lang="${language}">
<head>
    <title>
        <spring:message code="sicknote.convert.title"/>
    </title>
    <uv:custom-head/>
    <script defer src="<asset:url value='sick_note_convert.js' />"></script>
</head>
<body>

<spring:url var="URL_PREFIX" value="/web"/>

<uv:menu/>

<div class="content">

    <c:set var="METHOD" value="POST"/>
    <c:set var="ACTION" value="${URL_PREFIX}/sicknote/${sickNote.id}/convert"/>

    <form:form method="${METHOD}" action="${ACTION}" modelAttribute="sickNoteConvertForm" class="form-horizontal">

        <div class="container">

            <div class="row">

                <div class="col-xs-12 col-sm-12 col-md-6">
                    <legend>
                        <spring:message code="sicknote.convert.title"/>
                    </legend>

                    <div class="form-group">
                        <form:hidden path="person" value="${sickNoteConvertForm.person.id}"/>
                        <label class="control-label col-sm-12 col-md-4">
                            <spring:message code='sicknote.data.person'/>:
                        </label>

                        <div class="col-md-7">
                            <c:out value="${sickNoteConvertForm.person.niceName}"/>
                        </div>
                    </div>

                    <div class="form-group is-required">
                        <label class="control-label col-md-4">
                            <spring:message code="application.data.vacationType"/>:
                        </label>

                        <div class="col-md-7">
                            <form:select path="vacationType" size="1" cssClass="form-control"
                                         cssErrorClass="form-control error">
                                <c:forEach items="${vacationTypes}" var="vacationType">
                                    <option value="${vacationType.id}">
                                        <spring:message code="${vacationType.messageKey}"/>
                                    </option>
                                </c:forEach>
                            </form:select>
                            <span class="help-inline"><form:errors path="vacationType" cssClass="error"/></span>
                        </div>
                    </div>

                    <div class="form-group">
                        <form:hidden path="dayLength"/>
                        <form:hidden path="startDate"/>
                        <form:hidden path="endDate"/>

                        <label class="control-label col-md-4">
                            <spring:message code="absence.period"/>:
                        </label>

                        <div class="col-md-7 tw-text-sm">
                            <uv:date date="${sickNoteConvertForm.startDate}"/> - <uv:date
                            date="${sickNoteConvertForm.endDate}"/>, <spring:message
                            code="${sickNoteConvertForm.dayLength}"/>
                        </div>
                    </div>

                    <div class="form-group is-required">
                        <label class="control-label col-md-4">
                            <spring:message code="application.data.reason"/>:
                        </label>

                        <div class="col-md-7">
                            <small>
                                <span id="count-chars"></span> <spring:message code="action.comment.maxChars"/>
                            </small>
                            <form:textarea id="reason" path="reason" cssClass="form-control"
                                           cssErrorClass="form-control error" rows="2"
                                           onkeyup="count(this.value, 'count-chars');"
                                           onkeydown="maxChars(this,200); count(this.value, 'count-chars');"/>
                            <span class="help-inline">
                                <form:errors path="reason" cssClass="error"/>
                            </span>
                        </div>

                    </div>

                </div>

                <div class="col-xs-12 col-sm-12 col-md-6">

                    <legend>
                        <spring:message code="sicknote.title"/>
                    </legend>

                    <div class="box tw-mb-8">
                        <span class="box-icon-container">
                            <span class="box-icon tw-bg-red-600 tw-text-white">
                            <c:choose>
                                <c:when test="${sickNote.sickNoteType == 'SICK_NOTE_CHILD'}">
                                    <uv:icon-child className="tw-w-8 tw-h-8" />
                                </c:when>
                                <c:otherwise>
                                    <uv:icon-medkit className="tw-w-8 tw-h-8" />
                                </c:otherwise>
                            </c:choose>
                            </span>
                        </span>
                        <span class="box-text tw-flex tw-flex-col">
                            <span class="tw-text-sm tw-text-black tw-text-opacity-75">
                                <spring:message code="sicknotes.details.box.person.has" arguments="${sickNote.person.niceName}" />
                            </span>
                            <span class="tw-my-1 tw-text-lg tw-font-medium">
                                <spring:message code="${sickNote.sickNoteType.messageKey}" />
                            </span>
                            <span class="tw-text-sm tw-text-black tw-text-opacity-75">
                                <c:choose>
                                    <c:when test="${sickNote.startDate == sickNote.endDate}">
                                        <c:set var="SICK_NOTE_DATE">
                                            <spring:message code="${sickNote.weekDayOfStartDate}.short"/>,
                                            <uv:date date="${sickNote.startDate}"/>
                                        </c:set>
                                        <c:set var="SICK_NOTE_DAY_LENGTH">
                                            <spring:message code="${sickNote.dayLength}"/>
                                        </c:set>
                                        <spring:message
                                            code="absence.period.singleDay"
                                            arguments="${SICK_NOTE_DATE};${SICK_NOTE_DAY_LENGTH}"
                                            argumentSeparator=";"
                                        />
                                    </c:when>
                                    <c:otherwise>
                                        <c:set var="SICK_NOTE_START_DATE">
                                            <spring:message code="${sickNote.weekDayOfStartDate}.short"/>,
                                            <uv:date date="${sickNote.startDate}"/>
                                        </c:set>
                                        <c:set var="SICK_NOTE_END_DATE">
                                            <spring:message code="${sickNote.weekDayOfEndDate}.short"/>,
                                            <uv:date date="${sickNote.endDate}"/>
                                        </c:set>
                                        <spring:message
                                            code="absence.period.multipleDays"
                                            arguments="${SICK_NOTE_START_DATE};${SICK_NOTE_END_DATE}"
                                            argumentSeparator=";"
                                        />
                                    </c:otherwise>
                                </c:choose>
                            </span>
                        </span>
                    </div>

                    <table class="list-table striped-table bordered-table tw-text-sm">
                        <tbody>
                        <tr>
                            <td>
                                <spring:message code="absence.period.duration"/>
                            </td>
                            <td>
                                = <uv:number number="${sickNote.workDays}"/> <spring:message
                                code="duration.workDays"/>
                            </td>
                        </tr>
                        <tr>
                            <td><spring:message code="sicknote.data.aub.short"/></td>
                            <td>
                                <div class="tw-flex tw-items-center">
                                <c:choose>
                                    <c:when test="${sickNote.aubPresent}">
                                        <uv:icon-check className="tw-w-4 tw-h-4" />
                                        &nbsp;<uv:date date="${sickNote.aubStartDate}"/> - <uv:date date="${sickNote.aubEndDate}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <uv:icon-x className="tw-w-4 tw-h-4" />
                                        &nbsp;<spring:message code="sicknote.data.aub.notPresent"/>
                                    </c:otherwise>
                                </c:choose>
                                </div>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>

            </div>

            <div class="row">

                <div class="col-xs-12">

                    <hr/>

                    <button class="btn btn-success col-xs-12 col-sm-5 col-md-2" type="submit"><spring:message
                        code="action.save"/></button>
                    <a class="btn btn-default col-xs-12 col-sm-5 col-md-2 pull-right"
                       href="${URL_PREFIX}/sicknote/${sickNote.id}"><spring:message code="action.cancel"/></a>


                </div>

            </div>

        </div>

    </form:form>
</div>

</body>
</html>
