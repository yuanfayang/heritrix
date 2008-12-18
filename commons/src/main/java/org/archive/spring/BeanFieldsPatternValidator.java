package org.archive.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class BeanFieldsPatternValidator implements Validator {
    public class PropertyPatternRule {
        String propertyName;
        Pattern requiredPattern; 
        String errorMessage;
        public PropertyPatternRule(String name, String pat, String msg) {
            propertyName = name;
            requiredPattern = Pattern.compile(pat);
            errorMessage = msg.replace("@@", pat);
        }
        public void test(BeanWrapperImpl wrapper, Errors errors) {
            Matcher m = requiredPattern.matcher(
                    (CharSequence)wrapper.getPropertyValue(propertyName));
            if(!m.matches()) {
                errors.rejectValue(propertyName, null, errorMessage);
            }
        }

    }

    Class clazz; 
    List<PropertyPatternRule> rules; 
    
    public BeanFieldsPatternValidator(Class clazz, String ... fieldsPatterns) {
        this.clazz = clazz;
        if((fieldsPatterns.length % 3)!=0) {
            throw new IllegalArgumentException(
                    "variable arguments must be multiple of 3");
        }
        rules = new ArrayList<PropertyPatternRule>(); 
        for(int i = 0; i < fieldsPatterns.length; i=i+3) {
            rules.add(new PropertyPatternRule(fieldsPatterns[i],fieldsPatterns[i+1],fieldsPatterns[i+2]));
        }
    }

    @SuppressWarnings("unchecked")
    public boolean supports(Class cls) {
        return this.clazz.isAssignableFrom(cls);
    }

    public void validate(Object target, Errors errors) {
        BeanWrapperImpl w = new BeanWrapperImpl(target);
        for(PropertyPatternRule rule : rules) {
            rule.test(w,errors);
        }
    }

}
