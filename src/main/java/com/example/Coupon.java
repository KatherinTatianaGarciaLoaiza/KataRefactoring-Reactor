package com.example;

import static com.example.HelperKata.validateDateIsMinor;
import static com.example.HelperKata.validateDateRegex;

public class Coupon {

    private String code;
    private String date;

    public Coupon(String code, String date) {
        this.code = code;
        this.date = date;
        validateDate();
        assignBono();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void validateDate(){
        if (!validateDateRegex(this.date)) {
            HelperKata.errorMessage = ExperienceErrorsEnum.FILE_ERROR_DATE_PARSE.toString();
        } else if (validateDateIsMinor(this.date)) {
            HelperKata.errorMessage = ExperienceErrorsEnum.FILE_DATE_IS_MINOR_OR_EQUALS.toString();
        } else {
            HelperKata.dateValidated = this.date;
        }
    }

    private void assignBono(){
        HelperKata.bonoEnviado = this.code;
    }
}
