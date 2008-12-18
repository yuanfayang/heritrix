package org.archive.spring;

import java.util.List;

import org.springframework.validation.AbstractErrors;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public class ContextErrors extends AbstractErrors implements Errors {
    private static final long serialVersionUID = 1L;

    public void addAllErrors(Errors errors) {
        // TODO Auto-generated method stub

    }

    public List getAllErrors() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getErrorCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public FieldError getFieldError() {
        // TODO Auto-generated method stub
        return null;
    }

    public FieldError getFieldError(String field) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getFieldErrorCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getFieldErrorCount(String field) {
        // TODO Auto-generated method stub
        return 0;
    }

    public List getFieldErrors() {
        // TODO Auto-generated method stub
        return null;
    }

    public List getFieldErrors(String field) {
        // TODO Auto-generated method stub
        return null;
    }

    public Class getFieldType(String field) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getFieldValue(String field) {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectError getGlobalError() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getGlobalErrorCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public List getGlobalErrors() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getNestedPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getObjectName() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasErrors() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasFieldErrors() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasFieldErrors(String field) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasGlobalErrors() {
        // TODO Auto-generated method stub
        return false;
    }

    public void popNestedPath() throws IllegalStateException {
        // TODO Auto-generated method stub

    }

    public void pushNestedPath(String subPath) {
        // TODO Auto-generated method stub

    }

    public void reject(String errorCode) {
        // TODO Auto-generated method stub

    }

    public void reject(String errorCode, String defaultMessage) {
        // TODO Auto-generated method stub

    }

    public void reject(String errorCode, Object[] errorArgs,
            String defaultMessage) {
        // TODO Auto-generated method stub

    }

    public void rejectValue(String field, String errorCode) {
        // TODO Auto-generated method stub

    }

    public void rejectValue(String field, String errorCode,
            String defaultMessage) {
        // TODO Auto-generated method stub

    }

    public void rejectValue(String field, String errorCode, Object[] errorArgs,
            String defaultMessage) {
        // TODO Auto-generated method stub

    }

    public void setNestedPath(String nestedPath) {
        // TODO Auto-generated method stub

    }

}
